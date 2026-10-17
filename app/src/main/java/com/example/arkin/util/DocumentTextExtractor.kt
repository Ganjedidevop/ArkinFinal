package com.example.arkin.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class ExtractedDocument(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val text: String,
    val pageCount: Int,
    val isOcrUsed: Boolean
)

sealed class ExtractionResult {
    data class Success(val document: ExtractedDocument) : ExtractionResult()
    data class Failure(val errorMessage: String) : ExtractionResult()
}

object DocumentTextExtractor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Format byte count into human-readable size string (e.g. 1.2 MB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val group = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, group.toDouble())
        return String.format(java.util.Locale.US, "%.1f %s", value, units[group])
    }

    /**
     * Inspect file metadata from a content:// or file:// URI.
     */
    fun getFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "Document"
        var size = 0L
        try {
            if (uri.scheme == "file" || (uri.scheme == null && uri.path?.startsWith("/") == true)) {
                val file = java.io.File(uri.path ?: "")
                if (file.exists()) {
                    return Pair(file.name, file.length())
                }
            }
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
            name = uri.lastPathSegment ?: "Document"
        }
        return Pair(name, size)
    }

    /**
     * Extract full text from any PDF, Image, or Text file.
     */
    suspend fun extractText(
        context: Context,
        uri: Uri,
        onProgress: (step: String, progress: Float) -> Unit = { _, _ -> }
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val (fileName, fileSize) = getFileMetadata(context, uri)
        val mimeType = context.contentResolver.getType(uri)?.lowercase()
            ?: when {
                fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                else -> "application/octet-stream"
            }

        try {
            onProgress("Reading document...", 0.1f)

            when {
                mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    extractPdfText(context, uri, fileName, fileSize, onProgress)
                }
                mimeType.startsWith("image/") || isImageFile(fileName) -> {
                    extractImageText(context, uri, fileName, fileSize, mimeType, onProgress)
                }
                mimeType.startsWith("text/") || fileName.endsWith(".txt", ignoreCase = true) || fileName.endsWith(".csv", ignoreCase = true) -> {
                    extractPlainText(context, uri, fileName, fileSize, mimeType, onProgress)
                }
                else -> {
                    // Try PDF first, if fails try plain text
                    try {
                        extractPdfText(context, uri, fileName, fileSize, onProgress)
                    } catch (_: Exception) {
                        extractPlainText(context, uri, fileName, fileSize, mimeType, onProgress)
                    }
                }
            }
        } catch (e: Exception) {
            ExtractionResult.Failure("Failed to extract document contents: ${e.localizedMessage ?: e.message}")
        }
    }

    private suspend fun extractPdfText(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        onProgress: (step: String, progress: Float) -> Unit
    ): ExtractionResult {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = openSafeFileDescriptor(context, uri)
                ?: return ExtractionResult.Failure("Unable to open PDF file descriptor.")
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            if (pageCount == 0) {
                return ExtractionResult.Failure("PDF document is empty (0 pages).")
            }

            val extractedPages = StringBuilder()
            val maxPagesToProcess = minOf(pageCount, 10) // Process up to 10 pages for timetables/assignments

            for (pageIndex in 0 until maxPagesToProcess) {
                val progressFraction = 0.2f + (0.7f * (pageIndex.toFloat() / maxPagesToProcess))
                onProgress("Extracting text from page ${pageIndex + 1} of $maxPagesToProcess...", progressFraction)

                val page = renderer.openPage(pageIndex)
                // Render at 2x scale (144 dpi) for high accuracy OCR
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                try {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val visionText = recognizer.process(inputImage).await()
                    val pageText = visionText.text.trim()

                    if (pageText.isNotBlank()) {
                        if (maxPagesToProcess > 1) {
                            extractedPages.appendLine("--- Page ${pageIndex + 1} ---")
                        }
                        extractedPages.appendLine(pageText)
                        extractedPages.appendLine()
                    }
                } finally {
                    bitmap.recycle()
                }
            }

            val finalText = extractedPages.toString().trim()
            if (finalText.isBlank()) {
                return ExtractionResult.Failure(
                    "No readable text or characters could be detected in '$fileName'. " +
                            "Please ensure the document contains clear text or upload a clearer scan."
                )
            }

            onProgress("Text extraction complete", 1.0f)
            return ExtractionResult.Success(
                ExtractedDocument(
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = "application/pdf",
                    text = finalText,
                    pageCount = pageCount,
                    isOcrUsed = true
                )
            )
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private suspend fun extractImageText(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        onProgress: (step: String, progress: Float) -> Unit
    ): ExtractionResult {
        onProgress("Analyzing photo with OCR...", 0.4f)
        val bitmap = openSafeInputStream(context, uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return ExtractionResult.Failure("Unable to decode photo image.")

        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()
            val text = visionText.text.trim()

            if (text.isBlank()) {
                ExtractionResult.Failure("No readable text found in this photo. Please upload a clearer photo with legible writing.")
            } else {
                onProgress("Text recognition complete", 1.0f)
                ExtractionResult.Success(
                    ExtractedDocument(
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        text = text,
                        pageCount = 1,
                        isOcrUsed = true
                    )
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun extractPlainText(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        onProgress: (step: String, progress: Float) -> Unit
    ): ExtractionResult {
        onProgress("Reading text document...", 0.5f)
        val text = openSafeInputStream(context, uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: return ExtractionResult.Failure("Unable to read text stream.")

        if (text.isBlank()) {
            return ExtractionResult.Failure("Text document is empty.")
        }

        onProgress("Document ready", 1.0f)
        return ExtractionResult.Success(
            ExtractedDocument(
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                text = text.trim(),
                pageCount = 1,
                isOcrUsed = false
            )
        )
    }

    private fun openSafeInputStream(context: Context, uri: Uri): java.io.InputStream? {
        return if (uri.scheme == "file" || (uri.scheme == null && uri.path?.startsWith("/") == true)) {
            val f = java.io.File(uri.path ?: "")
            if (f.exists()) java.io.FileInputStream(f) else null
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    private fun openSafeFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return if (uri.scheme == "file" || (uri.scheme == null && uri.path?.startsWith("/") == true)) {
            val f = java.io.File(uri.path ?: "")
            if (f.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY) else null
        } else {
            context.contentResolver.openFileDescriptor(uri, "r")
        }
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".bmp")
    }
}

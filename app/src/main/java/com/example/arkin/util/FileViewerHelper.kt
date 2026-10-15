package com.example.arkin.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object FileViewerHelper {

    fun openDocument(context: Context, uriString: String?, fileName: String? = null) {
        if (uriString.isNullOrBlank()) {
            Toast.makeText(context, "No attachment available to open", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = Uri.parse(uriString)
            var mimeType = context.contentResolver.getType(uri)

            if (mimeType == null) {
                val ext = (fileName ?: uri.lastPathSegment.orEmpty()).substringAfterLast('.', "").lowercase()
                mimeType = when (ext) {
                    "pdf" -> "application/pdf"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "txt" -> "text/plain"
                    "doc", "docx" -> "application/msword"
                    else -> "*/*"
                }
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Cannot access file: permission expired or file moved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }
}

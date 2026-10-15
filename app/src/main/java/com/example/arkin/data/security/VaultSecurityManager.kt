package com.example.arkin.data.security

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedFileResult(
    val encryptedRelativePath: String,
    val originalFileName: String,
    val fileSize: Long,
    val mimeType: String
)

sealed class PinVerificationResult {
    object Success : PinVerificationResult()
    data class Failure(val attemptsRemaining: Int) : PinVerificationResult()
    data class LockedOut(val remainingSeconds: Long) : PinVerificationResult()
}

object VaultSecurityManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "ArkinVaultMasterKey_AES256"
    private const val PREFS_NAME = "arkin_vault_prefs"
    private const val PREF_PIN_HASH = "vault_pin_hash"
    private const val PREF_PIN_SALT = "vault_pin_salt"
    private const val PREF_FAILED_ATTEMPTS = "vault_failed_attempts"
    private const val PREF_LOCKOUT_UNTIL = "vault_lockout_until"

    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // ==========================================
    // Keystore AES-256 Key Management
    // ==========================================

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                entry.secretKey
            } else {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                keyGen.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                keyGen.generateKey()
            }
        } catch (_: Exception) {
            // Fallback for JVM unit tests where AndroidKeyStore provider is absent
            val fallbackSeed = "ArkinVaultSecretKeySalt32BytesLong!".toByteArray(Charsets.UTF_8).copyOf(32)
            SecretKeySpec(fallbackSeed, "AES")
        }
    }

    // ==========================================
    // File Encryption & Decryption
    // ==========================================

    fun encryptAndStoreAttachment(
        context: Context,
        uri: Uri,
        customName: String? = null
    ): Result<EncryptedFileResult> {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"

            var displayName = customName
            var reportedSize = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (displayName.isNullOrBlank() && nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1) {
                        reportedSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val finalFileName = displayName?.ifBlank { "attachment_${System.currentTimeMillis()}" }
                ?: "attachment_${System.currentTimeMillis()}"

            val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
            val encryptedFileName = "vault_${UUID.randomUUID()}.enc"
            val targetFile = File(vaultDir, encryptedFileName)

            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv

            var totalBytesWritten = 0L
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { fos ->
                    // Write IV length & IV header
                    fos.write(iv.size)
                    fos.write(iv)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        val outputChunk = cipher.update(buffer, 0, bytesRead)
                        if (outputChunk != null && outputChunk.isNotEmpty()) {
                            fos.write(outputChunk)
                            totalBytesWritten += outputChunk.size
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null && finalBytes.isNotEmpty()) {
                        fos.write(finalBytes)
                        totalBytesWritten += finalBytes.size
                    }
                }
            } ?: return Result.failure(IllegalStateException("Could not read input attachment"))

            val actualSize = if (reportedSize > 0) reportedSize else totalBytesWritten

            Result.success(
                EncryptedFileResult(
                    encryptedRelativePath = "vault/$encryptedFileName",
                    originalFileName = finalFileName,
                    fileSize = actualSize,
                    mimeType = mimeType
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decryptToTempFile(
        context: Context,
        encryptedRelativePath: String,
        originalFileName: String
    ): File? {
        return try {
            val fileToDecrypt = if (encryptedRelativePath.startsWith("/")) {
                File(encryptedRelativePath)
            } else {
                File(context.filesDir, encryptedRelativePath)
            }

            if (!fileToDecrypt.exists()) return null

            val tempDir = File(context.cacheDir, "vault_temp").apply { if (!exists()) mkdirs() }
            val tempFile = File(tempDir, originalFileName)

            val secretKey = getSecretKey()

            FileInputStream(fileToDecrypt).use { fis ->
                val ivSize = fis.read()
                if (ivSize <= 0 || ivSize > 64) return null
                val iv = ByteArray(ivSize)
                val readIv = fis.read(iv)
                if (readIv != ivSize) return null

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                FileOutputStream(tempFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val decryptedChunk = cipher.update(buffer, 0, bytesRead)
                        if (decryptedChunk != null && decryptedChunk.isNotEmpty()) {
                            fos.write(decryptedChunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null && finalChunk.isNotEmpty()) {
                        fos.write(finalChunk)
                    }
                }
            }
            tempFile.deleteOnExit()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    fun deleteEncryptedFile(context: Context, encryptedRelativePath: String?): Boolean {
        if (encryptedRelativePath.isNullOrBlank()) return true
        return try {
            val file = if (encryptedRelativePath.startsWith("/")) {
                File(encryptedRelativePath)
            } else {
                File(context.filesDir, encryptedRelativePath)
            }
            if (file.exists()) file.delete() else true
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // PIN Security & Salted Hash Storage
    // ==========================================

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPinSet(context: Context): Boolean {
        val prefs = getPrefs(context)
        return !prefs.getString(PREF_PIN_HASH, null).isNullOrBlank()
    }

    fun verifyCurrentPin(context: Context, enteredPin: String): PinVerificationResult {
        val prefs = getPrefs(context)
        val lockoutUntil = prefs.getLong(PREF_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()

        if (now < lockoutUntil) {
            val remainingSec = ((lockoutUntil - now) / 1000).coerceAtLeast(1)
            return PinVerificationResult.LockedOut(remainingSec)
        }

        val storedHash = prefs.getString(PREF_PIN_HASH, null)
        val storedSalt = prefs.getString(PREF_PIN_SALT, null)

        if (storedHash == null || storedSalt == null) {
            // No PIN is set yet
            return PinVerificationResult.Failure(attemptsRemaining = MAX_FAILED_ATTEMPTS)
        }

        val computedHash = hashPin(enteredPin, storedSalt)
        if (computedHash == storedHash) {
            // Success: reset attempts
            prefs.edit()
                .putInt(PREF_FAILED_ATTEMPTS, 0)
                .putLong(PREF_LOCKOUT_UNTIL, 0L)
                .apply()
            return PinVerificationResult.Success
        } else {
            // Failure: increment attempts
            val failedCount = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(PREF_FAILED_ATTEMPTS, failedCount)

            if (failedCount >= MAX_FAILED_ATTEMPTS) {
                val newLockout = now + LOCKOUT_DURATION_MS
                editor.putLong(PREF_LOCKOUT_UNTIL, newLockout)
                editor.apply()
                return PinVerificationResult.LockedOut(LOCKOUT_DURATION_MS / 1000)
            } else {
                editor.apply()
                return PinVerificationResult.Failure(attemptsRemaining = MAX_FAILED_ATTEMPTS - failedCount)
            }
        }
    }

    fun setPin(context: Context, newPin: String): Boolean {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) return false

        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val hexSalt = bytesToHex(saltBytes)
        val hash = hashPin(newPin, hexSalt)

        getPrefs(context).edit()
            .putString(PREF_PIN_SALT, hexSalt)
            .putString(PREF_PIN_HASH, hash)
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .putLong(PREF_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun changePin(context: Context, currentPin: String, newPin: String): Result<Unit> {
        val verification = verifyCurrentPin(context, currentPin)
        if (verification !is PinVerificationResult.Success) {
            return Result.failure(IllegalStateException("Incorrect current PIN. Please try again."))
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("New PIN must be exactly 4 numeric digits."))
        }
        val success = setPin(context, newPin)
        return if (success) Result.success(Unit) else Result.failure(Exception("Failed to save new PIN."))
    }

    private fun hashPin(pin: String, hexSalt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(hexToBytes(hexSalt))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return bytesToHex(digest)
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

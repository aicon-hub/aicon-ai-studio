package dev.aicon.studio.model

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** SHA-256 helpers for verifying downloaded model files. */
object Sha256 {

    fun ofStream(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(256 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun ofFile(file: File): String = file.inputStream().buffered().use { ofStream(it) }

    fun matches(file: File, expectedHex: String): Boolean =
        ofFile(file).equals(expectedHex.trim(), ignoreCase = true)
}

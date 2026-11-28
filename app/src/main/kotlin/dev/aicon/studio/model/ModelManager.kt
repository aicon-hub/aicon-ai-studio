package dev.aicon.studio.model

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads catalog models into app-private storage. Downloads start only
 * from a user tap (network policy), stream to a .part file, and become
 * visible to the engine after the SHA-256 check passes. A failed check
 * deletes the file. Vision models download their mmproj adapter the same
 * way, as a second verified file.
 */
object ModelManager {

    enum class Status { DOWNLOADING, VERIFYING, DONE, FAILED, CANCELED }

    data class DownloadState(
        val status: Status,
        val bytesDone: Long = 0L,
        val bytesTotal: Long = 0L,
        val error: String? = null,
    ) {
        val progress: Float
            get() = if (bytesTotal > 0) bytesDone.toFloat() / bytesTotal.toFloat() else 0f
    }

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadState>> = _downloads.asStateFlow()

    private val cancelRequested = mutableSetOf<String>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun modelDir(info: ModelInfo): File = File(appContext.filesDir, "models/${info.id}")

    fun installedFile(info: ModelInfo): File? {
        val f = File(modelDir(info), info.fileName)
        return if (f.isFile && f.length() == info.sizeBytes) f else null
    }

    fun mmprojFile(info: ModelInfo): File? {
        val m = info.mmproj ?: return null
        val f = File(modelDir(info), m.fileName)
        return if (f.isFile && f.length() == m.sizeBytes) f else null
    }

    fun isInstalled(info: ModelInfo): Boolean =
        installedFile(info) != null && (info.mmproj == null || mmprojFile(info) != null)

    fun download(info: ModelInfo) {
        if (!info.downloadable) return
        if (_downloads.value[info.id]?.status == Status.DOWNLOADING) return
        cancelRequested.remove(info.id)
        scope.launch { runDownload(info) }
    }

    fun cancel(id: String) {
        cancelRequested.add(id)
    }

    fun delete(info: ModelInfo) {
        cancel(info.id)
        modelDir(info).deleteRecursively()
        _downloads.value = _downloads.value - info.id
    }

    private fun setState(id: String, state: DownloadState) {
        _downloads.value = _downloads.value + (id to state)
    }

    private class Canceled : Exception()

    /** Streams one URL to [target] via a .part file and verifies its SHA-256. */
    private fun fetchOne(
        id: String,
        url: String,
        sha256: String,
        target: File,
        baseDone: Long,
        grandTotal: Long,
    ) {
        val part = File(target.parentFile, target.name + ".part")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            connection.inputStream.use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var done = 0L
                    var lastReported = -1L
                    while (true) {
                        if (id in cancelRequested) throw Canceled()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (done - lastReported >= 4L * 1024 * 1024) {
                            lastReported = done
                            setState(id, DownloadState(Status.DOWNLOADING, baseDone + done, grandTotal))
                        }
                    }
                    setState(id, DownloadState(Status.DOWNLOADING, baseDone + done, grandTotal))
                }
            }
            setState(id, DownloadState(Status.VERIFYING, baseDone + part.length(), grandTotal))
            if (!Sha256.matches(part, sha256)) {
                part.delete()
                setState(id, DownloadState(Status.FAILED, 0, grandTotal,
                    "Integrity check failed. The file was deleted."))
                throw Canceled()
            }
            if (target.exists()) target.delete()
            if (!part.renameTo(target)) {
                part.delete()
                setState(id, DownloadState(Status.FAILED, 0, grandTotal,
                    "Could not finalize the file."))
                throw Canceled()
            }
        } finally {
            part.delete()
            connection?.disconnect()
        }
    }

    private fun runDownload(info: ModelInfo) {
        val dir = modelDir(info)
        dir.mkdirs()
        val grandTotal = info.sizeBytes + (info.mmproj?.sizeBytes ?: 0L)
        try {
            fetchOne(
                id = info.id,
                url = info.downloadUrl!!,
                sha256 = info.sha256!!,
                target = File(dir, info.fileName),
                baseDone = 0L,
                grandTotal = grandTotal,
            )
            info.mmproj?.let { m ->
                fetchOne(
                    id = info.id,
                    url = m.downloadUrl,
                    sha256 = m.sha256,
                    target = File(dir, m.fileName),
                    baseDone = info.sizeBytes,
                    grandTotal = grandTotal,
                )
            }
            setState(info.id, DownloadState(Status.DONE, grandTotal, grandTotal))
        } catch (e: Canceled) {
            if (info.id in cancelRequested) {
                setState(info.id, DownloadState(Status.CANCELED))
            }
            // A failed verify/finalize already set FAILED; leave it.
        } catch (e: Exception) {
            setState(info.id, DownloadState(
                if (info.id in cancelRequested) Status.CANCELED else Status.FAILED,
                0, grandTotal,
                if (info.id in cancelRequested) null else (e.message ?: "Download failed"),
            ))
        } finally {
            cancelRequested.remove(info.id)
        }
    }
}

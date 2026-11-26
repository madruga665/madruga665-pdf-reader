package com.madruga665.pdfreader.core.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class AndroidSafBackupFileGateway(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupFileGateway {

    companion object {
        const val MAX_BACKUP_BYTES: Int = 5 * 1024 * 1024
        private const val BUFFER_SIZE: Int = 8192
    }

    override suspend fun writeText(uriString: String, content: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val uri = Uri.parse(uriString)
                val bytes = content.toByteArray(Charsets.UTF_8)
                val truncatedStream = try {
                    context.contentResolver.openOutputStream(uri, "wt")
                } catch (_: Exception) {
                    null
                }
                if (truncatedStream != null) {
                    try {
                        truncatedStream.write(bytes)
                        truncatedStream.flush()
                    } finally {
                        truncatedStream.close()
                    }
                    return@withContext Result.success(Unit)
                }

                val fallbackStream = context.contentResolver.openOutputStream(uri)
                    ?: return@withContext Result.failure(IOException("Não foi possível abrir o arquivo para escrita."))
                try {
                    fallbackStream.write(bytes)
                    fallbackStream.flush()
                } finally {
                    fallbackStream.close()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun readText(uriString: String): Result<String> =
        withContext(ioDispatcher) {
            try {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IOException("Não foi possível abrir o arquivo selecionado."))
                try {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(BUFFER_SIZE)
                    var totalBytes = 0
                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break
                        totalBytes += read
                        if (totalBytes > MAX_BACKUP_BYTES) {
                            return@withContext Result.failure(
                                IOException("Arquivo excede o tamanho máximo de backup (5 MB).")
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                    Result.success(output.toString(Charsets.UTF_8.name()))
                } finally {
                    inputStream.close()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

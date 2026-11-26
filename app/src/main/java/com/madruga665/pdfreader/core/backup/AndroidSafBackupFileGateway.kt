package com.madruga665.pdfreader.core.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class AndroidSafBackupFileGateway(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupFileGateway {

    companion object {
        const val MAX_BACKUP_BYTES: Long = BackupFileGateway.MAX_BACKUP_BYTES
        private const val BUFFER_SIZE: Int = 8192
    }

    override suspend fun writeFileToUri(uriString: String, sourceFile: File): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                if (!sourceFile.exists() || sourceFile.length() <= 0L) {
                    return@withContext Result.failure(IOException("Arquivo temporário de backup indisponível."))
                }
                val uri = Uri.parse(uriString)
                val outputStream = try {
                    context.contentResolver.openOutputStream(uri, "wt")
                } catch (_: Exception) {
                    null
                } ?: context.contentResolver.openOutputStream(uri)
                    ?: return@withContext Result.failure(IOException("Não foi possível abrir o arquivo de destino para escrita."))

                outputStream.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                        }
                        out.flush()
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun copyUriToTempFile(
        uriString: String,
        destinationFile: File,
        maxBytes: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Não foi possível abrir o arquivo selecionado."))

            destinationFile.parentFile?.mkdirs()
            inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        totalBytes += read
                        if (totalBytes > maxBytes) {
                            return@withContext Result.failure(
                                IOException("Arquivo excede o tamanho máximo permitido de backup (20 MB).")
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

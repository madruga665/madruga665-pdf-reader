package com.madruga665.pdfreader.core.storage

import android.content.Context
import com.madruga665.pdfreader.core.backup.AndroidSafBackupFileGateway
import com.madruga665.pdfreader.core.backup.BackupRepository
import com.madruga665.pdfreader.core.backup.SqliteBackupRepository
import com.madruga665.pdfreader.core.backup.SqliteBackupValidator
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.documents.AndroidDocumentAvailabilityChecker
import com.madruga665.pdfreader.core.documents.DocumentAvailabilityChecker
import com.madruga665.pdfreader.core.home.FavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.home.SqliteFavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.SqliteRecentsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Process-scoped holder for the local SQLite database, stores, repositories,
 * and one-time asynchronous legacy DataStore migration gate.
 *
 * Keeps the SQLite connection alive across Activity recreation/configuration
 * changes while ensuring UI queries suspend until initial migration completes
 * without blocking the main thread.
 */
class PdfReaderAppStorage private constructor(
    appContext: Context
) {
    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val migrationReady = CompletableDeferred<Unit>()

    val appDatabase: SqliteAppDatabase = SqliteAppDatabase.createAndroid(appContext)
    val documentStore: PdfDocumentStore = SqlitePdfDocumentStore(
        database = appDatabase,
        awaitReady = { migrationReady.await() }
    )
    val recentsRepository: RecentsRepository = SqliteRecentsRepository(documentStore)
    val favoriteFoldersRepository: FavoriteFoldersRepository =
        SqliteFavoriteFoldersRepository(appContext, documentStore)
    val availabilityChecker: DocumentAvailabilityChecker =
        AndroidDocumentAvailabilityChecker(appContext)
    val backupRepository: BackupRepository

    init {
        val appVersion = try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }

        backupRepository = SqliteBackupRepository(
            database = appDatabase,
            documentStore = documentStore,
            validator = SqliteBackupValidator(appDatabase),
            fileGateway = AndroidSafBackupFileGateway(appContext),
            stagingDirectory = appContext.cacheDir,
            appVersion = appVersion
        )

        migrationScope.launch {
            try {
                runCatching {
                    LegacyDataStoreMigrator(
                        legacyReader = DataStoreLegacyReader(appContext),
                        documentStore = documentStore
                    ).migrateIfNeeded()
                }
            } finally {
                migrationReady.complete(Unit)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: PdfReaderAppStorage? = null

        fun getInstance(context: Context): PdfReaderAppStorage {
            return instance ?: synchronized(this) {
                instance ?: PdfReaderAppStorage(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

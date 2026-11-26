package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.home.DataStoreRecentsRepository
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.model.SpeedPreset
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object BackupJsonCodec {

    private const val MIN_SPEED = 10.0f
    private const val MAX_SPEED = 500.0f
    private const val MIN_ZOOM = 0.5f
    private const val MAX_ZOOM = 4.0f
    private const val MIN_OFFSET = 0.0f
    private const val MAX_OFFSET = 1.0f

    private val fileNameDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    fun suggestedBackupFileName(
        epochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val formatted = Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .format(fileNameDateFormatter)
        return "pdf-reader-backup-$formatted.json"
    }

    fun encode(payload: BackupPayload): String {
        val root = JSONObject().apply {
            put("schemaVersion", payload.schemaVersion)
            put("exportedAt", payload.exportedAt)
            put("appVersion", payload.appVersion)
            put("preferences", JSONObject().apply {
                put("globalDefaultSpeedPxPerSec", sanitizeSpeed(payload.globalDefaultSpeedPxPerSec).toDouble())
                put("readingSessions", JSONArray().apply {
                    payload.readingSessions.forEach { session ->
                        put(JSONObject().apply {
                            put("documentUri", session.documentUri)
                            put("fileName", session.fileName)
                            put("currentPageIndex", session.currentPageIndex.coerceAtLeast(0))
                            put("normalizedScrollRatio", sanitizeOffset(session.normalizedScrollRatio).toDouble())
                            put("zoomScale", sanitizeZoom(session.zoomScale).toDouble())
                            put("preferredSpeedPxPerSec", sanitizeSpeed(session.preferredSpeedPxPerSec).toDouble())
                            if (session.customDefaultSpeedPxPerSec != null) {
                                put("customDefaultSpeedPxPerSec", sanitizeSpeed(session.customDefaultSpeedPxPerSec).toDouble())
                            } else {
                                put("customDefaultSpeedPxPerSec", JSONObject.NULL)
                            }
                            put("lastReadTimestamp", session.lastReadTimestamp.coerceAtLeast(1L))
                        })
                    }
                })
            })
            put("recentDocuments", JSONArray().apply {
                payload.recentDocuments.take(DataStoreRecentsRepository.MAX_RECENTS).forEach { doc ->
                    put(JSONObject().apply {
                        put("uri", doc.uri)
                        put("fileName", doc.fileName)
                        put("pageCount", doc.pageCount)
                        put("lastReadPageIndex", doc.lastReadPageIndex)
                        put("progressRatio", doc.progressRatio.toDouble())
                        put("lastAccessedTimestamp", doc.lastAccessedTimestamp)
                        put("isAvailable", doc.isAvailable)
                    })
                }
            })
            put("favoriteFolders", JSONArray().apply {
                payload.favoriteFolders.forEach { folder ->
                    put(JSONObject().apply {
                        put("treeUri", folder.treeUri)
                        put("displayName", folder.displayName)
                        put("pathSummary", folder.pathSummary)
                        put("pinnedTimestamp", folder.pinnedTimestamp)
                        put("cachedFileCount", folder.cachedFileCount)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun decode(rawJson: String): BackupParseResult {
        if (rawJson.isBlank()) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.EMPTY_FILE,
                message = "O arquivo de backup está vazio."
            )
        }

        val root = try {
            JSONObject(rawJson)
        } catch (_: JSONException) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_JSON,
                message = "Arquivo JSON inválido ou corrompido."
            )
        }

        if (!root.has("schemaVersion") || root.isNull("schemaVersion")) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "Campo obrigatório 'schemaVersion' ausente no backup."
            )
        }

        val schemaVersion = try {
            val rawVersion = root.get("schemaVersion")
            if (rawVersion is Number && rawVersion.toDouble() == rawVersion.toInt().toDouble()) {
                rawVersion.toInt()
            } else {
                return BackupParseResult.Failure(
                    reason = BackupErrorReason.INVALID_STRUCTURE,
                    message = "Versão do esquema inválida no arquivo de backup."
                )
            }
        } catch (_: Exception) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "Versão do esquema inválida no arquivo de backup."
            )
        }

        if (schemaVersion != CURRENT_BACKUP_SCHEMA_VERSION) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.UNSUPPORTED_SCHEMA_VERSION,
                message = "Versão de backup não suportada (v$schemaVersion). Esta versão suporta apenas v$CURRENT_BACKUP_SCHEMA_VERSION."
            )
        }

        val prefsObj = root.optJSONObject("preferences")
            ?: return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "Estrutura de backup inválida: seção 'preferences' ausente."
            )

        val sessionsArr = prefsObj.optJSONArray("readingSessions")
            ?: return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "Estrutura de backup inválida: lista 'readingSessions' ausente."
            )

        val recentsArr = root.optJSONArray("recentDocuments")
            ?: return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "Estrutura de backup inválida: lista 'recentDocuments' ausente."
            )

        val foldersArr = root.optJSONArray("favoriteFolders")
            ?: return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "Estrutura de backup inválida: lista 'favoriteFolders' ausente."
            )

        val exportedAt = root.optLong("exportedAt", System.currentTimeMillis()).coerceAtLeast(1L)
        val appVersion = root.optString("appVersion", "1.0.0").ifBlank { "1.0.0" }
        val globalSpeed = sanitizeSpeed(
            prefsObj.optDouble("globalDefaultSpeedPxPerSec", SpeedPreset.MEDIUM.speedPxPerSec.toDouble()).toFloat()
        )

        val sessions = parseSessions(sessionsArr, exportedAt)
        val recents = parseRecents(recentsArr, exportedAt)
        val folders = parseFolders(foldersArr, exportedAt)

        val payload = BackupPayload(
            schemaVersion = schemaVersion,
            exportedAt = exportedAt,
            appVersion = appVersion,
            globalDefaultSpeedPxPerSec = globalSpeed,
            readingSessions = sessions,
            recentDocuments = recents,
            favoriteFolders = folders
        )

        return BackupParseResult.Success(
            payload = payload,
            summary = payload.toSummary()
        )
    }

    private fun parseSessions(array: JSONArray, fallbackTimestamp: Long): List<BackupReadingSession> {
        val byUri = linkedMapOf<String, BackupReadingSession>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val uri = obj.optString("documentUri", "").trim()
            if (uri.isEmpty()) continue

            val customSpeed: Float? = if (obj.has("customDefaultSpeedPxPerSec") && !obj.isNull("customDefaultSpeedPxPerSec")) {
                val raw = obj.optDouble("customDefaultSpeedPxPerSec", Double.NaN).toFloat()
                if (raw.isFinite()) raw.coerceIn(MIN_SPEED, MAX_SPEED) else null
            } else {
                null
            }

            val parsed = BackupReadingSession(
                documentUri = uri,
                fileName = obj.optString("fileName", "").trim(),
                currentPageIndex = obj.optInt("currentPageIndex", 0).coerceAtLeast(0),
                normalizedScrollRatio = sanitizeOffset(obj.optDouble("normalizedScrollRatio", 0.0).toFloat()),
                zoomScale = sanitizeZoom(obj.optDouble("zoomScale", 1.0).toFloat()),
                preferredSpeedPxPerSec = sanitizeSpeed(
                    obj.optDouble("preferredSpeedPxPerSec", SpeedPreset.MEDIUM.speedPxPerSec.toDouble()).toFloat()
                ),
                customDefaultSpeedPxPerSec = customSpeed,
                lastReadTimestamp = obj.optLong("lastReadTimestamp", fallbackTimestamp).let {
                    if (it > 0L) it else fallbackTimestamp
                }
            )

            val existing = byUri[uri]
            if (existing == null) {
                byUri[uri] = parsed
            } else {
                val winner = if (parsed.lastReadTimestamp > existing.lastReadTimestamp) parsed else existing
                val loser = if (winner === parsed) existing else parsed
                byUri[uri] = winner.copy(
                    customDefaultSpeedPxPerSec = winner.customDefaultSpeedPxPerSec ?: loser.customDefaultSpeedPxPerSec,
                    fileName = winner.fileName.ifBlank { loser.fileName }
                )
            }
        }
        return byUri.values.toList()
    }

    private fun parseRecents(array: JSONArray, fallbackTimestamp: Long): List<RecentDocument> {
        val byUri = linkedMapOf<String, RecentDocument>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val uri = obj.optString("uri", "").trim()
            val fileName = obj.optString("fileName", "").trim()
            if (uri.isEmpty() || fileName.isEmpty()) continue

            val pageCount = obj.optInt("pageCount", 1).coerceAtLeast(1)
            val lastReadPageIndex = obj.optInt("lastReadPageIndex", 0).coerceIn(0, pageCount - 1)
            val progressRatio = sanitizeOffset(obj.optDouble("progressRatio", 0.0).toFloat())
            val timestamp = obj.optLong("lastAccessedTimestamp", fallbackTimestamp).let {
                if (it != 0L) it else fallbackTimestamp
            }
            val isAvailable = obj.optBoolean("isAvailable", true)

            val doc = RecentDocument(
                uri = uri,
                fileName = fileName,
                pageCount = pageCount,
                lastReadPageIndex = lastReadPageIndex,
                progressRatio = progressRatio,
                lastAccessedTimestamp = timestamp,
                isAvailable = isAvailable
            )

            val existing = byUri[uri]
            if (existing == null || doc.lastAccessedTimestamp > existing.lastAccessedTimestamp) {
                byUri[uri] = doc
            }
        }
        return byUri.values
            .sortedByDescending { it.lastAccessedTimestamp }
            .take(DataStoreRecentsRepository.MAX_RECENTS)
    }

    private fun parseFolders(array: JSONArray, fallbackTimestamp: Long): List<FavoriteFolder> {
        val byUri = linkedMapOf<String, FavoriteFolder>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val treeUri = obj.optString("treeUri", "").trim()
            val displayName = obj.optString("displayName", "").trim()
            if (treeUri.isEmpty() || displayName.isEmpty()) continue

            val pathSummary = obj.optString("pathSummary", "")
            val pinnedTimestamp = obj.optLong("pinnedTimestamp", fallbackTimestamp).let {
                if (it != 0L) it else fallbackTimestamp
            }
            val cachedFileCount = obj.optInt("cachedFileCount", 0).coerceAtLeast(0)

            val folder = FavoriteFolder(
                treeUri = treeUri,
                displayName = displayName,
                pathSummary = pathSummary,
                pinnedTimestamp = pinnedTimestamp,
                cachedFileCount = cachedFileCount
            )

            val existing = byUri[treeUri]
            if (existing == null || folder.pinnedTimestamp > existing.pinnedTimestamp) {
                byUri[treeUri] = folder
            }
        }
        return byUri.values.sortedByDescending { it.pinnedTimestamp }
    }

    private fun sanitizeSpeed(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_SPEED, MAX_SPEED) else SpeedPreset.MEDIUM.speedPxPerSec

    private fun sanitizeZoom(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_ZOOM, MAX_ZOOM) else 1.0f

    private fun sanitizeOffset(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_OFFSET, MAX_OFFSET) else 0.0f
}

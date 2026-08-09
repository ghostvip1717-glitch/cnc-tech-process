package com.cnctech.process.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogItemPhotoEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.entity.PartEntity
import com.cnctech.process.data.entity.PartPhotoEntity
import com.cnctech.process.data.entity.SetupEntity
import com.cnctech.process.data.entity.TechProcessEntity
import com.cnctech.process.data.photo.PhotoStorage
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class MigrationImporter(
    private val context: Context,
    private val db: AppDatabase,
    private val photoStorage: PhotoStorage,
) {
    suspend fun importFromZip(uri: Uri): String {
        val staging = File(context.cacheDir, "migration_import_${System.currentTimeMillis()}").also {
            it.mkdirs()
        }
        try {
            context.contentResolver.openInputStream(uri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/')
                        if (!entry.isDirectory && isSafeZipPath(name)) {
                            val outFile = File(staging, name)
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Не удалось открыть архив миграции")

            val dataFile = File(staging, "data.json")
            if (!dataFile.exists()) error("В архиве нет data.json")
            val root = JSONObject(dataFile.readText())

            val catalogItems = root.optJSONArray("catalogItems") ?: JSONArray()
            val parts = root.optJSONArray("parts") ?: JSONArray()
            val techProcesses = root.optJSONArray("techProcesses") ?: JSONArray()

            var setupCount = 0
            var operationCount = 0
            var photoCount = 0

            db.withTransaction {
                clearAllData()
                photoStorage.clearAllPhotos()

                for (i in 0 until catalogItems.length()) {
                    val item = catalogItems.getJSONObject(i)
                    val id = item.getLong("id")
                    val type = CatalogType.valueOf(item.getString("type"))
                    val note = if (item.isNull("note")) null else item.optString("note").ifBlank { null }
                    db.catalogDao().insert(
                        CatalogItemEntity(
                            id = id,
                            type = type,
                            name = item.getString("name"),
                            note = note,
                            stockQty = null,
                            minStockThreshold = 3,
                        ),
                    )
                    photoCount += importPhotos(
                        relativePaths = item.optJSONArray("photos"),
                        staging = staging,
                        targetDir = photoStorage.catalogDir(id),
                    ) { path, sortOrder ->
                        db.catalogDao().insertPhoto(
                            CatalogItemPhotoEntity(
                                catalogItemId = id,
                                filePath = path,
                                sortOrder = sortOrder,
                            ),
                        )
                    }
                }

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val id = part.getLong("id")
                    db.partDao().insert(
                        PartEntity(
                            id = id,
                            number = part.getString("number"),
                            title = part.getString("title"),
                            createdAt = part.optLong("createdAt", System.currentTimeMillis()),
                            machiningTimeMinutes = null,
                            programCount = null,
                            note = null,
                        ),
                    )
                    photoCount += importPhotos(
                        relativePaths = part.optJSONArray("photos"),
                        staging = staging,
                        targetDir = photoStorage.partDir(id),
                    ) { path, sortOrder ->
                        db.partDao().insertPhoto(
                            PartPhotoEntity(
                                partId = id,
                                filePath = path,
                                sortOrder = sortOrder,
                            ),
                        )
                    }
                }

                for (i in 0 until techProcesses.length()) {
                    val tp = techProcesses.getJSONObject(i)
                    val tpId = tp.getLong("id")
                    db.techProcessDao().insert(
                        TechProcessEntity(id = tpId, partId = tp.getLong("partId")),
                    )
                    val setups = tp.optJSONArray("setups") ?: JSONArray()
                    for (s in 0 until setups.length()) {
                        val setup = setups.getJSONObject(s)
                        val setupId = setup.getLong("id")
                        db.techProcessDao().insertSetup(
                            SetupEntity(
                                id = setupId,
                                techProcessId = tpId,
                                order = setup.getInt("order"),
                                jawId = setup.getLong("jawId"),
                                note = null,
                            ),
                        )
                        setupCount++
                        val ops = setup.optJSONArray("operations") ?: JSONArray()
                        for (o in 0 until ops.length()) {
                            val op = ops.getJSONObject(o)
                            val comment = if (op.isNull("comment")) {
                                null
                            } else {
                                op.optString("comment").ifBlank { null }
                            }
                            db.techProcessDao().insertOperation(
                                OperationEntity(
                                    id = op.getLong("id"),
                                    setupId = setupId,
                                    order = op.getInt("order"),
                                    opNumber = op.getString("opNumber"),
                                    title = op.getString("title"),
                                    toolId = op.getLong("toolId"),
                                    plateId = op.getLong("plateId"),
                                    comment = comment,
                                ),
                            )
                            operationCount++
                        }
                    }
                }
            }

            return "Импорт: деталей ${parts.length()}, справочник ${catalogItems.length()}, " +
                "установов $setupCount, операций $operationCount, фото $photoCount"
        } finally {
            staging.deleteRecursively()
        }
    }

    private suspend fun clearAllData() {
        db.techProcessDao().clearAllOperationPhotos()
        db.techProcessDao().clearAllSetupPhotos()
        db.techProcessDao().clearAllOperations()
        db.techProcessDao().clearAllSetups()
        db.techProcessDao().clearAllTechProcesses()
        db.partDao().clearAllPhotos()
        db.partDao().clearAll()
        db.catalogDao().clearAllPhotos()
        db.catalogDao().clearAllCompatibility()
        db.catalogDao().clearAll()
    }

    private suspend fun importPhotos(
        relativePaths: JSONArray?,
        staging: File,
        targetDir: File,
        insert: suspend (path: String, sortOrder: Int) -> Unit,
    ): Int {
        if (relativePaths == null) return 0
        var count = 0
        for (i in 0 until relativePaths.length()) {
            val relative = relativePaths.getString(i).replace('\\', '/')
            if (relative.contains("..")) continue
            val source = File(staging, "photos/$relative")
            if (!source.exists()) {
                // Also accept paths already including photos/ prefix
                val alt = File(staging, relative)
                if (!alt.exists()) continue
                val copied = photoStorage.copyFile(alt, targetDir, preferredName = "${i}.${alt.extension.ifBlank { "jpg" }}")
                insert(copied.absolutePath, i)
                count++
                continue
            }
            val copied = photoStorage.copyFile(
                source,
                targetDir,
                preferredName = "${i}.${source.extension.ifBlank { "jpg" }}",
            )
            insert(copied.absolutePath, i)
            count++
        }
        return count
    }

    private fun isSafeZipPath(name: String): Boolean {
        if (name.contains("..")) return false
        if (name.startsWith("/")) return false
        return name == "data.json" || name.startsWith("photos/")
    }
}

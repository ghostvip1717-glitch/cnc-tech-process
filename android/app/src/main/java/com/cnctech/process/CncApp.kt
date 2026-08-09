package com.cnctech.process

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cnctech.process.data.backup.BackupManager
import com.cnctech.process.data.backup.MigrationImporter
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.photo.PhotoStorage
import com.cnctech.process.data.prefs.ThemePrefs
import com.cnctech.process.data.repository.CatalogRepository
import com.cnctech.process.data.repository.PartRepository
import com.cnctech.process.data.repository.TechProcessRepository
import com.cnctech.process.ui.theme.ThemeVariant

class CncApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var photoStorage: PhotoStorage
        private set
    lateinit var partRepository: PartRepository
        private set
    lateinit var catalogRepository: CatalogRepository
        private set
    lateinit var techProcessRepository: TechProcessRepository
        private set
    lateinit var backupManager: BackupManager
        private set
    lateinit var migrationImporter: MigrationImporter
        private set

    var themeVariant by mutableStateOf(ThemeVariant.Light)
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        themeVariant = ThemePrefs.get(this)
        rebuildGraph()
    }

    fun applyThemeVariant(variant: ThemeVariant) {
        ThemePrefs.set(this, variant)
        themeVariant = variant
    }

    fun rebuildGraph() {
        database = AppDatabase.getInstance(this)
        photoStorage = PhotoStorage(this)
        partRepository = PartRepository(database, photoStorage)
        catalogRepository = CatalogRepository(database, photoStorage)
        techProcessRepository = TechProcessRepository(database, catalogRepository, photoStorage)
        backupManager = BackupManager(this, photoStorage)
        migrationImporter = MigrationImporter(this, database, photoStorage)
    }

    companion object {
        lateinit var instance: CncApp
            private set
    }
}

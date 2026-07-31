package com.cnctech.process

import android.app.Application
import com.cnctech.process.data.backup.BackupManager
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.photo.PhotoStorage
import com.cnctech.process.data.repository.CatalogRepository
import com.cnctech.process.data.repository.PartRepository
import com.cnctech.process.data.repository.TechProcessRepository

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

    override fun onCreate() {
        super.onCreate()
        instance = this
        rebuildGraph()
    }

    fun rebuildGraph() {
        database = AppDatabase.getInstance(this)
        photoStorage = PhotoStorage(this)
        partRepository = PartRepository(database, photoStorage)
        catalogRepository = CatalogRepository(database, photoStorage)
        techProcessRepository = TechProcessRepository(database, catalogRepository)
        backupManager = BackupManager(this, photoStorage)
    }

    companion object {
        lateinit var instance: CncApp
            private set
    }
}

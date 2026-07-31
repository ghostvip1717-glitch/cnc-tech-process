package com.cnctech.process.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cnctech.process.data.dao.CatalogDao
import com.cnctech.process.data.dao.PartDao
import com.cnctech.process.data.dao.TechProcessDao
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogItemPhotoEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.entity.PartEntity
import com.cnctech.process.data.entity.PartPhotoEntity
import com.cnctech.process.data.entity.SetupEntity
import com.cnctech.process.data.entity.TechProcessEntity

class Converters {
    @TypeConverter
    fun fromCatalogType(value: CatalogType): String = value.name

    @TypeConverter
    fun toCatalogType(value: String): CatalogType = CatalogType.valueOf(value)
}

@Database(
    entities = [
        PartEntity::class,
        PartPhotoEntity::class,
        CatalogItemEntity::class,
        CatalogItemPhotoEntity::class,
        TechProcessEntity::class,
        SetupEntity::class,
        OperationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun partDao(): PartDao
    abstract fun catalogDao(): CatalogDao
    abstract fun techProcessDao(): TechProcessDao

    companion object {
        const val DB_NAME = "cnc_tech_process.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }

        fun closeAndClear() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        fun reopen(context: Context): AppDatabase {
            closeAndClear()
            return getInstance(context)
        }
    }
}

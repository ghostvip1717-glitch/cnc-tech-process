package com.cnctech.process.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogItemPhotoEntity
import com.cnctech.process.data.entity.CatalogType
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query(
        """
        SELECT * FROM catalog_items
        WHERE type = :type
          AND (:q = '' OR name LIKE '%' || :q || '%' OR IFNULL(note, '') LIKE '%' || :q || '%')
        ORDER BY name COLLATE NOCASE ASC
        """,
    )
    fun observeByType(type: CatalogType, q: String): Flow<List<CatalogItemEntity>>

    @Query("SELECT * FROM catalog_items WHERE type = :type ORDER BY name COLLATE NOCASE ASC")
    suspend fun listByType(type: CatalogType): List<CatalogItemEntity>

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun getById(id: Long): CatalogItemEntity?

    @Query("SELECT * FROM catalog_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<CatalogItemEntity>

    @Query("SELECT * FROM catalog_items WHERE type = :type AND name = :name LIMIT 1")
    suspend fun findByTypeAndName(type: CatalogType, name: String): CatalogItemEntity?

    @Insert
    suspend fun insert(item: CatalogItemEntity): Long

    @Update
    suspend fun update(item: CatalogItemEntity)

    @Query("DELETE FROM catalog_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM setups WHERE jawId = :catalogItemId")
    suspend fun countSetupUsages(catalogItemId: Long): Int

    @Query("SELECT COUNT(*) FROM operations WHERE toolId = :catalogItemId OR plateId = :catalogItemId")
    suspend fun countOperationUsages(catalogItemId: Long): Int

    @Query(
        """
        SELECT * FROM catalog_item_photos
        WHERE catalogItemId = :catalogItemId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    fun observePhotos(catalogItemId: Long): Flow<List<CatalogItemPhotoEntity>>

    @Query(
        """
        SELECT * FROM catalog_item_photos
        WHERE catalogItemId = :catalogItemId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    suspend fun getPhotos(catalogItemId: Long): List<CatalogItemPhotoEntity>

    @Query("SELECT * FROM catalog_item_photos WHERE id = :id")
    suspend fun getPhoto(id: Long): CatalogItemPhotoEntity?

    @Insert
    suspend fun insertPhoto(photo: CatalogItemPhotoEntity): Long

    @Update
    suspend fun updatePhoto(photo: CatalogItemPhotoEntity)

    @Query("DELETE FROM catalog_item_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM catalog_item_photos WHERE catalogItemId = :catalogItemId")
    suspend fun maxPhotoSortOrder(catalogItemId: Long): Int

    @Query("SELECT * FROM catalog_item_photos")
    suspend fun getAllPhotos(): List<CatalogItemPhotoEntity>

    @Query(
        """
        SELECT p.* FROM catalog_item_photos p
        INNER JOIN (
            SELECT catalogItemId, MIN(sortOrder) AS minOrder
            FROM catalog_item_photos
            GROUP BY catalogItemId
        ) first ON p.catalogItemId = first.catalogItemId AND p.sortOrder = first.minOrder
        """,
    )
    suspend fun getFirstPhotos(): List<CatalogItemPhotoEntity>

    @Query("DELETE FROM catalog_item_photos")
    suspend fun clearAllPhotos()

    @Query("DELETE FROM catalog_items")
    suspend fun clearAll()
}

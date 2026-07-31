package com.cnctech.process.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cnctech.process.data.entity.PartEntity
import com.cnctech.process.data.entity.PartPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Query(
        """
        SELECT * FROM parts
        WHERE (:q = '' OR number LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%')
        ORDER BY number COLLATE NOCASE ASC
        """,
    )
    fun observeParts(q: String): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts WHERE id = :id")
    fun observePart(id: Long): Flow<PartEntity?>

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getPart(id: Long): PartEntity?

    @Insert
    suspend fun insert(part: PartEntity): Long

    @Update
    suspend fun update(part: PartEntity)

    @Query("DELETE FROM parts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM part_photos WHERE partId = :partId ORDER BY sortOrder ASC, id ASC")
    fun observePhotos(partId: Long): Flow<List<PartPhotoEntity>>

    @Query("SELECT * FROM part_photos WHERE partId = :partId ORDER BY sortOrder ASC, id ASC")
    suspend fun getPhotos(partId: Long): List<PartPhotoEntity>

    @Query("SELECT * FROM part_photos WHERE id = :id")
    suspend fun getPhoto(id: Long): PartPhotoEntity?

    @Insert
    suspend fun insertPhoto(photo: PartPhotoEntity): Long

    @Update
    suspend fun updatePhoto(photo: PartPhotoEntity)

    @Query("DELETE FROM part_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM part_photos WHERE partId = :partId")
    suspend fun maxPhotoSortOrder(partId: Long): Int

    @Query("SELECT * FROM part_photos")
    suspend fun getAllPhotos(): List<PartPhotoEntity>
}

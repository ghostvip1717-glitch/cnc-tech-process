package com.cnctech.process.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.entity.OperationPhotoEntity
import com.cnctech.process.data.entity.SetupEntity
import com.cnctech.process.data.entity.SetupPhotoEntity
import com.cnctech.process.data.entity.TechProcessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TechProcessDao {
    @Query("SELECT * FROM tech_processes WHERE partId = :partId LIMIT 1")
    suspend fun getByPartId(partId: Long): TechProcessEntity?

    @Query("SELECT * FROM tech_processes WHERE partId = :partId LIMIT 1")
    fun observeByPartId(partId: Long): Flow<TechProcessEntity?>

    @Insert
    suspend fun insert(tp: TechProcessEntity): Long

    @Query("DELETE FROM tech_processes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM setups WHERE techProcessId = :techProcessId ORDER BY `order` ASC, id ASC")
    fun observeSetups(techProcessId: Long): Flow<List<SetupEntity>>

    @Query("SELECT * FROM setups WHERE techProcessId = :techProcessId ORDER BY `order` ASC, id ASC")
    suspend fun getSetups(techProcessId: Long): List<SetupEntity>

    @Query("SELECT * FROM setups WHERE id = :id")
    suspend fun getSetup(id: Long): SetupEntity?

    @Query("SELECT * FROM setups WHERE id = :id")
    fun observeSetup(id: Long): Flow<SetupEntity?>

    @Insert
    suspend fun insertSetup(setup: SetupEntity): Long

    @Update
    suspend fun updateSetup(setup: SetupEntity)

    @Query("DELETE FROM setups WHERE id = :id")
    suspend fun deleteSetup(id: Long)

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM setups WHERE techProcessId = :techProcessId")
    suspend fun maxSetupOrder(techProcessId: Long): Int

    @Query("SELECT * FROM operations WHERE setupId = :setupId ORDER BY `order` ASC, id ASC")
    fun observeOperations(setupId: Long): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations WHERE setupId = :setupId ORDER BY `order` ASC, id ASC")
    suspend fun getOperations(setupId: Long): List<OperationEntity>

    @Query("SELECT * FROM operations WHERE id = :id")
    suspend fun getOperation(id: Long): OperationEntity?

    @Insert
    suspend fun insertOperation(operation: OperationEntity): Long

    @Update
    suspend fun updateOperation(operation: OperationEntity)

    @Query("DELETE FROM operations WHERE id = :id")
    suspend fun deleteOperation(id: Long)

    @Query("DELETE FROM operations WHERE setupId = :setupId")
    suspend fun deleteOperationsForSetup(setupId: Long)

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM operations WHERE setupId = :setupId")
    suspend fun maxOperationOrder(setupId: Long): Int

    @Query(
        """
        SELECT o.* FROM operations o
        INNER JOIN setups s ON s.id = o.setupId
        WHERE s.techProcessId = :techProcessId
        """,
    )
    suspend fun getOperationsForTechProcess(techProcessId: Long): List<OperationEntity>

    // --- setup photos ---

    @Query(
        """
        SELECT * FROM setup_photos
        WHERE setupId = :setupId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    fun observeSetupPhotos(setupId: Long): Flow<List<SetupPhotoEntity>>

    @Query(
        """
        SELECT * FROM setup_photos
        WHERE setupId = :setupId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    suspend fun getSetupPhotos(setupId: Long): List<SetupPhotoEntity>

    @Query("SELECT * FROM setup_photos WHERE id = :id")
    suspend fun getSetupPhoto(id: Long): SetupPhotoEntity?

    @Insert
    suspend fun insertSetupPhoto(photo: SetupPhotoEntity): Long

    @Update
    suspend fun updateSetupPhoto(photo: SetupPhotoEntity)

    @Query("DELETE FROM setup_photos WHERE id = :id")
    suspend fun deleteSetupPhoto(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM setup_photos WHERE setupId = :setupId")
    suspend fun maxSetupPhotoSortOrder(setupId: Long): Int

    @Query("DELETE FROM setup_photos")
    suspend fun clearAllSetupPhotos()

    // --- operation photos ---

    @Query(
        """
        SELECT * FROM operation_photos
        WHERE operationId = :operationId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    fun observeOperationPhotos(operationId: Long): Flow<List<OperationPhotoEntity>>

    @Query(
        """
        SELECT p.* FROM operation_photos p
        INNER JOIN operations o ON o.id = p.operationId
        WHERE o.setupId = :setupId
        ORDER BY p.operationId ASC, p.sortOrder ASC, p.id ASC
        """,
    )
    fun observeOperationPhotosForSetup(setupId: Long): Flow<List<OperationPhotoEntity>>

    @Query(
        """
        SELECT * FROM operation_photos
        WHERE operationId = :operationId
        ORDER BY sortOrder ASC, id ASC
        """,
    )
    suspend fun getOperationPhotos(operationId: Long): List<OperationPhotoEntity>

    @Query("SELECT * FROM operation_photos WHERE id = :id")
    suspend fun getOperationPhoto(id: Long): OperationPhotoEntity?

    @Insert
    suspend fun insertOperationPhoto(photo: OperationPhotoEntity): Long

    @Update
    suspend fun updateOperationPhoto(photo: OperationPhotoEntity)

    @Query("DELETE FROM operation_photos WHERE id = :id")
    suspend fun deleteOperationPhoto(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM operation_photos WHERE operationId = :operationId")
    suspend fun maxOperationPhotoSortOrder(operationId: Long): Int

    @Query("DELETE FROM operation_photos")
    suspend fun clearAllOperationPhotos()

    @Query("DELETE FROM operations")
    suspend fun clearAllOperations()

    @Query("DELETE FROM setups")
    suspend fun clearAllSetups()

    @Query("DELETE FROM tech_processes")
    suspend fun clearAllTechProcesses()
}

package com.cnctech.process.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.entity.SetupEntity
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
}

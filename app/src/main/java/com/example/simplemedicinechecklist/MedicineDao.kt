package com.example.simplemedicinechecklist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine)

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    // Medicine Records (History)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MedicineRecord)

    @Query("SELECT * FROM medicine_records WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<MedicineRecord>>

    @Query("SELECT * FROM medicine_records WHERE date LIKE :monthQuery")
    fun getRecordsForMonth(monthQuery: String): Flow<List<MedicineRecord>>

    @Query("DELETE FROM medicine_records WHERE date < :thresholdDate")
    suspend fun pruneOldRecords(thresholdDate: String)
}

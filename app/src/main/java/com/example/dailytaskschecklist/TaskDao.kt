package com.example.dailytaskschecklist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE name = :name LIMIT 1")
    suspend fun getTaskByName(name: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Task Records (History)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TaskRecord)

    @Query("SELECT * FROM task_records WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<TaskRecord>>

    @Query("SELECT * FROM task_records WHERE date LIKE :monthQuery")
    fun getRecordsForMonth(monthQuery: String): Flow<List<TaskRecord>>

    @Query("DELETE FROM task_records WHERE date < :thresholdDate")
    suspend fun pruneOldRecords(thresholdDate: String)
}

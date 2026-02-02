package com.example.dailytaskschecklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val todayRecords: StateFlow<List<TaskRecord>> = _currentDate
        .flatMapLatest { date -> dao.getRecordsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Updates the date and returns true if the date has changed.
     */
    fun updateDate(): Boolean {
        val newDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (_currentDate.value != newDate) {
            _currentDate.value = newDate
            // If date changed, we should ensure records are fresh
            return true
        }
        return false
    }

    fun getRecordsForMonth(monthOffset: Int): Flow<List<TaskRecord>> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)
        val monthQuery = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time) + "%"
        return dao.getRecordsForMonth(monthQuery)
    }

    fun toggleTaskTaken(task: Task, timeSlot: String) {
        viewModelScope.launch {
            val updatedTask = when (timeSlot) {
                "Morning" -> task.copy(isTakenMorning = !task.isTakenMorning)
                "Afternoon" -> task.copy(isTakenAfternoon = !task.isTakenAfternoon)
                "Evening" -> task.copy(isTakenEvening = !task.isTakenEvening)
                else -> task
            }
            dao.updateTask(updatedTask)
        }
    }

    fun saveDailyRecords(tasks: List<Task>, timeSlot: String) {
        viewModelScope.launch {
            val date = _currentDate.value
            tasks.forEach { task ->
                val wasTaken = when (timeSlot) {
                    "Morning" -> task.isTakenMorning
                    "Afternoon" -> task.isTakenAfternoon
                    "Evening" -> task.isTakenEvening
                    else -> false
                }
                dao.insertRecord(
                    TaskRecord(
                        taskName = task.name,
                        taskDetails = task.numberOfTablets,
                        date = date,
                        timeSlot = timeSlot,
                        wasTaken = wasTaken
                    )
                )
                dao.updateTask(task.copy(taskTaken = true))
            }
        }
    }

    fun resetAllTasks() {
        viewModelScope.launch {
            val allTasks = dao.getAllTasks().first()
            allTasks.forEach { task ->
                dao.updateTask(
                    task.copy(
                        isTakenMorning = false,
                        isTakenAfternoon = false,
                        isTakenEvening = false,
                        taskTaken = false
                    )
                )
            }
        }
    }

    fun pruneOldRecords() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -1)
            
            val thresholdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            dao.pruneOldRecords(thresholdDate)
        }
    }

    fun addTask(name: String, tablets: String, times: String) {
        viewModelScope.launch {
            dao.insertTask(Task(name = name, numberOfTablets = tablets, times = times))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }
}

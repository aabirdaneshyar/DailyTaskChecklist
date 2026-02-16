package com.example.dailytaskschecklist

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(private val dao: TaskDao, private val context: Context) : ViewModel() {

    private val sharedPref: SharedPreferences = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)

    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _themePreference = MutableStateFlow(getSavedThemePreference())
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    val todayRecords: StateFlow<List<TaskRecord>> = _currentDate
        .flatMapLatest { date -> dao.getRecordsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initial check on cold start
        checkAndResetForNewDay()
    }

    private fun getSavedThemePreference(): ThemePreference {
        val themeName = sharedPref.getString("theme_preference", ThemePreference.Light.name)
        return try {
            ThemePreference.valueOf(themeName ?: ThemePreference.Light.name)
        } catch (e: Exception) {
            ThemePreference.Light
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        sharedPref.edit().putString("theme_preference", preference.name).apply()
        _themePreference.value = preference
    }

    /**
     * Updates the date and returns true if the date has changed.
     * If the date has changed, it resets all tasks and prunes old records.
     */
    fun updateDate(): Boolean {
        return checkAndResetForNewDay()
    }

    private fun checkAndResetForNewDay(): Boolean {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastResetDate = sharedPref.getString("last_reset_date", "")

        if (todayStr != lastResetDate) {
            // New day detected
            _currentDate.value = todayStr
            resetAllTasksImmediately()
            pruneOldRecords()
            
            // Persist the new date
            sharedPref.edit().putString("last_reset_date", todayStr).apply()
            return true
        }
        
        // Even if not a new day, ensure _currentDate is correct for UI
        if (_currentDate.value != todayStr) {
            _currentDate.value = todayStr
        }
        
        return false
    }

    private fun resetAllTasksImmediately() {
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

    fun getRecordsForMonth(monthOffset: Int): Flow<List<TaskRecord>> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)
        val monthQuery = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time) + "%"
        return dao.getRecordsForMonth(monthQuery)
    }

    fun toggleTaskTaken(task: Task, timeSlot: TimeSlot) {
        viewModelScope.launch {
            val updatedTask = when (timeSlot) {
                TimeSlot.Morning -> task.copy(isTakenMorning = !task.isTakenMorning)
                TimeSlot.Afternoon -> task.copy(isTakenAfternoon = !task.isTakenAfternoon)
                TimeSlot.Evening -> task.copy(isTakenEvening = !task.isTakenEvening)
            }
            dao.updateTask(updatedTask)
        }
    }

    fun saveDailyRecords(tasks: List<Task>, timeSlot: TimeSlot) {
        viewModelScope.launch {
            val date = _currentDate.value
            tasks.forEach { task ->
                val wasTaken = when (timeSlot) {
                    TimeSlot.Morning -> task.isTakenMorning
                    TimeSlot.Afternoon -> task.isTakenAfternoon
                    TimeSlot.Evening -> task.isTakenEvening
                }
                dao.insertRecord(
                    TaskRecord(
                        taskName = task.name,
                        taskDetails = task.numberOfTablets,
                        taskPriority = task.priority,
                        date = date,
                        timeSlot = timeSlot.title,
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
            
            val thresholdDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            dao.pruneOldRecords(thresholdDate)
        }
    }

    suspend fun addTask(name: String, tablets: String, times: List<TimeSlot>, priority: Priority): Boolean {
        val existingTask = dao.getTaskByName(name)
        return if (existingTask == null) {
            dao.insertTask(Task(name = name, numberOfTablets = tablets, times = times, priority = priority.title))
            true
        } else {
            false
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }
}

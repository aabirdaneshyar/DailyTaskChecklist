package com.example.simplemedicinechecklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MedicineViewModel(private val dao: MedicineDao) : ViewModel() {

    val medicines: StateFlow<List<Medicine>> = dao.getAllMedicines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val todayRecords: StateFlow<List<MedicineRecord>> = _currentDate
        .flatMapLatest { date -> dao.getRecordsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateDate() {
        val newDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (_currentDate.value != newDate) {
            _currentDate.value = newDate
        }
    }

    fun getRecordsForMonth(monthOffset: Int): Flow<List<MedicineRecord>> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)
        val monthQuery = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time) + "%"
        return dao.getRecordsForMonth(monthQuery)
    }

    fun toggleMedicineTaken(medicine: Medicine, timeSlot: String) {
        viewModelScope.launch {
            val updatedMedicine = when (timeSlot) {
                "Breakfast" -> medicine.copy(isTakenBreakfast = !medicine.isTakenBreakfast)
                "Lunch" -> medicine.copy(isTakenLunch = !medicine.isTakenLunch)
                "Dinner" -> medicine.copy(isTakenDinner = !medicine.isTakenDinner)
                else -> medicine
            }
            dao.updateMedicine(updatedMedicine)
        }
    }

    fun saveDailyRecords(medicines: List<Medicine>, timeSlot: String) {
        viewModelScope.launch {
            val date = _currentDate.value
            medicines.forEach { medicine ->
                val wasTaken = when (timeSlot) {
                    "Breakfast" -> medicine.isTakenBreakfast
                    "Lunch" -> medicine.isTakenLunch
                    "Dinner" -> medicine.isTakenDinner
                    else -> false
                }
                dao.insertRecord(
                    MedicineRecord(
                        medicineName = medicine.name,
                        date = date,
                        timeSlot = timeSlot,
                        wasTaken = wasTaken
                    )
                )
                dao.updateMedicine(medicine.copy(medicineTaken = true))
            }
        }
    }

    fun resetAllMedicines() {
        viewModelScope.launch {
            val allMedicines = dao.getAllMedicines().first()
            allMedicines.forEach { medicine ->
                dao.updateMedicine(
                    medicine.copy(
                        isTakenBreakfast = false,
                        isTakenLunch = false,
                        isTakenDinner = false,
                        medicineTaken = false
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

    fun addMedicine(name: String, tablets: String, times: String) {
        viewModelScope.launch {
            dao.insertMedicine(Medicine(name = name, numberOfTablets = tablets, times = times))
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            dao.deleteMedicine(medicine)
        }
    }
}

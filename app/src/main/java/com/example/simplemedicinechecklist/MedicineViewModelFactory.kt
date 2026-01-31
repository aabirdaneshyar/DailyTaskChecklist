package com.example.simplemedicinechecklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MedicineViewModelFactory(private val dao: MedicineDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicineViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

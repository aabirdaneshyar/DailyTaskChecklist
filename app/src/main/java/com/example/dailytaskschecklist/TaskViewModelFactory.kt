package com.example.dailytaskschecklist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TaskViewModelFactory(
    private val dao: TaskDao,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(dao, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

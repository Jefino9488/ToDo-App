package com.framework.minimaltodoapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.framework.minimaltodoapp.data.AppDatabase
import com.framework.minimaltodoapp.data.DatabaseProvider
import com.framework.minimaltodoapp.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.getDatabase(application)
    val tasks: Flow<List<Task>> = db.taskDao().getAllTasks()

    fun addTask(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().insert(Task(title = title))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            db.taskDao().delete(task)
        }
    }
}
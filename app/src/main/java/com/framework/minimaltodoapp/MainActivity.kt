package com.framework.minimaltodoapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framework.minimaltodoapp.viewmodel.TodoViewModel
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.rememberCoroutineScope
import com.framework.minimaltodoapp.ui.theme.MinimalTodoAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinimalTodoAppTheme {
                MinimalTodoApp()
            }
        }
    }
}

@Composable
fun MinimalTodoApp(viewModel: TodoViewModel = viewModel(factory = TodoViewModelFactory(LocalContext.current.applicationContext as Application))) {
    var taskInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            showPermissionDialog = true
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let {
                viewModel.addTask(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = taskInput,
                    onValueChange = { taskInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Add Task") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (taskInput.isNotBlank()) {
                        viewModel.addTask(taskInput)
                        taskInput = ""
                    }
                }) {
                    Text("Add")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your task")
                    }
                    speechLauncher.launch(intent)
                }) {
                    Text("🎤")
                }
            }
            TaskList(viewModel)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("This app needs microphone access to add tasks via voice. Please grant the permission in Settings.") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

class TodoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun TaskList(viewModel: TodoViewModel) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
        items(tasks) { task ->
            var offsetX by remember { mutableStateOf(0f) }
            val dragState = rememberDraggableState { delta ->
                offsetX += delta
                if (offsetX < -100f) {
                    coroutineScope.launch {
                        viewModel.deleteTask(task)
                        offsetX = 0f
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { viewModel.updateTask(task.copy(isCompleted = it)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = task.title, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
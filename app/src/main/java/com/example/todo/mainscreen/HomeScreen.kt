package com.example.todo.mainscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todo.DatabaseProvider
import com.example.todo.loadPreferenceString
import com.example.todo.ui.theme.emerald
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.todo.R
import java.util.*
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import com.example.todo.Task
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onSettingsClick: () -> Unit,
    onClearAllTasks: () -> Unit,
    onSortTasks: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "To Do",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Menu"
                    )
                }

                // Menu on the left
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sort by time") },
                        onClick = {
                            menuExpanded = false
                            onSortTasks()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear all tasks") },
                        onClick = {
                            menuExpanded = false
                            showDialog = true
                        }
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorResource(id = R.color.primary_blue),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )

    if(showDialog){
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Delete all tasks? ?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to delete all tasks?",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = {showDialog = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            showDialog = false
                            onClearAllTasks()
                        }) {
                            Text("Delete all tasks", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAddTaskClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateTask: (Task) -> Unit
) {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.getDatabase(context) }
    val taskDao = db.taskDao()

    var tasks = remember { mutableStateOf<List<Task>>(emptyList()) }
    var searchQuery = remember { mutableStateOf("") }

    val hideDoneKey = context.getString(R.string.hide_done_key)
    val hideCompleted = remember {
        mutableStateOf(
            loadPreferenceString(context, hideDoneKey)?.toBooleanStrictOrNull() == true
        )
    }

    // Fetch tasks from DB
    LaunchedEffect(hideCompleted.value) {
        tasks.value = if (hideCompleted.value) {
            taskDao.getAllTasks().filterNot { it.isCompleted }
        } else {
            taskDao.getAllTasks()
        }
    }

    // Filter tasks based on search query
    val filteredTasks = tasks.value.filter {
        it.title.contains(searchQuery.value, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column {
                // Top bar
                TopBar(
                    onSettingsClick = onSettingsClick,
                    onClearAllTasks = {
                        CoroutineScope(Dispatchers.IO).launch {
                            taskDao.deleteAllTasks()
                            tasks.value = emptyList()
                        }
                    },
                    onSortTasks = {
                        tasks.value = tasks.value.sortedBy { it.dueTime }
                    }
                )

                // Searcher to look for task by title
                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    placeholder = { Text("Search tasks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        // Add button
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = emerald
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks) { task ->
                    TaskCard(task = task, onNavigate = onNavigateTask) {
                        val updatedTask = task.copy(isCompleted = !task.isCompleted)

                        CoroutineScope(Dispatchers.IO).launch {
                            taskDao.updateTask(updatedTask)
                            // refresh after update
                            tasks.value = if (hideCompleted.value) {
                                taskDao.getAllTasks().filterNot { it.isCompleted }
                            } else {
                                taskDao.getAllTasks()
                            }
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun TaskCard(
    task: Task,
    onNavigate: (Task) -> Unit,
    onToggleComplete: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(task) }
            .background(
                color = colorResource(id = R.color.primary_blue).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Check icon
        IconButton(
            onClick = { onToggleComplete(task) },
            modifier = Modifier
                .size(24.dp)
        ) {
            if (task.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = colorResource(id = R.color.primary_green)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Circle,
                    contentDescription = "Incomplete",
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Task info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium)

            if (task.description.isNotBlank()) {
                Text(text = task.description, style = MaterialTheme.typography.bodyMedium)
            }

            if (task.dueTime > 0L) {
                Text(
                    text = "Due: ${Date(task.dueTime)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (task.category.isNotBlank()) {
                Text(
                    text = "Category: ${task.category}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (task.notify) {
                Text(
                    text = "Notifications On",
                    style = MaterialTheme.typography.bodySmall
                )
            }


        }

        // If there are any attachments show them
        if (task.attachments.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Attachments",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}















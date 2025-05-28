import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.TopAppBar
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.todo.R
import java.util.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todo.AppDatabase
import com.example.todo.DatabaseProvider
import com.example.todo.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.outlined.Circle
import com.example.todo.loadPreferenceString
import com.example.todo.savePreferenceString
import com.example.todo.uriToFilePath
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ToDoApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAddTaskClick = { navController.navigate("add_task") },
                onSettingsClick = { navController.navigate("settings")},
                onNavigateTask = {navController.navigate("edit_task/{taskId}")}
            )
        }
        composable("add_task") {
            AddTaskScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }

        composable("edit_task/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
            if (taskId != null) {
                EditTaskScreen(taskId = taskId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun EditTaskScreen(taskId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val taskDao = db.taskDao()

    var task = remember { mutableStateOf<Task?>(null) }

    // Load task from DB
    LaunchedEffect(taskId) {
        task.value = taskDao.getTaskById(taskId.toInt())
    }

    task.value?.let { t ->
        EditTaskContent(
            initialTask = t,
            onSave = { updatedTask ->
                CoroutineScope(Dispatchers.IO).launch {
                    taskDao.updateTask(updatedTask.copy(id = taskId.toInt()))
                }
                onBack()
            },
            onBack = onBack
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskContent(
    initialTask: Task,
    onSave: (Task) -> Unit,
    onBack: () -> Unit
) {
    val title = remember { mutableStateOf(initialTask.title) }
    val description = remember { mutableStateOf(initialTask.description) }
    var category = remember { mutableStateOf(initialTask.category) }

    val dueDate = remember { mutableStateOf(
        if (initialTask.dueTime > 0L)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(initialTask.dueTime))
        else ""
    )}

    val notify = remember { mutableStateOf(initialTask.notify) }
    val attachments = remember { mutableStateListOf<String>().apply { addAll(initialTask.attachments) } }
    val context = LocalContext.current

    val categoriesKey = context.getString(R.string.categories);
    val categories = loadPreferenceString(context, categoriesKey)

    var expanded = remember { mutableStateOf(false) }


    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var selectedDate = remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime = remember { mutableStateOf<LocalTime?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            uris.forEach { uri ->
                val path = uriToFilePath(context, uri)
                // Add to list
                if (path != null) attachments.add(path)
            }
        }
    )

    // Time Picker
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedTime.value = LocalTime.of(hourOfDay, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    // Date picker
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate.value = LocalDate.of(year, month + 1, dayOfMonth)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Task" ) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = title.value,
                onValueChange = { title.value = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description.value,
                onValueChange = { description.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )


            // Category selection
            OutlinedTextField(
                value = category.value,
                onValueChange = {},
                label = { Text("Category") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = true }
            )

            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { categoryItem ->
                    DropdownMenuItem(
                        text = { Text(categoryItem) },
                        onClick = {     // After click select category
                            category.value = categoryItem
                            expanded.value = false
                        }
                    )
                }
            }

            // Select time
            OutlinedTextField(
                value = dueDate.value,
                onValueChange = { },
                label = { Text("Complete By (Date & Time)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        datePickerDialog.show()
                    },
                readOnly = true
            )


            // Attachments Section
            Text("Attachments (${attachments.size})")
            LazyRow {
                items(attachments) { path ->
                    Text(text = path.substringAfterLast('/'), modifier = Modifier.padding(4.dp))
                }
            }

            Button(onClick = {
                filePickerLauncher.launch("*/*")
            }) {
                Text("Add Attachment")
            }

            // Notifications
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Enable Notifications")
                Spacer(
                    modifier = Modifier
                        .width(8.dp)

                )
                // Enabling notifications
                Switch(checked = notify.value, onCheckedChange = { notify.value = it })
            }

            // Save button
            Button(onClick = {
                val time = if (dueDate.value.trim().isEmpty()) {
                    0L
                } else {
                    formatter.parse(dueDate.value)?.time ?: 0L
                }

                onSave(
                    initialTask.copy(
                        title = title.value,
                        description = description.value,
                        category = category.value,
                        dueTime = time,
                        notify = notify.value,
                        attachments = attachments.toList()
                    )
                )
            }) {
                Text("Save Changes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val taskDao = db.taskDao()

    var title = remember { mutableStateOf("") }
    var description = remember { mutableStateOf("") }

    val categoriesKey = context.getString(R.string.categories)
    val categories = loadPreferenceString(context, categoriesKey)
    var category = remember { mutableStateOf("") }
    var expanded = remember { mutableStateOf(false) }

    var dueDate = remember { mutableStateOf("") }
    var notify = remember { mutableStateOf(false) }
    var attachments = remember { mutableStateListOf<String>() }

    // Date and time
    val calendar = Calendar.getInstance()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    var selectedDate = remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime = remember { mutableStateOf<LocalTime?>(null) }


    // Time Picker
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            selectedTime.value = LocalTime.of(hourOfDay, minute)

            val date = selectedDate.value
            if (date != null) {
                val dateTime = LocalDateTime.of(date, selectedTime.value!!)
                dueDate.value = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            }
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    // Date picker
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate.value = LocalDate.of(year, month + 1, dayOfMonth)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            uris.forEach { uri ->
                val path = uriToFilePath(context, uri)
                // Add to list
                if (path != null) attachments.add(path)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement
                .spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title.value,
                onValueChange = { title.value = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description.value,
                onValueChange = { description.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            // Category selection
            OutlinedTextField(
                value = category.value,
                onValueChange = {},
                label = { Text("Category") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded.value = true }
            )

            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { categoryItem ->
                    DropdownMenuItem(
                        text = { Text(categoryItem) },
                        onClick = {     // After click select category
                            category.value = categoryItem
                            expanded.value = false
                        }
                    )
                }
            }

            // Select time
            OutlinedTextField(
                value = dueDate.value,
                onValueChange = { },
                label = { Text("Complete By (Date & Time)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        datePickerDialog.show()
                    },
                readOnly = true
            )

            // Attachments Section
            Text("Attachments (${attachments.size})")
            LazyRow {
                items(attachments) { path ->
                    Text(text = path.substringAfterLast('/'), modifier = Modifier.padding(4.dp))
                }
            }

            Button(onClick = {
                filePickerLauncher.launch("*/*")
            }) {
                Text("Add Attachment")
            }

            // Notifications
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Enable Notifications")
                Spacer(
                    modifier = Modifier
                        .width(8.dp)

                )
                // Enabling notifications
                Switch(checked = notify.value, onCheckedChange = { notify.value = it })
            }

            Button(
                onClick = {
                    val time = try {
                        LocalDateTime.parse(dueDate.value, formatter).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        0L
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val task = Task(
                            title = title.value,
                            description = description.value,
                            category = category.value,
                            dueTime = time,
                            notify = notify.value,
                            isCompleted = false,
                            creationTime = System.currentTimeMillis(),
                            attachments = attachments.toList()
                        )
                        // Insert task do database
                        taskDao.insertTask(task)
                    }
                    // Go back to home
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary_green),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)

            ) {
                Text("Add Task")
            }
        }
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
    var tasks = remember { mutableStateOf<List<Task>>(emptyList()) }

    val taskDao = db.taskDao()

    val prefs = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
    val hideCompleted = remember { mutableStateOf(prefs.getBoolean("hide_completed", false)) }

    // Fetch tasks from DB
    LaunchedEffect(hideCompleted.value) {
        tasks.value = if (hideCompleted.value) {
            taskDao.getAllTasks().filterNot { it.isCompleted }
        } else {
            taskDao.getAllTasks()
        }
    }

    Scaffold(
        topBar = { TopBar(onSettingsClick = onSettingsClick) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = colorResource(id = R.color.primary_green)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        if (tasks.value.isEmpty()) {    // If no tasks show a text
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks.value) { task ->
                    TaskCard(task = task, onNavigate = onNavigateTask) { updatedTask ->
                        val updatedTask = task.copy(isCompleted = !task.isCompleted)    // Switch task status
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
            .clickable { onNavigate }
            .background(
                color = colorResource(id = R.color.primary_blue).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onToggleComplete(task)  },
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
                    text = "🔔 Notifications On",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "To Do",
                    modifier = Modifier
                        .align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO: handle list icon click */ }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "List"
                )
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
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    // Preferences key
    val categoriesKey = context.getString(R.string.categories_key)
    val hideDoneKey = context.getString(R.string.hide_done_key)
    val notificationTimeBefore = context.getString(R.string.notification_time_before_key)

    val categories = remember { mutableStateListOf<String>().apply { addAll(loadPreferenceString(context,categoriesKey)) } }
    val newCategory = remember { mutableStateOf("") }

    val hideCompleted = remember { mutableStateOf(prefs.getBoolean("hide_completed", false)) }
    val notificationLeadTime = remember { mutableStateOf(prefs.getInt("notification_lead_minutes", 10)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Category", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCategory.value,
                    onValueChange = { newCategory.value = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newCategory.value.isNotBlank() && !categories.contains(newCategory.value)) {
                        categories.add(newCategory.value)
                        savePreferenceString(context, categoriesKey, categories)
                        newCategory.value = ""
                    }
                }) {
                    Text("Add")
                }
            }

            // Categories settings
            Text("Current Categories:")
            LazyColumn {
                items(categories) { category ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(category, modifier = Modifier.weight(1f))

                        IconButton(onClick = {
                            categories.remove(category)
                            // Save after removing a category
                            savePreferenceString(context, categoriesKey, categories)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete category")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hide Completed Tasks")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = hideCompleted.value,
                    onCheckedChange = {
                        hideCompleted.value = it
                        prefs.edit().putBoolean("hide_completed", it).apply()
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notification lead time (minutes):")
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = notificationLeadTime.value.toString(),
                    onValueChange = {
                        val parsed = it.toIntOrNull() ?: 0
                        notificationLeadTime.value = parsed
                        prefs.edit().putInt("notification_lead_minutes", parsed).apply()
                    },
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

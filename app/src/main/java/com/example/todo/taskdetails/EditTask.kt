package com.example.todo.taskdetails

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.DatabaseProvider
import com.example.todo.R
import com.example.todo.Task
import com.example.todo.cancelNotification
import com.example.todo.getFileNameFromUri
import com.example.todo.loadPreferenceListString
import com.example.todo.loadPreferenceString
import com.example.todo.openFile
import com.example.todo.scheduleNotification
import com.example.todo.ui.theme.emerald
import com.example.todo.ui.theme.prussianBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EditTaskScreen(taskId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val taskDao = db.taskDao()
    val coroutineScope = rememberCoroutineScope()
    val notificationTimeBefore = context.getString(R.string.notification_time_before_key)
    var task = remember { mutableStateOf<Task?>(null) }

    // Load task from DB
    LaunchedEffect(taskId) {
        task.value = taskDao.getTaskById(taskId.toInt())
    }

    task.value?.let { t ->
        EditTaskContent(
            initialTask = t,
            onSave = { updatedTask ->
                coroutineScope.launch {
                    // Get current notification time
                    val notificationLeadTime = loadPreferenceString(context,notificationTimeBefore) ?: "5"
                    val notificationTimeMils = notificationLeadTime.toLong() * 60 * 1000

                    // Cancel notification if exists
                    cancelNotification(context, updatedTask.id, updatedTask.title)

                    taskDao.updateTask(updatedTask.copy(id = taskId.toInt()))

                    // Set new notification if enabled
                    if (updatedTask.notify && updatedTask.dueTime - notificationTimeMils > System.currentTimeMillis()) {
                        scheduleNotification(
                            context = context,
                            timeInMillis = updatedTask.dueTime,
                            taskId = updatedTask.id,
                            title = updatedTask.title
                        )
                    }

                    withContext(Dispatchers.Main) {
                        onBack()
                    }
                }
            },
            onDelete = { deletedTask ->
                coroutineScope.launch {

                    cancelNotification(context, deletedTask.id, deletedTask.title)

                    taskDao.deleteTask(deletedTask.copy(id = taskId.toInt()))
                    withContext(Dispatchers.Main) {
                        onBack()
                    }
                }
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
    onDelete: (Task) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val title = remember { mutableStateOf(initialTask.title) }
    val description = remember { mutableStateOf(initialTask.description) }

    val formattedCreationDate = remember(initialTask.creationTime) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        formatter.format(Date(initialTask.creationTime))
    }

    // Categories
    val categoriesKey = context.getString(R.string.categories_key)
    val categories = loadPreferenceListString(context, categoriesKey)
    var category = remember { mutableStateOf(initialTask.category) }
    var expanded = remember { mutableStateOf(false) }

    val notify = remember { mutableStateOf(initialTask.notify) }
    val attachments = remember {
        mutableStateListOf<Uri>().apply {
            addAll(initialTask.attachments.map { Uri.parse(it) })
        }
    }

    // Date and time
    val calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var selectedDate = remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime = remember { mutableStateOf<LocalTime?>(null) }
    val openDialog = remember { mutableStateOf(false) }

    val dueDate = remember { mutableStateOf(
        if (initialTask.dueTime > 0L)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(initialTask.dueTime))
        else ""
    )}

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
            attachments.addAll(uris)
        }
    )

    var isError = remember { mutableStateOf(false) }

    // Confirmation dialog for delete

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = {
                Text(
                    text = "Delete Task?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to delete this task? This action cannot be undone.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { openDialog.value = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            onDelete(initialTask)
                            openDialog.value = false
                            onBack()
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Task") },
                // Icon to back to prev screen
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Delete button
                    IconButton(
                        onClick = {
                            openDialog.value = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            tint = Color.Red
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Title
            OutlinedTextField(
                value = title.value,
                onValueChange = {
                    title.value = it
                    isError.value = it.isBlank()
                },
                isError = isError.value,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Show here info that title is required
            if (isError.value) {
                Text(
                    text = "Title is required",
                    color = Color(0xFFF80202),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Description
            OutlinedTextField(
                value = description.value,
                onValueChange = { description.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            // Category selection closed in box
            Box(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                ) {
                    categories.forEach { categoryItem ->
                        DropdownMenuItem(
                            text = { Text(categoryItem) },
                            onClick = {
                                category.value = categoryItem
                                expanded.value = false
                            }
                        )
                    }
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

            // Creation time
            OutlinedTextField(
                value = formattedCreationDate,
                onValueChange = {}, // Required even if disabled
                label = { Text("Created:") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )

            // Notifications
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Enable Notifications")
                Spacer(
                    modifier = Modifier
                        .width(8.dp)

                )
                // Enabling notifications
                Switch(checked = notify.value, onCheckedChange = { notify.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = Color.White,
                        checkedTrackColor = emerald,
                        uncheckedTrackColor = Color.Gray
                    ))
            }


            // Attachments section
            Button(
                onClick = {
                    filePickerLauncher.launch("*/*") // Allow any file type
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                , shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = prussianBlue
                )
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Attachment")
            }

            Text("Attachments: (${attachments.size})")
            LazyRow {
                itemsIndexed(attachments) { index, uri ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = getFileNameFromUri(context, uri),
                            modifier = Modifier.clickable {
                                openFile(context, uri)
                            },
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            attachments.remove(uri)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Attachment")
                        }
                    }
                }   }


            // Save button
            Button(
                onClick = {
                    if (title.value.isBlank()) {
                        isError.value = true
                    } else {
                        isError.value = false
                        val dueTimeMillis = if (dueDate.value.isNotBlank()) {
                            formatter.parse(dueDate.value)?.time ?: 0L
                        } else {
                            0L
                        }

                        val updatedTask = initialTask.copy(
                            title = title.value.trim(),
                            description = description.value.trim(),
                            category = category.value,
                            notify = notify.value,
                            attachments = attachments.map { it.toString() },
                            dueTime = dueTimeMillis
                        )

                        onSave(updatedTask)
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = emerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                , shape = RoundedCornerShape(6.dp)
            ) {
                Text("Save", color = Color.White)
            }
        }
    }
}

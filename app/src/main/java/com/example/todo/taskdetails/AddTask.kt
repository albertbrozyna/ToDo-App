package com.example.todo.taskdetails

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.util.Log
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.DatabaseProvider
import com.example.todo.R
import com.example.todo.Task
import com.example.todo.getFileNameFromUri
import com.example.todo.loadPreferenceListString
import com.example.todo.loadPreferenceString
import com.example.todo.openFile
import com.example.todo.scheduleNotification
import com.example.todo.ui.theme.emerald
import com.example.todo.ui.theme.prussianBlue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val taskDao = db.taskDao()

    var title = remember { mutableStateOf("") }
    var description = remember { mutableStateOf("") }

    val categoriesKey = context.getString(R.string.categories_key)
    val categories = loadPreferenceListString(context, categoriesKey)
    var category = remember { mutableStateOf("") }
    var expanded = remember { mutableStateOf(false) }

    var dueDate = remember { mutableStateOf("") }
    var notify = remember { mutableStateOf(false) }
    var attachments = remember { mutableStateListOf<Uri>() }

    // Notification key
    val notificationTimeBefore = context.getString(R.string.notification_time_before_key)

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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            attachments.addAll(uris)
        }
    )

    var isError = remember { mutableStateOf(false) }

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
                actions = {}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
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
                Switch(
                    checked = notify.value, onCheckedChange = { notify.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = Color.White,
                        checkedTrackColor = emerald,
                        uncheckedTrackColor = Color.Gray
                    )
                )
            }

            // Attachments section
            Button(
                onClick = {
                    filePickerLauncher.launch("*/*") // Allow any file type
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally), shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = prussianBlue
                )
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Attachment")
            }


            LazyRow {
                itemsIndexed(attachments) { index, uri ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                Color.LightGray.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
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
                }
            }

            Button(
                onClick = {
                    // Get a lead time before not
                    val notificationLeadTime =
                        loadPreferenceString(context, notificationTimeBefore) ?: "5"
                    val notificationTimeMils = notificationLeadTime.toLong() * 60 * 1000

                    val time = try {
                        LocalDateTime.parse(dueDate.value, formatter).atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        0L
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        if (title.value.isBlank()) {
                            isError.value = true
                        } else {
                            isError.value = false

                            val task = Task(
                                title = title.value,
                                description = description.value,
                                category = category.value,
                                dueTime = time,
                                notify = notify.value,
                                isCompleted = false,
                                creationTime = System.currentTimeMillis(),
                                attachments = attachments.map { it.toString() }
                            )

                            // Insert and get id
                            val id = taskDao.insertTask(task).toInt()

                            val timeToNot = task.dueTime - notificationTimeMils
                            val date = Date(timeToNot)
                            val formatter =
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            Log.d(
                                "ReminderDebug",
                                "Notification will fire at: ${formatter.format(date)}"
                            )

                            // Add a notification
                            if (task.notify && timeToNot > System.currentTimeMillis()) {
                                scheduleNotification(
                                    context = context,
                                    timeInMillis = task.dueTime - notificationTimeMils,
                                    title = task.title,
                                    taskId = id
                                )
                            }
                        }

                    }
                    // Go back to home
                    if (!title.value.isBlank()) {
                        onBack()
                    }

                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = emerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally), shape = RoundedCornerShape(6.dp)

            ) {
                Text("Add Task")
            }
        }
    }
}
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.TopAppBar
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.todo.R
import java.util.*



@Composable
fun ToDoApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAddTaskClick = { navController.navigate("add_task") }
            )
        }
        composable("add_task") {
            AddTaskScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAddTaskClick: () -> Unit
) {
    Scaffold(
        topBar = { TopBar() },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = colorResource(id = R.color.primary_green)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }


    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues = paddingValues),
            contentAlignment = Alignment.Center
        ) {

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
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
            IconButton(onClick = { /* TODO: handle settings click */ }) {
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
fun AddTaskScreen(onBack: () -> Unit) {
    var title = remember { mutableStateOf("") }
    var description = remember { mutableStateOf("") }
    var categoryInput = remember { mutableStateOf("") }
    var dueDate = remember { mutableStateOf("") }
    var notify = remember { mutableStateOf(false) }

    var expanded = remember { mutableStateOf(false) }
    val categories = listOf("Work", "Personal", "Shopping", "Other")
    val filteredCategories = categories.filter {
        it.contains(categoryInput.value, ignoreCase = true) && it != categoryInput.value
    }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Time Picker
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val time = String.format("%02d:%02d", hourOfDay, minute)
            dueDate.value = "${dueDate.value} $time"
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    // Date pick
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            dueDate.value = selectedDate
            timePickerDialog.show() // After date show time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
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

            // Category input with suggestions
            Box {
                OutlinedTextField(
                    value = categoryInput.value,
                    onValueChange = {
                        categoryInput.value = it
                        expanded.value = true
                    },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = expanded.value && filteredCategories.isNotEmpty(),
                    onDismissRequest = { expanded.value = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredCategories.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                categoryInput.value = suggestion
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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Notifications")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = notify.value, onCheckedChange = { notify.value = it })
            }

            Button(
                onClick = {
                    println("Task Saved: $title, $description, $categoryInput, $dueDate, notify=$notify")
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .background(colorResource(id = R.color.primary_green))
            ) {
                Text("Add Task")
            }
        }
    }
}
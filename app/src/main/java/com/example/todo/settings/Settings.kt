package com.example.todo.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.todo.R
import com.example.todo.loadPreferenceListString
import com.example.todo.loadPreferenceString
import com.example.todo.savePreferenceListString
import com.example.todo.savePreferenceString
import com.example.todo.ui.theme.emerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    // Preferences key
    val categoriesKey = context.getString(R.string.categories_key)
    val hideDoneKey = context.getString(R.string.hide_done_key)
    val notificationTimeBefore = context.getString(R.string.notification_time_before_key)

    val categories = remember { mutableStateListOf<String>().apply { addAll(loadPreferenceListString(context,categoriesKey)) } }
    val newCategory = remember { mutableStateOf("") }

    // Toggle visibility of categories
    val showCategories = remember { mutableStateOf(false) }

    val hideCompleted = remember { mutableStateOf(prefs.getBoolean("hide_completed", false)) }
    val notificationLeadTime = remember {
        mutableStateOf(loadPreferenceString(context, notificationTimeBefore) ?: "5")
    }

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

            // Add category field and under it button to show current categories
            Text("Add Category", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCategory.value,
                    onValueChange = { newCategory.value = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Adding categories
                Button(
                    onClick = {
                        if (newCategory.value.isNotBlank() && !categories.contains(newCategory.value)) {
                            categories.add(newCategory.value)
                            savePreferenceListString(context, categoriesKey, categories)
                            newCategory.value = ""
                        }
                    },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emerald,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Add category",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }



            // Categories settings section
            TextButton(onClick = { showCategories.value = !showCategories.value }) {
                Text(if (showCategories.value) "Hide Categories" else "Show Categories")
            }

            // show categories
            if (showCategories.value) {
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
                                savePreferenceListString(context, categoriesKey, categories)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete category",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(top = 8.dp))

            // A button to hide done tasks
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hide Completed Tasks")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = hideCompleted.value,
                    onCheckedChange = {
                        hideCompleted.value = it
                        savePreferenceString(context = context,hideDoneKey,hideCompleted.value.toString())
                    }
                )
            }

            // Notification time section
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notification lead time (minutes):")
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = notificationLeadTime.value,
                    onValueChange = {input ->
                        if (input.all { it.isDigit() }) {
                            notificationLeadTime.value = input
                            savePreferenceString(context, notificationTimeBefore, input)
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                )
            }
        }
    }
}


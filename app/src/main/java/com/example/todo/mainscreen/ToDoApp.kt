package com.example.todo.mainscreen


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todo.createNotificationChannel
import com.example.todo.settings.SettingsScreen
import com.example.todo.taskdetails.AddTaskScreen
import com.example.todo.taskdetails.EditTaskScreen

@Composable
fun ToDoApp(startTaskId: Int? = null) {
    val navController = rememberNavController()

    LaunchedEffect(startTaskId) {
        if (startTaskId != null) {
            navController.navigate("edit_task/$startTaskId")
        }
    }

    val context = LocalContext.current

    // Create a notification channel
    createNotificationChannel(context)


    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAddTaskClick = { navController.navigate("add_task") },
                onSettingsClick = { navController.navigate("settings")},
                onNavigateTask = { task ->
                    navController.navigate("edit_task/${task.id}")
                }
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

package com.example.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.todo.mainscreen.ToDoApp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val taskIdFromNotification = intent?.getIntExtra("taskId", -1)?.takeIf { it != -1 }

            setContent {
                ToDoApp(startTaskId = taskIdFromNotification)
            }
        }
    }
}
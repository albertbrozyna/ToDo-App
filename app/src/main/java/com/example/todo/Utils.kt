package com.example.todo

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import java.io.File
import kotlin.random.Random

// Save preference string separated by comma
fun savePreferenceListString(context: Context, key: String, preference: List<String>) {
    val appName = context.getString(R.string.app_name)
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(appName, Context.MODE_PRIVATE)

    val joined = preference.joinToString(",")
    sharedPreferences.edit { putString(key, joined) }
}

fun loadPreferenceListString(context: Context, key: String): List<String> {
    val appName = context.getString(R.string.app_name)
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(appName, Context.MODE_PRIVATE)

    val saved = sharedPreferences.getString(key, null)
    return saved?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}



// Save preference string
fun savePreferenceString(context: Context, key: String, preference: String) {
    val appName = context.getString(R.string.app_name)

    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(appName, Context.MODE_PRIVATE)

    sharedPreferences.edit { putString(key, preference) }
}

// Load preference string
fun loadPreferenceString(context: Context, key: String): String? {
    val appName = context.getString(R.string.app_name)

    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(appName, Context.MODE_PRIVATE)

    return sharedPreferences.getString(key, null)
}

fun openFile(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, context.contentResolver.getType(uri))
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Brak aplikacji do otwarcia pliku", Toast.LENGTH_SHORT).show()
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                result = cursor.getString(nameIndex)
            }
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result ?: "Unknown"
}

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        "todo_channel_id",
        "ToDo Notifications",
        NotificationManager.IMPORTANCE_HIGH
    )

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

            val title = intent.getStringExtra("title") ?: "Task Reminder"

            val taskId = intent.getIntExtra("taskId", -1)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("taskId", taskId)
            }

            val openPendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, "todo_channel_id")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Reminder")
                .setContentText(title)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)

        }
    }
}

fun scheduleNotification(context: Context, timeInMillis: Long,taskId: Int, title: String) {
    val pendingIntent = getAlarmPendingIntent(context, taskId, title)

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }
}

fun getAlarmPendingIntent(context: Context, taskId: Int, title: String): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("taskId", taskId)
    }

    return PendingIntent.getBroadcast(
        context,
        taskId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun cancelNotification(context: Context, taskId: Int, title: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = getAlarmPendingIntent(context, taskId, title)
    alarmManager.cancel(pendingIntent)
}
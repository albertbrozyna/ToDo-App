package com.example.todo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.edit
import java.io.File

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
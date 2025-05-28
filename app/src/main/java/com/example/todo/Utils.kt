package com.example.todo

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit

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

// function to convert a uri to path
fun uriToFilePath(context: Context, uri: Uri): String? {
    return uri.path
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

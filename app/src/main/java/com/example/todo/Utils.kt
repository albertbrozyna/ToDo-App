package com.example.todo

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

// Save preference string seperated by comma
fun savePreferenceString(context: Context, key: String, preference: List<String>) {
    val appName = context.getString(R.string.app_name)
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(appName, Context.MODE_PRIVATE)

    val joined = preference.joinToString(",")
    sharedPreferences.edit().putString(key, joined).apply()
}

fun loadPreferenceString(context: Context, key: String): List<String> {
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
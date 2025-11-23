package com.skul9x.tracuu.utils

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs

    fun log(tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = "[$time] [$tag]: $message"
        // Print to Android Logcat as well
        android.util.Log.d(tag, message)
        // Add to internal list (keep last 200 lines to avoid memory issues)
        if (_logs.size > 200) {
            _logs.removeAt(0)
        }
        _logs.add(entry)
    }

    fun error(tag: String, message: String, e: Exception? = null) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val entry = "[$time] [ERROR] [$tag]: $message ${e?.message ?: ""}"
        android.util.Log.e(tag, message, e)
        _logs.add(entry)
        e?.stackTrace?.take(5)?.forEach { 
            _logs.add("    at $it")
        }
    }

    fun clear() {
        _logs.clear()
    }
    
    fun getAllLogsAsString(): String {
        return _logs.joinToString("\n")
    }
}
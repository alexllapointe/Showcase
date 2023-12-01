package edu.iu.alex.showcase

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GestureViewModel : ViewModel() {
    private val _gestureLogs = MutableLiveData<List<String>>() // Use List<String> instead of MutableList<String>
    val gestureLogs: LiveData<List<String>> get() = _gestureLogs

    init {
        _gestureLogs.value = emptyList()
    }

    /*
    * Add gesture logs as live data. Used in gesture area when movement is detected.
    *
     */

    fun addGestureLog(log: String) {
        val currentLogs = _gestureLogs.value ?: emptyList()
        val updatedLogs = listOf(log) + currentLogs
        _gestureLogs.value = updatedLogs
    }

}
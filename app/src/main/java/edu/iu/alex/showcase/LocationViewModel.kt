package edu.iu.alex.showcase

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel : ViewModel() {
    private val _locationData = MutableLiveData<String>()
    val locationData: LiveData<String> = _locationData

    private val _pressureData = MutableLiveData<String>()
    val pressureData: LiveData<String> = _pressureData

    private val _temperatureData = MutableLiveData<Float>()
    val temperatureData: LiveData<Float> = _temperatureData

    fun updatePressureData(pressure: Float) {
        _pressureData.postValue("$pressure")
    }

    fun updateLocationData(newLocation: String) {
        Log.d("LocationViewModel", "Updating location data: $newLocation")
        _locationData.postValue(newLocation)
    }

    fun updateTemperatureData(temperature: Float) {
        _temperatureData.postValue(temperature)
    }
}

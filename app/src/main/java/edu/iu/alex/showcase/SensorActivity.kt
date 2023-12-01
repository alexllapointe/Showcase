package edu.iu.alex.showcase

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class SensorActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    private var temperatureSensor: Sensor? = null
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var pressureEventListener: SensorEventListener
    private var isLocationFetchingInProgress = false


    /*
    * Check for permission to access location, if not then request it.
    *
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                // Permissions are granted, proceed with fetching location
                Log.d("SensorActivity","Permissions granted.")
                fetchLocation()
            } else {
                Log.d("SensorActivity", "Permissions not granted.")
                // Handle the case where permissions are not granted
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        locationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]

        if (temperatureSensor == null) {
            Log.d("SensorActivity", "Temperature sensor not available")
        }

        setContent {
            SensorScreen(locationViewModel)
        }


    }

    /*
    * Main Activity composable.
    *
     */
    @Composable
    fun SensorScreen(locationViewModel: LocationViewModel) {
        Log.d("SensorActivity","Sensor Screen called.")
        // Observe the LiveData object and get its value
        val locationData = locationViewModel.locationData.observeAsState(initial = "Fetching, Fetching").value
        val pressureData = locationViewModel.pressureData.observeAsState(initial = "Fetching").value
        val temperatureData = locationViewModel.temperatureData.observeAsState(initial = "Fetching").value

        LaunchedEffect(temperatureData) {
            Log.d("SensorActivity", "Temperature Data Recomposed: $temperatureData")
        }

        // Split location data into city and state
        val (city, state) = locationData.split(", ").let { locationParts ->
            if (locationParts.size >= 2) {
                Pair(locationParts[0], locationParts[1])
            } else {
                Pair("Unknown", "Unknown")
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sensor Playground",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            Column(Modifier.padding(16.dp)) {
                Text(text = "Name: Alex LaPointe")
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = "Location: $state, $city")
                Spacer(modifier = Modifier.height(5.dp))
            }

            Column(
                Modifier.padding(16.dp)
            ) {
                Text(text = "Temperature: $temperatureData °C")
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = "Air Pressure: $pressureData hPa")
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            GesturePlaygroundButton(onFling = { navigateToGestureActivity() })

            Spacer(modifier = Modifier.weight(1f))
        }

    }

    /*
    * Main Activity composable.
    *
     */
    @Composable
    fun GesturePlaygroundButton(onFling: () -> Unit) {
        var startOffsetX = 0f
        var startOffsetY = 0f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {  },
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                startOffsetX = offset.x
                                startOffsetY = offset.y
                            },
                            onDragEnd = {
                                startOffsetX = 0f
                                startOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                val endOffsetX = startOffsetX + dragAmount.x
                                val endOffsetY = startOffsetY + dragAmount.y

                                if (kotlin.math.abs(endOffsetX - startOffsetX) > 100 || kotlin.math.abs(
                                        endOffsetY - startOffsetY
                                    ) > 100) {
                                    onFling()
                                }

                                change.consume()
                            }
                        )
                    }
            ) {
                Text("Gesture Playground")
            }
        }
    }

    /*
    * Navigation to the Gesture Activity
    * Called when the button is flung.
    *
    * Starts the Gesture Activity using intent.
    *
     */

    private fun navigateToGestureActivity() {
        Log.d("SensorActivity", "Navigating to Gesture Activity")
        val intent = Intent(this, GestureActivity::class.java)
        startActivity(intent)
        finish()
    }

    /*
    * Registers sensors for use.
    *
    *
     */
    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
        pressureSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        temperatureSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("Temperature Sensor", "Sensor Registered.")
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PRESSURE -> {
                val pressure = event.values[0] // Pressure in hPa
                locationViewModel.updatePressureData(pressure)
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                val temperature = event.values[0] // Temperature in degrees Celsius
                Log.d("SensorActivity", "Temperature Sensor value: $temperature")
                locationViewModel.updateTemperatureData(temperature)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Handle sensor accuracy changes if needed
    }


    private fun checkAndRequestPermissions() {
        Log.d("SensorActivity","Check and Request Permissions called.")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions are already granted, proceed with the operation
            fetchLocation()
        } else {
            // Permissions are not granted, request them
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun fetchLocation() {
        Log.d("SensorActivity","fetchLocation called.")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Sensor Activity", "Location permissions not granted.")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                location?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        getAddressFromLocation(location.latitude, location.longitude)
                    }
                }
            }
            .addOnFailureListener {
                Log.d("Sensor Activity", "Failed to get the current location.")
            }
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double) {
        Log.d("SensorActivity", "getAddressFromLocation called for lat: $latitude, lng: $longitude")

        if (!Geocoder.isPresent()) {
            Log.e("SensorActivity", "Geocoder service not available")
            locationViewModel.updateLocationData("Geocoder not available")
            return
        }

        try {
            val geocoder = Geocoder(this@SensorActivity, Locale.getDefault())
            val addresses = withContext(Dispatchers.IO) {
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: "Unknown City"
                    val state = address.adminArea ?: "Unknown State"
                    val locationString = "$city, $state"
                    locationViewModel.updateLocationData(locationString)
                } else {
                    locationViewModel.updateLocationData("Location not found")
                }
            }
        } catch (e: IOException) {
            Log.e("SensorActivity", "Error in geocoding: ${e.message}")
            locationViewModel.updateLocationData("Error fetching location")
        }
    }

}

package edu.iu.alex.showcase

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start SensorActivity
        val intent = Intent(this, SensorActivity::class.java)
        startActivity(intent)
        // Finish MainActivity so it's removed from the activity stack
        finish()
    }
}
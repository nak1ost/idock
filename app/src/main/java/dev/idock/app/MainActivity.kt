package dev.idock.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.idock.app.data.Prefs
import dev.idock.app.device.UsbDeviceMonitor
import dev.idock.app.ui.AppRoot

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var monitor: UsbDeviceMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The app draws its own fade behind the navigation bar.
            window.isNavigationBarContrastEnforced = false
        }
        prefs = Prefs(applicationContext)
        monitor = UsbDeviceMonitor(applicationContext)
        setContent { AppRoot(prefs = prefs, monitor = monitor) }
    }

    override fun onStart() {
        super.onStart()
        monitor.start()
    }

    override fun onStop() {
        monitor.stop()
        super.onStop()
    }

    // The system delivers "iPhone plugged in" here when the app is already open.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        monitor.refresh()
    }
}

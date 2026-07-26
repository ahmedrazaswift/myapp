package com.example

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.MotorcycleRepository
import com.example.ui.MotorcycleViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices

@SuppressLint("InvalidFragmentVersionForActivityResult")
class MainActivity : ComponentActivity() {
    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(this, 0, intent, flags)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            Log.d("MainActivity", "Location permissions granted.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    registerGeofence()
                }
            } else {
                registerGeofence()
            }
        } else {
            Log.e("MainActivity", "Location permissions denied. Geofence registration skipped.")
        }
    }

    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Background location permission granted.")
        } else {
            Log.w("MainActivity", "Background location permission denied. Geofencing might only work in foreground.")
        }
        registerGeofence()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val locationContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            applicationContext.createAttributionContext("garage_location")
        } else {
            applicationContext
        }
        geofencingClient = LocationServices.getGeofencingClient(locationContext)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = MotorcycleRepository(database.motorcycleDao())
        
        val factory = ViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[MotorcycleViewModel::class.java]

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        VideoSplashScreen(
                            onVideoFinished = {
                                showSplash = false 
                            }
                        )
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }

        checkLocationPermissionsAndSetupGeofence()
    }

    private fun checkLocationPermissionsAndSetupGeofence() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    registerGeofence()
                }
            } else {
                registerGeofence()
            }
        }
    }

    private fun registerGeofence() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "Cannot register geofence: ACCESS_FINE_LOCATION not granted.")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId("garage_traffic_geofence")
            .setCircularRegion(25.14806, 51.57208, 100f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    Log.d("MainActivity", "Geofence registered successfully for 5G5F+363, Al Wukair (100m radius)")
                }
                addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to register geofence: ${e.message}", e)
                }
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Security exception registering geofence: ${e.message}")
        }
    }
}

@Composable
fun VideoSplashScreen(onVideoFinished: () -> Unit) {
    var finishedCalled by remember { mutableStateOf(false) }
    val safeOnVideoFinished = {
        if (!finishedCalled) {
            finishedCalled = true
            onVideoFinished()
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3500)
        Log.d("MainActivity", "Splash timeout reached. Transitioning automatically.")
        safeOnVideoFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                Log.d("MainActivity", "Splash tapped. Skipping video.")
                safeOnVideoFinished()
            }
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    val videoPath = "android.resource://${context.packageName}/raw/splash_video"
                    setVideoURI(Uri.parse(videoPath))
                    setOnCompletionListener {
                        safeOnVideoFinished()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("MainActivity", "Error playing splash video: what=$what, extra=$extra. Bypassing splash screen.")
                        safeOnVideoFinished()
                        true
                    }
                    start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            com.example.util.AppointmentNotificationHelper.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.e("MainApplication", "Failed to create notification channel", e)
        }
        try {
            val prefs = getSharedPreferences("firebase_settings_prefs", Context.MODE_PRIVATE)
            val projId = prefs.getString("project_id", "advance-auto-motor-tradin-g") ?: "advance-auto-motor-tradin-g"
            val appId = prefs.getString("app_id", "1:894118784300:web:811b2f6d755ea63bbf1089") ?: "1:894118784300:web:811b2f6d755ea63bbf1089"
            val apiKey = prefs.getString("api_key", "AIzaSyAbB6Nj4Wk4IcI-dYYIMGsTEjoYC2_E9Ng") ?: "AIzaSyAbB6Nj4Wk4IcI-dYYIMGsTEjoYC2_E9Ng"

            try {
                val existingApp = FirebaseApp.getInstance()
                val existingOptions = existingApp.options
                if (existingOptions.projectId != projId || existingOptions.applicationId != appId || existingOptions.apiKey != apiKey) {
                    Log.d("MainApplication", "Firebase options mismatch. Re-initializing...")
                    existingApp.delete()
                    throw IllegalStateException("Reinitialize")
                }
                Log.d("MainApplication", "FirebaseApp already initialized with project: ${existingApp.options.projectId}. Keeping existing instance.")
            } catch (e: Exception) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(projId)
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MainApplication", "Firebase manually initialized successfully as default app with project: $projId.")
            }
        } catch (ex: Exception) {
            Log.e("MainApplication", "Failed to manually initialize Firebase: ${ex.message}", ex)
        }
    }
}

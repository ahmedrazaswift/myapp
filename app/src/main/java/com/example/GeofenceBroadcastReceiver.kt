package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "GeofencingEvent error code: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val increment = if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) 1L else -1L
            Log.d("GeofenceReceiver", "Geofence transition: $transition. Increment: $increment")

            // 1. Update Firestore as requested
            try {
                val prefs = context.getSharedPreferences("firebase_settings_prefs", Context.MODE_PRIVATE)
                val projId = prefs.getString("project_id", "advance-auto-motor-tradin-g") ?: "advance-auto-motor-tradin-g"
                val appId = prefs.getString("app_id", "1:894118784300:web:811b2f6d755ea63bbf1089") ?: "1:894118784300:web:811b2f6d755ea63bbf1089"
                val apiKey = prefs.getString("api_key", "AIzaSyAbB6Nj4Wk4IcI-dYYIMGsTEjoYC2_E9Ng") ?: "AIzaSyAbB6Nj4Wk4IcI-dYYIMGsTEjoYC2_E9Ng"

                try {
                    val existingApp = com.google.firebase.FirebaseApp.getInstance()
                    val existingOptions = existingApp.options
                    if (existingOptions.projectId != projId || existingOptions.applicationId != appId || existingOptions.apiKey != apiKey) {
                        existingApp.delete()
                        throw IllegalStateException("Reinitialize")
                    }
                } catch (e: Exception) {
                    try {
                        val options = com.google.firebase.FirebaseOptions.Builder()
                            .setProjectId(projId)
                            .setApplicationId(appId)
                            .setApiKey(apiKey)
                            .build()
                        com.google.firebase.FirebaseApp.initializeApp(context, options)
                    } catch (exc: Exception) {
                        Log.e("GeofenceReceiver", "Failed to defensively initialize Firebase in Receiver", exc)
                    }
                }

                val db = FirebaseFirestore.getInstance()
                db.collection("garages")
                    .document("main_garage")
                    .update("current_bikes", FieldValue.increment(increment))
                    .addOnSuccessListener {
                        Log.d("GeofenceReceiver", "Firestore updated current_bikes by $increment successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("GeofenceReceiver", "Firestore update failed", e)
                    }
            } catch (e: Exception) {
                Log.e("GeofenceReceiver", "Firestore not initialized or error occurred", e)
            }

            // 2. Local State Sync & SharedPreferences
            // This ensures instant UI updates on device and supports offline/simulated interactions.
            val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val current = sharedPrefs.getInt("current_bikes", 5)
            val newValue = (current + increment.toInt()).coerceAtLeast(0)
            
            sharedPrefs.edit().putInt("current_bikes", newValue).apply()

            // 3. Update 7 days traffic history record
            // Store the maximum current riders for today
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val historyKey = "traffic_history_$todayStr"
            val storedMax = sharedPrefs.getInt(historyKey, 0)
            if (newValue > storedMax) {
                sharedPrefs.edit().putInt(historyKey, newValue).apply()
            }
            
            // Broadcast standard local intent to notify running activities of the change
            val updateIntent = Intent("com.example.ACTION_GEOGRAPHY_UPDATE")
            updateIntent.putExtra("current_bikes", newValue)
            context.sendBroadcast(updateIntent)
        } else {
            Log.e("GeofenceReceiver", "Invalid geofence transition: $transition")
        }
    }
}

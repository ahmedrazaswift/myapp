package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.Appointment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppointmentNotificationHelper {

    const val CHANNEL_ID = "appointment_reminders_channel"
    private const val CHANNEL_NAME = "Scheduled Service Reminders"
    private const val CHANNEL_DESC = "Notifications for upcoming scheduled motorcycle service appointments"

    /**
     * Ensures the notification channel exists on Android O (API 26) and higher.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a system Android notification to remind the user about a specific scheduled appointment.
     */
    fun sendAppointmentReminderNotification(
        context: Context,
        appointment: Appointment,
        titleOverride: String? = null,
        messageOverride: String? = null
    ): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "APPOINTMENTS")
            putExtra("APPOINTMENT_ID", appointment.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (appointment.id.toInt() + 5000).coerceAtLeast(1000),
            intent,
            pendingIntentFlags
        )

        val dateFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        val formattedDate = dateFmt.format(Date(appointment.preferredDate))
        val serviceTypeClean = appointment.serviceType.replace("_", " ")

        val title = titleOverride ?: "⏰ Upcoming Service Appointment Reminder"
        val apptNumStr = if (appointment.appointmentNumber.isNotBlank()) " (#${appointment.appointmentNumber})" else ""
        val contentText = messageOverride ?: "Reminder: Bike ${appointment.bikePlate}$apptNumStr is scheduled for $formattedDate ($serviceTypeClean)."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (appointment.id.toInt() + 10000).coerceAtLeast(1)
            notificationManager.notify(notificationId, builder.build())
            Log.d("AppointmentNotifyHelper", "Sent reminder notification for appointment #${appointment.id}")
            true
        } catch (e: SecurityException) {
            Log.e("AppointmentNotifyHelper", "Notification permission not granted: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("AppointmentNotifyHelper", "Failed to send notification: ${e.message}", e)
            false
        }
    }

    /**
     * Checks all appointments for any upcoming bookings (e.g. within the next 48 hours or scheduled for today)
     * and sends system notifications for un-notified items.
     */
    fun checkAndTriggerUpcomingReminders(
        context: Context,
        appointments: List<Appointment>
    ): Int {
        val now = System.currentTimeMillis()
        val fortyEightHoursMs = 48 * 3600 * 1000L

        val prefs = context.getSharedPreferences("appointment_notifications_prefs", Context.MODE_PRIVATE)

        val upcomingList = appointments.filter { appt ->
            (appt.status == "CONFIRMED" || appt.status == "PENDING") &&
                    appt.preferredDate >= (now - 12 * 3600 * 1000L) &&
                    appt.preferredDate <= (now + fortyEightHoursMs)
        }

        var count = 0
        for (appt in upcomingList) {
            val notifiedKey = "notified_appt_${appt.id}_${appt.preferredDate}"
            if (!prefs.getBoolean(notifiedKey, false)) {
                val sent = sendAppointmentReminderNotification(context, appt)
                if (sent) {
                    prefs.edit().putBoolean(notifiedKey, true).apply()
                    count++
                }
            }
        }
        return count
    }
}

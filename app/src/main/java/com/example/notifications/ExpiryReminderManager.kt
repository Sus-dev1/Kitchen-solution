package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.PantryItem
import java.util.Calendar

private const val PREFS_REMINDER = "kitchen_expiry_reminder_prefs"
private const val KEY_REMINDER_ENABLED = "expiry_reminder_enabled"
private const val KEY_URGENT_SUMMARY = "urgent_items_summary"
private const val KEY_URGENT_COUNT = "urgent_items_count"

const val CHANNEL_EXPIRY_ALERTS = "kitchen_expiry_alerts"
private const val NOTIFICATION_ID_EXPIRY = 4201
private const val REQUEST_CODE_ALARM = 4202

class ExpiryReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (!isExpiryReminderEnabled(context)) return
    val prefs = context.applicationContext.getSharedPreferences(PREFS_REMINDER, Context.MODE_PRIVATE)
    val count = prefs.getInt(KEY_URGENT_COUNT, 0)
    val summary = prefs.getString(KEY_URGENT_SUMMARY, "").orEmpty()
    if (count > 0 && summary.isNotBlank()) {
      showExpiryNotification(context, summary, count)
    }
  }
}

fun isExpiryReminderEnabled(context: Context): Boolean {
  return context.applicationContext
    .getSharedPreferences(PREFS_REMINDER, Context.MODE_PRIVATE)
    .getBoolean(KEY_REMINDER_ENABLED, false)
}

fun setExpiryReminderEnabled(context: Context, enabled: Boolean) {
  context.applicationContext
    .getSharedPreferences(PREFS_REMINDER, Context.MODE_PRIVATE)
    .edit()
    .putBoolean(KEY_REMINDER_ENABLED, enabled)
    .apply()
  if (enabled) {
    scheduleDailyExpiryReminder(context)
  } else {
    cancelDailyExpiryReminder(context)
  }
}

fun updateCachedExpiringPantrySummary(context: Context, pantryItems: List<PantryItem>) {
  val urgent =
    pantryItems
      .filter { it.expiringSoon }
      .sortedBy { it.daysUntilExpiry }
  val summary =
    if (urgent.isEmpty()) ""
    else urgent.take(3).joinToString(", ") { "${it.name} (H-${it.daysUntilExpiry})" }
  context.applicationContext
    .getSharedPreferences(PREFS_REMINDER, Context.MODE_PRIVATE)
    .edit()
    .putInt(KEY_URGENT_COUNT, urgent.size)
    .putString(KEY_URGENT_SUMMARY, summary)
    .apply()
}

fun hasNotificationPermission(context: Context): Boolean {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED
  } else {
    true
  }
}

fun ensureExpiryNotificationChannel(context: Context) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val channel =
      NotificationChannel(
        CHANNEL_EXPIRY_ALERTS,
        "Pengingat Bahan Hampir Basi",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Notifikasi harian untuk bahan dapur yang mendekati kedaluwarsa."
      }
    manager?.createNotificationChannel(channel)
  }
}

fun showExpiryNotification(
  context: Context,
  urgentSummary: String,
  urgentCount: Int,
): Boolean {
  if (!hasNotificationPermission(context)) return false
  ensureExpiryNotificationChannel(context)

  val openAppIntent =
    Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
  val contentPendingIntent =
    PendingIntent.getActivity(
      context,
      0,
      openAppIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  val title =
    if (urgentCount > 0) "$urgentCount Bahan Dapur Hampir Kedaluwarsa!"
    else "Cek Stok Dapur Anda Hari Ini"
  val body =
    if (urgentSummary.isNotBlank()) {
      "Segera olah: $urgentSummary. Ketuk untuk lihat menu 100% cocok dari AI Matcher."
    } else {
      "Belum ada bahan yang mendesak basi. Yuk rencanakan menu harian di Kitchen Solution!"
    }

  val notification =
    NotificationCompat.Builder(context, CHANNEL_EXPIRY_ALERTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(contentPendingIntent)
      .build()

  return try {
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPIRY, notification)
    true
  } catch (_: SecurityException) {
    false
  }
}

fun scheduleDailyExpiryReminder(context: Context) {
  ensureExpiryNotificationChannel(context)
  val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
  val intent = Intent(context.applicationContext, ExpiryReminderReceiver::class.java)
  val pendingIntent =
    PendingIntent.getBroadcast(
      context.applicationContext,
      REQUEST_CODE_ALARM,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  val triggerCal =
    Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 8)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

  try {
    alarmManager.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      triggerCal.timeInMillis,
      AlarmManager.INTERVAL_DAY,
      pendingIntent,
    )
  } catch (_: Exception) {
    // Ignore on restricted environments
  }
}

fun cancelDailyExpiryReminder(context: Context) {
  val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
  val intent = Intent(context.applicationContext, ExpiryReminderReceiver::class.java)
  val pendingIntent =
    PendingIntent.getBroadcast(
      context.applicationContext,
      REQUEST_CODE_ALARM,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  try {
    alarmManager.cancel(pendingIntent)
  } catch (_: Exception) {}
}

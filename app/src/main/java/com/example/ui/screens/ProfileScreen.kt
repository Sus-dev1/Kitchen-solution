package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.UserProfile
import com.example.notifications.hasNotificationPermission
import com.example.notifications.isExpiryReminderEnabled
import com.example.notifications.setExpiryReminderEnabled
import com.example.notifications.showExpiryNotification
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Rose500

@Composable
fun ProfileScreen(
  profile: UserProfile?,
  isOnline: Boolean = true,
  urgentExpiringCount: Int = 0,
  urgentExpiringSummary: String = "",
  onSavePreferences: (String, String, String, Int) -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  var showEditDialog by remember { mutableStateOf(false) }
  var reminderEnabled by remember { mutableStateOf(isExpiryReminderEnabled(context)) }
  var notificationStatusMessage by remember { mutableStateOf<String?>(null) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        reminderEnabled = true
        setExpiryReminderEnabled(context, true)
        notificationStatusMessage = "Pengingat harian jam 08:00 aktif!"
      } else {
        reminderEnabled = false
        setExpiryReminderEnabled(context, false)
        notificationStatusMessage = "Izin notifikasi ditolak di pengaturan perangkat."
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          shape = CircleShape,
          color = Emerald700,
          border = androidx.compose.foundation.BorderStroke(4.dp, Emerald100),
          shadowElevation = 4.dp,
          modifier = Modifier.size(84.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Restaurant,
              contentDescription = "Avatar Chef",
              tint = Color.White,
              modifier = Modifier.size(38.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = profile?.displayName ?: "Chef Alex Wibowo",
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = profile?.email?.ifBlank { "Akun Google Tersinkron" } ?: "Akun Google Tersinkron",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Preferensi Dapur & Diet",
              style = MaterialTheme.typography.titleSmall,
            )
            TextButton(onClick = { showEditDialog = true }) {
              Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Ubah")
            }
          }

          HorizontalDivider()

          PreferenceRow(
            label = "Jenis Diet",
            value = profile?.dietType ?: "Halal / General",
            highlight = true,
          )
          PreferenceRow(
            label = "Alergi Makanan",
            value = profile?.allergies?.ifBlank { "Tidak ada" } ?: "Kacang Tanah",
          )
          PreferenceRow(
            label = "Porsi Standar",
            value = "${profile?.defaultServings ?: 2} Orang",
          )
          PreferenceRow(
            label = "Sesi Login",
            value = "Tetap Masuk Otomatis",
            highlight = true,
          )
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Pengingat & Sinkronisasi Offline",
            style = MaterialTheme.typography.titleSmall,
          )
          HorizontalDivider()
          PreferenceRow(
            label = "Status Koneksi",
            value =
              if (isOnline) "Online • Sinkron Cloud"
              else "Mode Offline • Cache Lokal",
            highlight = isOnline,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Notifikasi Bahan Hampir Basi",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Pengingat harian otomatis pukul 08:00",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = reminderEnabled,
              onCheckedChange = { enable ->
                if (enable) {
                  if (hasNotificationPermission(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    reminderEnabled = true
                    setExpiryReminderEnabled(context, true)
                    notificationStatusMessage = "Pengingat harian jam 08:00 aktif!"
                  } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                } else {
                  reminderEnabled = false
                  setExpiryReminderEnabled(context, false)
                  notificationStatusMessage = "Pengingat harian dimatikan."
                }
              },
              colors = SwitchDefaults.colors(checkedTrackColor = Emerald700),
              modifier = Modifier.testTag("expiry_reminder_switch"),
            )
          }

          if (notificationStatusMessage != null) {
            Text(
              text = notificationStatusMessage.orEmpty(),
              style = MaterialTheme.typography.labelSmall,
              color = Emerald700,
            )
          }

          OutlinedButton(
            onClick = {
              if (hasNotificationPermission(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val sent = showExpiryNotification(context, urgentExpiringSummary, urgentExpiringCount)
                notificationStatusMessage =
                  if (sent) "Notifikasi pengingat berhasil dikirim!"
                  else "Gagal mengirim notifikasi."
              } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("test_expiry_notification_button"),
          ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tes Notifikasi Sekarang")
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onSignOut,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.sign_out), fontWeight = FontWeight.Bold)
      }
    }
  }

  if (showEditDialog) {
    EditPreferencesDialog(
      initialProfile = profile,
      onDismiss = { showEditDialog = false },
      onSave = { name, diet, allergies, servings ->
        onSavePreferences(name, diet, allergies, servings)
        showEditDialog = false
      },
    )
  }
}

@Composable
private fun PreferenceRow(
  label: String,
  value: String,
  highlight: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (highlight) {
      Surface(shape = RoundedCornerShape(8.dp), color = Emerald50) {
        Text(
          text = value,
          style = MaterialTheme.typography.labelLarge,
          color = Emerald700,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    } else {
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun EditPreferencesDialog(
  initialProfile: UserProfile?,
  onDismiss: () -> Unit,
  onSave: (String, String, String, Int) -> Unit,
) {
  var name by remember { mutableStateOf(initialProfile?.displayName ?: "Chef Alex") }
  var diet by remember { mutableStateOf(initialProfile?.dietType ?: "Halal / General") }
  var allergies by remember { mutableStateOf(initialProfile?.allergies ?: "Kacang Tanah") }
  var servings by remember { mutableIntStateOf(initialProfile?.defaultServings ?: 2) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Ubah Preferensi Dapur", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nama Panggilan") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = diet,
          onValueChange = { diet = it },
          label = { Text("Jenis Diet") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = allergies,
          onValueChange = { allergies = it },
          label = { Text("Alergi Makanan") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Porsi Standar", style = MaterialTheme.typography.bodyMedium)
          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (servings > 1) servings-- }) { Text("-") }
            Text("$servings Orang", fontWeight = FontWeight.Bold)
            TextButton(onClick = { if (servings < 20) servings++ }) { Text("+") }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name, diet, allergies, servings) },
        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Batal") }
    },
  )
}

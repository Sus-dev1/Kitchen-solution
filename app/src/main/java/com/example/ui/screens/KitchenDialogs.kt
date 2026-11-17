package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.model.SubstitutionAnalysis
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950
import java.io.File

@Composable
fun ScanAndAddStockDialog(
  isLoading: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onScanBitmap: (Bitmap) -> Unit,
  onScanText: (String) -> Unit,
  onAddManual: (name: String, quantity: Int, unit: String, location: String, days: Int) -> Unit,
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var cameraPermissionNotice by remember { mutableStateOf<String?>(null) }

  // Manual form state
  var itemName by remember { mutableStateOf("") }
  var itemQty by remember { mutableStateOf("1") }
  var itemUnit by remember { mutableStateOf("pcs") }
  var itemLocation by remember { mutableStateOf("Kulkas (Fridge)") }
  var itemDays by remember { mutableStateOf("7") }

  // Text receipt state
  var receiptText by remember { mutableStateOf("1 kg Beras, 1 L Minyak Goreng, 250 g Wortel") }

  // Zero-permission Android Photo Picker (Play Policy compliant)
  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      if (uri != null) {
        val bitmap = decodeScaledBitmap(context, uri)
        if (bitmap != null) {
          onScanBitmap(bitmap)
        }
      }
    }

  val cameraCaptureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
      val targetUri = pendingCameraUri
      if (captured && targetUri != null) {
        val bitmap = decodeScaledBitmap(context, targetUri)
        if (bitmap != null) {
          onScanBitmap(bitmap)
        }
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        cameraPermissionNotice = null
        val uri = createTempReceiptImageUri(context)
        pendingCameraUri = uri
        if (uri != null) {
          cameraCaptureLauncher.launch(uri)
        }
      } else {
        cameraPermissionNotice =
          "Izin kamera ditolak. Anda masih bisa memilih foto struk dari galeri atau menempel teks."
      }
    }

  AlertDialog(
    onDismissRequest = { if (!isLoading) onDismiss() },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Emerald700)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Tambah / Scan Struk", style = MaterialTheme.typography.titleMedium)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TabRow(selectedTabIndex = selectedTab) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Scan AI") },
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Input Manual") },
          )
        }

        val combinedError = errorMessage ?: cameraPermissionNotice
        if (combinedError != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = combinedError,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        if (selectedTab == 0) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Emerald50,
            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(30.dp),
              )
              Text(
                text = "Foto langsung struk belanja atau pilih dari galeri agar otomatis diekstrak ke Smart Pantry.",
                style = MaterialTheme.typography.bodySmall,
                color = Emerald950,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
                  onClick = {
                    val hasPerm =
                      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                      cameraPermissionNotice = null
                      val uri = createTempReceiptImageUri(context)
                      pendingCameraUri = uri
                      if (uri != null) {
                        cameraCaptureLauncher.launch(uri)
                      }
                    } else {
                      cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                  },
                  enabled = !isLoading,
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = Emerald700,
                      contentColor = Color.White,
                    ),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("take_receipt_camera"),
                ) {
                  Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Kamera")
                }

                OutlinedButton(
                  onClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  enabled = !isLoading,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("pick_receipt_photo"),
                ) {
                  Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Galeri")
                }
              }
            }
          }

          HorizontalDivider()

          Text(
            text = "Atau tempel teks daftar belanja / struk:",
            style = MaterialTheme.typography.labelMedium,
          )
          OutlinedTextField(
            value = receiptText,
            onValueChange = { receiptText = it },
            minLines = 2,
            maxLines = 4,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
          )
          Button(
            onClick = { onScanText(receiptText) },
            enabled = !isLoading && receiptText.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("scan_text_submit"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Menganalisis Struk...")
            } else {
              Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Ekstrak ke Smart Pantry")
            }
          }
        } else {
          OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Nama Bahan") },
            placeholder = { Text("Contoh: Wortel, Bawang Bombay") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = itemQty,
              onValueChange = { itemQty = it.filter { ch -> ch.isDigit() } },
              label = { Text("Jumlah") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              enabled = !isLoading,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = itemUnit,
              onValueChange = { itemUnit = it },
              label = { Text("Satuan") },
              placeholder = { Text("g / pcs / kg") },
              singleLine = true,
              enabled = !isLoading,
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = itemLocation,
              onValueChange = { itemLocation = it },
              label = { Text("Lokasi") },
              singleLine = true,
              enabled = !isLoading,
              modifier = Modifier.weight(1.4f),
            )
            OutlinedTextField(
              value = itemDays,
              onValueChange = { itemDays = it.filter { ch -> ch.isDigit() } },
              label = { Text("Exp (Hari)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              enabled = !isLoading,
              modifier = Modifier.weight(1f),
            )
          }
          Button(
            onClick = {
              onAddManual(
                itemName,
                itemQty.toIntOrNull() ?: 1,
                itemUnit,
                itemLocation,
                itemDays.toIntOrNull() ?: 7,
              )
            },
            enabled = !isLoading && itemName.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("manual_add_stock_submit"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan ke Pantry")
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Tutup") }
    },
  )
}

@Composable
fun SubstitutionDialog(
  initialIngredient: String,
  initialRecipeContext: String,
  isLoading: Boolean,
  analysis: SubstitutionAnalysis?,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onAskSubstitution: (ingredient: String, recipeContext: String) -> Unit,
  onAddShoppingItem: (String) -> Unit,
) {
  var ingredientInput by remember(initialIngredient) { mutableStateOf(initialIngredient) }
  var recipeInput by remember(initialRecipeContext) { mutableStateOf(initialRecipeContext) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Emerald700)
        Spacer(modifier = Modifier.width(8.dp))
        Text("AI Substitusi Akurat", style = MaterialTheme.typography.titleMedium)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Cari pengganti bahan dengan rasio takaran tepat, profil rasa, dan cek otomatis terhadap stok kulkas Anda.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = ingredientInput,
          onValueChange = { ingredientInput = it },
          label = { Text("Bahan yang Kurang / Diganti") },
          placeholder = { Text("Contoh: Santan Kental, Mentega, Kecap Ikan") },
          singleLine = true,
          enabled = !isLoading,
          modifier = Modifier.fillMaxWidth().testTag("sub_ingredient_input"),
        )
        OutlinedTextField(
          value = recipeInput,
          onValueChange = { recipeInput = it },
          label = { Text("Untuk Masakan (Opsional)") },
          placeholder = { Text("Contoh: Rendang, Kue Bolu, Tumisan") },
          singleLine = true,
          enabled = !isLoading,
          modifier = Modifier.fillMaxWidth().testTag("sub_recipe_input"),
        )
        Button(
          onClick = { onAskSubstitution(ingredientInput, recipeInput) },
          enabled = !isLoading && ingredientInput.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sub_submit_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Menganalisis Alternatif...")
          } else {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cari Pengganti Akurat")
          }
        }

        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        if (analysis != null) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Emerald50,
            border = BorderStroke(1.dp, Emerald100),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Strategi Chef AI untuk ${analysis.originalIngredient}:",
                style = MaterialTheme.typography.labelMedium,
                color = Emerald800,
                fontWeight = FontWeight.Bold,
              )
              Text(text = analysis.summary, style = MaterialTheme.typography.bodySmall, color = Emerald950)
            }
          }

          analysis.options.forEach { opt ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = opt.substituteName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                  )
                  Surface(
                    shape = CircleShape,
                    color = if (opt.matchScore >= 85) Emerald100 else Amber100,
                  ) {
                    Text(
                      text = "${opt.matchScore}% Akurat",
                      style = MaterialTheme.typography.labelSmall,
                      color = if (opt.matchScore >= 85) Emerald800 else Amber800,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }

                Text(
                  text = "Takaran: ${opt.conversionRatio}",
                  style = MaterialTheme.typography.labelMedium,
                  color = Emerald700,
                )
                Text(
                  text = opt.flavorImpact,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (opt.inPantry) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(15.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Tersedia di Pantry",
                        style = MaterialTheme.typography.labelSmall,
                        color = Emerald800,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  } else {
                    TextButton(
                      onClick = { onAddShoppingItem(opt.substituteName) },
                      modifier = Modifier.padding(0.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("+ Daftar Belanja", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Selesai") }
    },
  )
}

@Suppress("DEPRECATION")
private fun decodeScaledBitmap(context: android.content.Context, uri: Uri): Bitmap? {
  return try {
    val raw =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
          decoder.isMutableRequired = true
        }
      } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
      }
    val maxDim = 1280
    if (raw.width > maxDim || raw.height > maxDim) {
      val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
      Bitmap.createScaledBitmap(
        raw,
        (raw.width * ratio).toInt().coerceAtLeast(1),
        (raw.height * ratio).toInt().coerceAtLeast(1),
        true,
      )
    } else {
      raw
    }
  } catch (_: Exception) {
    null
  }
}

private fun createTempReceiptImageUri(context: Context): Uri? {
  return try {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "receipt_${System.currentTimeMillis()}.jpg")
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
  } catch (_: Exception) {
    null
  }
}

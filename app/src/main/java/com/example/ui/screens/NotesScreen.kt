package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.JournalNote
import com.example.model.NotesTab
import com.example.model.ShoppingItem
import com.example.ui.components.shareShoppingList
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import java.util.Locale

@Composable
fun NotesScreen(
  activeTab: NotesTab,
  shoppingItems: List<ShoppingItem>,
  journalNotes: List<JournalNote>,
  onSelectTab: (NotesTab) -> Unit,
  onAddShoppingItem: (String) -> Unit,
  onToggleShoppingItem: (ShoppingItem) -> Unit,
  onDeleteShoppingItem: (ShoppingItem) -> Unit,
  onAddJournalNote: (String, String, Double) -> Unit,
  onDeleteJournalNote: (JournalNote) -> Unit,
) {
  val context = LocalContext.current
  var shoppingInput by remember { mutableStateOf("") }
  var showJournalDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Column {
        Text(
          text = stringResource(R.string.notes_title),
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = stringResource(R.string.notes_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item {
      Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        TabRow(
          selectedTabIndex = if (activeTab == NotesTab.SHOPPING) 0 else 1,
          containerColor = Color.Transparent,
        ) {
          Tab(
            selected = activeTab == NotesTab.SHOPPING,
            onClick = { onSelectTab(NotesTab.SHOPPING) },
            text = { Text("Daftar Belanja (${shoppingItems.size})") },
            modifier = Modifier.testTag("tab_shopping"),
          )
          Tab(
            selected = activeTab == NotesTab.JOURNAL,
            onClick = { onSelectTab(NotesTab.JOURNAL) },
            text = { Text("Jurnal Resep (${journalNotes.size})") },
            modifier = Modifier.testTag("tab_journal"),
          )
        }
      }
    }

    if (activeTab == NotesTab.SHOPPING) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = shoppingInput,
            onValueChange = { shoppingInput = it },
            placeholder = { Text("+ Tambah belanjaan...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).testTag("shopping_input"),
          )
          Button(
            onClick = {
              val clean = shoppingInput.trim()
              if (clean.isNotEmpty()) {
                onAddShoppingItem(clean)
                shoppingInput = ""
              }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
            modifier = Modifier.height(54.dp).testTag("shopping_add_button"),
          ) {
            Text("Tambah")
          }
        }
      }

      if (shoppingItems.isNotEmpty()) {
        item {
          OutlinedButton(
            onClick = { shareShoppingList(context, shoppingItems) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_shopping_button"),
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bagikan Daftar Belanja (${shoppingItems.size} item)")
          }
        }
      }

      if (shoppingItems.isEmpty()) {
        item {
          EmptyNotesPlaceholder(
            icon = Icons.Filled.ShoppingBasket,
            title = "Daftar belanja kosong",
            subtitle = "Tambahkan bahan secara manual atau langsung dari resep yang kurang bahannya.",
          )
        }
      } else {
        items(shoppingItems, key = { it.id.ifBlank { it.name } }) { item ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Checkbox(
                  checked = item.checked,
                  onCheckedChange = { onToggleShoppingItem(item) },
                  colors = CheckboxDefaults.colors(checkedColor = Emerald700),
                )
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.bodyMedium,
                  textDecoration =
                    if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                  color =
                    if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = Emerald100) {
                  Text(
                    text = item.sourceTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald800,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
                IconButton(
                  onClick = { onDeleteShoppingItem(item) },
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Hapus ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }
    } else {
      item {
        Button(
          onClick = { showJournalDialog = true },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_journal_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tulis Catatan Jurnal Resep")
        }
      }

      if (journalNotes.isEmpty()) {
        item {
          EmptyNotesPlaceholder(
            icon = Icons.Filled.Book,
            title = "Belum ada jurnal resep",
            subtitle = "Catat takaran bumbu rahasia atau modifikasi masakan favorit Anda di sini.",
          )
        }
      } else {
        items(journalNotes, key = { it.id.ifBlank { it.recipeTitle } }) { note ->
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = note.recipeTitle,
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Amber500,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = String.format(Locale.US, "%.1f", note.rating),
                    style = MaterialTheme.typography.labelLarge,
                    color = Amber500,
                    fontWeight = FontWeight.Bold,
                  )
                  IconButton(
                    onClick = { onDeleteJournalNote(note) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = "Hapus catatan",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "\"${note.note}\"",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(10.dp),
                )
              }

              Text(
                text = "Dicatat: ${note.dateLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showJournalDialog) {
    AddJournalNoteDialog(
      onDismiss = { showJournalDialog = false },
      onSave = { title, body, rating ->
        onAddJournalNote(title, body, rating)
        showJournalDialog = false
      },
    )
  }
}

@Composable
private fun EmptyNotesPlaceholder(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(icon, contentDescription = null, tint = Emerald700, modifier = Modifier.size(34.dp))
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun AddJournalNoteDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, Double) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var noteText by remember { mutableStateOf("") }
  var rating by remember { mutableFloatStateOf(4.8f) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Catatan Jurnal Resep", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Nama Resep") },
          placeholder = { Text("Contoh: Ayam Tumis Kecap V2") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Catatan / Tips Bumbu") },
          placeholder = { Text("Tambahkan 1/2 sdt lada hitam...") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        Column {
          Text(
            text = "Rating: ${String.format(Locale.US, "%.1f", rating)} / 5.0",
            style = MaterialTheme.typography.labelMedium,
          )
          Slider(
            value = rating,
            onValueChange = { rating = it },
            valueRange = 1f..5f,
            steps = 7,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, noteText, rating.toDouble()) },
        enabled = title.isNotBlank() && noteText.isNotBlank(),
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

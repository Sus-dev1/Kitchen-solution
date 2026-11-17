package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PantryItem
import com.example.ui.theme.Amber100
import com.example.ui.theme.Amber50
import com.example.ui.theme.Amber800
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Purple100
import com.example.ui.theme.Purple700
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose800
import com.example.ui.theme.Sky100
import com.example.ui.theme.Sky700

private enum class PantrySortMode(val label: String) {
  EXPIRY("Cepat Basi"),
  LOW_STOCK("Stok Tipis"),
  NAME_ASC("Nama A-Z"),
}

private enum class PantryLocationFilter(val label: String, val keyword: String) {
  ALL("Semua", ""),
  FRIDGE("Kulkas", "kulkas"),
  FREEZER("Freezer", "freezer"),
  ROOM("Suhu Ruang", "ruang"),
  RACK("Rak Dapur", "rak"),
}

@Composable
fun PantryScreen(
  pantryItems: List<PantryItem>,
  onAdjustQty: (PantryItem, Int) -> Unit,
  onDeleteItem: (PantryItem) -> Unit,
  onOpenAddOrScan: () -> Unit,
) {
  val totalItems = pantryItems.size
  val expiringSoonCount = remember(pantryItems) { pantryItems.count { it.expiringSoon } }
  val lowStockCount = remember(pantryItems) { pantryItems.count { it.lowStock } }

  var searchQuery by remember { mutableStateOf("") }
  var selectedLocation by remember { mutableStateOf(PantryLocationFilter.ALL) }
  var sortMode by remember { mutableStateOf(PantrySortMode.EXPIRY) }

  val filteredItems =
    remember(pantryItems, searchQuery, selectedLocation, sortMode) {
      val base =
        pantryItems.filter { item ->
          val matchesQuery =
            searchQuery.isBlank() ||
              item.name.contains(searchQuery, ignoreCase = true) ||
              item.location.contains(searchQuery, ignoreCase = true)
          val matchesLocation =
            selectedLocation == PantryLocationFilter.ALL ||
              item.location.contains(selectedLocation.keyword, ignoreCase = true)
          matchesQuery && matchesLocation
        }
      when (sortMode) {
        PantrySortMode.EXPIRY ->
          base.sortedWith(compareBy<PantryItem> { it.daysUntilExpiry }.thenBy { it.name })
        PantrySortMode.LOW_STOCK ->
          base.sortedWith(compareBy<PantryItem> { !it.lowStock }.thenBy { it.quantity }.thenBy { it.name })
        PantrySortMode.NAME_ASC -> base.sortedBy { it.name.lowercase() }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.pantry_title),
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = stringResource(R.string.pantry_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onOpenAddOrScan,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
          modifier = Modifier.height(48.dp).testTag("pantry_add_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Tambah Stok", style = MaterialTheme.typography.labelLarge)
        }
      }
    }

    // Dynamic Summary Stats (fixes hardcoded 12 / 2 / 1 bug in HTML prototype)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        PantryStatCard(
          label = "TOTAL STOK",
          value = "$totalItems Item",
          containerColor = MaterialTheme.colorScheme.surface,
          borderColor = MaterialTheme.colorScheme.outline,
          labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
          valueColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        PantryStatCard(
          label = "HAMPIR BASI",
          value = "$expiringSoonCount Item",
          containerColor = Amber50,
          borderColor = Amber100,
          labelColor = Amber800,
          valueColor = Amber800,
          modifier = Modifier.weight(1f),
        )
        PantryStatCard(
          label = "STOK MENIPIS",
          value = "$lowStockCount Item",
          containerColor = Rose50,
          borderColor = Rose100,
          labelColor = Rose800,
          valueColor = Rose800,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Cari nama bahan di Smart Pantry...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Filled.Close, contentDescription = "Bersihkan pencarian")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("pantry_search_input"),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(PantryLocationFilter.entries.size) { index ->
            val loc = PantryLocationFilter.entries[index]
            FilterChip(
              selected = selectedLocation == loc,
              onClick = { selectedLocation = loc },
              label = { Text(loc.label, style = MaterialTheme.typography.labelSmall) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = Emerald700,
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          PantrySortMode.entries.forEach { mode ->
            FilterChip(
              selected = sortMode == mode,
              onClick = { sortMode = mode },
              label = { Text("Urut: ${mode.label}", style = MaterialTheme.typography.labelSmall) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
          }
        }
      }
    }

    if (filteredItems.isEmpty()) {
      item {
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
            Icon(
              imageVector = Icons.Filled.Inventory2,
              contentDescription = null,
              tint = Emerald700,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text =
                if (pantryItems.isEmpty()) "Pantry masih kosong"
                else "Tidak ada bahan yang cocok dengan filter",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text =
                if (pantryItems.isEmpty())
                  "Ketuk \"Tambah Stok\" untuk memindai struk belanjaan atau menambah bahan dapur secara manual."
                else "Coba ubah kata kunci pencarian atau pilih filter lokasi \"Semua\".",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(filteredItems, key = { it.id.ifBlank { it.name } }) { item ->
        PantryItemCard(
          item = item,
          onDecrease = { onAdjustQty(item, -item.stepDelta) },
          onIncrease = { onAdjustQty(item, item.stepDelta) },
          onDelete = { onDeleteItem(item) },
          modifier = Modifier.animateItem(),
        )
      }
    }
  }
}

@Composable
private fun PantryStatCard(
  label: String,
  value: String,
  containerColor: Color,
  borderColor: Color,
  labelColor: Color,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = containerColor,
    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = labelColor,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = valueColor,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun PantryItemCard(
  item: PantryItem,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (icon, bgColor, tintColor) = categoryVisuals(item.category)
  val isUrgent = item.expiringSoon

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = bgColor,
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = item.name,
              tint = tintColor,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = item.name,
              style = MaterialTheme.typography.titleSmall,
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isUrgent) Rose100 else Emerald100,
            ) {
              Text(
                text =
                  if (isUrgent) "H-${item.daysUntilExpiry} Expired"
                  else "Aman (H-${item.daysUntilExpiry})",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUrgent) Rose800 else Emerald700,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Lokasi: ${item.location}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
          onClick = onDecrease,
          colors =
            IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().size(36.dp),
        ) {
          Icon(Icons.Filled.Remove, contentDescription = "Kurangi ${item.name}", modifier = Modifier.size(16.dp))
        }
        Text(
          text = "${item.quantity} ${item.unit}",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.width(64.dp),
        )
        FilledTonalIconButton(
          onClick = onIncrease,
          colors =
            IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().size(36.dp),
        ) {
          Icon(Icons.Filled.Add, contentDescription = "Tambah ${item.name}", modifier = Modifier.size(16.dp))
        }
        IconButton(
          onClick = onDelete,
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

private fun categoryVisuals(category: String): Triple<ImageVector, Color, Color> =
  when (category.lowercase()) {
    "protein" -> Triple(Icons.Filled.SetMeal, Rose100, Rose800)
    "egg" -> Triple(Icons.Filled.Egg, Emerald100, Emerald700)
    "spice", "vegetable" -> Triple(Icons.Filled.Spa, Purple100, Purple700)
    "dairy" -> Triple(Icons.Filled.Kitchen, Sky100, Sky700)
    "sauce" -> Triple(Icons.Filled.LocalDrink, Amber100, Amber800)
    "grain" -> Triple(Icons.Filled.Grain, Amber100, Amber800)
    else -> Triple(Icons.Filled.Restaurant, Emerald100, Emerald700)
  }

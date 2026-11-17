package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.PantryItem
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald950

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
  messages: List<ChatMessage>,
  pantryItems: List<PantryItem>,
  isLoading: Boolean,
  errorMessage: String?,
  onSendMessage: (String) -> Unit,
  onClearChat: () -> Unit,
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val activeStockCount = remember(pantryItems) { pantryItems.count { it.quantity > 0 } }

  LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val starterPrompts = remember {
    listOf(
      "Masak apa dari stok kulkas saya?",
      "Info gizi & kalori Dada Ayam 100g",
      "Cara menyimpan Bawang Putih agar awet",
      "Tips masak telur agar tidak lengket",
    )
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Top Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Surface(shape = CircleShape, color = Emerald100, modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = null,
                tint = Emerald700,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = stringResource(R.string.chat_title),
                style = MaterialTheme.typography.titleMedium,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(shape = CircleShape, color = Emerald100) {
                Text(
                  text = "$activeStockCount stok aktif",
                  style = MaterialTheme.typography.labelSmall,
                  color = Emerald800,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
            Text(
              text = stringResource(R.string.chat_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (messages.isNotEmpty()) {
          IconButton(
            onClick = onClearChat,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_chat_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteSweep,
              contentDescription = "Bersihkan Obrolan",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Chat Feed
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (messages.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Emerald50,
            border = BorderStroke(1.dp, Emerald100),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = Emerald700,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Halo! Saya Chef AI Dapurmu",
                  style = MaterialTheme.typography.titleSmall,
                  color = Emerald950,
                  fontWeight = FontWeight.Bold,
                )
              }
              Text(
                text =
                  "Saya mengenal $activeStockCount bahan di Smart Pantry serta preferensi diet & alergi Anda. Tanyakan resep masakan apa saja, teknik memasak, kandungan gizi, hingga cara mengolah bahan tertentu.",
                style = MaterialTheme.typography.bodySmall,
                color = Emerald950,
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                starterPrompts.forEach { prompt ->
                  AssistChip(
                    onClick = { onSendMessage(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelMedium) },
                    colors =
                      AssistChipDefaults.assistChipColors(
                        containerColor = Color.White,
                        labelColor = Emerald800,
                      ),
                  )
                }
              }
            }
          }
        }
      }

      items(messages, key = { it.id }) { msg ->
        ChatBubble(message = msg)
      }

      if (errorMessage != null) {
        val lastUserText = messages.lastOrNull { it.isFromUser }?.text
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
              if (!lastUserText.isNullOrBlank() && !isLoading) {
                androidx.compose.material3.TextButton(
                  onClick = { onSendMessage(lastUserText) },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                  Text("Kirim Ulang Pertanyaan")
                }
              }
            }
          }
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

    // Bottom Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Tanya masakan, bumbu, atau info bahan...") },
          maxLines = 3,
          shape = RoundedCornerShape(24.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions =
            KeyboardActions(
              onSend = {
                if (inputText.isNotBlank() && !isLoading) {
                  val toSend = inputText
                  inputText = ""
                  onSendMessage(toSend)
                }
              }
            ),
          modifier = Modifier.weight(1f).testTag("chat_input"),
        )

        Surface(
          shape = CircleShape,
          color = if (inputText.isNotBlank() && !isLoading) Emerald700 else Emerald100,
        ) {
          IconButton(
            onClick = {
              val toSend = inputText
              inputText = ""
              onSendMessage(toSend)
            },
            enabled = inputText.isNotBlank() && !isLoading,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_send_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Emerald700,
              )
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Kirim Pesan",
                tint = if (inputText.isNotBlank()) Color.White else Emerald700,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
  ) {
    Surface(
      shape =
        RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (message.isFromUser) 18.dp else 4.dp,
          bottomEnd = if (message.isFromUser) 4.dp else 18.dp,
        ),
      color = if (message.isFromUser) Emerald700 else MaterialTheme.colorScheme.surface,
      border =
        if (message.isFromUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      shadowElevation = 1.dp,
      modifier = Modifier.widthIn(max = 520.dp),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (!message.isFromUser) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = Emerald600,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (message.isStreaming) "Chef AI sedang mengetik..." else "Chef AI",
              style = MaterialTheme.typography.labelSmall,
              color = Emerald700,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (message.text.isBlank() && message.isStreaming) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = Emerald600,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Menyiapkan jawaban kuliner...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (message.isFromUser) Color.White else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

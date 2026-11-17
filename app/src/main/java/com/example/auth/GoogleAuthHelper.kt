package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Auth"
private const val AUTH_PREFS_NAME = "kitchen_auth_prefs"
private const val KEY_KEEP_SIGNED_IN = "keep_signed_in"

internal fun markSessionActive(context: Context) {
  context.applicationContext
    .getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    .edit()
    .putBoolean(KEY_KEEP_SIGNED_IN, true)
    .apply()
}

internal fun clearSessionFlag(context: Context) {
  context.applicationContext
    .getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    .edit()
    .putBoolean(KEY_KEEP_SIGNED_IN, false)
    .apply()
}

internal fun shouldKeepSignedIn(context: Context): Boolean =
  context.applicationContext
    .getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    .getBoolean(KEY_KEEP_SIGNED_IN, false)

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private suspend fun tryGoogleIdCredentialSignIn(
  context: Context,
  credentialManager: CredentialManager,
  clientId: String,
  filterByAuthorizedAccounts: Boolean,
  timeoutMillis: Long,
): Boolean {
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  val activityContext = context.findActivity() ?: context
  val cred =
    withTimeoutOrNull(timeoutMillis) { credentialManager.getCredential(activityContext, request) }
      ?.credential ?: return false
  if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    markSessionActive(context)
    return true
  }
  return false
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    markSessionActive(context)
    onAuthSuccess()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return
    }
  val previouslySignedIn = shouldKeepSignedIn(context)
  scope.launch {
    try {
      // Stage 1: Silent login with previously authorized Credential Manager accounts
      val stage1Success =
        try {
          tryGoogleIdCredentialSignIn(
            context = context,
            credentialManager = credentialManager,
            clientId = clientId,
            filterByAuthorizedAccounts = true,
            timeoutMillis = 5_000L,
          )
        } catch (_: Exception) {
          false
        }
      if (stage1Success) {
        onAuthSuccess()
        return@launch
      }

      // Stage 2: If user already logged in previously and did not sign out, auto-restore via Google account on device
      if (previouslySignedIn) {
        val stage2Success =
          try {
            tryGoogleIdCredentialSignIn(
              context = context,
              credentialManager = credentialManager,
              clientId = clientId,
              filterByAuthorizedAccounts = false,
              timeoutMillis = 8_000L,
            )
          } catch (_: Exception) {
            false
          }
        if (stage2Success) {
          onAuthSuccess()
          return@launch
        }
      }

      onUnauthenticated()
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onAuthError("Konfigurasi Google Sign-In tidak ditemukan.")
      return
    }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val activityContext = context.findActivity() ?: context
      val cred = credentialManager.getCredential(activityContext, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        markSessionActive(context)
        onAuthSuccess()
      } else {
        onAuthError("Tipe kredensial tidak valid.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Gagal masuk dengan Google.")
    }
  }
}

fun signOut(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  clearSessionFlag(context)
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

@Composable
fun SessionRestoringScreen() {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Emerald950, Emerald900, MaterialTheme.colorScheme.background)
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.padding(24.dp),
    ) {
      Surface(shape = CircleShape, color = Emerald700, modifier = Modifier.size(64.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.RestaurantMenu,
            contentDescription = stringResource(R.string.app_name),
            tint = Color.White,
            modifier = Modifier.size(32.dp),
          )
        }
      }
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White,
      )
      CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        color = Emerald100,
        strokeWidth = 3.dp,
      )
      Text(
        text = "Memulihkan sesi akun Anda...",
        style = MaterialTheme.typography.bodySmall,
        color = Emerald100,
      )
    }
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  skipInitialAutoCheck: Boolean = false,
  externalAutoChecking: Boolean = false,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isSigningIn by remember { mutableStateOf(false) }
  var internalAutoChecking by remember(skipInitialAutoCheck) { mutableStateOf(!skipInitialAutoCheck) }
  val isAutoChecking = internalAutoChecking || externalAutoChecking
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Silent auto-login runs quietly in background without locking the primary button
  LaunchedEffect(skipInitialAutoCheck) {
    if (skipInitialAutoCheck) {
      internalAutoChecking = false
      return@LaunchedEffect
    }
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        internalAutoChecking = false
        onAuthSuccess()
      },
      onUnauthenticated = { internalAutoChecking = false },
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Emerald950, Emerald900, MaterialTheme.colorScheme.background)
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 480.dp).fillMaxSize().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Box(modifier = Modifier.fillMaxWidth().height(176.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_kitchen_hero),
              contentDescription = stringResource(R.string.app_name),
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color.Transparent, Emerald950.copy(alpha = 0.88f))
                    )
                  )
            )
            Row(
              modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(shape = CircleShape, color = Emerald700, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Filled.RestaurantMenu,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.headlineSmall,
                  color = Color.White,
                )
                Text(
                  text = "Masuk 1 Ketuk • Tanpa Password",
                  style = MaterialTheme.typography.labelMedium,
                  color = Emerald100,
                )
              }
            }
          }

          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.auth_tagline),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            QuickBenefitRow("Otomatis masuk saat aplikasi dibuka kembali")
            QuickBenefitRow("Resep 100% cocok, Explorer Dunia, & Chef AI Chat")
            QuickBenefitRow("Stok kulkas & daftar belanja aman di akun Google")

            if (isAutoChecking && !isSigningIn) {
              Surface(shape = RoundedCornerShape(10.dp), color = Emerald50, modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Emerald700)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Mengecek sesi Google Anda secara otomatis...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald950,
                  )
                }
              }
            }

            if (errorMessage != null) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = errorMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(10.dp),
                  textAlign = TextAlign.Center,
                )
              }
            }

            Button(
              onClick = {
                errorMessage = null
                isSigningIn = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isSigningIn = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isSigningIn = false
                    errorMessage = msg
                  },
                  scope = scope,
                  onAuthCancelled = { isSigningIn = false },
                )
              },
              enabled = !isSigningIn,
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
              modifier = Modifier.fillMaxWidth().height(54.dp).testTag("google_sign_in_button"),
            ) {
              if (isSigningIn) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = Color.White,
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Membuka Pilihan Akun...", style = MaterialTheme.typography.labelLarge)
              } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.sign_in_with_google),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickBenefitRow(text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = Icons.Filled.CheckCircle,
      contentDescription = null,
      tint = Emerald700,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

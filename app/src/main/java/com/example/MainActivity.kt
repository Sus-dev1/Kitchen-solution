package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.SessionRestoringScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.markSessionActive
import com.example.auth.shouldKeepSignedIn
import com.example.auth.signOut
import com.example.data.KitchenAiService
import com.example.data.KitchenRepository
import com.example.data.observeNetworkConnectivity
import com.example.firebase.configureAppCheck
import com.example.model.MainDestination
import com.example.model.NotesTab
import com.example.model.Recipe
import com.example.model.UiState
import com.example.notifications.updateCachedExpiringPantrySummary
import com.example.ui.KitchenViewModel
import com.example.ui.screens.AiMatcherScreen
import com.example.ui.screens.AssignRecipeToMealPlanDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CookingModeScreen
import com.example.ui.screens.ExplorerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MealPlannerScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SavedRecipesScreen
import com.example.ui.screens.ScanAndAddStockDialog
import com.example.ui.screens.SubstitutionDialog
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.ui.theme.KitchenSolutionTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Required: Configure App Check with launch intent before Compose UI or ViewModels
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      KitchenSolutionTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          KitchenAppRoot()
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    configureAppCheck(this, intent)
  }
}

@Composable
fun KitchenAppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val auth = remember { Firebase.auth }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  LaunchedEffect(user?.uid) {
    if (user != null) {
      markSessionActive(context)
    }
  }

  var isRestoringSession by remember {
    mutableStateOf(auth.currentUser == null && shouldKeepSignedIn(context))
  }
  var startupAutoCheckDone by remember { mutableStateOf(auth.currentUser != null) }

  LaunchedEffect(user, startupAutoCheckDone) {
    if (user == null && !startupAutoCheckDone) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          isRestoringSession = false
          startupAutoCheckDone = true
        },
        onUnauthenticated = {
          isRestoringSession = false
          startupAutoCheckDone = true
        },
        scope = scope,
      )
    } else if (user != null) {
      isRestoringSession = false
    }
  }

  when {
    user != null -> {
      key(user.uid) {
        MainKitchenShell(
          currentUserId = user.uid,
          displayName = user.displayName ?: "Chef Alex",
          email = user.email ?: "",
        )
      }
    }
    isRestoringSession -> SessionRestoringScreen()
    else ->
      AuthScreen(
        onAuthSuccess = {},
        skipInitialAutoCheck = true,
        externalAutoChecking = !startupAutoCheckDone,
      )
  }
}

@Composable
fun MainKitchenShell(
  currentUserId: String,
  displayName: String,
  email: String,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: KitchenViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            try {
              db.firestoreSettings =
                firestoreSettings {
                  setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
            } catch (_: Exception) {
              // Firestore may already be initialized in process
            }
            KitchenViewModel(
              repository = KitchenRepository(db),
              aiService = KitchenAiService(),
              currentUserId = currentUserId,
              initialDisplayName = displayName,
              initialEmail = email,
            )
          }
        },
    )

  val isOnline by observeNetworkConnectivity(context).collectAsStateWithLifecycle(initialValue = true)
  val destination by viewModel.currentDestination.collectAsStateWithLifecycle()
  val notesTab by viewModel.notesTab.collectAsStateWithLifecycle()
  val recipeFilter by viewModel.recipeFilter.collectAsStateWithLifecycle()
  val explorerCategory by viewModel.explorerCategory.collectAsStateWithLifecycle()
  val explorerQuery by viewModel.explorerQuery.collectAsStateWithLifecycle()

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val pantryState by viewModel.pantryState.collectAsStateWithLifecycle()
  val shoppingState by viewModel.shoppingState.collectAsStateWithLifecycle()
  val journalState by viewModel.journalState.collectAsStateWithLifecycle()
  val savedRecipesState by viewModel.savedRecipesState.collectAsStateWithLifecycle()
  val mealPlansState by viewModel.mealPlansState.collectAsStateWithLifecycle()

  val matcherRecipes by viewModel.matcherRecipes.collectAsStateWithLifecycle()
  val explorerRecipes by viewModel.explorerRecipes.collectAsStateWithLifecycle()
  val aiRefreshing by viewModel.aiRefreshing.collectAsStateWithLifecycle()
  val aiErrorMessage by viewModel.aiErrorMessage.collectAsStateWithLifecycle()
  val explorerAiLoading by viewModel.explorerAiLoading.collectAsStateWithLifecycle()
  val explorerAiError by viewModel.explorerAiError.collectAsStateWithLifecycle()
  val explorerAiResultIds by viewModel.explorerAiResultIds.collectAsStateWithLifecycle()

  val showScanDialog by viewModel.showScanDialog.collectAsStateWithLifecycle()
  val scanLoading by viewModel.scanLoading.collectAsStateWithLifecycle()
  val scanErrorMessage by viewModel.scanErrorMessage.collectAsStateWithLifecycle()

  val showSubstitutionDialog by viewModel.showSubstitutionDialog.collectAsStateWithLifecycle()
  val substitutionLoading by viewModel.substitutionLoading.collectAsStateWithLifecycle()
  val substitutionAnalysis by viewModel.substitutionAnalysis.collectAsStateWithLifecycle()
  val substitutionError by viewModel.substitutionError.collectAsStateWithLifecycle()
  val substitutionSeedIngredient by viewModel.substitutionSeedIngredient.collectAsStateWithLifecycle()
  val substitutionSeedRecipe by viewModel.substitutionSeedRecipe.collectAsStateWithLifecycle()

  val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
  val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()
  val chatError by viewModel.chatError.collectAsStateWithLifecycle()

  val cookingSession by viewModel.cookingSession.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(viewModel) {
    viewModel.snackbarEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
  }

  // Handle back button cleanly on secondary screens
  BackHandler(enabled = destination != MainDestination.HOME && cookingSession == null) {
    viewModel.navigateTo(MainDestination.HOME)
  }

  val profile = (profileState as? UiState.Success)?.data
  val pantryItems = (pantryState as? UiState.Success)?.data ?: emptyList()
  val shoppingItems = (shoppingState as? UiState.Success)?.data ?: emptyList()
  val journalNotes = (journalState as? UiState.Success)?.data ?: emptyList()
  val savedRecipes = (savedRecipesState as? UiState.Success)?.data ?: emptyList()
  val mealPlans = (mealPlansState as? UiState.Success)?.data ?: emptyList()

  val savedRecipeModels = remember(savedRecipes) { savedRecipes.map { it.toRecipe() } }
  val savedRecipeTitles =
    remember(savedRecipes) { savedRecipes.map { it.title.trim().lowercase() }.toSet() }
  var recipePendingSchedule by remember { mutableStateOf<Recipe?>(null) }

  val expiringUrgent =
    remember(pantryItems) { pantryItems.filter { it.expiringSoon }.sortedBy { it.daysUntilExpiry } }
  val expiringSummaryText =
    remember(expiringUrgent) {
      expiringUrgent.take(3).joinToString(", ") { "${it.name} (H-${it.daysUntilExpiry})" }
    }
  LaunchedEffect(pantryItems) {
    updateCachedExpiringPantrySummary(context, pantryItems)
  }

  val combinedMatcherPool =
    remember(matcherRecipes, explorerRecipes, savedRecipeModels) {
      (matcherRecipes + explorerRecipes + savedRecipeModels).distinctBy {
        it.title.trim().lowercase()
      }
    }

  val activeCooking = cookingSession
  if (activeCooking != null) {
    CookingModeScreen(
      session = activeCooking,
      onClose = viewModel::closeCookingMode,
      onPrevStep = viewModel::prevCookingStep,
      onNextStep = viewModel::nextCookingStep,
      onToggleTimer = viewModel::toggleTimer,
      onResetTimer = viewModel::resetTimer,
      onToggleVoiceReadout = viewModel::toggleVoiceReadout,
      onDismissDeductDialog = viewModel::dismissDeductDialog,
      onConfirmAutoDeduct = viewModel::confirmAutoDeduct,
    )
    return
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpanded = maxWidth >= 600.dp

    val isHomeActive =
      destination == MainDestination.HOME ||
        destination == MainDestination.AI_MATCHER ||
        destination == MainDestination.EXPLORER ||
        destination == MainDestination.SAVED_RECIPES ||
        destination == MainDestination.MEAL_PLANNER ||
        destination == MainDestination.PANTRY
    val isChatActive = destination == MainDestination.CHAT
    val isNotesActive = destination == MainDestination.NOTES
    val isProfileActive = destination == MainDestination.PROFILE

    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!isExpanded) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            NavigationBarItem(
              selected = isHomeActive,
              onClick = { viewModel.navigateTo(MainDestination.HOME) },
              icon = {
                Icon(
                  imageVector = if (isHomeActive) Icons.Filled.Home else Icons.Outlined.Home,
                  contentDescription = stringResource(R.string.nav_home),
                )
              },
              label = { Text(stringResource(R.string.nav_home)) },
              colors =
                NavigationBarItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
              modifier = Modifier.testTag("nav_home"),
            )
            NavigationBarItem(
              selected = isChatActive,
              onClick = { viewModel.navigateTo(MainDestination.CHAT) },
              icon = {
                Icon(
                  imageVector =
                    if (isChatActive) Icons.AutoMirrored.Filled.Chat
                    else Icons.AutoMirrored.Outlined.Chat,
                  contentDescription = stringResource(R.string.nav_chat),
                )
              },
              label = { Text(stringResource(R.string.nav_chat)) },
              colors =
                NavigationBarItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
              modifier = Modifier.testTag("nav_chat"),
            )
            NavigationBarItem(
              selected = isNotesActive,
              onClick = { viewModel.navigateTo(MainDestination.NOTES, NotesTab.SHOPPING) },
              icon = {
                Icon(
                  imageVector =
                    if (isNotesActive) Icons.AutoMirrored.Filled.StickyNote2
                    else Icons.AutoMirrored.Outlined.StickyNote2,
                  contentDescription = stringResource(R.string.nav_notes),
                )
              },
              label = { Text(stringResource(R.string.nav_notes)) },
              colors =
                NavigationBarItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
              modifier = Modifier.testTag("nav_notes"),
            )
            NavigationBarItem(
              selected = isProfileActive,
              onClick = { viewModel.navigateTo(MainDestination.PROFILE) },
              icon = {
                Icon(
                  imageVector = if (isProfileActive) Icons.Filled.Person else Icons.Outlined.Person,
                  contentDescription = stringResource(R.string.nav_profile),
                )
              },
              label = { Text(stringResource(R.string.nav_profile)) },
              colors =
                NavigationBarItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
              modifier = Modifier.testTag("nav_profile"),
            )
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (isExpanded) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            NavigationRailItem(
              selected = isHomeActive,
              onClick = { viewModel.navigateTo(MainDestination.HOME) },
              icon = {
                Icon(
                  imageVector = if (isHomeActive) Icons.Filled.Home else Icons.Outlined.Home,
                  contentDescription = stringResource(R.string.nav_home),
                )
              },
              label = { Text(stringResource(R.string.nav_home)) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
            )
            NavigationRailItem(
              selected = isChatActive,
              onClick = { viewModel.navigateTo(MainDestination.CHAT) },
              icon = {
                Icon(
                  imageVector =
                    if (isChatActive) Icons.AutoMirrored.Filled.Chat
                    else Icons.AutoMirrored.Outlined.Chat,
                  contentDescription = stringResource(R.string.nav_chat),
                )
              },
              label = { Text(stringResource(R.string.nav_chat)) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
            )
            NavigationRailItem(
              selected = isNotesActive,
              onClick = { viewModel.navigateTo(MainDestination.NOTES, NotesTab.SHOPPING) },
              icon = {
                Icon(
                  imageVector =
                    if (isNotesActive) Icons.AutoMirrored.Filled.StickyNote2
                    else Icons.AutoMirrored.Outlined.StickyNote2,
                  contentDescription = stringResource(R.string.nav_notes),
                )
              },
              label = { Text(stringResource(R.string.nav_notes)) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
            )
            NavigationRailItem(
              selected = isProfileActive,
              onClick = { viewModel.navigateTo(MainDestination.PROFILE) },
              icon = {
                Icon(
                  imageVector = if (isProfileActive) Icons.Filled.Person else Icons.Outlined.Person,
                  contentDescription = stringResource(R.string.nav_profile),
                )
              },
              label = { Text(stringResource(R.string.nav_profile)) },
              colors =
                NavigationRailItemDefaults.colors(
                  selectedIconColor = Emerald700,
                  selectedTextColor = Emerald700,
                  indicatorColor = Emerald100,
                ),
            )
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            when {
              pantryState is UiState.Loading && destination == MainDestination.HOME -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  CircularProgressIndicator(color = Emerald700)
                }
              }

              pantryState is UiState.Error && destination == MainDestination.PANTRY -> {
                Box(
                  modifier = Modifier.fillMaxSize().padding(24.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = (pantryState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                }
              }

              else -> {
                AnimatedContent(
                  targetState = destination,
                  transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 32 }) togetherWith
                      fadeOut(tween(150))
                  },
                  label = "main_dest_transition",
                ) { activeDest ->
                  when (activeDest) {
                  MainDestination.HOME ->
                    HomeScreen(
                      profile = profile,
                      pantryItems = pantryItems,
                      recipes = combinedMatcherPool,
                      isOnline = isOnline,
                      onNavigate = { dest, tab -> viewModel.navigateTo(dest, tab) },
                      onStartCooking = { viewModel.startCookingMode(it) },
                      onOpenScan = viewModel::openScanDialog,
                      onOpenSubstitution = { viewModel.openSubstitutionDialog() },
                    )

                  MainDestination.AI_MATCHER ->
                    AiMatcherScreen(
                      recipes = combinedMatcherPool,
                      pantryItems = pantryItems,
                      savedRecipeTitles = savedRecipeTitles,
                      activeFilter = recipeFilter,
                      isRefreshing = aiRefreshing,
                      aiErrorMessage = aiErrorMessage,
                      onSelectFilter = viewModel::selectRecipeFilter,
                      onRefreshAi = viewModel::refreshAiRecipes,
                      onAddShoppingItem = { name ->
                        viewModel.addToShoppingList(
                          itemName = name,
                          sourceTag = "Auto AI",
                          navigateToNotes = true,
                        )
                      },
                      onStartCooking = { viewModel.startCookingMode(it) },
                      onAskSubstitution = { ingredient, recipeTitle ->
                        viewModel.openSubstitutionDialog(ingredient, recipeTitle)
                        viewModel.askAiSubstitution(ingredient, recipeTitle)
                      },
                      onToggleBookmark = viewModel::toggleSaveRecipe,
                      onScheduleMeal = { recipePendingSchedule = it },
                    )

                  MainDestination.EXPLORER ->
                    ExplorerScreen(
                      recipes = explorerRecipes,
                      pantryItems = pantryItems,
                      savedRecipeTitles = savedRecipeTitles,
                      selectedCategory = explorerCategory,
                      searchQuery = explorerQuery,
                      aiMatchedIds = explorerAiResultIds,
                      isAiSearching = explorerAiLoading,
                      aiErrorMessage = explorerAiError,
                      onSelectCategory = viewModel::selectExplorerCategory,
                      onQueryChange = viewModel::updateExplorerQuery,
                      onSearchWithAi = { viewModel.searchExplorerWithAi() },
                      onBack = { viewModel.navigateTo(MainDestination.HOME) },
                      onAddShoppingItem = { name ->
                        viewModel.addToShoppingList(
                          itemName = name,
                          sourceTag = "Explorer",
                          navigateToNotes = true,
                        )
                      },
                      onStartCooking = { viewModel.startCookingMode(it) },
                      onAskSubstitution = { ingredient, recipeTitle ->
                        viewModel.openSubstitutionDialog(ingredient, recipeTitle)
                        viewModel.askAiSubstitution(ingredient, recipeTitle)
                      },
                      onToggleBookmark = viewModel::toggleSaveRecipe,
                      onScheduleMeal = { recipePendingSchedule = it },
                    )

                  MainDestination.SAVED_RECIPES ->
                    SavedRecipesScreen(
                      savedRecipes = savedRecipes,
                      pantryItems = pantryItems,
                      onBack = { viewModel.navigateTo(MainDestination.HOME) },
                      onAddShoppingItem = { name ->
                        viewModel.addToShoppingList(
                          itemName = name,
                          sourceTag = "Favorit",
                          navigateToNotes = true,
                        )
                      },
                      onStartCooking = { viewModel.startCookingMode(it) },
                      onAskSubstitution = { ingredient, recipeTitle ->
                        viewModel.openSubstitutionDialog(ingredient, recipeTitle)
                        viewModel.askAiSubstitution(ingredient, recipeTitle)
                      },
                      onToggleBookmark = viewModel::toggleSaveRecipe,
                      onScheduleMeal = { recipePendingSchedule = it },
                      onCreateCustomRecipe = viewModel::createCustomRecipe,
                    )

                  MainDestination.MEAL_PLANNER ->
                    MealPlannerScreen(
                      mealPlans = mealPlans,
                      availableRecipes = combinedMatcherPool,
                      pantryItems = pantryItems,
                      onBack = { viewModel.navigateTo(MainDestination.HOME) },
                      onAssignMeal = viewModel::assignMealPlan,
                      onDeleteMeal = viewModel::deleteMealPlan,
                      onSyncWeeklyShopping = {
                        viewModel.syncWeeklyMissingToShoppingList(combinedMatcherPool)
                      },
                      onStartCooking = { viewModel.startCookingMode(it) },
                    )

                  MainDestination.CHAT ->
                    ChatScreen(
                      messages = chatMessages,
                      pantryItems = pantryItems,
                      isLoading = chatLoading,
                      errorMessage = chatError,
                      onSendMessage = viewModel::sendChatMessage,
                      onClearChat = viewModel::clearChat,
                    )

                  MainDestination.PANTRY ->
                    PantryScreen(
                      pantryItems = pantryItems,
                      onAdjustQty = viewModel::adjustPantryQuantity,
                      onDeleteItem = viewModel::deletePantryItem,
                      onOpenAddOrScan = viewModel::openScanDialog,
                    )

                  MainDestination.NOTES ->
                    NotesScreen(
                      activeTab = notesTab,
                      shoppingItems = shoppingItems,
                      journalNotes = journalNotes,
                      onSelectTab = viewModel::selectNotesTab,
                      onAddShoppingItem = { name ->
                        viewModel.addToShoppingList(name, sourceTag = "Manual", navigateToNotes = false)
                      },
                      onToggleShoppingItem = viewModel::toggleShoppingItem,
                      onDeleteShoppingItem = viewModel::deleteShoppingItem,
                      onAddJournalNote = viewModel::addJournalNote,
                      onDeleteJournalNote = viewModel::deleteJournalNote,
                    )

                  MainDestination.PROFILE ->
                    ProfileScreen(
                      profile = profile,
                      isOnline = isOnline,
                      urgentExpiringCount = expiringUrgent.size,
                      urgentExpiringSummary = expiringSummaryText,
                      onSavePreferences = viewModel::updateProfile,
                      onSignOut = { signOut(context, credentialManager, scope) },
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  val pendingSchedule = recipePendingSchedule
  if (pendingSchedule != null) {
    AssignRecipeToMealPlanDialog(
      recipe = pendingSchedule,
      onDismiss = { recipePendingSchedule = null },
      onConfirm = { day, slot ->
        viewModel.assignMealPlan(day, slot, pendingSchedule)
        recipePendingSchedule = null
      },
    )
  }

  if (showScanDialog) {
    ScanAndAddStockDialog(
      isLoading = scanLoading,
      errorMessage = scanErrorMessage,
      onDismiss = viewModel::closeScanDialog,
      onScanBitmap = viewModel::scanReceiptBitmap,
      onScanText = viewModel::scanReceiptText,
      onAddManual = viewModel::addManualPantryItem,
    )
  }

  if (showSubstitutionDialog) {
    SubstitutionDialog(
      initialIngredient = substitutionSeedIngredient,
      initialRecipeContext = substitutionSeedRecipe,
      isLoading = substitutionLoading,
      analysis = substitutionAnalysis,
      errorMessage = substitutionError,
      onDismiss = viewModel::closeSubstitutionDialog,
      onAskSubstitution = viewModel::askAiSubstitution,
      onAddShoppingItem = { name ->
        viewModel.addToShoppingList(name, sourceTag = "Substitusi", navigateToNotes = false)
      },
    )
  }
}

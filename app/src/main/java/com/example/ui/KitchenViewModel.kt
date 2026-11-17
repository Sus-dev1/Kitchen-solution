package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KitchenAiService
import com.example.data.KitchenRepository
import com.example.model.ChatMessage
import com.example.model.ExplorerCategory
import com.example.model.JournalNote
import com.example.model.MainDestination
import com.example.model.MealPlanEntry
import com.example.model.NotesTab
import com.example.model.PantryDeduction
import com.example.model.PantryItem
import com.example.model.ReceiptScanItem
import com.example.model.Recipe
import com.example.model.RecipeFilter
import com.example.model.RecipeIngredient
import com.example.model.RecipeNutrition
import com.example.model.RecipeStep
import com.example.model.SavedRecipe
import com.example.model.ShoppingItem
import com.example.model.StarterCatalog
import com.example.model.SubstitutionAnalysis
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CookingSession(
  val recipe: Recipe,
  val currentStepIndex: Int = 0,
  val remainingSeconds: Int = recipe.steps.firstOrNull()?.durationSeconds ?: 180,
  val timerRunning: Boolean = false,
  val showDeductDialog: Boolean = false,
  val pendingDeductions: List<PantryDeduction> = emptyList(),
  val voiceReadoutEnabled: Boolean = true,
)

class KitchenViewModel(
  private val repository: KitchenRepository,
  private val aiService: KitchenAiService,
  private val currentUserId: String,
  private val initialDisplayName: String,
  private val initialEmail: String,
) : ViewModel() {

  private companion object {
    const val TAG = "KitchenVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }

  private fun <T> unwrapFirestoreStream(
    flow: kotlinx.coroutines.flow.Flow<T>,
    fallbackMessage: String,
  ): StateFlow<UiState<T>> =
    flow
      .map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackMessage, rootError)
        emit(UiState.Error(rootError.message ?: fallbackMessage))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val profileState: StateFlow<UiState<UserProfile?>> =
    unwrapFirestoreStream(
      repository.observeUserProfile(currentUserId),
      "Gagal memuat profil dapur.",
    )

  val pantryState: StateFlow<UiState<List<PantryItem>>> =
    unwrapFirestoreStream(
      repository.observePantryItems(currentUserId),
      "Gagal memuat stok Smart Pantry.",
    )

  val shoppingState: StateFlow<UiState<List<ShoppingItem>>> =
    unwrapFirestoreStream(
      repository.observeShoppingItems(currentUserId),
      "Gagal memuat daftar belanja.",
    )

  val journalState: StateFlow<UiState<List<JournalNote>>> =
    unwrapFirestoreStream(
      repository.observeJournalNotes(currentUserId),
      "Gagal memuat jurnal resep.",
    )

  val savedRecipesState: StateFlow<UiState<List<SavedRecipe>>> =
    unwrapFirestoreStream(
      repository.observeSavedRecipes(currentUserId),
      "Gagal memuat koleksi resep tersimpan.",
    )

  val mealPlansState: StateFlow<UiState<List<MealPlanEntry>>> =
    unwrapFirestoreStream(
      repository.observeMealPlans(currentUserId),
      "Gagal memuat rencana menu mingguan.",
    )

  private val _currentDestination = MutableStateFlow(MainDestination.HOME)
  val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

  private val _notesTab = MutableStateFlow(NotesTab.SHOPPING)
  val notesTab: StateFlow<NotesTab> = _notesTab.asStateFlow()

  private val _recipeFilter = MutableStateFlow(RecipeFilter.ALL_100)
  val recipeFilter: StateFlow<RecipeFilter> = _recipeFilter.asStateFlow()

  private val _explorerCategory = MutableStateFlow(ExplorerCategory.ALL)
  val explorerCategory: StateFlow<ExplorerCategory> = _explorerCategory.asStateFlow()

  private val _explorerQuery = MutableStateFlow("")
  val explorerQuery: StateFlow<String> = _explorerQuery.asStateFlow()

  // Matcher recipes (strictly 100%-match candidates + AI generated)
  private val _matcherRecipes = MutableStateFlow(StarterCatalog.matcherStarterRecipes)
  val matcherRecipes: StateFlow<List<Recipe>> = _matcherRecipes.asStateFlow()

  // World Explorer recipes (local curated + dynamically discovered via Gemini AI)
  private val _explorerRecipes = MutableStateFlow(StarterCatalog.worldExplorerRecipes)
  val explorerRecipes: StateFlow<List<Recipe>> = _explorerRecipes.asStateFlow()

  private val _aiRefreshing = MutableStateFlow(false)
  val aiRefreshing: StateFlow<Boolean> = _aiRefreshing.asStateFlow()

  private val _aiErrorMessage = MutableStateFlow<String?>(null)
  val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

  private val _explorerAiLoading = MutableStateFlow(false)
  val explorerAiLoading: StateFlow<Boolean> = _explorerAiLoading.asStateFlow()

  private val _explorerAiError = MutableStateFlow<String?>(null)
  val explorerAiError: StateFlow<String?> = _explorerAiError.asStateFlow()

  private val _explorerAiResultIds = MutableStateFlow<Set<String>>(emptySet())
  val explorerAiResultIds: StateFlow<Set<String>> = _explorerAiResultIds.asStateFlow()

  private val _showScanDialog = MutableStateFlow(false)
  val showScanDialog: StateFlow<Boolean> = _showScanDialog.asStateFlow()

  private val _scanLoading = MutableStateFlow(false)
  val scanLoading: StateFlow<Boolean> = _scanLoading.asStateFlow()

  private val _scanErrorMessage = MutableStateFlow<String?>(null)
  val scanErrorMessage: StateFlow<String?> = _scanErrorMessage.asStateFlow()

  private val _showSubstitutionDialog = MutableStateFlow(false)
  val showSubstitutionDialog: StateFlow<Boolean> = _showSubstitutionDialog.asStateFlow()

  private val _substitutionLoading = MutableStateFlow(false)
  val substitutionLoading: StateFlow<Boolean> = _substitutionLoading.asStateFlow()

  private val _substitutionAnalysis = MutableStateFlow<SubstitutionAnalysis?>(null)
  val substitutionAnalysis: StateFlow<SubstitutionAnalysis?> = _substitutionAnalysis.asStateFlow()

  private val _substitutionError = MutableStateFlow<String?>(null)
  val substitutionError: StateFlow<String?> = _substitutionError.asStateFlow()

  private val _substitutionSeedIngredient = MutableStateFlow("Daun Bawang")
  val substitutionSeedIngredient: StateFlow<String> = _substitutionSeedIngredient.asStateFlow()

  private val _substitutionSeedRecipe = MutableStateFlow("")
  val substitutionSeedRecipe: StateFlow<String> = _substitutionSeedRecipe.asStateFlow()

  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  private val _chatLoading = MutableStateFlow(false)
  val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

  private val _chatError = MutableStateFlow<String?>(null)
  val chatError: StateFlow<String?> = _chatError.asStateFlow()

  private val _cookingSession = MutableStateFlow<CookingSession?>(null)
  val cookingSession: StateFlow<CookingSession?> = _cookingSession.asStateFlow()

  private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

  private var timerJob: Job? = null
  private var chatJob: Job? = null

  init {
    viewModelScope.launch {
      val result =
        repository.ensureInitialUserData(
          userId = currentUserId,
          displayName = initialDisplayName,
          email = initialEmail,
        )
      if (result.isFailure) {
        _snackbarEvents.tryEmit(
          result.exceptionOrNull()?.localizedMessage ?: "Gagal menyiapkan data awal."
        )
      }
    }
  }

  fun navigateTo(destination: MainDestination, notesTabOverride: NotesTab? = null) {
    if (notesTabOverride != null) {
      _notesTab.value = notesTabOverride
    }
    _currentDestination.value = destination

    if (destination == MainDestination.AI_MATCHER) {
      val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
      val hasStock = currentPantry.any { it.quantity > 0 }
      val allKnown = (_matcherRecipes.value + _explorerRecipes.value).distinctBy { it.title.lowercase() }
      val fullMatchCount = allKnown.count { it.evaluateMatch(currentPantry).matchPercent == 100 }
      if (hasStock && fullMatchCount < 2 && !_aiRefreshing.value) {
        refreshAiRecipes()
      }
    }
  }

  fun selectNotesTab(tab: NotesTab) {
    _notesTab.value = tab
  }

  fun selectRecipeFilter(filter: RecipeFilter) {
    _recipeFilter.value = filter
  }

  fun selectExplorerCategory(category: ExplorerCategory) {
    _explorerCategory.value = category
    _explorerAiResultIds.value = emptySet()
  }

  fun updateExplorerQuery(query: String) {
    _explorerQuery.value = query
    _explorerAiResultIds.value = emptySet()
  }

  fun openScanDialog() {
    _scanErrorMessage.value = null
    _showScanDialog.value = true
  }

  fun closeScanDialog() {
    _showScanDialog.value = false
    _scanErrorMessage.value = null
  }

  fun openSubstitutionDialog(initialIngredient: String? = null, recipeContext: String = "") {
    if (!initialIngredient.isNullOrBlank()) {
      _substitutionSeedIngredient.value = initialIngredient
    }
    _substitutionSeedRecipe.value = recipeContext
    _substitutionError.value = null
    _showSubstitutionDialog.value = true
  }

  fun closeSubstitutionDialog() {
    _showSubstitutionDialog.value = false
  }

  fun refreshAiRecipes() {
    val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    val currentProfile = (profileState.value as? UiState.Success)?.data
    if (_aiRefreshing.value) return

    viewModelScope.launch {
      _aiRefreshing.value = true
      _aiErrorMessage.value = null
      val result = aiService.generateMatchedRecipes(currentPantry, currentProfile)
      _aiRefreshing.value = false
      result
        .onSuccess { generated ->
          val strictlyFullMatch =
            generated.filter { it.evaluateMatch(currentPantry).matchPercent == 100 }
          if (strictlyFullMatch.isNotEmpty()) {
            _matcherRecipes.value =
              (strictlyFullMatch + _matcherRecipes.value).distinctBy {
                it.title.trim().lowercase(Locale.ROOT)
              }
            _snackbarEvents.tryEmit(
              "${strictlyFullMatch.size} menu 100% cocok baru dari Gemini AI siap dimasak!"
            )
          } else {
            _aiErrorMessage.value =
              "Beberapa ide AI membutuhkan bahan tambahan, sehingga disembunyikan. Ketuk segarkan lagi untuk variasi 100% cocok lainnya."
          }
        }
        .onFailure { error ->
          val msg = error.localizedMessage ?: "Gagal memuat resep AI."
          _aiErrorMessage.value = msg
          _snackbarEvents.tryEmit(msg)
        }
    }
  }

  fun searchExplorerWithAi(queryOverride: String? = null) {
    if (_explorerAiLoading.value) return
    val activeQuery = (queryOverride ?: _explorerQuery.value).trim()
    val category = _explorerCategory.value
    val profile = (profileState.value as? UiState.Success)?.data

    viewModelScope.launch {
      _explorerAiLoading.value = true
      _explorerAiError.value = null
      val result = aiService.searchWorldRecipes(activeQuery, category, profile)
      _explorerAiLoading.value = false
      result
        .onSuccess { discovered ->
          _explorerAiResultIds.value = discovered.map { it.id }.toSet()
          _explorerRecipes.value =
            (discovered + _explorerRecipes.value).distinctBy {
              it.title.trim().lowercase(Locale.ROOT)
            }
          _snackbarEvents.tryEmit("${discovered.size} resep dunia baru berhasil ditemukan!")
        }
        .onFailure { error ->
          val msg = error.localizedMessage ?: "Gagal mencari resep dengan Gemini AI."
          _explorerAiError.value = msg
          _snackbarEvents.tryEmit(msg)
        }
    }
  }

  fun askAiSubstitution(missingIngredient: String, recipeContext: String = "") {
    val cleanIngredient = missingIngredient.trim()
    if (cleanIngredient.isBlank()) {
      _snackbarEvents.tryEmit("Masukkan nama bahan yang ingin dicari penggantinya.")
      return
    }
    _substitutionSeedIngredient.value = cleanIngredient
    _substitutionSeedRecipe.value = recipeContext
    val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    viewModelScope.launch {
      _substitutionLoading.value = true
      _substitutionError.value = null
      _substitutionAnalysis.value = null
      val result =
        aiService.suggestSubstitution(cleanIngredient, recipeContext.trim(), currentPantry)
      _substitutionLoading.value = false
      result
        .onSuccess { analysis -> _substitutionAnalysis.value = analysis }
        .onFailure { error ->
          _substitutionError.value =
            error.localizedMessage ?: "Gagal menghubungi AI untuk analisis substitusi."
        }
    }
  }

  fun sendChatMessage(rawText: String) {
    val text = rawText.trim()
    if (text.isBlank() || _chatLoading.value) return

    val historyBefore = _chatMessages.value
    val userMsg =
      ChatMessage(
        id = "u_${System.currentTimeMillis()}",
        isFromUser = true,
        text = text,
      )
    val assistantMsgId = "a_${System.currentTimeMillis() + 1}"
    val placeholderMsg =
      ChatMessage(
        id = assistantMsgId,
        isFromUser = false,
        text = "",
        isStreaming = true,
      )

    _chatMessages.value = historyBefore + userMsg + placeholderMsg
    _chatLoading.value = true
    _chatError.value = null

    val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    val currentProfile = (profileState.value as? UiState.Success)?.data

    chatJob?.cancel()
    chatJob =
      viewModelScope.launch {
        val builder = StringBuilder()
        try {
          aiService
            .sendChatMessageStream(
              history = historyBefore,
              userMessage = text,
              pantryItems = currentPantry,
              profile = currentProfile,
            )
            .collect { chunk ->
              builder.append(chunk)
              val accumulated = builder.toString()
              _chatMessages.value =
                _chatMessages.value.map { msg ->
                  if (msg.id == assistantMsgId) msg.copy(text = accumulated, isStreaming = true)
                  else msg
                }
            }
          val finalOutput =
            builder.toString().ifBlank { "Maaf, saya belum mendapat jawaban. Coba tanyakan ulang." }
          _chatMessages.value =
            _chatMessages.value.map { msg ->
              if (msg.id == assistantMsgId) msg.copy(text = finalOutput, isStreaming = false)
              else msg
            }
        } catch (e: TimeoutCancellationException) {
          Log.e(TAG, "Chef AI chat timed out", e)
          _chatMessages.value =
            _chatMessages.value
              .filterNot { it.id == assistantMsgId && it.text.isBlank() }
              .map { if (it.id == assistantMsgId) it.copy(isStreaming = false) else it }
          _chatError.value =
            "Koneksi ke Chef AI melebihi batas waktu. Periksa koneksi internet lalu ketuk Kirim Ulang."
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Chef AI chat failed", e)
          _chatMessages.value =
            _chatMessages.value
              .filterNot { it.id == assistantMsgId && it.text.isBlank() }
              .map { if (it.id == assistantMsgId) it.copy(isStreaming = false) else it }
          val detail = e.localizedMessage ?: e.message ?: e::class.java.simpleName
          _chatError.value = "Gagal menghubungi Chef AI: $detail"
        } finally {
          _chatLoading.value = false
        }
      }
  }

  fun clearChat() {
    chatJob?.cancel()
    _chatLoading.value = false
    _chatError.value = null
    _chatMessages.value = emptyList()
  }

  fun adjustPantryQuantity(item: PantryItem, delta: Int) {
    viewModelScope.launch {
      val result = repository.adjustPantryQuantity(currentUserId, item, delta)
      if (result.isFailure) {
        _snackbarEvents.tryEmit("Gagal memperbarui stok ${item.name}.")
      } else {
        val nextQty = (item.quantity + delta).coerceAtLeast(0)
        _snackbarEvents.tryEmit("Stok ${item.name}: $nextQty ${item.unit}")
      }
    }
  }

  fun deletePantryItem(item: PantryItem) {
    viewModelScope.launch {
      val result = repository.deletePantryItem(currentUserId, item.id)
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("${item.name} dihapus dari Pantry.")
      } else {
        _snackbarEvents.tryEmit("Gagal menghapus ${item.name}.")
      }
    }
  }

  fun addManualPantryItem(
    name: String,
    quantity: Int,
    unit: String,
    location: String,
    daysUntilExpiry: Int,
  ) {
    val cleanName = name.trim()
    if (cleanName.isBlank()) {
      _scanErrorMessage.value = "Nama bahan tidak boleh kosong."
      return
    }
    val existing = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    val entry =
      ReceiptScanItem(
        name = cleanName,
        quantity = quantity.coerceAtLeast(1),
        unit = unit.ifBlank { "pcs" },
        location = location.ifBlank { "Rak Dapur" },
        daysUntilExpiry = daysUntilExpiry.coerceIn(0, 3650),
        category = inferCategory(cleanName),
      )
    viewModelScope.launch {
      _scanLoading.value = true
      val result = repository.addOrMergePantryItems(currentUserId, listOf(entry), existing)
      _scanLoading.value = false
      if (result.isSuccess) {
        _showScanDialog.value = false
        _snackbarEvents.tryEmit("${entry.name} (${entry.quantity} ${entry.unit}) ditambahkan!")
      } else {
        _scanErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan stok."
      }
    }
  }

  fun scanReceiptBitmap(bitmap: Bitmap) {
    val existing = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    viewModelScope.launch {
      _scanLoading.value = true
      _scanErrorMessage.value = null
      val aiResult = aiService.scanReceiptImage(bitmap)
      aiResult
        .onSuccess { scannedItems ->
          val saveResult = repository.addOrMergePantryItems(currentUserId, scannedItems, existing)
          _scanLoading.value = false
          if (saveResult.isSuccess) {
            _showScanDialog.value = false
            val summary = scannedItems.joinToString(", ") { "${it.name} (${it.quantity} ${it.unit})" }
            _snackbarEvents.tryEmit("Struk terbaca! Ditambahkan: $summary")
          } else {
            _scanErrorMessage.value =
              saveResult.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan hasil scan."
          }
        }
        .onFailure { error ->
          _scanLoading.value = false
          _scanErrorMessage.value = error.localizedMessage ?: "Gagal membaca foto struk."
        }
    }
  }

  fun scanReceiptText(rawText: String) {
    val clean = rawText.trim()
    if (clean.isBlank()) {
      _scanErrorMessage.value = "Masukkan daftar belanjaan atau isi struk terlebih dahulu."
      return
    }
    val existing = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    viewModelScope.launch {
      _scanLoading.value = true
      _scanErrorMessage.value = null
      val aiResult = aiService.parseReceiptText(clean)
      aiResult
        .onSuccess { parsedItems ->
          val saveResult = repository.addOrMergePantryItems(currentUserId, parsedItems, existing)
          _scanLoading.value = false
          if (saveResult.isSuccess) {
            _showScanDialog.value = false
            _snackbarEvents.tryEmit("${parsedItems.size} bahan dari struk berhasil masuk ke Pantry!")
          } else {
            _scanErrorMessage.value =
              saveResult.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan ke Pantry."
          }
        }
        .onFailure { error ->
          _scanLoading.value = false
          _scanErrorMessage.value = error.localizedMessage ?: "Gagal memproses teks struk."
        }
    }
  }

  fun addToShoppingList(itemName: String, sourceTag: String = "Manual", navigateToNotes: Boolean = false) {
    val clean = itemName.trim()
    if (clean.isBlank()) return
    viewModelScope.launch {
      val result = repository.addShoppingItem(currentUserId, clean, sourceTag)
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("$clean ditambahkan ke Daftar Belanja!")
        if (navigateToNotes) {
          navigateTo(MainDestination.NOTES, NotesTab.SHOPPING)
        }
      } else {
        _snackbarEvents.tryEmit("Gagal menambahkan $clean.")
      }
    }
  }

  fun toggleShoppingItem(item: ShoppingItem) {
    viewModelScope.launch {
      repository.toggleShoppingItem(currentUserId, item.id, !item.checked)
    }
  }

  fun deleteShoppingItem(item: ShoppingItem) {
    viewModelScope.launch {
      val result = repository.deleteShoppingItem(currentUserId, item.id)
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("${item.name} dihapus.")
      }
    }
  }

  fun addJournalNote(recipeTitle: String, note: String, rating: Double) {
    val cleanTitle = recipeTitle.trim()
    val cleanNote = note.trim()
    if (cleanTitle.isBlank() || cleanNote.isBlank()) {
      _snackbarEvents.tryEmit("Judul resep dan catatan tidak boleh kosong.")
      return
    }
    val idLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    val dateLabel = SimpleDateFormat("dd MMM yyyy", idLocale).format(Date())
    viewModelScope.launch {
      val result = repository.addJournalNote(currentUserId, cleanTitle, cleanNote, rating, dateLabel)
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("Catatan jurnal resep tersimpan!")
      } else {
        _snackbarEvents.tryEmit("Gagal menyimpan catatan jurnal.")
      }
    }
  }

  fun deleteJournalNote(note: JournalNote) {
    viewModelScope.launch {
      val result = repository.deleteJournalNote(currentUserId, note.id)
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("Catatan jurnal dihapus.")
      }
    }
  }

  fun updateProfile(displayName: String, dietType: String, allergies: String, defaultServings: Int) {
    viewModelScope.launch {
      val result =
        repository.updateProfilePreferences(
          currentUserId,
          displayName,
          dietType,
          allergies,
          defaultServings,
        )
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("Preferensi dapur berhasil disimpan!")
      } else {
        _snackbarEvents.tryEmit("Gagal menyimpan preferensi profil.")
      }
    }
  }

  fun startCookingMode(recipe: Recipe, servingsOverride: Int? = null) {
    val activeRecipe =
      if (servingsOverride != null && servingsOverride != recipe.servings) {
        recipe.scaledForServings(servingsOverride)
      } else {
        recipe
      }
    timerJob?.cancel()
    val firstDuration = activeRecipe.steps.firstOrNull()?.durationSeconds ?: 180
    val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    val matchStatus = activeRecipe.evaluateMatch(currentPantry)
    _cookingSession.value =
      CookingSession(
        recipe = activeRecipe,
        currentStepIndex = 0,
        remainingSeconds = firstDuration,
        timerRunning = false,
        showDeductDialog = false,
        pendingDeductions = matchStatus.deductions,
      )
  }

  fun toggleSaveRecipe(recipe: Recipe) {
    val savedList = (savedRecipesState.value as? UiState.Success)?.data ?: emptyList()
    val targetDocId = SavedRecipe.buildDocId(recipe)
    val existing =
      savedList.firstOrNull {
        it.id == targetDocId ||
          it.id == recipe.id ||
          it.title.equals(recipe.title.trim(), ignoreCase = true)
      }
    viewModelScope.launch {
      if (existing != null) {
        val result = repository.deleteSavedRecipe(currentUserId, existing.id)
        if (result.isSuccess) {
          _snackbarEvents.tryEmit("\"${recipe.title}\" dihapus dari Favorit.")
        } else {
          _snackbarEvents.tryEmit("Gagal menghapus resep dari Favorit.")
        }
      } else {
        val result = repository.saveRecipe(currentUserId, recipe, isCustom = recipe.isCustom)
        if (result.isSuccess) {
          _snackbarEvents.tryEmit("\"${recipe.title}\" disimpan ke Favorit & tersedia offline!")
        } else {
          _snackbarEvents.tryEmit("Gagal menyimpan resep ke Favorit.")
        }
      }
    }
  }

  fun createCustomRecipe(
    title: String,
    description: String,
    durationMinutes: Int,
    servings: Int,
    category: ExplorerCategory,
    caloriesKcal: Int,
    ingredientsText: String,
    stepsText: String,
  ) {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    val ingredients =
      ingredientsText
        .lines()
        .map { it.trim().trimStart('-', '*', '•').trim() }
        .filter { it.isNotBlank() }
        .map { line ->
          val coreKeyword = line.substringBefore("(").trim().split(" ").take(2).joinToString(" ")
          RecipeIngredient(
            name = line,
            amountLabel = line,
            pantryMatchKeyword = coreKeyword.ifBlank { line },
            deductAmount = 1,
            isBasicStaple = false,
          )
        }
        .ifEmpty {
          listOf(RecipeIngredient("Bahan Utama Secukupnya", "Secukupnya", "Bahan", 0, true))
        }
    val steps =
      stepsText
        .lines()
        .map { it.trim().trimStart('-', '*', '•').trim() }
        .filter { it.isNotBlank() }
        .map { RecipeStep(instruction = it, durationSeconds = 180) }
        .ifEmpty { listOf(RecipeStep("Masak bahan hingga matang sempurna.", 180)) }

    val customRecipe =
      Recipe(
        id = "custom_${System.currentTimeMillis()}",
        title = cleanTitle,
        description = description.trim().ifBlank { "Resep racikan pribadi di Kitchen Solution." },
        durationMinutes = durationMinutes.coerceIn(5, 240),
        servings = servings.coerceIn(1, 20),
        difficulty = "Pribadi",
        category = category,
        isWebVerified = false,
        isAiGenerated = false,
        isCustom = true,
        nutrition =
          RecipeNutrition(
            caloriesKcal = caloriesKcal.coerceIn(80, 2500),
            proteinGrams = 20,
            carbsGrams = 28,
            fatGrams = 12,
          ),
        ingredients = ingredients,
        steps = steps,
      )

    viewModelScope.launch {
      val result = repository.saveRecipe(currentUserId, customRecipe, isCustom = true)
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("Resep \"$cleanTitle\" berhasil dibuat & disimpan!")
      } else {
        _snackbarEvents.tryEmit("Gagal menyimpan resep buatan sendiri.")
      }
    }
  }

  fun deleteSavedRecipe(docId: String) {
    viewModelScope.launch {
      val result = repository.deleteSavedRecipe(currentUserId, docId)
      if (result.isFailure) {
        _snackbarEvents.tryEmit("Gagal menghapus resep tersimpan.")
      }
    }
  }

  fun assignMealPlan(dayOfWeek: String, mealSlot: String, recipe: Recipe) {
    val pantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    val status = recipe.evaluateMatch(pantry)
    val missing =
      status.ingredientStates
        .filter { !it.availableInPantry }
        .joinToString(", ") { it.ingredient.name }
    viewModelScope.launch {
      val result = repository.saveMealPlan(currentUserId, dayOfWeek, mealSlot, recipe, missing)
      if (result.isSuccess) {
        _snackbarEvents.tryEmit("\"${recipe.title}\" dijadwalkan untuk $dayOfWeek ($mealSlot).")
      } else {
        _snackbarEvents.tryEmit("Gagal menyimpan jadwal menu.")
      }
    }
  }

  fun deleteMealPlan(planId: String) {
    viewModelScope.launch {
      val result = repository.deleteMealPlan(currentUserId, planId)
      if (result.isFailure) {
        _snackbarEvents.tryEmit("Gagal menghapus jadwal menu.")
      }
    }
  }

  fun syncWeeklyMissingToShoppingList(recipePool: List<Recipe>) {
    val plans = (mealPlansState.value as? UiState.Success)?.data ?: emptyList()
    val pantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    val existingShopping = (shoppingState.value as? UiState.Success)?.data ?: emptyList()
    if (plans.isEmpty()) {
      _snackbarEvents.tryEmit("Jadwalkan minimal satu menu dulu untuk menyinkronkan belanja.")
      return
    }

    val missingNames = mutableListOf<String>()
    plans.forEach { entry ->
      val matchedRecipe =
        recipePool.firstOrNull {
          it.id == entry.recipeId || it.title.equals(entry.recipeTitle, ignoreCase = true)
        }
      if (matchedRecipe != null) {
        val status = matchedRecipe.evaluateMatch(pantry)
        status.ingredientStates
          .filter { !it.availableInPantry }
          .forEach { missingNames.add(it.ingredient.name) }
      } else if (entry.missingItemsSummary.isNotBlank()) {
        entry.missingItemsSummary
          .split(",")
          .map { it.trim() }
          .filter { it.isNotBlank() }
          .forEach { missingNames.add(it) }
      }
    }

    if (missingNames.isEmpty()) {
      _snackbarEvents.tryEmit("Semua bahan untuk jadwal minggu ini sudah tersedia di Smart Pantry!")
      return
    }

    viewModelScope.launch {
      val result =
        repository.addBatchShoppingItems(
          userId = currentUserId,
          itemNames = missingNames,
          existingItems = existingShopping,
          sourceTag = "Meal Plan",
        )
      result.onSuccess { addedCount ->
        if (addedCount > 0) {
          _snackbarEvents.tryEmit("$addedCount bahan kurang ditambahkan ke Daftar Belanja!")
        } else {
          _snackbarEvents.tryEmit("Semua bahan kurang sudah ada di Daftar Belanja Anda.")
        }
      }.onFailure {
        _snackbarEvents.tryEmit("Gagal menyinkronkan bahan ke Daftar Belanja.")
      }
    }
  }

  fun closeCookingMode() {
    timerJob?.cancel()
    _cookingSession.value = null
  }

  fun nextCookingStep() {
    val session = _cookingSession.value ?: return
    val totalSteps = session.recipe.steps.size
    if (session.currentStepIndex < totalSteps - 1) {
      timerJob?.cancel()
      val nextIdx = session.currentStepIndex + 1
      val nextDuration = session.recipe.steps[nextIdx].durationSeconds
      _cookingSession.value =
        session.copy(
          currentStepIndex = nextIdx,
          remainingSeconds = nextDuration,
          timerRunning = false,
        )
    } else {
      timerJob?.cancel()
      val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
      val deductions = session.recipe.evaluateMatch(currentPantry).deductions
      _cookingSession.value =
        session.copy(
          timerRunning = false,
          showDeductDialog = true,
          pendingDeductions = deductions,
        )
    }
  }

  fun prevCookingStep() {
    val session = _cookingSession.value ?: return
    if (session.currentStepIndex > 0) {
      timerJob?.cancel()
      val prevIdx = session.currentStepIndex - 1
      val prevDuration = session.recipe.steps[prevIdx].durationSeconds
      _cookingSession.value =
        session.copy(
          currentStepIndex = prevIdx,
          remainingSeconds = prevDuration,
          timerRunning = false,
        )
    }
  }

  fun toggleTimer() {
    val session = _cookingSession.value ?: return
    if (session.timerRunning) {
      timerJob?.cancel()
      _cookingSession.value = session.copy(timerRunning = false)
    } else {
      if (session.remainingSeconds <= 0) {
        val resetSecs = session.recipe.steps[session.currentStepIndex].durationSeconds
        _cookingSession.value = session.copy(remainingSeconds = resetSecs, timerRunning = true)
      } else {
        _cookingSession.value = session.copy(timerRunning = true)
      }
      timerJob?.cancel()
      timerJob =
        viewModelScope.launch {
          while (true) {
            delay(1000L)
            val current = _cookingSession.value ?: break
            if (!current.timerRunning) break
            if (current.remainingSeconds > 1) {
              _cookingSession.value = current.copy(remainingSeconds = current.remainingSeconds - 1)
            } else {
              _cookingSession.value = current.copy(remainingSeconds = 0, timerRunning = false)
              _snackbarEvents.tryEmit("Timer langkah selesai!")
              break
            }
          }
        }
    }
  }

  fun resetTimer() {
    val session = _cookingSession.value ?: return
    timerJob?.cancel()
    val defaultSecs = session.recipe.steps[session.currentStepIndex].durationSeconds
    _cookingSession.value = session.copy(remainingSeconds = defaultSecs, timerRunning = false)
  }

  fun toggleVoiceReadout() {
    val session = _cookingSession.value ?: return
    _cookingSession.value = session.copy(voiceReadoutEnabled = !session.voiceReadoutEnabled)
  }

  fun dismissDeductDialog() {
    val session = _cookingSession.value ?: return
    _cookingSession.value = session.copy(showDeductDialog = false)
  }

  fun confirmAutoDeduct() {
    val session = _cookingSession.value ?: return
    val currentPantry = (pantryState.value as? UiState.Success)?.data ?: emptyList()
    val deductions = session.pendingDeductions
    viewModelScope.launch {
      val result = repository.deductIngredients(currentUserId, deductions, currentPantry)
      closeCookingMode()
      if (result.isSuccess) {
        if (deductions.isNotEmpty()) {
          _snackbarEvents.tryEmit("Stok bahan terpakai otomatis diperbarui di Smart Pantry!")
        } else {
          _snackbarEvents.tryEmit("Masakan selesai! Selamat menikmati.")
        }
        navigateTo(MainDestination.PANTRY)
      } else {
        _snackbarEvents.tryEmit("Gagal memperbarui stok pantry.")
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
    chatJob?.cancel()
  }

  private fun inferCategory(name: String): String {
    val lower = name.lowercase(Locale.ROOT)
    return when {
      lower.contains("ayam") || lower.contains("daging") || lower.contains("ikan") -> "protein"
      lower.contains("telur") -> "egg"
      lower.contains("bawang") || lower.contains("cabai") || lower.contains("jahe") -> "spice"
      lower.contains("mentega") || lower.contains("susu") || lower.contains("keju") -> "dairy"
      lower.contains("kecap") || lower.contains("saus") || lower.contains("minyak") -> "sauce"
      lower.contains("beras") || lower.contains("tepung") -> "grain"
      else -> "pantry"
    }
  }
}

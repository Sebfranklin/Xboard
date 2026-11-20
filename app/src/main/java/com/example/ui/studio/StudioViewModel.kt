package com.example.ui.studio

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KeyCraftApplication
import com.example.data.AutoCorrectLevel
import com.example.data.BigramEntity
import com.example.data.ClipboardItemEntity
import com.example.data.KeyCraftRepository
import com.example.data.KeyboardSettings
import com.example.data.LearnedWordEntity
import com.example.data.OneHandedMode
import com.example.data.SnippetEntity
import com.example.ime.KeyCraftImeService
import com.example.ui.keyboard.EnterActionType
import com.example.ui.keyboard.KeyboardController
import com.example.ui.keyboard.SandboxInputTarget
import com.example.ui.theme.KeyboardThemeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ImeSystemStatus(
    val isEnabled: Boolean = false,
    val isActiveDefault: Boolean = false
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    val repository: KeyCraftRepository = (application as KeyCraftApplication).container.repository

    val settings: StateFlow<KeyboardSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KeyboardSettings()
    )

    val snippets: StateFlow<List<SnippetEntity>> = repository.snippetsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val learnedWords: StateFlow<List<LearnedWordEntity>> = repository.learnedWordsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val topBigrams: StateFlow<List<BigramEntity>> = repository.topBigramsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val clipboardItems: StateFlow<List<ClipboardItemEntity>> = repository.clipboardFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _imeStatus = MutableStateFlow(ImeSystemStatus())
    val imeStatus: StateFlow<ImeSystemStatus> = _imeStatus.asStateFlow()

    val sandboxTarget = SandboxInputTarget(
        initialText = "Tap or glide across keys. Try typing teh, recieve, or ;omw "
    )
    val sandboxController = KeyboardController(repository, sandboxTarget).apply {
        setEnvironmentFlags(EnterActionType.NEWLINE, showGlobe = false)
    }

    init {
        refreshImeStatus()
        viewModelScope.launch {
            settings.collect { s ->
                sandboxController.refreshContext(s, snippets.value)
            }
        }
        viewModelScope.launch {
            snippets.collect { list ->
                sandboxController.refreshContext(settings.value, list)
            }
        }
    }

    fun refreshImeStatus() {
        val app = getApplication<Application>()
        val imm = app.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val pkg = app.packageName
        val serviceName = KeyCraftImeService::class.java.name
        val enabled = imm?.enabledInputMethodList?.any {
            it.packageName == pkg || it.serviceInfo.name == serviceName
        } ?: false

        val defaultIme = runCatching {
            Settings.Secure.getString(app.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        }.getOrNull().orEmpty()
        val isActive = defaultIme.contains(pkg)

        _imeStatus.value = ImeSystemStatus(isEnabled = enabled, isActiveDefault = isActive)
    }

    fun loadSandboxPreset(preset: String) {
        sandboxTarget.clearAll()
        sandboxTarget.commitText(preset)
        sandboxController.refreshContext(settings.value, snippets.value)
    }

    fun clearSandbox() {
        sandboxTarget.clearAll()
        sandboxController.refreshContext(settings.value, snippets.value)
    }

    fun addOrUpdateSnippet(trigger: String, expansion: String, category: String, existingId: Int = 0) {
        viewModelScope.launch {
            repository.addOrUpdateSnippet(
                SnippetEntity(
                    id = existingId,
                    trigger = trigger,
                    expansion = expansion,
                    category = category
                )
            )
        }
    }

    fun toggleSnippet(snippet: SnippetEntity) {
        viewModelScope.launch { repository.toggleSnippet(snippet) }
    }

    fun deleteSnippet(id: Int) {
        viewModelScope.launch { repository.deleteSnippet(id) }
    }

    fun addCustomWord(word: String) {
        viewModelScope.launch { repository.addCustomWord(word) }
    }

    fun deleteLearnedWord(word: String) {
        viewModelScope.launch { repository.deleteLearnedWord(word) }
    }

    fun resetAutoLearnedMemory() {
        viewModelScope.launch { repository.resetAutoLearnedMemory() }
    }

    fun toggleClipboardPin(item: ClipboardItemEntity) {
        viewModelScope.launch { repository.toggleClipboardPin(item) }
    }

    fun deleteClipboardItem(id: Int) {
        viewModelScope.launch { repository.deleteClipboardItem(id) }
    }

    fun clearUnpinnedClipboard() {
        viewModelScope.launch { repository.clearUnpinnedClipboard() }
    }

    fun setTheme(style: KeyboardThemeStyle) {
        viewModelScope.launch { repository.setTheme(style) }
    }

    fun setShowNumberRow(enabled: Boolean) {
        viewModelScope.launch { repository.setShowNumberRow(enabled) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticFeedback(enabled) }
    }

    fun setKeyPopups(enabled: Boolean) {
        viewModelScope.launch { repository.setKeyPopups(enabled) }
    }

    fun setAutoCapitalize(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoCapitalize(enabled) }
    }

    fun setDoubleSpacePeriod(enabled: Boolean) {
        viewModelScope.launch { repository.setDoubleSpacePeriod(enabled) }
    }

    fun setAutoCorrectLevel(level: AutoCorrectLevel) {
        viewModelScope.launch { repository.setAutoCorrectLevel(level) }
    }

    fun setGlideTypingEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGlideTypingEnabled(enabled) }
    }

    fun setOneHandedMode(mode: OneHandedMode) {
        viewModelScope.launch { repository.setOneHandedMode(mode) }
    }

    fun setKeyboardScale(scale: Float) {
        viewModelScope.launch { repository.setKeyboardScale(scale) }
    }
}

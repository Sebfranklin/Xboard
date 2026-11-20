package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.KeyCraftApplication
import com.example.MainActivity
import com.example.data.KeyboardSettings
import com.example.data.OneHandedMode
import com.example.data.SnippetEntity
import com.example.ui.keyboard.EnterActionType
import com.example.ui.keyboard.InputTarget
import com.example.ui.keyboard.KeyCraftKeyboardView
import com.example.ui.keyboard.KeyboardController
import com.example.ui.keyboard.KeyboardLayoutMode
import com.example.ui.keyboard.ShiftState
import com.example.ui.theme.KeyCraftTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeyCraftImeService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    internal lateinit var controller: KeyboardController
        private set

    private var latestSettings = KeyboardSettings()
    private var latestSnippets: List<SnippetEntity> = emptyList()

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!clipText.isNullOrBlank()) {
            (application as KeyCraftApplication).container.repository.recordClipboardTextAsync(clipText)
        }
    }

    internal val imeInputTarget = object : InputTarget {
        override fun getTextBeforeCursor(maxChars: Int): String {
            return currentInputConnection?.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()
        }

        override fun commitText(text: String) {
            currentInputConnection?.commitText(text, 1)
        }

        override fun setComposingText(text: String) {
            currentInputConnection?.setComposingText(text, 1)
        }

        override fun finishComposingText() {
            currentInputConnection?.finishComposingText()
        }

        override fun deleteBeforeCursor(count: Int) {
            if (count <= 0) return
            val ic = currentInputConnection ?: return
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                ic.deleteSurroundingText(count, 0)
            }
        }

        override fun moveCursor(delta: Int) {
            val ic = currentInputConnection ?: return
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
            if (extracted != null) {
                val cur = extracted.selectionStart
                val maxLen = extracted.text?.length ?: 0
                val next = (cur + delta).coerceIn(0, maxLen)
                ic.setSelection(next, next)
            } else {
                val keyCode = if (delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                repeat(kotlin.math.abs(delta)) {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                }
            }
        }

        override fun performEnterAction(action: EnterActionType) {
            val ic = currentInputConnection ?: return
            val imeAction = when (action) {
                EnterActionType.DONE -> EditorInfo.IME_ACTION_DONE
                EnterActionType.GO -> EditorInfo.IME_ACTION_GO
                EnterActionType.SEARCH -> EditorInfo.IME_ACTION_SEARCH
                EnterActionType.SEND -> EditorInfo.IME_ACTION_SEND
                EnterActionType.NEXT -> EditorInfo.IME_ACTION_NEXT
                EnterActionType.NEWLINE -> EditorInfo.IME_ACTION_NONE
            }
            if (imeAction != EditorInfo.IME_ACTION_NONE) {
                val handled = ic.performEditorAction(imeAction)
                if (!handled) {
                    ic.commitText("\n", 1)
                }
            } else {
                ic.commitText("\n", 1)
            }
        }

        override fun switchToNextInputMethod() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToNextInputMethod(false)
            } else {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                @Suppress("DEPRECATION")
                imm?.switchToNextInputMethod(window?.window?.attributes?.token, false)
            }
        }

        override fun openAppSettings() {
            val intent = Intent(this@KeyCraftImeService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        moveLifecycleTo(Lifecycle.State.CREATED)

        val repo = (application as KeyCraftApplication).container.repository
        controller = KeyboardController(repo, imeInputTarget)

        serviceScope.launch {
            repo.settingsFlow.collectLatest { latestSettings = it }
        }
        serviceScope.launch {
            repo.snippetsFlow.collectLatest { latestSnippets = it }
        }

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    override fun onCreateInputView(): View {
        val repo = (application as KeyCraftApplication).container.repository

        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KeyCraftImeService)
            setViewTreeViewModelStoreOwner(this@KeyCraftImeService)
            setViewTreeSavedStateRegistryOwner(this@KeyCraftImeService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val settings by repo.settingsFlow.collectAsState(initial = latestSettings)
                val snippets by repo.snippetsFlow.collectAsState(initial = latestSnippets)
                val clips by repo.clipboardFlow.collectAsState(initial = emptyList())

                KeyCraftTheme {
                    KeyCraftKeyboardView(
                        controller = controller,
                        settings = settings,
                        snippets = snippets,
                        clipboardItems = clips,
                        modifier = Modifier.navigationBarsPadding(),
                        onToggleOneHanded = { mode: OneHandedMode ->
                            serviceScope.launch { repo.setOneHandedMode(mode) }
                        },
                        onToggleClipboardPin = { item ->
                            serviceScope.launch { repo.toggleClipboardPin(item) }
                        },
                        onDeleteClipboard = { id ->
                            serviceScope.launch { repo.deleteClipboardItem(id) }
                        },
                        onClearUnpinnedClipboard = {
                            serviceScope.launch { repo.clearUnpinnedClipboard() }
                        },
                        onOpenSettings = { imeInputTarget.openAppSettings() }
                    )
                }
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        // Keep the host editor visible alongside the soft keyboard.
        return false
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val inputType = info?.inputType ?: 0
        val typeClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        val isPasswordField = when (typeClass) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }

        val noSuggestionsFlag = (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        val isStructuredField = typeClass == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_URI ||
                variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
            )

        val initialLayout = when (typeClass) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE -> KeyboardLayoutMode.SYMBOLS_1
            else -> KeyboardLayoutMode.QWERTY
        }

        val initialShift = when {
            (inputType and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0 -> ShiftState.CAPS_LOCK
            (currentInputConnection?.getCursorCapsMode(inputType) ?: 0) != 0 -> ShiftState.ONCE
            else -> ShiftState.OFF
        }

        val imeAction = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val noEnterAction = ((info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        val actionType = if ((isMultiLine && imeAction == EditorInfo.IME_ACTION_UNSPECIFIED) || noEnterAction) {
            EnterActionType.NEWLINE
        } else {
            when (imeAction) {
                EditorInfo.IME_ACTION_SEARCH -> EnterActionType.SEARCH
                EditorInfo.IME_ACTION_SEND -> EnterActionType.SEND
                EditorInfo.IME_ACTION_GO -> EnterActionType.GO
                EditorInfo.IME_ACTION_DONE -> EnterActionType.DONE
                EditorInfo.IME_ACTION_NEXT -> EnterActionType.NEXT
                else -> EnterActionType.NEWLINE
            }
        }

        val showGlobe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            shouldOfferSwitchingToNextInputMethod()
        } else {
            true
        }

        controller.setEnvironmentFlags(
            enterAction = actionType,
            showGlobe = showGlobe,
            isPasswordField = isPasswordField,
            disableAutoCorrectForField = isPasswordField || noSuggestionsFlag || isStructuredField,
            initialLayoutMode = initialLayout,
            initialShiftState = initialShift
        )
        controller.refreshContext(latestSettings, latestSnippets)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        controller.resetComposingState()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        if (::controller.isInitialized) {
            controller.resetComposingState()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (candidatesStart == -1 && candidatesEnd == -1) {
            controller.resetComposingState()
        }
        controller.refreshContext(latestSettings, latestSnippets)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && ::controller.isInitialized) {
            val mode = controller.uiState.value.layoutMode
            if (mode != KeyboardLayoutMode.QWERTY) {
                controller.setLayoutMode(KeyboardLayoutMode.QWERTY)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        moveLifecycleTo(Lifecycle.State.RESUMED)
        controller.refreshContext(latestSettings, latestSnippets)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        controller.resetComposingState()
        moveLifecycleTo(Lifecycle.State.CREATED)
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        moveLifecycleTo(Lifecycle.State.DESTROYED)
        store.clear()
        serviceScope.cancel()
    }

    private fun moveLifecycleTo(targetState: Lifecycle.State) {
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = targetState
        }
    }
}

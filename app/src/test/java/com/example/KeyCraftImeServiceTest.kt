package com.example

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import com.example.data.KeyboardSettings
import com.example.ime.KeyCraftImeService
import com.example.ui.keyboard.EnterActionType
import com.example.ui.keyboard.InputTarget
import com.example.ui.keyboard.KeyboardController
import com.example.ui.keyboard.KeyboardLayoutMode
import com.example.ui.keyboard.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeyCraftImeServiceTest {

    private class TestInputMethodServiceController {
        val controller = Robolectric.buildService(KeyCraftImeService::class.java)
        val service: KeyCraftImeService = controller.create().get()
    }

    @Test
    fun `onCreateInputView attaches ComposeView with lifecycle and saved state owners`() {
        val harness = TestInputMethodServiceController()
        val inputView = harness.service.onCreateInputView()

        assertTrue(inputView is ComposeView)
        assertNotNull(inputView.findViewTreeLifecycleOwner())
        assertNotNull(inputView.findViewTreeSavedStateRegistryOwner())
        assertEquals(Lifecycle.State.CREATED, harness.service.lifecycle.currentState)

        harness.service.onWindowShown()
        assertEquals(Lifecycle.State.RESUMED, harness.service.lifecycle.currentState)

        harness.service.onWindowHidden()
        assertEquals(Lifecycle.State.CREATED, harness.service.lifecycle.currentState)

        harness.controller.destroy()
        assertEquals(Lifecycle.State.DESTROYED, harness.service.lifecycle.currentState)
    }

    @Test
    fun `onStartInputView configures search action, password guard, and numeric layout`() {
        val harness = TestInputMethodServiceController()
        val service = harness.service

        val searchInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        service.onStartInputView(searchInfo, false)
        assertEquals(EnterActionType.SEARCH, service.controller.uiState.value.enterAction)
        assertFalse(service.controller.uiState.value.isPasswordField)
        assertEquals(KeyboardLayoutMode.QWERTY, service.controller.uiState.value.layoutMode)

        val passwordInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        service.onStartInputView(passwordInfo, false)
        assertTrue(service.controller.uiState.value.isPasswordField)
        assertTrue(service.controller.uiState.value.disableAutoCorrectForField)

        val numberInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        service.onStartInputView(numberInfo, false)
        assertEquals(KeyboardLayoutMode.SYMBOLS_1, service.controller.uiState.value.layoutMode)

        harness.controller.destroy()
    }

    @Test
    fun `composing, space commit, autocorrect, and backspace render via EditText InputConnection`() {
        val harness = TestInputMethodServiceController()
        val editText = EditText(RuntimeEnvironment.getApplication())
        val ic = editText.onCreateInputConnection(EditorInfo())
        assertNotNull(ic)

        val fakeTarget = object : InputTarget {
            override fun getTextBeforeCursor(maxChars: Int): String =
                ic!!.getTextBeforeCursor(maxChars, 0)?.toString().orEmpty()
            override fun commitText(text: String) { ic!!.commitText(text, 1) }
            override fun setComposingText(text: String) { ic!!.setComposingText(text, 1) }
            override fun finishComposingText() { ic!!.finishComposingText() }
            override fun deleteBeforeCursor(count: Int) { ic!!.deleteSurroundingText(count, 0) }
            override fun moveCursor(delta: Int) {}
            override fun performEnterAction(action: EnterActionType) { ic!!.commitText("\n", 1) }
        }

        val repo = (RuntimeEnvironment.getApplication() as KeyCraftApplication).container.repository
        val kbController = KeyboardController(repo, fakeTarget)
        kbController.setEnvironmentFlags(
            enterAction = EnterActionType.NEWLINE,
            showGlobe = false,
            initialShiftState = ShiftState.OFF
        )
        val settings = KeyboardSettings(autoCapitalize = false)

        // Type "teh" -> composing text renders in EditText
        kbController.onCharacterPressed("t", settings, emptyList())
        kbController.onCharacterPressed("e", settings, emptyList())
        kbController.onCharacterPressed("h", settings, emptyList())
        assertEquals("teh", editText.text.toString())

        // Space commits spatial autocorrection "the "
        kbController.onSpacePressed(settings, emptyList())
        assertEquals("the ", editText.text.toString())

        // Backspace immediately reverts autocorrect back to "teh"
        kbController.onBackspacePressed(settings, emptyList())
        assertEquals("teh", editText.text.toString())

        shadowOf(Looper.getMainLooper()).idle()
        harness.controller.destroy()
    }

    @Test
    fun `back key closes emoji or symbols subpanel before dismissing IME`() {
        val harness = TestInputMethodServiceController()
        val service = harness.service
        service.onCreateInputView()
        service.onStartInputView(EditorInfo(), false)

        service.controller.setLayoutMode(KeyboardLayoutMode.EMOJI)
        val consumed = service.onKeyDown(KeyEvent.KEYCODE_BACK, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        assertTrue(consumed)
        assertEquals(KeyboardLayoutMode.QWERTY, service.controller.uiState.value.layoutMode)

        shadowOf(Looper.getMainLooper()).idle()
        harness.controller.destroy()
    }
}

package com.example.tvsignage.ui

import android.view.KeyEvent

/**
 * Detects the D-pad Konami-style secret sequence (Up-Up-Down-Down-OK) or long press OK
 * to open the admin settings menu on Android TV remote.
 */
class SecretMenuTrigger(private val onTrigger: () -> Unit) {

    private val targetSequence = listOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_CENTER
    )

    private val currentInput = mutableListOf<Int>()
    private var lastKeyTime = 0L
    private val timeoutMs = 3000L

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Support long-press DPAD_CENTER / ENTER
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) && event.isLongPress) {
            currentInput.clear()
            onTrigger()
            return true
        }

        val now = System.currentTimeMillis()
        if (now - lastKeyTime > timeoutMs) {
            currentInput.clear()
        }
        lastKeyTime = now

        currentInput.add(keyCode)
        if (currentInput.size > targetSequence.size) {
            currentInput.removeAt(0)
        }

        if (currentInput == targetSequence) {
            currentInput.clear()
            onTrigger()
            return true
        }

        return false
    }
}

package com.hirden.flomofloat

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.petterp.floatingx.system.container.FxWindowContainer

/** The overlay owns input only after a user touches one of its editors. */
class OverlayInputController(private val container: FxWindowContainer, ids: IntArray) {
    private val fields: List<View> = ids.map { container.contentView?.findViewById<View>(it) }.filterNotNull()
    private val imm = container.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    private var activeField: View? = null
    private var pending: Runnable? = null
    private var generation = 0
    private var editing = false
    private var closed = false
    private val windowFocus = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
        if (!closed && editing) {
            if (focused) activeField?.let { scheduleKeyboard(it) } else releaseInput(hideKeyboard = false)
        }
    }

    init { bind() }

    @SuppressLint("ClickableViewAccessibility") // Editors keep their normal touch / accessibility handling.
    private fun bind() {
        container.isFocusableInTouchMode = true
        container.requestFocus()
        setInputFocusable(false)
        container.viewTreeObserver.addOnWindowFocusChangeListener(windowFocus)
        container.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                // This is a separate notification; the original touch still reaches the other app.
                // Do not issue an IME hide after the other app may already have started input.
                releaseInput(hideKeyboard = false)
                true
            } else false
        }
        fields.forEach { field ->
            field.setOnTouchListener { view, event ->
                if (!closed && event.actionMasked == MotionEvent.ACTION_DOWN && view.isEnabled) {
                    editing = true; activeField = view
                    setInputFocusable(true)
                    scheduleKeyboard(view)
                }
                false
            }
        }
        container.onImeBack = { releaseInput(hideKeyboard = true) }
    }

    private fun scheduleKeyboard(field: View) {
        pending?.let(container::removeCallbacks)
        val request = ++generation
        pending = Runnable {
            pending = null
            if (!closed && editing && request == generation && activeField === field &&
                container.isAttachedToWm && field.isAttachedToWindow && field.isEnabled) {
                field.requestFocus()
                // Focus may arrive after the first posted request. The window-focus listener retries.
                if (container.hasWindowFocus()) imm?.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
            }
        }.also { container.post(it) }
    }

    private fun releaseInput(hideKeyboard: Boolean) {
        val owned = editing
        editing = false; generation++
        pending?.let(container::removeCallbacks); pending = null
        if (hideKeyboard && owned && container.hasWindowFocus()) {
            activeField?.windowToken?.let { imm?.hideSoftInputFromWindow(it, 0) }
        }
        activeField = null
        setInputFocusable(false)
        container.requestFocus()
    }

    private fun setInputFocusable(focusable: Boolean) {
        // A passive panel stays below another app's IME. An active editor must be an IME target.
        container.windowParams.flags = if (focusable) {
            container.windowParams.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
        } else container.windowParams.flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        container.setWindowFocusable(focusable)
    }

    fun close() {
        if (closed) return
        closed = true
        if (container.viewTreeObserver.isAlive) container.viewTreeObserver.removeOnWindowFocusChangeListener(windowFocus)
        releaseInput(hideKeyboard = true)
        fields.forEach { it.setOnTouchListener(null) }
        container.setOnTouchListener(null); container.onImeBack = null
    }
}

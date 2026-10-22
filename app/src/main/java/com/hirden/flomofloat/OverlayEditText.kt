package com.hirden.flomofloat

import android.content.Context
import android.annotation.SuppressLint
import android.widget.EditText

/** Reports the completed native gesture, after TextView sets its release handling. */
@SuppressLint("ViewConstructor") // Created by Ui.edit; never inflated from XML.
class OverlayEditText(context: Context) : EditText(context) {
    internal var onLongPressResult: ((Boolean) -> Unit)? = null
    override fun performLongClick(): Boolean {
        val handled = super.performLongClick()
        onLongPressResult?.invoke(handled)
        return handled
    }
}

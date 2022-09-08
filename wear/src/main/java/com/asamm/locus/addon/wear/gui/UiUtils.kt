package com.asamm.locus.addon.wear.gui

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/*
 * Created by milan on 16.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
object UiUtils {

    /**
     * Hide keyboard currently focused on certain view object.
     */
    fun hideKeyboard(view: View?) {
        // check view
        if (view == null) {
            return
        }

        // handle view focus
        view.requestFocus()
        view.postDelayed(Runnable {

            // hide keyboard
            val manager =
                    view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }, 200)
    }

    /**
     * Move a cursor or select a content of edit text and display keyboard.
     */
    fun showKeyboard(et: EditText) {
        if (et.requestFocus()) {
            val imm = et.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(et, InputMethodManager.SHOW_FORCED)
        }
    }
}
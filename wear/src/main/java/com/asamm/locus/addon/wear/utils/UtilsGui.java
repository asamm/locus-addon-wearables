package com.asamm.locus.addon.wear.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/*
 * Created by milan on 16.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class UtilsGui {

    /**
     * Hide keyboard currently focused on certain view object.
     *
     * @param view view with focused keyboard
     */
    public static void hideKeyboard(View view) {
        // check view
        if (view == null) {
            return;
        }

        // handle view focus
        view.requestFocus();
        view.postDelayed(() -> {
            // hide keyboard
            InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }, 200);
    }

    /**
     * Move a cursor or select a content of edit text and display keyboard.
     *
     * @param et edit text
     */
    public static void showKeyboard(final EditText et) {
        // check parameters
        if (et == null) {
            return;
        }

        et.requestFocus();

        // show keyboard with small delay
        et.postDelayed(() -> {
            // get context
            Context ctx = et.getContext();
            if (ctx == null) {
                return;
            }
            // http://stackoverflow.com/a/6475381/836138
            InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(et, InputMethodManager.SHOW_FORCED);
        }, 200);
    }
}

/*
 * Created by milan on 08.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.custom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.widget.EditText;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate;

/**
 * Empty activity with edit text outside of screen
 * and an ugly hack right after to start to show inputSoftKeyboard bound to this invisible edittext
 */
public class EditTextKeyboardActivity extends LocusWearActivity {
    @Override
    protected DataPayload getInitialCommandType() {
        return null;
    }

    @Override
    protected DataPath getInitialCommandResponseType() {
        return null;
    }

    @Override
    public void registerHwKeyActions(LocusWearActivityHwKeyDelegate delegate) {
    }

    @Override
    protected boolean isMakeHandshakeOnStart() {
        return false;
    }

    @Override
    public boolean isChildLocusWearActivity() {
        return true;
    }

    private EditText editText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_text_keyboard);
        editText = findViewById(R.id.edit_text);
        editText.requestFocus();
        editText.setOnEditorActionListener((v, actionId, event) -> {
            Intent data = new Intent();
            data.putExtra(WaypointInputTextActivity.KEY_RESULT_DATA, v.getText().toString());
            setResult(RESULT_OK, data);
            finish();
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        editText.requestFocus();
        new Handler().postDelayed(() -> {
            // simulate click to edittext to open keyboard
            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
        }, 250);
    }
}

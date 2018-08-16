/*
 * Created by milan on 01.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.custom;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate;
import com.asamm.locus.addon.wear.utils.UtilsGui;

import java.util.List;

import locus.api.utils.Logger;

/**
 * Text input activity to input new waypoint name
 */
public class WaypointInputTextActivity extends LocusWearActivity {
    public static final String KEY_RESULT_DATA = "KEY_RESULT_DATA";
    private static final int REQUEST_SPEECH = 2;

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
        setContentView(R.layout.activity_waypoint_text_input);

        editText = findViewById(R.id.edit_text);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            UtilsGui.hideKeyboard(editText);
            finishWithResult(v.getText().toString());
            return false;
        });

        Button btnKeyboard = findViewById(R.id.btn_keyboard);
        btnKeyboard.setOnClickListener(v -> {
            // show dummy 1x1px sized EditText and open keyboard
            editText.setVisibility(View.VISIBLE);
            UtilsGui.showKeyboard(editText);
        });
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        int numInputMethods = imm.getEnabledInputMethodList().size();
        Logger.logD("KEYBOARD", "num input methods: "+numInputMethods);
        if ( numInputMethods == 0) {
            btnKeyboard.setVisibility(View.GONE);
            editText.setVisibility(View.GONE);
        }

        Button btnSpeech = findViewById(R.id.btn_mic);
        btnSpeech.setOnClickListener(v -> displaySpeechRecognizer());

        Button btnDefault = findViewById(R.id.btn_ok);
        btnDefault.setOnClickListener(v -> finishWithResult(""));
    }

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, REQUEST_SPEECH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            finishWithResult(spokenText);
        }
    }

    private void finishWithResult(String result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_RESULT_DATA, result);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}

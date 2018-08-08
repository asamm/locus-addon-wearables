package com.asamm.locus.addon.wear.gui.custom;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.ImageButton;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import locus.api.utils.Logger;

/*
 * Created by milan on 01.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class WaypointInputTextActivity extends LocusWearActivity {
    public static final String KEY_RESULT_DATA = "KEY_RESULT_DATA";
    private static final int REQUEST_CODE_KEYBOARD = 1;
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

    private ImageButton btnKeyboard;
    private ImageButton btnSpeech;
    private Button btnDefault;

    private String defaultValue = new SimpleDateFormat("HH:mm:ss").format(new Date());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint_text_input);

        btnKeyboard = findViewById(R.id.btn_keyboard);
        btnKeyboard.setOnClickListener(v -> {
            Logger.logD("WaypointInputTextActivity", "keyboard click");
            Intent i = new Intent(WaypointInputTextActivity.this, EditTextKeyboardActivity.class);
            startActivityForResult(i, REQUEST_CODE_KEYBOARD);
        });

        btnSpeech = findViewById(R.id.btn_mic);
        btnSpeech.setOnClickListener(v -> displaySpeechRecognizer());

        btnDefault = findViewById(R.id.btn_ok);
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
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_KEYBOARD) {
            String result = data.getStringExtra(KEY_RESULT_DATA);
            finishWithResult(result);

        } else if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            Logger.logD("WaypointInputTextActivity", "spoken " + spokenText);
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

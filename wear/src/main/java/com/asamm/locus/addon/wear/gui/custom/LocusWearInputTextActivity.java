package com.asamm.locus.addon.wear.gui.custom;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.asamm.locus.addon.wear.R;
import com.asamm.locus.addon.wear.common.communication.DataPath;
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload;
import com.asamm.locus.addon.wear.gui.LocusWearActivity;
import com.asamm.locus.addon.wear.gui.LocusWearActivityHwKeyDelegate;

import java.text.SimpleDateFormat;
import java.util.Date;

import locus.api.utils.Logger;

/*
 * Created by milan on 01.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
public class LocusWearInputTextActivity extends LocusWearActivity {
    public static final String KEY_RESULT_DATA = "KEY_RESULT_DATA";
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


    private EditText inputText;
    private Button btnPositive;


    private String defaultValue = new SimpleDateFormat("HH:mm:ss").format(new Date());
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_input);
        inputText = findViewById(R.id.ed_input_text);
        btnPositive = findViewById(R.id.btn_ok);
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString().trim();
                if (text.isEmpty()) {
                    text = defaultValue;
                }
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_RESULT_DATA,text);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        inputText.setHint(defaultValue);
//        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                return false;
//            }
//        });
    }

}

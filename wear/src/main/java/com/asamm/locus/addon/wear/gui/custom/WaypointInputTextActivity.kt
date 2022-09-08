/*
 * Created by milan on 01.08.2018.
 * This code is part of Locus project from Asamm Software, s. r. o.
 * Copyright (C) 2018
 */
package com.asamm.locus.addon.wear.gui.custom

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.asamm.locus.addon.wear.R
import com.asamm.locus.addon.wear.common.communication.DataPath
import com.asamm.locus.addon.wear.common.communication.containers.DataPayload
import com.asamm.locus.addon.wear.gui.LocusWearActivity
import com.asamm.locus.addon.wear.gui.UiUtils.hideKeyboard
import com.asamm.locus.addon.wear.gui.UiUtils.showKeyboard

/**
 * Text input activity to input new waypoint name
 */
class WaypointInputTextActivity : LocusWearActivity() {

    override val initialCommandType: DataPayload<*>?
        get() = null

    override val initialCommandResponseType: DataPath?
        get() = null

    override val isMakeHandshakeOnStart: Boolean
        get() = false

    override val isChildLocusWearActivity: Boolean
        get() = true

    private lateinit var editText: EditText

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waypoint_text_input)
        editText = findViewById(R.id.edit_text)
        editText.setOnEditorActionListener { v: TextView, _: Int, _: KeyEvent? ->
            hideKeyboard(editText)
            finishWithResult(v.text.toString())
            true
        }
        val btnKeyboard = findViewById<Button>(R.id.btn_keyboard)
        btnKeyboard.setOnClickListener {
            showKeyboard(editText)
        }
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val numInputMethods = imm.enabledInputMethodList.size
        if (numInputMethods == 0) {
            btnKeyboard.visibility = View.GONE
        }
        val btnSpeech = findViewById<Button>(R.id.btn_mic)
        btnSpeech.setOnClickListener { displaySpeechRecognizer() }
        val btnDefault = findViewById<Button>(R.id.btn_ok)
        btnDefault.setOnClickListener { finishWithResult("") }
    }

    // Create an intent that can start the Speech Recognizer activity
    private fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, REQUEST_SPEECH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            val results: List<String>? = data!!.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
            )
            val spokenText = results!![0]
            finishWithResult(spokenText)
        }
    }

    private fun finishWithResult(result: String) {
        val resultIntent = Intent()
        resultIntent.putExtra(KEY_RESULT_DATA, result)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    companion object {

        const val KEY_RESULT_DATA = "KEY_RESULT_DATA"

        private const val REQUEST_SPEECH = 2
    }
}
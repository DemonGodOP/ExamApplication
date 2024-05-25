package com.example.examapplication;

import android.content.Context;
import android.content.Intent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

public class WakeWordHelper {

    private PorcupineManager porcupineManager = null;

    private static final String ACCESS_KEY = "vyX1T407gf7bmUXzlE+08iePeSrL0y63T7d2EFsaj2DcPY9YzV5Fog==";

    private final Porcupine.BuiltInKeyword defaultKeyword = Porcupine.BuiltInKeyword.PORCUPINE;

    private Context context;
    private AState.AppState currentState;

    WakeWordListener wakeWordListener;


    public WakeWordHelper(Context context, AState.AppState initialState,WakeWordListener listener) {
        this.context = context;
        this.currentState = initialState;
        this.wakeWordListener=listener;

        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeyword(defaultKeyword)
                    .setSensitivity(0.7f)
                    .build(context, porcupineManagerCallback);
        } catch (PorcupineException e) {
            displayError("Failed to create Porcupine manager.");
        }
    }

    private final PorcupineManagerCallback porcupineManagerCallback = new PorcupineManagerCallback() {
        @Override
        public void invoke(int keywordIndex) {
            try {
                // Need to stop porcupine manager before speechRecognizer can start listening.
                porcupineManager.stop();
            } catch (PorcupineException e) {
                displayError("Failed to stop Porcupine.");
                return;
            }

            // Display toast message indicating the wake word has been detected.
            currentState = AState.AppState.STT;
            if(wakeWordListener!=null){
                wakeWordListener.onWakeWordDetected();
            }
        }
    };

    public void startListening() {
        try {
            porcupineManager.start();
            Toast.makeText(context, "WakeWord Detection Started", Toast.LENGTH_SHORT).show();
        } catch (PorcupineException e) {
            displayError("Failed to start Porcupine.");
        }
    }

    public void stopListening() {
        try {
            porcupineManager.stop();
        } catch (PorcupineException e) {
            displayError("Failed to stop Porcupine.");
        }
    }

    private void displayError(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

package com.example.examapplication;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

            String keywordPaths = AssetFileHelper.copyAssetToCache(context, "Exam-Care_en_android_v3_0_0.ppn");

            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath(keywordPaths)
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

    public static class AssetFileHelper {

        public static String copyAssetToCache(Context context, String assetName) {
            AssetManager assetManager = context.getAssets();
            File outFile = new File(context.getCacheDir(), assetName);
            try (InputStream inputStream = assetManager.open(assetName);
                 OutputStream outputStream = new FileOutputStream(outFile)) {

                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                return outFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

}


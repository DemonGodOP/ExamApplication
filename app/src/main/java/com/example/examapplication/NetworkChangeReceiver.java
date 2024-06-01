package com.example.examapplication;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

public class NetworkChangeReceiver extends BroadcastReceiver  {
    private TextToSpeech textToSpeech;
    public void onReceive(final Context context, final Intent intent) {
        if (!isNetworkAvailable(context)) {
            // Network connection is lost
            //speakMessage(context, "No Internet connection, Please check your network connectivity");
            Intent networkIntent = new Intent("NETWORK_STATE_CHANGED");
            networkIntent.putExtra("NETWORK_STATE", -1); // -1 indicates network unavailable
            LocalBroadcastManager.getInstance(context).sendBroadcast(networkIntent);
        }
        else{
            //speakMessage(context,"Internet is Available Now.");
            Intent networkIntent = new Intent("NETWORK_STATE_CHANGED");
            networkIntent.putExtra("NETWORK_STATE", 1); // 1 indicates network available
            LocalBroadcastManager.getInstance(context).sendBroadcast(networkIntent);
        }
    }

    private void speakMessage(Context context, String message) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                Locale locale = new Locale("en","US");
                //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
                Voice voice = new Voice("en-us-x-tpf-local", locale, 400, 200, false, null); // Example voice
                textToSpeech.setVoice(voice);
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
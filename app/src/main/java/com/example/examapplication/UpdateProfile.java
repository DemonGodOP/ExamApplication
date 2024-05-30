package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class UpdateProfile extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener{
    EditText UP_Name, UP_Phone, UP_Institute,UP_UserName;
    String Name, Phone, Institute,  Username, finalRole, email;
    FirebaseAuth authProfile;
    ProgressBar UP_progressBar;

    TextView UPTH;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1

    String Rl;
    FirebaseUser firebaseUser;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;

    AState.AppState appstate;
    String STTData;

    WakeWordHelper wakeWordHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);
        Intent intent = getIntent();

        Rl= intent.getStringExtra("Rl");
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

        UP_progressBar = findViewById(R.id.UP_progressBar);
        UP_Name = findViewById(R.id.UP_Name);
        UP_Phone= findViewById(R.id.UP_Phone);
        UP_Institute = findViewById(R.id.UP_Institute);
        UP_UserName=findViewById(R.id.UP_UserName);

        UPTH=findViewById(R.id.UPTH);
        UPTH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Rl.equals("Teacher")) {
                    Intent intent = new Intent(UpdateProfile.this,  Profile.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Rl", "Teacher");
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(UpdateProfile.this,  Profile.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Rl", "Student");
                    startActivity(intent);
                    finish();
                }
            }
        });
        authProfile = FirebaseAuth.getInstance();
        firebaseUser = authProfile.getCurrentUser();


        showProfile(firebaseUser);

        Button buttonUpdateProfile = findViewById(R.id.UP_Button);
        buttonUpdateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile(firebaseUser);
            }
        });
        if(Rl.equals("Student")) {
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            appstate = AState.AppState.TTS;
            if (hasRecordPermission()) {
                wakeWordHelper = new WakeWordHelper(this, appstate, this);
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                speechRecognizer.setRecognitionListener(new UpdateProfile.SpeechListener());
            } else {
                // Permission already granted
                requestRecordPermission();
            }
        }

            handler = new Handler();//2

            isUserInteracted = false;
            isTTSInitialized = false;

            toastRunnable = new Runnable() {
                @Override
                public void run() {
                    if(Rl.equals("Student"))
                        Repeat();
                }
            };

            // Start the initial delay
            startToastTimer();//2

    }
    @Override //3
    protected void onResume() {
        super.onResume();
        // Reset the timer whenever the user interacts with the app
        resetToastTimer();
        if(Rl.equals("Student")) {
            isUserInteracted = false; // Reset user interaction flag
            if (textToSpeech != null) {
                int ttsResult = textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care, Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                if (ttsResult == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
                //Enter the Condition Over here that is tts to take input from the user if they wants us to repeat the introduction and change r respectively.
            /*boolean r=false;
            if(r==true){
                StarUpRepeat();
            } // Restart the TTS when the activity is resumed
            else{
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();
            }*/
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(Rl.equals("Student")) {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }
            if (wakeWordHelper != null) {
                wakeWordHelper.stopListening();
                appstate = AState.AppState.TTS;
            }
            if (textToSpeech != null) {
                textToSpeech.stop();
            }
        }
        pauseToastTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(Rl.equals("Student")) {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening(); // Destroy the speech recognizer when the app is no longer visible
            }
            if (textToSpeech != null) {
                textToSpeech.stop();
            }

            if (wakeWordHelper != null) {
                wakeWordHelper.stopListening();
            }
        }
        pauseToastTimer();
    }
    @Override
    protected void onDestroy() {
        // Release resources
        if(Rl.equals("Student")) {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
            if (speechRecognizer != null) {
                speechRecognizer.destroy(); // Destroy the speech recognizer when the app is no longer visible
            }
            if (wakeWordHelper != null) {
                wakeWordHelper.stopListening();
            }
        }
        handler.removeCallbacks(toastRunnable);
        super.onDestroy();
    }//3
    private boolean hasRecordPermission() {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 ||
                grantResults[0] == PackageManager.PERMISSION_DENIED) {
            // handle permission denied
            Toast.makeText(this, "App Cannot be Used Without Record Permission", Toast.LENGTH_SHORT).show();
        } else {
            wakeWordHelper=new WakeWordHelper(this,appstate,this);
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new UpdateProfile.SpeechListener());
        }
    }


    private class SpeechListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    Toast.makeText(UpdateProfile.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(UpdateProfile.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(UpdateProfile.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(UpdateProfile.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    return;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Toast.makeText(UpdateProfile.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(UpdateProfile.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Toast.makeText(UpdateProfile.this, "No speech input.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(UpdateProfile.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if(appstate==AState.AppState.STT) {
                STTData = data.get(0).toLowerCase();
            }
            else if(appstate== AState.AppState.AUTOMATE){
                if(data.get(0).toLowerCase()!=null)
                    Automate(data.get(0).toLowerCase());
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speechRecognizer.startListening(speechRecognizerIntent);
                            Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

            ArrayList<String> data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if(appstate==AState.AppState.STT) {
                STTData = data.get(0).toLowerCase();
            }
        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    }

    // Method to start the Toast timer
    private void startToastTimer() {
        handler.postDelayed(toastRunnable, 30000); // 1 minute delay
    }

    // Method to reset the Toast timer
    private void resetToastTimer() {
        handler.removeCallbacks(toastRunnable);
        startToastTimer();
    }

    private void pauseToastTimer() {
        handler.removeCallbacks(toastRunnable);
    }

    // Callback when TTS engine finishes speaking
    UtteranceProgressListener utteranceProgressListener=new UtteranceProgressListener() {

        @Override
        public void onStart(String utteranceId) {
            Log.d(TAG, "onStart ( utteranceId :"+utteranceId+" ) ");
        }

        @Override
        public void onError(String utteranceId) {
            Log.d(TAG, "onError ( utteranceId :"+utteranceId+" ) ");
        }

        @Override
        public void onDone(String utteranceId) {
            if(utteranceId.equals("TTS_UTTERANCE_STARTWAKEWORD")){
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();
                resetToastTimer();
                Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
            }
            else if(utteranceId.equals("TTS_UTTERANCE_ONINIT")){
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String YN = STTData;
                        if (YN != null && YN.equals("yes")) {
                            StarUpRepeat();
                        } else {
                            int tts1 = textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                            if (tts1 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                    }
                }, 5000);
            }
            else if(utteranceId.equals("TTS_UTTERANCE_EDIT")){
                wakeWordHelper.stopListening();
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String change = STTData;
                        if (change.equals("name")) {
                            int tts3 = textToSpeech.speak("Please say your new name", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_EDIT_NAME");
                            if (tts3 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                        else if (change.equals("phone number")) {
                            int tts3 = textToSpeech.speak("Please say your new phone number", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_EDIT_PHONE");
                            if (tts3 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                        else if (change.equals("institute")) {
                            int tts3 = textToSpeech.speak("Please say your new institute", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_EDIT_INSTITUTE");
                            if (tts3 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }

                        }
                        else if (change.equals("username")) {
                            int tts3 = textToSpeech.speak("Please say your new username", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_EDIT_USERNAME");
                            if (tts3 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        } else {
                            int tts1 = textToSpeech.speak("Wrong input provided"+change+"Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                            if (tts1 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                    }
                },5000);
                    }
            else if(utteranceId.equals("TTS_UTTERANCE_EDIT_NAME")){
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String name2 = STTData;
                        ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(email, name2, Phone, Institute, Username, finalRole);
                        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                        assert firebaseUser != null;
                        referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(UpdateProfile.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(UpdateProfile.this, Profile.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("Rl", Rl);
                                startActivity(intent);
                                finish();
                            }
                        });
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int tts1 = textToSpeech.speak("Name Updated. Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                if (tts1 == TextToSpeech.SUCCESS) {
                                    // Pause the timer until TTS completes
                                    pauseToastTimer();
                                }
                            }
                        },2000);
                    }
                },5000);
            }
            else if(utteranceId.equals("TTS_UTTERANCE_EDIT_INSTITUTE")){
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String institute2 = STTData;
                        ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(email, Name, Phone, institute2, Username, finalRole);
                        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                        assert firebaseUser != null;
                        referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(UpdateProfile.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(UpdateProfile.this, Profile.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("Rl", Rl);
                                startActivity(intent);
                                finish();
                            }
                        });
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int tts1 = textToSpeech.speak("Institute Updated. Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                if (tts1 == TextToSpeech.SUCCESS) {
                                    // Pause the timer until TTS completes
                                    pauseToastTimer();
                                }
                            }
                        },2000);
                    }
                },5000);
            }
            else if(utteranceId.equals("TTS_UTTERANCE_EDIT_PHONE")){
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String phone2 = STTData;
                        String Phone3 = phone2.replaceAll(" ", "");
                        ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(email, Name, Phone3, Institute, Username, finalRole);
                        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                        assert firebaseUser != null;
                        referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(UpdateProfile.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(UpdateProfile.this, Profile.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("Rl", Rl);
                                startActivity(intent);
                                finish();
                            }
                        });
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int tts1 = textToSpeech.speak("Phone Updated. Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                if (tts1 == TextToSpeech.SUCCESS) {
                                    // Pause the timer until TTS completes
                                    pauseToastTimer();
                                }
                            }
                        },2000);
                    }
                },5000);
            }
            else if(utteranceId.equals("TTS_UTTERANCE_EDIT_USERNAME")){
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(UpdateProfile.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String username2 = STTData;
                        ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(email, Name, Phone, Institute, username2, finalRole);
                        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                        assert firebaseUser != null;
                        referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(UpdateProfile.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(UpdateProfile.this, Profile.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("Rl", Rl);
                                startActivity(intent);
                                finish();
                            }
                        });
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                int tts1 = textToSpeech.speak("UserName Updated. Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                if (tts1 == TextToSpeech.SUCCESS) {
                                    // Pause the timer until TTS completes
                                    pauseToastTimer();
                                }
                            }
                        },2000);
                    }
                },5000);
            }
            resetToastTimer();
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(Rl.equals("Student")) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 1) {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // TTS engine is available, initialize TextToSpeech
                    textToSpeech = new TextToSpeech(this, this);
                    textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
                } else {
                    // TTS engine is not installed, prompt the user to install it
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                }
            }
        }
    }



    @Override
    public void onInit(int status) {
        if(Rl.equals("Student")) {
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialization successful, set language and convert text to speech
                isTTSInitialized = true;
                textToSpeech.setLanguage(Locale.US);
                //Locale locale = new Locale("en","IN");
                //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
                //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
                //textToSpeech.setVoice(voice);
                int ttsResult = textToSpeech.speak("Hello, Welcome to the Update Profile Page of Exam Care, Would you like to listen to a Detailed introduction of the page.Say Yes or No", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ONINIT");
                if (ttsResult == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            } else {
                // TTS initialization failed, handle error
                Log.e("TTS", "Initialization failed");
            }
        }
    }

    // Repeat The Introduction if Repeat Method is Triggered.
    public void StarUpRepeat(){
        resetToastTimer();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Update Profile Page of Exam Care, This page provides you with the facility, to " +
                "edit your profile, change password, change email, and delete your account" +
                "To edit your profile, please just say, Exam Care edit profile,and you can move on to the edit profile page " +
                "to change password, please just say, Exam Care change password,and you can move on to the change password page "+
                "to change Email, please just say, Exam Care change Email,and you can move on to the change Email page "+
                "To delete your profile, please just say, Exam Care delete my profile,and you can move on to the delete profile page." , TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
    }

    public void Repeat(){
        if(appstate== AState.AppState.WAKEWORD){
            wakeWordHelper.stopListening();
        }
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult=textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        //Enter the Condition Over here that is tts to take input from the user if they wants us to repeat the introduction and change r respectively.

    }



    public void Automate(String Temp) {
        wakeWordHelper.stopListening();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        if(Temp.equals("back")){
            Intent intent=new Intent(UpdateProfile.this,Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();
        }
        else if(Temp.equals("describe profile details")) {
            int tts1 = textToSpeech.speak("Your Name is, " + Name + ", Your email address is, " + email + ", your phone number is, " + Phone +
                    ", Your institute name is, " + Institute + ", your username is, " + Username , TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        else if (Temp.equals("edit profile")) {
            int tts2 = textToSpeech.speak("what do you want to change?", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_EDIT");
            if (tts2 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        else{
            int tts1=textToSpeech.speak("Wrong input provided. Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
    }
    private void showProfile(FirebaseUser firebaseUser) {
        String userIDofRegistered = firebaseUser.getUid();

//Extracting User Reference from Database for "Registered users"
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");

        UP_progressBar.setVisibility(View.VISIBLE);

        referenceProfile.child(userIDofRegistered).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                if (readUserDetails != null) {
                    Name = readUserDetails.name;
                    Phone = readUserDetails.phoneNo;
                    Institute = readUserDetails.institute;
                    Username = readUserDetails.userName;
                    finalRole=readUserDetails.finalRole;
                    email=firebaseUser.getEmail();
                    UP_Name.setHint(Name);
                    UP_Phone.setHint(Phone);
                    UP_Institute.setHint(Institute);
                    UP_UserName.setHint(Username);
                } else {
                    Toast.makeText(UpdateProfile.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                }
                UP_progressBar.setVisibility(View.GONE);
            }
                @Override
                public void onCancelled (@NonNull DatabaseError error){
                    Toast.makeText(UpdateProfile.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                    UP_progressBar.setVisibility(View.GONE);
                }
        });
    }

    public void updateProfile(FirebaseUser firebaseUser){
        Name=UP_Name.getText().toString();
        Phone=UP_Phone.getText().toString();
        Institute=UP_Institute.getText().toString();
        Username=UP_UserName.getText().toString();

        if (TextUtils.isEmpty(Name)) {
            Name=UP_Name.getHint().toString();
        }
        if (TextUtils.isEmpty(Phone)) {
            Phone=UP_Phone.getHint().toString();
        } else if(Phone.length()!=10){
            UP_Phone.setError("Valid PhoneNo. is required");
            UP_Phone.requestFocus();
        }
        if (TextUtils.isEmpty(Institute)) {
            Institute=UP_Institute.getHint().toString();
        }
        if (TextUtils.isEmpty(Username)) {
            Username=UP_UserName.getHint().toString();
        }
        ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(email, Name, Phone, Institute, Username, finalRole);
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        assert firebaseUser != null;
        referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(UpdateProfile.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(UpdateProfile.this, Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl",Rl);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onWakeWordDetected() {
        Toast.makeText(this, "Wakeword Detected"+appstate, Toast.LENGTH_SHORT).show();
        if(speechRecognizerIntent!=null){
            appstate= AState.AppState.AUTOMATE;
            pauseToastTimer();
            speechRecognizer.startListening(speechRecognizerIntent);
        }
        else{
            Toast.makeText(this, "Null Speech 2", Toast.LENGTH_SHORT).show();
        }
    }
}
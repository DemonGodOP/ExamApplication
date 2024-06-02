package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

public class ChangePassword extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener{
    FirebaseAuth authProfile;
    EditText CP_Password, CP_NewPassword, CP_ConfirmPassword;
    TextView CP_Text,CPTP;
    Button CP_Authenticate, CP_Button;
    ProgressBar CP_progressBar;
    String userPwdCurr;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted; // Flag to indicate if TextToSpeech engine is initialized
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
        setContentView(R.layout.activity_change_password);

        Rl=getIntent().getStringExtra("Rl");
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

        CP_Password= findViewById(R.id.CP_Password);
        CP_NewPassword = findViewById(R.id.CP_NewPassword);
        CP_ConfirmPassword= findViewById(R.id.CP_ConfirmPassword);
        CP_Text= findViewById(R.id.CP_Text);
        CP_progressBar = findViewById(R.id.CP_progressBar);
        CP_Authenticate=findViewById(R.id.CP_Authenticate);
        CP_Button = findViewById(R.id.CP_Button);

        CP_NewPassword.setEnabled(false);
        CP_ConfirmPassword.setEnabled(false);
        CP_Button.setEnabled(false);
        CPTP=findViewById(R.id.CPTP);
        CPTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Rl.equals("Teacher")) {
                    Intent intent = new Intent(ChangePassword.this, Profile.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Rl", "Teacher");
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(ChangePassword.this, Profile.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Rl", "Student");
                    startActivity(intent);
                    finish();
                }
            }
        });
        authProfile = FirebaseAuth.getInstance();
        firebaseUser = authProfile.getCurrentUser();

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
                speechRecognizer.setRecognitionListener(new ChangePassword.SpeechListener());
            } else {
                // Permission already granted
                requestRecordPermission();
            }
        }
        if(firebaseUser.equals("")) {
            Toast.makeText(ChangePassword.this, "Something went wrong! User's details not available", Toast.LENGTH_SHORT).show();
            Intent intent= new Intent(ChangePassword.this, Profile.class);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();
        }else{
            reAuthenticateUser(firebaseUser);
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
            speechRecognizer.setRecognitionListener(new ChangePassword.SpeechListener());
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
                    Toast.makeText(ChangePassword.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(ChangePassword.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(ChangePassword.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(ChangePassword.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    return;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Toast.makeText(ChangePassword.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(ChangePassword.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Toast.makeText(ChangePassword.this, "No speech input.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(ChangePassword.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ChangePassword.this, "Listening", Toast.LENGTH_SHORT).show();
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
                STTData=" ";
                appstate= AState.AppState.WAKEWORD;
                resetToastTimer();
                wakeWordHelper.startListening();
                Toast.makeText(ChangePassword.this, "Listening", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ChangePassword.this, "Listening", Toast.LENGTH_SHORT).show();
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
            else if(utteranceId.equals("TTS_UTTERANCE_CHANGE_PASSWORD")){
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(ChangePassword.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String userPwdCur = STTData.replace(" ","");
                        if(userPwdCur==null||userPwdCur.length()<8) {
                            int tts1 = textToSpeech.speak("Wrong Password Entered, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                            if (tts1 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                        else{
                            CP_Password.setText(userPwdCur);
                            AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), userPwdCur);
                            firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        CP_Password.setEnabled(false);
                                        CP_ConfirmPassword.setEnabled(true);
                                        CP_NewPassword.setEnabled(true);

                                        //enable change Pwd button. Disable authenticate button
                                        CP_Authenticate.setEnabled(false);
                                        CP_Button.setEnabled(true);
                                        CP_Text.setText("You are authenticated/verified." + "You can change your password now!");
                                        Toast.makeText(ChangePassword.this, "Password has been verified" + "Change password now", Toast.LENGTH_SHORT).show();

                                        //update color of change password button
                                        int color = ContextCompat.getColor(ChangePassword.this, R.color.dark_green);

                                        CP_Button.setBackgroundTintList(ColorStateList.valueOf(color));
                                        CP_Button.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View V) {
                                                changePwd(firebaseUser);
                                            }
                                        });
                                        AutomateChangePwd();
                                    } else {
                                        try {
                                            throw task.getException();
                                        } catch (FirebaseAuthInvalidCredentialsException e) {
                                            int tts2 = textToSpeech.speak("Password Cannot be less than 8 character,Please Start the Process again, Starting WakeWord Engine", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                            if (tts2 == TextToSpeech.SUCCESS) {
                                                // Pause the timer until TTS completes
                                                pauseToastTimer();
                                            }
                                            CP_Password.setError("Wrong Password Entered");
                                            CP_Password.requestFocus();
                                        } catch (Exception e) {
                                            Toast.makeText(ChangePassword.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                        }
                    }
                },7000);
            }
            else if(utteranceId.equals("TTS_UTTERANCE_AUTOMATE_PASSWORD")){
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(ChangePassword.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String change = STTData.replace(" ","");
                        if(change==null||change.length()<8) {
                            int tts1 = textToSpeech.speak("Password Cannot be less than 8 character,Please Start the Process again, Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                            if (tts1 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                        else {
                            CP_NewPassword.setText(change);
                            int tts2 = textToSpeech.speak("Please confirm your new password", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                            if (tts2 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    appstate = AState.AppState.STT;
                                    runOnUiThread(() -> {
                                        try {
                                            speechRecognizer.startListening(speechRecognizerIntent);
                                            Log.d("STT", "Speech recognizer started listening.");
                                        } catch (Exception e) {
                                            Log.e("STT", "Exception starting speech recognizer", e);
                                        }

                                        // Ensure the Toast is shown on the main thread
                                        Toast.makeText(ChangePassword.this, "Listening", Toast.LENGTH_SHORT).show();
                                    });
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            speechRecognizer.stopListening();
                                            String userPwdCur = STTData.replace(" ","");
                                            if(change==null||change.length()<8) {
                                                int tts1 = textToSpeech.speak("Password Cannot be less than 8 character,Please Start the Process again, Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                                if (tts1 == TextToSpeech.SUCCESS) {
                                                    // Pause the timer until TTS completes
                                                    pauseToastTimer();
                                                }
                                            }
                                            else {
                                                if (!change.matches(userPwdCur)) {
                                                    int tts3 = textToSpeech.speak("Password did not match. Please Restart the Process. Starting WakeWord Engine", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                                    if (tts3 == TextToSpeech.SUCCESS) {
                                                        // Pause the timer until TTS completes
                                                        pauseToastTimer();
                                                    }// Toast.makeText(ChangePassword.this, "Password did not match", Toast.LENGTH_SHORT).show();
                                                    CP_ConfirmPassword.setError("Please re-enter same password");
                                                    CP_ConfirmPassword.requestFocus();
                                                } else if (change.matches(CP_Password.getText().toString())) {
                                                    int tts4 = textToSpeech.speak("New password cannot be same as old password. Please Restart the Process. Starting WakeWord Engine", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                                    //Toast.makeText(ChangePassword.this, "New password cannot be same as old password", Toast.LENGTH_SHORT).show();
                                                    if (tts4 == TextToSpeech.SUCCESS) {
                                                        // Pause the timer until TTS completes
                                                        pauseToastTimer();
                                                    }
                                                    CP_NewPassword.setError("Please enter a new password");
                                                    CP_NewPassword.requestFocus();
                                                } else {
                                                    CP_progressBar.setVisibility(View.VISIBLE);
                                                    firebaseUser.updatePassword(userPwdCur).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                int tts5 = textToSpeech.speak("Password has been changed.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_GO_TO_PROFILE");
                                                                //Toast.makeText(ChangePassword.this, "New password cannot be same as old password", Toast.LENGTH_SHORT).show();
                                                                if (tts5 == TextToSpeech.SUCCESS) {
                                                                    // Pause the timer until TTS completes
                                                                    pauseToastTimer();
                                                                }
                                                            } else {
                                                                try {
                                                                    throw task.getException();
                                                                } catch (Exception e) {
                                                                    Toast.makeText(ChangePassword.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                            CP_progressBar.setVisibility(View.GONE);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }, 7000);
                                }
                            }, 4000);
                        }
                    }
                },7000);
            }
            else if(utteranceId.equals("TTS_UTTERANCE_GO_TO_PROFILE")){
                CP_progressBar.setVisibility(View.GONE);
                Toast.makeText(ChangePassword.this, "Password has been changed", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ChangePassword.this, Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl", Rl);
                startActivity(intent);
                finish();
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
                authProfile = FirebaseAuth.getInstance();
                firebaseUser = authProfile.getCurrentUser();
                // TTS initialization successful, set language and convert text to speech
                isTTSInitialized = true;
                textToSpeech.setLanguage(Locale.US);
                //Locale locale = new Locale("en","IN");
                //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
                //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
                //textToSpeech.setVoice(voice);
                int ttsResult = textToSpeech.speak("Hello, Welcome to the Change PassWord of Exam Care, Would you like to listen to a Detailed introduction of the page. Please say Yes or No", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ONINIT");
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
        textToSpeech.setSpeechRate(0.85f);
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Change Password Page of Exam Care, This page provides you with the facility, to " +
                "change your password, for that please say your old password in order to authenticate yourself, then new password," +
                "and after that say hello exam care password. Then a link will be sent to your " +
                "registered email id, from there you can change your password.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
            textToSpeech.setSpeechRate(1.0f);
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
        if(Temp.equals("repeat introduction")){
            StarUpRepeat();
        }else if(Temp.equals("back")){
            Intent intent=new Intent(ChangePassword.this,Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();
        }else if (Temp.equals("change password")) {
            CP_Password.setEnabled(true);
            CP_Password.setText("");
            CP_ConfirmPassword.setEnabled(false);
            CP_ConfirmPassword.setText("");
            CP_NewPassword.setEnabled(false);
            CP_NewPassword.setText("");


            //enable change Pwd button. Disable authenticate button
            CP_Authenticate.setEnabled(true);
            CP_Button.setEnabled(false);
            CP_Text.setText("Please Verify Your Password Before Continuing");

            CP_Button.setBackgroundColor(Color.parseColor("#ff6750a4"));
            int tts1 = textToSpeech.speak("Please say your old password", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_CHANGE_PASSWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        else{
            Toast.makeText(this, Temp, Toast.LENGTH_SHORT).show();
            int tts1=textToSpeech.speak("Wrong input provided "+Temp+ " Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
    }
    public void AutomateChangePwd () {
        int tts1 = textToSpeech.speak("Please say your new password", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_AUTOMATE_PASSWORD");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
    }
    private void reAuthenticateUser(FirebaseUser firebaseUser){
        CP_Authenticate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                userPwdCurr = CP_Password.getText().toString();

                if(TextUtils.isEmpty(userPwdCurr)){
                    Toast.makeText(ChangePassword.this, "password is needed", Toast.LENGTH_SHORT).show();
                    CP_Password.setError("Please enter your current password to authenticator");
                    CP_Password.requestFocus();
                }else {
                    CP_progressBar.setVisibility(View.VISIBLE);

                    //ReAuthenticate User now
                    AuthCredential credential= EmailAuthProvider.getCredential(firebaseUser.getEmail(), userPwdCurr);

                    firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task){
                            if(task.isSuccessful()){
                                CP_progressBar.setVisibility(View.GONE);

                                //Disable editText for current password, enable EditText for new password and confirm new password
                                CP_Password.setEnabled(false);
                                CP_ConfirmPassword.setEnabled(true);
                                CP_NewPassword.setEnabled(true);

                                //enable change Pwd button. Disable authenticate button
                                CP_Authenticate.setEnabled(false);
                                CP_Button.setEnabled(true);

                                //set TextView to show user is authenticated/verified
                                CP_Text.setText("You are authenticated/verified."+"You can changer password now!");
                                Toast.makeText(ChangePassword.this,"Password has been verified"+ "Change password now", Toast.LENGTH_SHORT).show();

                                //update color of change password button
                                int color=ContextCompat.getColor(ChangePassword.this, R.color.dark_green);;
                                CP_Button.setBackgroundTintList(ColorStateList.valueOf(color));
                                CP_Button.setOnClickListener(new View.OnClickListener(){
                                    @Override
                                    public void onClick(View V){
                                        changePwd(firebaseUser);
                                    }
                                });
                            }else {
                                try{
                                    throw task.getException();
                                }catch(FirebaseAuthInvalidCredentialsException e)
                                {
                                    CP_Password.setError("Wrong Password Entered");
                                    CP_Password.requestFocus();
                                } catch(Exception e){
                                    Toast.makeText(ChangePassword.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            CP_progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }
    private void changePwd(FirebaseUser firebaseUser){
        String userPwdNew = CP_NewPassword.getText().toString();
        String userPwdConfirmNew =  CP_ConfirmPassword.getText().toString();

        if(TextUtils.isEmpty(userPwdNew)){
            Toast.makeText(ChangePassword.this, "New password is needed", Toast.LENGTH_SHORT).show();
            CP_NewPassword.setError("Please enter your new password");
            CP_NewPassword.requestFocus();
        }else if(TextUtils.isEmpty(userPwdConfirmNew)){
            Toast.makeText(ChangePassword.this, "Please confirm your new password", Toast.LENGTH_SHORT).show();
            CP_ConfirmPassword.setError("Please re-enter your password");
            CP_ConfirmPassword.requestFocus();
        }else if(!userPwdNew.matches(userPwdConfirmNew)){
            Toast.makeText(ChangePassword.this, "Password did not match", Toast.LENGTH_SHORT).show();
            CP_ConfirmPassword.setError("Please re-enter same password");
            CP_ConfirmPassword.requestFocus();
        }
        else if(userPwdNew.matches(userPwdCurr)){
            Toast.makeText(ChangePassword.this, "New password cannot be same as old password", Toast.LENGTH_SHORT).show();
            CP_NewPassword.setError("Please enter a new password");
            CP_NewPassword.requestFocus();
        }else {
            CP_progressBar.setVisibility(View.VISIBLE);

            firebaseUser.updatePassword(userPwdNew).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task){
                    if(task.isSuccessful()){
                        Toast.makeText(ChangePassword.this, "Password has been changed", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ChangePassword.this, Profile.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("Rl", Rl);
                        startActivity(intent);
                        finish();
                    }else{
                        try{
                            throw task.getException();
                        }catch (Exception e){
                            Toast.makeText(ChangePassword.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    CP_progressBar.setVisibility(View.GONE);
                }
        });
        }
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
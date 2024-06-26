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
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class StudentGroupDetails extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener  {
    TextView SGDTSG,SGD_GN,SGD_SN,SGD_SC,SGD_CB,SGD_GD;

    ProgressBar SGD_progressBar;
    String GN,SN,SC,CB,GD;
    FirebaseAuth authProfile;

    String Group_ID;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    Group readUserDetails;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

    String STTData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_group_details);
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        SGD_GN = findViewById(R.id.SGD_GN);
        SGD_SN = findViewById(R.id.SGD_SN);
        SGD_SC = findViewById(R.id.SGD_SC);
        SGD_CB = findViewById(R.id.SGD_CB);
        SGD_GD = findViewById(R.id.SGD_GD);
        SGD_progressBar = findViewById(R.id.SGD_progressBar);

        Intent intent = getIntent();
        Group_ID = intent.getStringExtra("GROUP_ID");

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        appstate = AState.AppState.TTS;

        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        SGD_progressBar.setVisibility(View.VISIBLE);
        showGroupDetails(firebaseUser);

        SGDTSG=findViewById(R.id.SGDTSG);
        SGDTSG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentGroupDetails.this, StudentGroup.class);

                // Pass the unique key to the new activity
                intent.putExtra("GROUP_ID", Group_ID);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                // Start the new activity
                startActivity(intent);
                finish();
            }
        });

        if (hasRecordPermission()){
            wakeWordHelper=new WakeWordHelper(this,appstate,this);
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new StudentGroupDetails.SpeechListener());
        } else {
            // Permission already granted
            requestRecordPermission();
        }
        handler = new Handler();//2

        isUserInteracted = false;
        isTTSInitialized = false;

        toastRunnable = new Runnable() {
            @Override
            public void run() {
                Repeat();
            }
        };

        // Start the initial delay
        startToastTimer();//2
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(speechRecognizer!=null) {
            speechRecognizer.stopListening();
        }
        pauseToastTimer();
        if(wakeWordHelper!=null) {
            wakeWordHelper.stopListening();
            appstate= AState.AppState.TTS;
        }
        if(textToSpeech!=null) {
            textToSpeech.stop();
        }
    }

    @Override //3
    protected void onResume() {
        super.onResume();
        // Reset the timer whenever the user interacts with the app
        resetToastTimer();
        isUserInteracted = false; // Reset user interaction flag
        if (textToSpeech != null) {
            int ttsResult=textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care, Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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

    @Override
    protected void onStop() {
        super.onStop();
        if (speechRecognizer != null) {
            speechRecognizer.stopListening(); // Destroy the speech recognizer when the app is no longer visible
        }
        if(textToSpeech!=null){
            textToSpeech.stop();
        }
        pauseToastTimer();
        if(wakeWordHelper!=null) {
            wakeWordHelper.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        // Release resources
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy(); // Destroy the speech recognizer when the app is no longer visible
        }
        if(wakeWordHelper!=null) {
            wakeWordHelper.stopListening();
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
            speechRecognizer.setRecognitionListener(new StudentGroupDetails.SpeechListener());
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
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Toast.makeText(StudentGroupDetails.this, "No speech input.", Toast.LENGTH_SHORT).show();
                    if(appstate== AState.AppState.AUTOMATE){
                        int tts1 = textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                        if (tts1 == TextToSpeech.SUCCESS) {
                            // Pause the timer until TTS completes
                            pauseToastTimer();
                        }
                    }
                    else if(appstate== AState.AppState.STT){
                        STTData="";
                    }
                    break;
                case SpeechRecognizer.ERROR_AUDIO:
                    Toast.makeText(StudentGroupDetails.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(StudentGroupDetails.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(StudentGroupDetails.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(StudentGroupDetails.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
                    if(appstate== AState.AppState.AUTOMATE){
                        int tts1 = textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                        if (tts1 == TextToSpeech.SUCCESS) {
                            // Pause the timer until TTS completes
                            pauseToastTimer();
                        }
                    }
                    else if(appstate== AState.AppState.STT){
                        STTData="";
                    }
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    return;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Toast.makeText(StudentGroupDetails.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(StudentGroupDetails.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(StudentGroupDetails.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(StudentGroupDetails.this, "Listening", Toast.LENGTH_SHORT).show();
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
                resetToastTimer();
                wakeWordHelper.startListening();
                Toast.makeText(StudentGroupDetails.this, "Listening", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentGroupDetails.this, "Listening", Toast.LENGTH_SHORT).show();
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
            resetToastTimer();
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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



    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // TTS initialization successful, set language and convert text to speech
            isTTSInitialized = true;
            textToSpeech.setLanguage(Locale.US);
            //Locale locale = new Locale("en","IN");
            //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
            //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
            //textToSpeech.setVoice(voice);
            int ttsResult=textToSpeech.speak("Welcome to the student Group Details Page of Exam Care,  Would you like to listen to a Detailed introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ONINIT");
            if (ttsResult == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        } else {
            // TTS initialization failed, handle error
            Log.e("TTS", "Initialization failed");
        }
    }

    // Repeat The Introduction if Repeat Method is Triggered.
    public void StarUpRepeat(){
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        textToSpeech.setSpeechRate(0.85f);
        int ttsResult=textToSpeech.speak("Hello, Welcome to the student Group Details Page of Exam Care, This page provides you with the facility, to " +
                "know about your group name, subject name, subject code, creator, and group description, you just have to say Hello exam care, group details." +
                " or you can go back to the group page just by saying Exam care, Back, If you want me to repeat the introduction of the page again " +
                "please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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



    public void Automate(String Temp){
        wakeWordHelper.stopListening();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        appstate= AState.AppState.TTS;
        if(Temp.equals("repeat introduction")){
            StarUpRepeat();
        }
        else if(Temp.equals("back")){
            Intent intent = new Intent(StudentGroupDetails.this, StudentGroup.class);

            // Pass the unique key to the new activity
            intent.putExtra("GROUP_ID", Group_ID);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            // Start the new activity
            startActivity(intent);
            finish();
        }
        else if(Temp.equals("group details")){
            int tts1=textToSpeech.speak("Group Name of the Current group is"+readUserDetails.Group_Name+"Subject Name is"+readUserDetails.Subject_Code+"Subject Code is"+readUserDetails.Subject_Code+"Teacher Name is"+readUserDetails.TeacherName+"Group Description is"+readUserDetails.Description, TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
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

    public void showGroupDetails(FirebaseUser firebaseUser){

        //Extracting User Reference from Database for "Registered Users"
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).child("Groups").child(Group_ID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                readUserDetails=snapshot.getValue(Group.class);
                if(readUserDetails != null){
                    GN=readUserDetails.Group_Name;
                    SN=readUserDetails.Subject_Name;
                    SC=readUserDetails.Subject_Code;
                    CB=readUserDetails.TeacherName;
                    GD=readUserDetails.Description;
                    SGD_GN.setText(GN);
                    SGD_SN.setText(SN);
                    SGD_SC.setText(SC);
                    SGD_CB.setText(CB);
                    SGD_GD.setText(GD);
                }else {
                    Toast.makeText(StudentGroupDetails.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
                SGD_progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error){

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
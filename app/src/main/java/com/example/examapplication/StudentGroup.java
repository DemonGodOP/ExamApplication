package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.ListView;
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
import java.util.List;
import java.util.Locale;

public class StudentGroup extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener {
    TextView SGTSHP,SG_GroupDetails,SG_Text,SG_NoText;
    ListView SG_LV;
    ProgressBar SG_P;
    String GROUP_ID;


    FirebaseAuth authProfile;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1


    FirebaseUser firebaseUser;


    List<Assignment> AssignmentList;

    private SpeechRecognizer speechRecognizer;

    private Intent speechRecognizerIntent;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

    String STTData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_group);



        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        appstate = AState.AppState.TTS;

        Intent intent=getIntent();
        GROUP_ID=intent.getStringExtra("GROUP_ID");
        SGTSHP=findViewById(R.id.SGTSHP);
        SG_GroupDetails=findViewById(R.id.SG_GroupDetails);
        SG_Text=findViewById(R.id.SG_Text);
        SG_NoText=findViewById(R.id.SG_NoText);
        SG_LV=findViewById(R.id.SG_LV);
        SG_P=findViewById(R.id.SG_P);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        SGTSHP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentGroup.this, StudentHomePage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl","Student");


                // Start the new activity
                startActivity(intent);

                finish();
            }
        });

        SG_GroupDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentGroup.this, StudentGroupDetails.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                // Pass the unique key to the new activity
                intent.putExtra("GROUP_ID", GROUP_ID);

                // Start the new activity
                startActivity(intent);
                finish();
            }
        });

        SG_P.setVisibility(View.VISIBLE);
        PopulateList();

        if (hasRecordPermission()){
            wakeWordHelper=new WakeWordHelper(this,appstate,this);
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new StudentGroup.SpeechListener());
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
            speechRecognizer.setRecognitionListener(new StudentGroup.SpeechListener());
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
                Toast.makeText(StudentGroup.this, "Listening", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentGroup.this, "Listening", Toast.LENGTH_SHORT).show();
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
            else if(utteranceId.length()<6){
                if (Integer.parseInt(utteranceId) < AssignmentList.size()) {
                    int j = Integer.parseInt(utteranceId);
                    wakeWordHelper.stopListening();
                    int tts1 = textToSpeech.speak((j + 1) + "Assignment name is" + AssignmentList.get(j).Name + "and Assignment Timing is" + AssignmentList.get(j).Timing + "Do you want to enter the current Assignment page please say yes or no", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                    if (tts1 == TextToSpeech.SUCCESS) {
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
                            });

                            // Ensure the Toast is shown on the main thread
                            Toast.makeText(StudentGroup.this, "Listening", Toast.LENGTH_SHORT).show();

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    speechRecognizer.stopListening();
                                    String YN = STTData;
                                    if (YN != null && YN.equals("yes")) {
                                        Assignment selectedAssignment = AssignmentList.get(j);

                                        DatabaseReference Checking = FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(selectedAssignment.Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                                        Checking.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.exists()) {
                                                    Intent intent = new Intent(StudentGroup.this, StudentFeedBack.class);

                                                    //intent.putExtra("GROUP_ID", GROUP_ID);
                                                    intent.putExtra("Assignment_ID", selectedAssignment.Assignment_ID);

                                                    intent.putExtra("Group_ID", GROUP_ID);

                                                    // Start the new activity
                                                    startActivity(intent);
                                                } else {
                                                    int tts2 = textToSpeech.speak("You have not yet taken this assignment. Do you want to start the begin attempting the assignment? Yes or No", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                                                    if (tts2 == TextToSpeech.SUCCESS) {
                                                        // Pause the timer until TTS completes
                                                        pauseToastTimer();
                                                    }
                                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            runOnUiThread(() -> {
                                                                try {
                                                                    speechRecognizer.startListening(speechRecognizerIntent);
                                                                    Log.d("STT", "Speech recognizer started listening.");
                                                                } catch (Exception e) {
                                                                    Log.e("STT", "Exception starting speech recognizer", e);
                                                                }
                                                                Toast.makeText(StudentGroup.this, "Listening", Toast.LENGTH_SHORT).show();
                                                            });


                                                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    speechRecognizer.stopListening();
                                                                    String YN2 = STTData;
                                                                    if (YN2 != null && YN2.equals("yes")) {
                                                                        boolean Active = selectedAssignment.Active;
                                                                        if (Active == true) {
                                                                            Intent intent = new Intent(StudentGroup.this, AssignmentSubmission.class);

                                                                            //intent.putExtra("GROUP_ID", GROUP_ID);
                                                                            intent.putExtra("Assignment_ID", selectedAssignment.Assignment_ID);

                                                                            intent.putExtra("Group_ID", GROUP_ID);

                                                                            // Start the new activity
                                                                            startActivity(intent);

                                                                            finish();
                                                                        } else {
                                                                            int tts3 = textToSpeech.speak("Assignment is Not Active Right Now. Please Wait until it activated by the owner of the Group.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                                                            if (tts3 == TextToSpeech.SUCCESS) {
                                                                                // Pause the timer until TTS completes
                                                                                pauseToastTimer();
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }, 5000);
                                                        }
                                                    }, 6000);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    } else {
                                        if(Integer.parseInt(utteranceId)+1<AssignmentList.size()) {
                                            int tts4 = textToSpeech.speak("Moving On toThe Next Assignement", TextToSpeech.QUEUE_FLUSH, null, Integer.parseInt(utteranceId) + 1 + "");
                                            if (tts4 == TextToSpeech.SUCCESS) {
                                                // Pause the timer until TTS completes
                                                pauseToastTimer();
                                            }
                                        }
                                        else{
                                            int tts1 = textToSpeech.speak("Assignment List Ended. Please Say Exam Care Repeat Introduction to Listen to the Introduction of the page", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                            if (tts1 == TextToSpeech.SUCCESS) {
                                                // Pause the timer until TTS completes
                                                pauseToastTimer();
                                            }
                                        }
                                    }
                                }
                            }, 5000);
                        }
                    }, 9000);
                }

                        }
            resetToastTimer();
        }
    };

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
                    Toast.makeText(StudentGroup.this, "No speech input.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentGroup.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(StudentGroup.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(StudentGroup.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(StudentGroup.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentGroup.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(StudentGroup.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(StudentGroup.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(StudentGroup.this, "Listening", Toast.LENGTH_SHORT).show();
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
                int ttsResult = textToSpeech.speak("Hello, Welcome to the Student Group Page of Exam Care. Do you like to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ONINIT");
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
        if(appstate== AState.AppState.WAKEWORD){
            wakeWordHelper.stopListening();
        }
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        textToSpeech.setSpeechRate(0.85f);
        int ttsResult = textToSpeech.speak("Hello, Welcome to the Student Group Page of Exam Care, This page provides you with the facility, to " +
                " know about your assignments which you can attempt, you just have to say, Hello Exam care, assignment list or you can Query about the Group Details, you just have to say, Hello Exam care, Group Details or you can go back to the homepage just say Hello Exam Care, Home Page", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
            textToSpeech.setSpeechRate(1.0f);
        }

    }

    public void Repeat(){
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult = textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
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
        else if(Temp.equals("HomePage")){
            Intent intent = new Intent(StudentGroup.this, StudentHomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl","Student");


            // Start the new activity
            startActivity(intent);

            finish();
        }
        else if(Temp.equals("group details")) {
            Intent intent = new Intent(StudentGroup.this, StudentGroupDetails.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            // Pass the unique key to the new activity
            intent.putExtra("GROUP_ID", GROUP_ID);

            // Start the new activity
            startActivity(intent);
            finish();
        }
        else if(Temp.equals("assignment list")) {
            if (AssignmentList.isEmpty()) {
                int tts0 = textToSpeech.speak("Currently No Assignments are posted in the Group. Please Wait until the group owner post some assignments.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                if (tts0 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else {
                int i=0;
                int tts5 = textToSpeech.speak("Reading the Assignment Names Now", TextToSpeech.QUEUE_FLUSH, null, i+"");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
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

    public void PopulateList() {
        DatabaseReference JoiningReference = FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID);


        // Listener to retrieve the groups
        JoiningReference.child("Assignments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                AssignmentList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Assignment assignment = snapshot.getValue(Assignment.class);
                    if (assignment != null) {
                        AssignmentList.add(assignment);
                    }
                }

                if (AssignmentList.isEmpty()) {
                    // If there are no groups, display the message TextView and hide the ListView
                    SG_NoText.setVisibility(View.VISIBLE);
                    SG_LV.setVisibility(View.GONE);
                    SG_P.setVisibility(View.GONE);
                    SG_Text.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    SG_NoText.setVisibility(View.GONE);
                    SG_LV.setVisibility(View.VISIBLE);
                    SG_Text.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    ADLV adapter = new ADLV(StudentGroup.this, R.layout.adlv, AssignmentList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    SG_LV.setAdapter(adapter);

                    SG_P.setVisibility(View.GONE);

                    SG_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                            Assignment selectedAssignment = (Assignment) parent.getItemAtPosition(position);

                            DatabaseReference Checking=FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(selectedAssignment.Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                            Checking.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.exists()){
                                        Intent intent = new Intent(StudentGroup.this, StudentFeedBack.class);

                                        //intent.putExtra("GROUP_ID", GROUP_ID);
                                        intent.putExtra("Assignment_ID",selectedAssignment.Assignment_ID);

                                        intent.putExtra("Group_ID",GROUP_ID);

                                        // Start the new activity
                                        startActivity(intent);
                                    }
                                    else{
                                        showAlertDialog(selectedAssignment);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showAlertDialog (Assignment selectedAssignment) {

        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(StudentGroup.this);
        builder.setTitle("Start Exam?");
        builder.setMessage("Do you want to Start the Exam? Once Started You Can't Exit Before Submitting.");


        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference Active=FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(selectedAssignment.Assignment_ID);
                Active.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Assignment assignment=snapshot.getValue(Assignment.class);
                        if(assignment!=null){
                            boolean Active=assignment.Active;
                            if(Active==true){
                                Intent intent = new Intent(StudentGroup.this, AssignmentSubmission.class);

                                //intent.putExtra("GROUP_ID", GROUP_ID);
                                intent.putExtra("Assignment_ID",selectedAssignment.Assignment_ID);

                                intent.putExtra("Group_ID",GROUP_ID);

                                // Start the new activity
                                startActivity(intent);

                                finish();
                            }
                            else{
                                Toast.makeText(StudentGroup.this, "Assignment is Not Active Right Now", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog=builder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red));
            }
        });

        //show the alert dialog
        alertDialog.show();
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
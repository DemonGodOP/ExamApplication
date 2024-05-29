package com.example.examapplication;
import android.Manifest;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


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


public class StudentHomePage extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener{

    TextView SignOut,SHM_NoText;
    FirebaseAuth authProfile;

    TextView S_Profile;
    ProgressBar SH_progressBar;

    TextView SHM_SG;

    Button SHM_Search;

    ListView SHM_LV;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1


    String Rl;

    List<Group> groupsList;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

    String STTData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        setContentView(R.layout.activity_student_home_page);
        Intent intent = getIntent();

        Rl= intent.getStringExtra("Rl");

        SH_progressBar=findViewById(R.id.SH_progressBar);
        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        //Check if email is verified before user can access their profile

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        appstate = AState.AppState.TTS;

        SHM_LV=findViewById(R.id.SHM_LV);
        SHM_Search=findViewById(R.id.SHM_Search);
        SHM_SG=findViewById(R.id.SHM_SG);
        SHM_NoText=findViewById(R.id.SHM_NoText);

        if (!firebaseUser.isEmailVerified()) {
            firebaseUser.sendEmailVerification();
            showAlertDialog();
        }
        SignOut=findViewById(R.id.S_SignOut);
        SignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SH_progressBar.setVisibility(View.VISIBLE);
                authProfile.signOut();
                Intent intent=new Intent(StudentHomePage.this,Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                SH_progressBar.setVisibility(View.GONE);
                finish();
            }
        });

        S_Profile=findViewById(R.id.S_Profile);
        S_Profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(StudentHomePage.this,Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("Rl","Student");
                startActivity(intent);
                finish();
            }
        });

        SHM_Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Group_ID=SHM_SG.getText().toString();
                if (TextUtils.isEmpty(Group_ID)) {
                    SHM_SG.setError("Please Enter a Group ID to Search");
                    SHM_SG.requestFocus();
                }else{
                    Intent intent = new Intent(StudentHomePage.this, SearchingGroup.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                    // Pass the unique key to the new activity
                    intent.putExtra("GROUP_ID", Group_ID);

                    // Start the new activity
                    startActivity(intent);

                    finish();
                }
            }
        });

        if (hasRecordPermission()){
            wakeWordHelper=new WakeWordHelper(this,appstate,this);
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new SpeechListener());
        } else {
            // Permission already granted
            requestRecordPermission();
        }
        handler = new Handler();//2


        toastRunnable = new Runnable() {
            @Override
            public void run() {
                Repeat();
            }
        };

        // Start the initial delay
        startToastTimer();//2
        SH_progressBar.setVisibility(View.VISIBLE);
        PopulateList(firebaseUser);

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
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
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
            speechRecognizer.setRecognitionListener(new SpeechListener());
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
                    Toast.makeText(StudentHomePage.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(StudentHomePage.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(StudentHomePage.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(StudentHomePage.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    return;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Toast.makeText(StudentHomePage.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(StudentHomePage.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Toast.makeText(StudentHomePage.this, "No speech input.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(StudentHomePage.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(StudentHomePage.this, "Listening", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(StudentHomePage.this, "Listening", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentHomePage.this, "Listening", Toast.LENGTH_SHORT).show();
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
            else if(utteranceId.equals("STT_UTTERANCE_SEARCHGROUP")){
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(StudentHomePage.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                appstate= AState.AppState.STT;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String groupId = STTData;
                        Intent intent = new Intent(StudentHomePage.this, SearchingGroup.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        // Pass the unique key to the new activity
                        intent.putExtra("GROUP_ID", groupId);
                        // Start the new activity
                        startActivity(intent);
                    }
                },5000);
            }
            else if(utteranceId.length()<6){
                wakeWordHelper.stopListening();
                int j=Integer.parseInt(utteranceId);
                appstate = AState.AppState.STT;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(StudentHomePage.this, "Listening", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            speechRecognizer.stopListening();
                            String YN = STTData;
                            if (YN != null && YN.equals("yes")) {
                                String selectedGroupId = groupsList.get(j).Group_ID; // Or however you store the ID in the Group class

                                // Create an intent to start a new activity
                                Intent intent = new Intent(StudentHomePage.this, StudentGroup.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("Rl", "Student");

                                // Pass the unique key to the new activity
                                intent.putExtra("GROUP_ID", selectedGroupId);

                                // Start the new activity
                                startActivity(intent);

                                finish();
                            } else {
                                int tts1 = textToSpeech.speak("No Input Detected, Continuing.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                                if (tts1 == TextToSpeech.SUCCESS) {
                                    // Pause the timer until TTS completes
                                    pauseToastTimer();
                                }
                            }
                        }
                    }, 5000);
                });
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
                    int ttsResult = textToSpeech.speak("Hello, Welcome to the Student Home Page of Exam Care, Would you like to listen to a Detailed introduction of the page. Say Yes or No", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ONINIT");
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
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Student Home Page of Exam Care, This page provides you with the facility, to " +
                "see your profile details, for this you have to say, hello Exam care, profile details, " +
                "you can also sign Out if you want, for this you have to say, hello Exam care, sign out, you can also search, existing groups for this,"+
                " that you want to join, you just to say, hello exam care,search group and enter group id, and finally you can check the groups," +
                "that you have already joined, by saying, hello exam care,group names. If you want me to repeat the introduction of the page again please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
            //appstate= AState.AppState.TTS;
            pauseToastTimer();
        }
        //Enter the Condition Over here that is tts to take input from the user if they wants us to repeat the introduction and change r respectively.
        /*boolean r=false;
        if(r==true){
            StarUpRepeat();
        }
        else{
            int tts1=textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1== TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }*/
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
        else if(Temp.equals("profile details")){
            Intent intent=new Intent(StudentHomePage.this,Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Rl","Student");
            startActivity(intent);
            finish();
        }
        else if(Temp.equals("sign out")) {
            SH_progressBar.setVisibility(View.VISIBLE);
            authProfile.signOut();
            Intent intent=new Intent(StudentHomePage.this,Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            SH_progressBar.setVisibility(View.GONE);
            finish();
        }
        else if(Temp.equals("search group")){
            int tts4=textToSpeech.speak("Please say your group Id now", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_SEARCHGROUP");
            if (tts4 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }

        }
        else if(Temp.equals("group names")){
            if(groupsList.isEmpty()){
                int tts5=textToSpeech.speak("You have not joined any groups yet. Please join a group by using the searching group functionality.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else {

                        for (int i = 0; i < groupsList.size(); i++) {
                            int tts5 = textToSpeech.speak("Index" + (i + 1) + "Group name is" + groupsList.get(i).Group_Name + "and Group ID is" + groupsList.get(i).Subject_Code+"Do you want to enter the Group", TextToSpeech.QUEUE_FLUSH, null, i+"");
                            if (tts5 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                }
                            },25000);
                        }
                    }
        }
        else{
            Toast.makeText(this, Temp, Toast.LENGTH_SHORT).show();
            int tts1=textToSpeech.speak("Wrong input provided "+Temp+ " Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        wakeWordHelper.startListening();
    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(StudentHomePage.this);
        builder.setTitle("Email Not Verified");
        builder.setMessage("Please verify your email now.You can not login without email verification.");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent=new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//to email app in new window and not within our app
                startActivity(intent);
            }
        });

        //create the AlertDialog
        AlertDialog alertDialog=builder.create();

        //show the alert dialog
        alertDialog.show();
    }

    public void PopulateList(FirebaseUser firebaseUser){
        //Group ListView
        String StudentId = firebaseUser.getUid();
        DatabaseReference teacherGroupsRef = FirebaseDatabase.getInstance()
                .getReference("Registered Users")
                .child(StudentId)
                .child("Groups");

        // Listener to retrieve the groups
        teacherGroupsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                groupsList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Group group = snapshot.getValue(Group.class);
                    if (group != null) {
                        groupsList.add(group);
                    }
                }

                if (groupsList.isEmpty()) {
                    // If there are no groups, display the message TextView and hide the ListView
                    SHM_NoText.setVisibility(View.VISIBLE);
                    SHM_LV.setVisibility(View.GONE);
                    SH_progressBar.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    SHM_NoText.setVisibility(View.GONE);
                    SHM_LV.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    TGRCL adapter = new TGRCL(StudentHomePage.this, R.layout.tgrcl, groupsList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    SHM_LV.setAdapter(adapter);

                    SH_progressBar.setVisibility(View.GONE);

                    SHM_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                            Group selectedGroup = (Group) parent.getItemAtPosition(position);

                            // Retrieve the unique key of the selected group
                            String selectedGroupId = selectedGroup.Group_ID; // Or however you store the ID in the Group class

                            // Create an intent to start a new activity
                            Intent intent = new Intent(StudentHomePage.this, StudentGroup.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("Rl","Student");

                            // Pass the unique key to the new activity
                            intent.putExtra("GROUP_ID",selectedGroupId);

                            // Start the new activity
                            startActivity(intent);

                            finish();
                        }
                    });
                }
            }
            @Override
            public void onCancelled (@NonNull DatabaseError databaseError){
                Toast.makeText(StudentHomePage.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
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
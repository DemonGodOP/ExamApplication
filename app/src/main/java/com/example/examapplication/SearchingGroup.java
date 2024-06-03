package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class SearchingGroup extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener{
    TextView SRGTSHM,SRG_GroupName,SRG_GroupSubjectCode,SRG_NoText,SG_GNT,SG_GSCT;
    Button SRG_Layout;
    String Group_ID,Username,Email;

    FirebaseAuth authProfile;

    FirebaseUser firebaseUser;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;


    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1

    Group foundGroup;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

    String STTData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching_group);
        Intent intent=getIntent();
        Group_ID=intent.getStringExtra("GROUP_ID");
        Toast.makeText(this, Group_ID, Toast.LENGTH_SHORT).show();



        SRGTSHM=findViewById(R.id.SRGTSHM);
        SRG_GroupName=findViewById(R.id.SRG_GroupName);
        SRG_GroupSubjectCode=findViewById(R.id.SRG_GroupSubjectCode);
        SRG_NoText=findViewById(R.id.SRG_NoText);
        SRG_Layout=findViewById(R.id.SRG_Layout);
        SG_GNT=findViewById(R.id.SG_GNT);
        SG_GSCT=findViewById(R.id.SG_GSCT);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        SRGTSHM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SearchingGroup.this, StudentHomePage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl","Student");

                // Pass the unique key to the new activity


                // Start the new activity
                startActivity(intent);

                finish();
            }
        });


        SRG_Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Current Participants").child(firebaseUser.getUid());
                database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){

                            ParticipantDetails participantDetails = snapshot.getValue(ParticipantDetails.class);
                            if (participantDetails != null) {
                                Intent intent = new Intent(SearchingGroup.this, StudentGroup.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                                // Pass the unique key to the new activity
                                intent.putExtra("GROUP_ID", Group_ID);

                                // Start the new activity
                                startActivity(intent);
                                finish();
                            } }else {
                                DatabaseReference database2 = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Joining Request").child(firebaseUser.getUid());
                                database2.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            ParticipantDetails participantDetails = snapshot.getValue(ParticipantDetails.class);
                                            if (participantDetails != null) {
                                                Toast.makeText(SearchingGroup.this, "Joining Request Not Accepted Yet", Toast.LENGTH_SHORT).show();
                                            } }else {
                                                showAlertDialog();
                                            }
                                    }
                                    @Override
                                    public void onCancelled (@NonNull DatabaseError error){
                                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();

                                    }
                                });
                            }

                    }
                    @Override
                    public void onCancelled (@NonNull DatabaseError error){
                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                        //UP_progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });

        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (hasRecordPermission()){
            wakeWordHelper=new WakeWordHelper(this,appstate,this);
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new SearchingGroup.SpeechListener());
        } else {
            // Permission already granted
            requestRecordPermission();
        }

        appstate = AState.AppState.TTS;


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
            speechRecognizer.setRecognitionListener(new SearchingGroup.SpeechListener());
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
                    Toast.makeText(SearchingGroup.this, "No speech input.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(SearchingGroup.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(SearchingGroup.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(SearchingGroup.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(SearchingGroup.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(SearchingGroup.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(SearchingGroup.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(SearchingGroup.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(SearchingGroup.this, "Listening", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
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
                Toast.makeText(SearchingGroup.this, "Listening", Toast.LENGTH_SHORT).show();
            }
            else if(utteranceId.equals("TTS_UTTERANCE_JOINGROUP")){
                wakeWordHelper.stopListening();
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(SearchingGroup.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                appstate= AState.AppState.STT;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String YN = STTData;
                        if (YN!=null&&YN.equals("yes")) {
                            DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                            referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                                    if (readUserDetails != null) {
                                        Username = readUserDetails.userName;
                                        Email = readUserDetails.email;
                                        ParticipantDetails participantDetails = new ParticipantDetails(Username, Email, firebaseUser.getUid());
                                        DatabaseReference joiningRequest = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Joining Request");
                                        joiningRequest.child(firebaseUser.getUid()).setValue(participantDetails);
                                        int tts3 = textToSpeech.speak("Joining Request Sent, Please Wait for the group owner to accept you. If you want to Go back, Say Exam Care, Home Page", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                        if (tts3 == TextToSpeech.SUCCESS) {
                                            // Pause the timer until TTS completes
                                            pauseToastTimer();
                                        }
                                    } else {
                                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        } else {
                            int tts4 = textToSpeech.speak("No or Wrong input provided. Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                            if (tts4 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                    }
                },5000);
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
        DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");


        groupsRef.child(Group_ID).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    DatabaseReference Ref = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID);
                    Ref.child("Group Details").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot Snapshot) {
                            foundGroup = Snapshot.getValue(Group.class);
                            if (foundGroup != null) {
                                SRG_NoText.setVisibility(View.GONE);
                                SRG_GroupName.setVisibility(View.VISIBLE);
                                SRG_GroupSubjectCode.setVisibility(View.VISIBLE);
                                SRG_Layout.setVisibility(View.VISIBLE);
                                SG_GSCT.setVisibility(View.VISIBLE);
                                SG_GNT.setVisibility(View.VISIBLE);
                                String Group_Name = foundGroup.Group_Name;
                                String Group_SubjectCode = foundGroup.Subject_Code;
                                SRG_GroupName.setText(Group_Name);
                                SRG_GroupSubjectCode.setText(Group_SubjectCode);
                                if (status == TextToSpeech.SUCCESS) {
                                    // TTS initialization successful, set language and convert text to speech
                                    isTTSInitialized = true;
                                    textToSpeech.setLanguage(Locale.US);
                                    //Locale locale = new Locale("en","IN");
                                    //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
                                    //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
                                    //textToSpeech.setVoice(voice);


                                    int ttsResult=textToSpeech.speak("Hello, Welcome to the Searching Group Page of Exam Care The Group Id that you searched for belongs to the group named"+foundGroup.Group_Name+"with subject code"+foundGroup.Subject_Code+"Do You Want to Enter or join the group if so say Exam Care, Enter Group", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                                    if (ttsResult == TextToSpeech.SUCCESS) {
                                        // Pause the timer until TTS completes
                                        pauseToastTimer();
                                    }
                                } else {
                                    int ttsResult=textToSpeech.speak("Hello, Welcome to the Searching Group Page of Exam Care, No Groups are present with the group id that you provided please go back to the homepage and try searching for the group again. For Going back to the HomePage say Exam Care, HomePage", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                                    if (ttsResult == TextToSpeech.SUCCESS) {
                                        // Pause the timer until TTS completes
                                        pauseToastTimer();
                                    }
                                    // TTS initialization failed, handle error
                                    Log.e("TTS", "Initialization failed");
                                }
                            }
                            else {
                                Toast.makeText(SearchingGroup.this, "No Data", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(SearchingGroup.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    SRG_NoText.setVisibility(View.VISIBLE);
                    SRG_GroupName.setVisibility(View.GONE);
                    SRG_GroupSubjectCode.setVisibility(View.GONE);
                    SRG_Layout.setVisibility(View.GONE);
                    SG_GSCT.setVisibility(View.GONE);
                    SG_GNT.setVisibility(View.GONE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SearchingGroup.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Repeat The Introduction if Repeat Method is Triggered.
    public void StarUpRepeat(){
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        String voice;
        if(foundGroup!=null){
            voice="Hello, Welcome to the Searching Group Page of Exam Care The Group Id that you searched for belongs to the group named"+foundGroup.Group_Name+"with subject code"+foundGroup.Subject_Code+"Do You Want to Enter or join the group if so say Exam Care, Enter Group";
        }
        else{
            voice="Hello, Welcome to the Searching Group Page of Exam Care, No Groups are present with the group id that you provided please go back to the homepage and try searching for the group again. For Going back to the HomePage say Exam Care, HomePage";
        }
        textToSpeech.setSpeechRate(0.85f);
        int ttsResult=textToSpeech.speak(voice, TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
            //appstate= AState.AppState.TTS;
            pauseToastTimer();
        }
    }



    public void Automate(String Temp){
        wakeWordHelper.stopListening();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        appstate= AState.AppState.TTS;
        if(Temp.equals("repeat introduction")){
            StarUpRepeat();
        }
        else if(Temp.equals("homepage")){
            Intent intent = new Intent(SearchingGroup.this, StudentHomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl","Student");

            // Start the new activity
            startActivity(intent);

            finish();
        }
        else if(Temp.equals("enter group")){
            if(foundGroup!=null) {
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Current Participants").child(firebaseUser.getUid());
                database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {

                            ParticipantDetails participantDetails = snapshot.getValue(ParticipantDetails.class);
                            if (participantDetails != null) {
                                Intent intent = new Intent(SearchingGroup.this, StudentGroup.class);

                                // Pass the unique key to the new activity
                                intent.putExtra("GROUP_ID", Group_ID);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                                // Start the new activity
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            DatabaseReference database2 = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Joining Request").child(firebaseUser.getUid());
                            database2.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists()) {
                                        ParticipantDetails participantDetails = snapshot.getValue(ParticipantDetails.class);
                                        if (participantDetails != null) {
                                            int ttsResult = textToSpeech.speak("Joining Request Not Accepted yet. Please Wait Until the Group Owner Accepts your joining request. If You want to Go Back Say Exam Care, Home Page", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                                            if (ttsResult == TextToSpeech.SUCCESS) {
                                                // Pause the timer until TTS completes
                                                pauseToastTimer();
                                            }
                                        }
                                    } else {
                                        int tts2 = textToSpeech.speak("You have not Joined the Group Yet, Do you want to send a joining request. If So Say Yes else No", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_JOINGROUP");
                                        if (tts2 == TextToSpeech.SUCCESS) {
                                            // Pause the timer until TTS completes
                                            pauseToastTimer();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                        //UP_progressBar.setVisibility(View.GONE);
                    }
                });
            }
            else {
                int tts1 = textToSpeech.speak("No Group Exists With Such Group ID. Please Go Back to the Homepage and try Again. Starting WakeWord Engine", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                if (tts1 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else{
            int tts1=textToSpeech.speak("Wrong input provided"+Temp+"Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(SearchingGroup.this);
        builder.setTitle("Send Joining Request");
        builder.setMessage("Do you want to Send a Joining Request?");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                        if (readUserDetails != null) {
                            Username = readUserDetails.userName;
                            Email = readUserDetails.email;
                            ParticipantDetails participantDetails=new ParticipantDetails(Username,Email,firebaseUser.getUid());
                            DatabaseReference joiningRequest= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Joining Request");
                            joiningRequest.child(firebaseUser.getUid()).setValue(participantDetails);
                        } else {
                            Toast.makeText(SearchingGroup.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onCancelled (@NonNull DatabaseError error){

                    }
                });
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //create the AlertDialog
        AlertDialog alertDialog=builder.create();

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
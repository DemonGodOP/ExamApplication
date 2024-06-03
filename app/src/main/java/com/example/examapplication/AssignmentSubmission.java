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
import android.os.CountDownTimer;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssignmentSubmission extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener {
    TextView AS_QN,AS_Q,AS_A,AS_Time;

    Button AS_Prev,AS_Next,AS_S;

    String Group_ID,Assignment_ID,Duration;

    List<String> Answers;

    List<String>Questions;

    FirebaseAuth authProfile;

    FirebaseUser firebaseUser;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1
    int n=0;

    CountDownTimer countDownTimer;

    long timeLeftInMillis;

    Assignment assignment;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

    String STTData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_submission);
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        Intent intent=getIntent();

        Group_ID=intent.getStringExtra("Group_ID");
        Assignment_ID=intent.getStringExtra("Assignment_ID");

        AS_QN=findViewById(R.id.AS_QN);
        AS_Q=findViewById(R.id.AS_Q);
        AS_A=findViewById(R.id.AS_A);
        AS_Prev=findViewById(R.id.AS_Prev);
        AS_Next=findViewById(R.id.AS_Next);
        AS_S=findViewById(R.id.AS_S);
        AS_Time=findViewById(R.id.AS_Time);

        AS_QN.setText(n+1+"");

        Answers=new ArrayList<>();

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        DatabaseReference database= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID);

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                assignment=snapshot.getValue(Assignment.class);
                if(assignment!=null){
                    Questions=assignment.Questions;
                    Duration=assignment.Duration;
                    AS_Q.setText(Questions.get(0));
                    timeLeftInMillis=Integer.parseInt(Duration)* 60000L;
                    startCountdownTimer();
                    if(n==Questions.size()-1){
                        AS_Next.setEnabled(false);
                    }
                }
                else{
                    Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

        AS_Prev.setEnabled(false);

        AS_Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Temp=AS_A.getText().toString();
                if(TextUtils.isEmpty(Temp)){
                    Temp="";
                }
                if(Answers.isEmpty()||n==Answers.size()-1){
                    Answers.add(Temp);
                }
                else{
                    Answers.set(n,Temp);
                }
                n++;
                AS_QN.setText(n+1+"");
                AS_Q.setText(Questions.get(n));
                if(n==Questions.size()-1){
                    AS_Next.setEnabled(false);
                }
                AS_Prev.setEnabled(true);
                if(n!=Answers.size()){
                    AS_A.setText(Answers.get(n));
                }
                else {
                    AS_A.setText("");
                }
            }
        });

        AS_Prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Temp=AS_A.getText().toString();
                if(TextUtils.isEmpty(Temp)){
                    Temp="";
                }
                if(Answers.isEmpty()||n==Answers.size()){
                    Answers.add(Temp);
                }
                else{
                    Answers.set(n,Temp);
                }
                n--;
                AS_QN.setText(n+1+"");
                AS_Q.setText(Questions.get(n));
                if(n==0){
                    AS_Prev.setEnabled(false);
                }
                AS_Next.setEnabled(true);
                AS_A.setText(Answers.get(n));
            }
        });

        AS_S.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

                userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                        if(readWriteUserDetails!=null){
                            String Temp=AS_A.getText().toString();
                            if(TextUtils.isEmpty(Temp)){
                                Temp="";
                            }
                            if(Answers.isEmpty()||n==Answers.size()){
                                Answers.add(Temp);
                            }
                            else {
                                Answers.set(n,Temp);
                            }
                            if(Answers.size()<Questions.size()){
                                while(Answers.size()<Questions.size()){
                                    Answers.add("");
                                }
                            }
                            String UserName=readWriteUserDetails.userName;
                            String Email=readWriteUserDetails.email;
                            DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                            SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                            newRef.setValue(submissionDetails);
                            Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("GROUP_ID",Group_ID);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        appstate = AState.AppState.TTS;
        if (hasRecordPermission()){
            wakeWordHelper=new WakeWordHelper(this,appstate,this);
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new AssignmentSubmission.SpeechListener());
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
            speechRecognizer.setRecognitionListener(new AssignmentSubmission.SpeechListener());
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
                    Toast.makeText(AssignmentSubmission.this, "No speech input.", Toast.LENGTH_SHORT).show();
                    if(appstate== AState.AppState.AUTOMATE||appstate == AState.AppState.ANSWERING){
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
                    Toast.makeText(AssignmentSubmission.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(AssignmentSubmission.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(AssignmentSubmission.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(AssignmentSubmission.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
                    if(appstate== AState.AppState.AUTOMATE||appstate == AState.AppState.ANSWERING){
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
                    Toast.makeText(AssignmentSubmission.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(AssignmentSubmission.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(AssignmentSubmission.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (appstate == AState.AppState.STT) {
                    STTData = data.get(0).toLowerCase();
                } else if (appstate == AState.AppState.AUTOMATE) {
                    if (data.get(0).toLowerCase() != null)
                        Automate(data.get(0).toLowerCase());
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                speechRecognizer.startListening(speechRecognizerIntent);
                                Toast.makeText(AssignmentSubmission.this, "Listening", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else if (appstate == AState.AppState.ANSWERING) {
                    STTData = data.get(0).toLowerCase();
                    String T = STTData;
                    AS_A.setText(T);
                    if (Answers.isEmpty() || n == Answers.size()) {
                        Answers.add(T);
                    } else {
                        Answers.set(n, T);
                    }
                    int tts2 = textToSpeech.speak("Your Answer has been recorded. Starting WakeWord Engine. Say Exam Care, Repeat Answer, To Listen, to the answer that has been recorded.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                    if (tts2 == TextToSpeech.SUCCESS) {
                        // Pause the timer until TTS completes
                        pauseToastTimer();
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
            if(utteranceId.equals("TTS_UTTERANCE_SUBMISSION")){
                submitTest();
            }
           else  if(utteranceId.equals("TTS_UTTERANCE_STARTWAKEWORD")){
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();
                resetToastTimer();
                Toast.makeText(AssignmentSubmission.this, "Listening", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(AssignmentSubmission.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String YN = STTData;
                        if (YN != null && YN.equals("yes")) {
                            StarUpRepeat();
                        } else {
                            String Q=Questions.get(n);
                            int tts5=textToSpeech.speak("Your Exam has Started. Question No."+(n+1)+", is"+Q+",Wake word engine started, you can surf through the page now", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                            if (tts5 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                    }
                }, 5000);
            }
            else if(utteranceId.equals("TTS_UTTERANCE_ANSWER")){
                wakeWordHelper.stopListening();
                appstate = AState.AppState.ANSWERING;
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(AssignmentSubmission.this, "Listening", Toast.LENGTH_SHORT).show();
                });
            }
            else if(utteranceId.equals("TTS_UTTERANCE_S1")){
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
                    Toast.makeText(AssignmentSubmission.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String YN = STTData;
                        if (YN!=null&&YN.equals("yes")) {
                            int tts11 = textToSpeech.speak("Your Assignment Submission Process has started once submitted you will " +
                                    "be redirected to the Student Group page from where you can check the assignment feedback.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_SUBMISSION");
                            if (tts11 == TextToSpeech.SUCCESS) {
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
        if (status == TextToSpeech.SUCCESS) {
            // TTS initialization successful, set language and convert text to speech
            isTTSInitialized = true;
            textToSpeech.setLanguage(Locale.US);
            //Locale locale = new Locale("en","IN");
            //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
            //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
            //textToSpeech.setVoice(voice);
            int ttsResult=textToSpeech.speak("Hello,your exam has started. Do you want to listen to the Detailed instructions of how to easily surf through this page. If So say, Yes else No", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ONINIT");
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
        timeLeftInMillis+=120000;
        String Q=Questions.get(n);
        int ttsResult=textToSpeech.speak("Hello,your exam has started. Please Start Answering the following questions within the given time frame of"
                +assignment.Duration+"mins"
                +"The questions will be read out to you one by one and your task will be to answer them with the best of your ability. To Answer a Question" +
                " you have to say Exam Care, Answer. You can also ask me to repeat the questions just by saying, Exam Care, Repeat Question or you can ask" +
                " me to repeat the answer by saying, Exam Care, Repeat answer. You can Surf through the examination with simple Commands like, in order to" +
                " go to the next question just say, Exam Care, Next, or Inorder to go to the previous Question say,Exam Care, Previous,You can also ask me to " +
                "to inform you about the time duration left to complete the assignment just say Exam Care, duration. and last but" +
                " not the least in order to submit the assignment, just say Exam Care, Submit. Please Carry on With Your Exam Now,  Question No."+(n+1)+", is"+Q+", Wake word engine started ", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
        else if(Temp.equals("answer")){
            int tts1=textToSpeech.speak("Please Start Answering Now.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ANSWER");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        else if(Temp.equals("repeat answer")){
            String A="";
            if(n< Answers.size()) {
                A = Answers.get(n);
            }
            if(A.length()==0){
                int tts3=textToSpeech.speak("You have not yet answered this question please answer the question first by using the Exam Care, Answer, Command.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts3 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else{
                int tts4=textToSpeech.speak("The Answer You Provided is: "+A+". Starting WakeWord engine, say Exam Care, Answer, in order to change the current answer.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts4 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("repeat question")){
            String Q=Questions.get(n);
            int tts5=textToSpeech.speak("Question No."+(n+1)+"is"+Q+". Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts5 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        else if(Temp.equals("next")){
            if(n==Questions.size()-1){
                int tts6=textToSpeech.speak("You Have Reached the End of the of the Assignment. If you want to Submit please say Exam Care, Submit", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts6 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
                if(Questions.size()>Answers.size()){
                    Answers.add("");
                }
            }
            else{
                if(Answers.isEmpty()||n>Answers.size()-1){
                    Answers.add("");
                }
                n++;
                AS_QN.setText(n+1+"");
                AS_Q.setText(Questions.get(n));
                if(n==Questions.size()-1){
                    AS_Next.setEnabled(false);
                }
                AS_Prev.setEnabled(true);
                if(n<Answers.size()){
                    AS_A.setText(Answers.get(n));
                }
                else {
                    AS_A.setText("");
                }
                String Q=Questions.get(n);
                int tts7=textToSpeech.speak("Question No."+(n+1)+"is"+Q+". Starting WakeWord Engine, Please Say Exam Care, Repeat Answer, in order to listen to the answer you provided.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts7 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("previous")){
            if(n==0){
                int tts8=textToSpeech.speak("You are already at the beginning of the Assignment. You can't use the" +
                        " previous command at this moment.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts8 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else{
                if(Answers.isEmpty()||n>Answers.size()-1){
                    Answers.add("");
                }
                n--;
                AS_QN.setText(n+1+"");
                AS_Q.setText(Questions.get(n));
                if(n==0){
                    AS_Prev.setEnabled(false);
                }
                AS_Next.setEnabled(true);
                AS_A.setText(Answers.get(n));
                String Q=Questions.get(n);
                int tts8=textToSpeech.speak("Question No."+(n+1)+"is"+Q+". Starting WakeWord Engine, Please Say Exam Care, Repeat Answer, in order to listen to the answer you provided.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts8 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("submit")){
            int tts10=textToSpeech.speak("Do you want to submit your assignment, once submitted you can't retake " +
                    "it or make any changes. Please say Yes or No", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_S1");
            if (tts10 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        else if(Temp.equals("duration")){
            int mins = (int) (timeLeftInMillis / 1000) / 60;
            int seconds = (int) (timeLeftInMillis / 1000) % 60;

            int tts11=textToSpeech.speak("You have"+mins+"minutes and"+seconds+"seconds left. Starting Wake Word Engine.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts11 == TextToSpeech.SUCCESS) {
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
    private boolean shouldAllowExit = false;

    @Override
    public void onBackPressed() {
        if (!shouldAllowExit) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit")
                    .setMessage("Your Assignment Will Be Submitted If You Exit Right Now?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

                            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                                    if(readWriteUserDetails!=null){
                                        String Temp=AS_A.getText().toString();
                                        if(TextUtils.isEmpty(Temp)){
                                            Temp="";
                                        }
                                        if(Answers.isEmpty()||n==Answers.size()){
                                            Answers.add(Temp);
                                        }
                                        else {
                                            Answers.set(n,Temp);
                                        }
                                        if(Answers.size()<Questions.size()){
                                            while(Answers.size()<Questions.size()){
                                                Answers.add("");
                                            }
                                        }
                                        String UserName=readWriteUserDetails.userName;
                                        String Email=readWriteUserDetails.email;
                                        DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                                        SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                                        newRef.setValue(submissionDetails);
                                        Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtra("GROUP_ID",Group_ID);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else{
                                        Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                            shouldAllowExit = true;
                            onBackPressed();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }


    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }
            @Override
            public void onFinish() {
                submitTest();
            }
        }.start();
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // Update a TextView with the remaining time
        AS_Time.setText(timeLeftFormatted);
    }

    private void submitTest() {
        DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                if(readWriteUserDetails!=null){
                    String Temp=AS_A.getText().toString();
                    if(TextUtils.isEmpty(Temp)){
                        Temp="";
                    }
                    if(Answers.isEmpty()||n==Answers.size()){
                        Answers.add(Temp);
                    }
                    else {
                        Answers.set(n,Temp);
                    }
                    if(Answers.size()<Questions.size()){
                        while(Answers.size()<Questions.size()){
                            Answers.add("");
                        }
                    }
                    String UserName=readWriteUserDetails.userName;
                    String Email=readWriteUserDetails.email;
                    DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                    SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                    newRef.setValue(submissionDetails);

                    Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("GROUP_ID",Group_ID);
                    startActivity(intent);
                    finish();
                }
                else{
                    Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void showAlertDialog () {

        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(AssignmentSubmission.this);
        builder.setTitle("Submit Exam?");
        builder.setMessage("Do you want to Submit the Exam?");


        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

                userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                        if(readWriteUserDetails!=null){
                            String Temp=AS_A.getText().toString();
                            if(TextUtils.isEmpty(Temp)){
                                Temp="";
                            }
                            if(Answers.isEmpty()||n==Answers.size()){
                                Answers.add(Temp);
                            }
                            else {
                                Answers.set(n,Temp);
                            }
                            if(Answers.size()<Questions.size()){
                                while(Answers.size()<Questions.size()){
                                    Answers.add("");
                                }
                            }
                            String UserName=readWriteUserDetails.userName;
                            String Email=readWriteUserDetails.email;
                            DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                            SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                            newRef.setValue(submissionDetails);
                            Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("GROUP_ID",Group_ID);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
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
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentFeedBack extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener{
   TextView SFTSG,SF_QN,SN_Q,SF_FText,SN_F,SF_AnswerText,SN_A;
   Button SF_Prev,SF_Next,SF_FB;

   String Group_ID,Assignment_ID;

   List<String>Questions;
   List<String> Answers;

   FirebaseAuth authProfile;
   FirebaseUser firebaseUser;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1

    FeedBackDetails feedBackDetails;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

    String STTData;
   int n=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_feed_back);
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        Intent intent=getIntent();
        Group_ID=intent.getStringExtra("Group_ID");
        Assignment_ID=intent.getStringExtra("Assignment_ID");

        SFTSG=findViewById(R.id.SFTSG);
        SF_QN=findViewById(R.id.SF_QN);
        SN_Q=findViewById(R.id.SN_Q);
        SF_FText=findViewById(R.id.SF_FText);
        SN_F=findViewById(R.id.SN_F);
        SF_AnswerText=findViewById(R.id.SF_AnswerText);
        SN_A=findViewById(R.id.SN_A);
        SF_Prev=findViewById(R.id.SF_Prev);
        SF_Next=findViewById(R.id.SF_Next);
        SF_FB=findViewById(R.id.SF_FB);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        SFTSG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        SF_FText.setVisibility(View.GONE);
        SN_F.setVisibility(View.GONE);

        DatabaseReference getQuestions= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID);
        getQuestions.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Assignment assignment=snapshot.getValue(Assignment.class);
                if(assignment!=null){
                    Questions=assignment.Questions;
                    SN_Q.setText(Questions.get(0));
                    SF_QN.setText("QN: "+(n+1));
                    if(n==Questions.size()-1){
                        SF_Next.setEnabled(false);
                    }
                }
                else{
                    Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

        DatabaseReference getAnswers= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
        getAnswers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SubmissionDetails submissionDetails=snapshot.getValue(SubmissionDetails.class);
                if(submissionDetails!=null){
                    Answers=submissionDetails.Answers;
                    SN_A.setText(Answers.get(0));
                }
                else{
                    Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

        SF_Prev.setEnabled(false);



        SF_Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n++;
                SF_QN.setText("QN: "+(n+1));
                SN_Q.setText(Questions.get(n));
                SN_A.setText(Answers.get(n));
                if(n==Questions.size()-1){
                    SF_Next.setEnabled(false);
                }
                SF_Prev.setEnabled(true);
            }
        });

        SF_Prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n--;
                SF_QN.setText("QN: "+(n+1));
                SN_Q.setText(Questions.get(n));
                SN_A.setText(Answers.get(n));
                if(n==0){
                    SF_Prev.setEnabled(false);
                }
                SF_Next.setEnabled(true);
            }
        });

        SF_FB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(SF_QN.getVisibility()==View.VISIBLE) {
                    SF_QN.setVisibility(View.GONE);
                    SN_Q.setVisibility(View.GONE);
                    SF_Prev.setVisibility(View.GONE);
                    SF_Next.setVisibility(View.GONE);
                    SN_A.setVisibility(View.GONE);
                    SF_AnswerText.setVisibility(View.GONE);

                    SN_F.setVisibility(View.VISIBLE);
                    SF_FText.setVisibility(View.VISIBLE);

                    SF_FB.setText("Answers");
                }
                else{
                    SF_QN.setVisibility(View.VISIBLE);
                    SN_Q.setVisibility(View.VISIBLE);
                    SF_Prev.setVisibility(View.VISIBLE);
                    SF_Next.setVisibility(View.VISIBLE);
                    SN_A.setVisibility(View.VISIBLE);
                    SF_AnswerText.setVisibility(View.VISIBLE);

                    SN_F.setVisibility(View.GONE);
                    SF_FText.setVisibility(View.GONE);

                    SF_FB.setText("FeedBack");
                }

                getAnswers.child("FeedBack").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        feedBackDetails = dataSnapshot.getValue(FeedBackDetails.class);
                        if (feedBackDetails != null) {
                            String feedback=feedBackDetails.FeedBack;
                            SN_F.setText(feedback);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(StudentFeedBack.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
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
            speechRecognizer.setRecognitionListener(new StudentFeedBack.SpeechListener());
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
            speechRecognizer.setRecognitionListener(new StudentFeedBack.SpeechListener());
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
                    Toast.makeText(StudentFeedBack.this, "No speech input.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentFeedBack.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(StudentFeedBack.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(StudentFeedBack.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(StudentFeedBack.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentFeedBack.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(StudentFeedBack.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(StudentFeedBack.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(StudentFeedBack.this, "Listening", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(StudentFeedBack.this, "Listening", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(StudentFeedBack.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String YN = STTData;
                        if (YN != null && YN.equals("yes")) {
                            StarUpRepeat();
                        }  else {
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
            int ttsResult=textToSpeech.speak("Hello, Welcome to the Student feedback Page of Exam Care,  Do you want to listen to the Detailed instructions of how to easily surf through this page. If So say, Yes else No", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ONINIT");
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
        textToSpeech.setSpeechRate(0.75f);
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Student feedback Page of Exam Care, This page provides you with the facility, to " +
                "to enquire about the feedback provided by your teacher for this particular assignment.for that you have to say, Hello Exam care, feedback," +
                " it also allows you to review your answers for various" +
                "questions as provided in this assignment, and for that you have to say, Hello Exam care, Review Assignment.  You can also ask me to repeat the questions just by saying, " +
                "Exam Care, Repeat Question or you can ask" +
                " me to repeat the answer by saying, Exam Care, Repeat answer. You can Surf through question review with simple Commands like, in order to" +
                " go to the next question just say, Exam Care, Next, or Inorder to go to the previous Question say,Exam Care, Previous or you can go back to the student group page just say " +
                "Exam care, back", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
        else if(Temp.equals("feedback")){
            if(!SF_FB.getText().toString().equals("FeedBack")){
                int tts5=textToSpeech.speak("You are already at the feedback page", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else {
                SF_QN.setVisibility(View.GONE);
                SN_Q.setVisibility(View.GONE);
                SF_Prev.setVisibility(View.GONE);
                SF_Next.setVisibility(View.GONE);
                SN_A.setVisibility(View.GONE);
                SF_AnswerText.setVisibility(View.GONE);

                SN_F.setVisibility(View.VISIBLE);
                SF_FText.setVisibility(View.VISIBLE);

                SF_FB.setText("Answers");
                if (feedBackDetails != null) {
                    String feedback = feedBackDetails.FeedBack;
                    SN_F.setText(feedback);
                    int tts1 = textToSpeech.speak(feedback+". Starting wakeword engine", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                    if (tts1 == TextToSpeech.SUCCESS) {
                        // Pause the timer until TTS completes
                        pauseToastTimer();
                    }
                } else {
                    int tts2 = textToSpeech.speak("feedback not yet provided by the teacher for this assignment. Starting wakeword engine", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                    if (tts2 == TextToSpeech.SUCCESS) {
                        // Pause the timer until TTS completes
                        pauseToastTimer();
                    }
                }
            }
        }
        else if(Temp.equals("review assignment")){
            if(!SF_FB.getText().toString().equals("Answers")){
                int tts5=textToSpeech.speak("You are already at the review assignment page", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else {
                SF_QN.setVisibility(View.VISIBLE);
                SN_Q.setVisibility(View.VISIBLE);
                SF_Prev.setVisibility(View.VISIBLE);
                SF_Next.setVisibility(View.VISIBLE);
                SN_A.setVisibility(View.VISIBLE);
                SF_AnswerText.setVisibility(View.VISIBLE);

                SN_F.setVisibility(View.GONE);
                SF_FText.setVisibility(View.GONE);

                SF_FB.setText("FeedBack");
                int tts3 = textToSpeech.speak("Now you can use the various functionalities as mentioned in the introduction in order to review your assignment.Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                if (tts3 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("repeat question")){
            if(SF_FB.getText().toString().equals("FeedBack")){
                String Q=Questions.get(n);
                int tts5=textToSpeech.speak("Question No."+(n+1)+"is"+Q+". Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else{
                int tts6=textToSpeech.speak("You are on the Feedback page go back to the review assignment page to review your" +
                        " assignment, for this you have to say, Exam care, Review Assignment", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts6 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("repeat answer")){
            if(SF_FB.getText().toString().equals("FeedBack")){
                String A=Answers.get(n);
                if(A.length()==0){
                    int tts3=textToSpeech.speak("You have not yet answered this question please answer the question,", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                    if (tts3 == TextToSpeech.SUCCESS) {
                        // Pause the timer until TTS completes
                        pauseToastTimer();
                    }
                }
                else{
                    int tts4=textToSpeech.speak("The Answer You Provided is: "+A+". Starting WakeWord engine.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                    if (tts4 == TextToSpeech.SUCCESS) {
                        // Pause the timer until TTS completes
                        pauseToastTimer();
                    }
                }
            }
            else{
                int tts7=textToSpeech.speak("You are on the Feedback page go back to the review assignment page to review your" +
                        " assignment, for this you have to say, Exam care, Review Assignment", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts7 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("next")){
            if(!SF_FB.getText().toString().equals("FeedBack")){
                int tts5=textToSpeech.speak("You are on the Feedback page go back to the review assignment page to review your" +
                        " assignment, for this you have to say, Exam care, Review Assignment", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else if(n==Questions.size()-1) {
                int tts6 = textToSpeech.speak("You Have Reached the End of the of the Assignment.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                if (tts6 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else{
                n++;
                SF_QN.setText("QN: "+(n+1));
                SN_Q.setText(Questions.get(n));
                SN_A.setText(Answers.get(n));
                if(n==Questions.size()-1){
                    SF_Next.setEnabled(false);
                }
                SF_Prev.setEnabled(true);
                String Q=Questions.get(n);
                int tts5=textToSpeech.speak("Question No."+(n+1)+"is"+Q+". Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("previous")){
            if(!SF_FB.getText().toString().equals("FeedBack")){
                int tts5=textToSpeech.speak("You are on the Feedback page go back to the review assignment page to review your" +
                        " assignment, for this you have to say, Exam care, Review Assignment", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else if(n==0){
                int tts8=textToSpeech.speak("You are already at the beginning of the Assignment. You can't use the" +
                        " previous command at this moment. Starting WakeWord Engine", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts8 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
            else{
                n--;
                SF_QN.setText("QN: "+(n+1));
                SN_Q.setText(Questions.get(n));
                SN_A.setText(Answers.get(n));
                if(n==0){
                    SF_Prev.setEnabled(false);
                }
                SF_Next.setEnabled(true);
                String Q=Questions.get(n);
                int tts5=textToSpeech.speak("Question No."+(n+1)+"is"+Q+". Starting WakeWord Engine.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                if (tts5 == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
            }
        }
        else if(Temp.equals("back")){
            Intent intent = new Intent(StudentFeedBack.this, StudentGroup.class);

            // Pass the unique key to the new activity
            intent.putExtra("GROUP_ID", Group_ID);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            // Start the new activity
            startActivity(intent);
            finish();
        }
        else{
            int tts1=textToSpeech.speak("Wrong input provided. Please start the process from the beginning. Sorry for any inconvenience. Starting WakeWord Engine", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
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


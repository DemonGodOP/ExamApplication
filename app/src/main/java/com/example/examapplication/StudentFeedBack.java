package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.List;
import java.util.Locale;

public class StudentFeedBack extends AppCompatActivity implements TextToSpeech.OnInitListener{
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
                        FeedBackDetails feedBackDetails = dataSnapshot.getValue(FeedBackDetails.class);
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
    @Override //3
    protected void onResume() {
        super.onResume();
        // Reset the timer whenever the user interacts with the app
        resetToastTimer();
        isUserInteracted = false; // Reset user interaction flag
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
            int ttsResult=textToSpeech.speak("Hello, Welcome to the Student feedback Page of Exam Care, This page provides you with the facility, to " +
                    "share about the experience while using the app, and for that you have to say, Hello Exam care, feedback.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        resetToastTimer();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Student feedback Page of Exam Care, This page provides you with the facility, to \" +\n" +
                "share about the experience while using the app, and for that you have to say, Hello Exam care, feedback..", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        Repeat();
    }

    public void Repeat(){
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult=textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        //Enter the Condition Over here that is tts to take input from the user if they wants us to repeat the introduction and change r respectively.
        boolean r=false;
        if(r==true){
            StarUpRepeat();
        }
    }



    @Override
    protected void onDestroy() {
        // Release resources
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        handler.removeCallbacks(toastRunnable);
    }//3
    public void VoiceLogin(){
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int tts1=textToSpeech.speak("Let's, Begin the Feedback Process.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        int tts2=textToSpeech.speak("Please Say, Exam Care and then your feedback", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts2 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String feedback=""; // Store the Email over here using STT.

        boolean YesLogin=false;//Edit This Using STT
        if (YesLogin == true) {
            //loginUser(Email,pwd);
        }
    }
}


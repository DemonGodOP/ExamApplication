package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssignmentDetails extends AppCompatActivity implements TextToSpeech.OnInitListener {
    TextView ASTIG,AD_Edit,AD_CA,AD_Details,AD_Text,AD_NoText;

    ListView AD_LV;

    String Group_ID,AssignmentID,Name,Timing;

    boolean Active;

    List<String> Questions;

    ProgressBar AD_P;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_details);
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        Intent intent=getIntent();
        Group_ID=intent.getStringExtra("GROUP_ID");

        AssignmentID=intent.getStringExtra("Assignment_ID");

        ASTIG=findViewById(R.id.ADTAA);
        AD_Edit=findViewById(R.id.AD_Details);
        AD_CA=findViewById(R.id.AD_CA);
        AD_P=findViewById(R.id.AD_P);

        AD_LV=findViewById(R.id.AD_LV);

        AD_Details=findViewById(R.id.AD_Details);

        AD_Text=findViewById(R.id.AD_Text);
        AD_NoText=findViewById(R.id.AD_NoText);

        ASTIG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        AD_CA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        AD_Details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AssignmentDetails.this, AssignmentInfo.class);

                intent.putExtra("GROUP_ID", Group_ID);

                intent.putExtra("Assignment_ID",AssignmentID);

                // Start the new activity
                startActivity(intent);
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
        AD_P.setVisibility(View.VISIBLE);
        PopulateList();
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
            int ttsResult=textToSpeech.speak("Hello, Welcome to the assignments Page of Exam Care, This page provides you with the facility, to \" +\n" +
                    "                \"to know about your assignment details, you just have to say, exam care and assignment details ..", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int ttsResult=textToSpeech.speak("Hello, Welcome to the assignments Page of Exam Care, This page provides you with the facility, to " +
                "to know about your assignment details, you just have to say, exam care and assignment details .", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int tts1=textToSpeech.speak("Let's, Know about your assignment Process.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        int tts2=textToSpeech.speak("Please Say, Exam Care and then your assignment details", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts2 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String assignDetails=""; // Store the Email over here using STT.

        boolean YesLogin=false;//Edit This Using STT
        if (YesLogin == true) {
           // loginUser(Email,pwd);
        }
    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(AssignmentDetails.this);
        builder.setTitle("Change Assignment State");
        builder.setMessage("Do you want to Change the State of the Assignment?");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(AssignmentID);
                database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Assignment assignment = snapshot.getValue(Assignment.class);
                        if (assignment != null) {
                            Questions=assignment.Questions;
                            Name=assignment.Name;
                            Timing=assignment.Timing;
                            Active=assignment.Active;
                            Assignment temp=new Assignment(Questions,!Active,Name,Timing,AssignmentID);
                            database.setValue(temp).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    dialogInterface.dismiss();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(AssignmentDetails.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(AssignmentDetails.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                        }
                        //UP_progressBar.setVisibility(View.GONE);
                    }
                    @Override
                    public void onCancelled (@NonNull DatabaseError error){
                        Toast.makeText(AssignmentDetails.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                        //UP_progressBar.setVisibility(View.GONE);
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

    public void PopulateList() {
        DatabaseReference JoiningReference = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(AssignmentID);


        // Listener to retrieve the groups
        JoiningReference.child("Submissions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<SubmissionDetails> SubmissionList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    SubmissionDetails submission = snapshot.getValue(SubmissionDetails.class);
                    if (submission != null) {
                        SubmissionList.add(submission);
                    }
                }

                if (SubmissionList.isEmpty()) {
                    // If there are no groups, display the message TextView and hide the ListView
                    AD_NoText.setVisibility(View.VISIBLE);
                    AD_LV.setVisibility(View.GONE);
                    AD_P.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    AD_NoText.setVisibility(View.GONE);
                    AD_LV.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    SDLV adapter = new SDLV(AssignmentDetails.this, R.layout.sdlv, SubmissionList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    AD_LV.setAdapter(adapter);

                    AD_P.setVisibility(View.GONE);

                    AD_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                            SubmissionDetails selectedSubmission = (SubmissionDetails) parent.getItemAtPosition(position);
                            Intent intent = new Intent(AssignmentDetails.this, SubmissionInfo.class);

                            intent.putExtra("GROUP_ID", Group_ID);

                            intent.putExtra("Assignment_ID",AssignmentID);

                            intent.putExtra("Submission_ID",selectedSubmission.UserID);

                            // Start the new activity
                            startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AssignmentDetails.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
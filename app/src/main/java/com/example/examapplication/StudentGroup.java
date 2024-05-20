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

public class StudentGroup extends AppCompatActivity implements TextToSpeech.OnInitListener {
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_group);



        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

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

                // Pass the unique key to the new activity
                intent.putExtra("GROUP_ID", GROUP_ID);


                // Start the new activity
                startActivity(intent);

                finish();
            }
        });

        SG_GroupDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentGroup.this, StudentGroupDetails.class);

                // Pass the unique key to the new activity
                intent.putExtra("GROUP_ID", GROUP_ID);

                // Start the new activity
                startActivity(intent);
            }
        });

        SG_P.setVisibility(View.VISIBLE);
        PopulateList();
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
                int ttsResult = textToSpeech.speak("Hello, Welcome to the Student Group Page of Exam Care, This page provides you with the facility, to " +
                        "                \" know about your assignments which you can attempt, you just have to say, Hello Exam care, StudentGroup .", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
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
        int ttsResult = textToSpeech.speak("Hello, Welcome to the Profile Page of Exam Care, This page provides you with the facility, to " +
                "see your profile details such as name, email, phone no, institute, username and role, you just have to say, Hello Exam care, profile details.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
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
        int ttsResult = textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        //Enter the Condition Over here that is tts to take input from the user if they wants us to repeat the introduction and change r respectively.
        boolean r = false;
        if (r == true) {
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
        int tts1=textToSpeech.speak("Let's, Begin the login Process.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        int tts2=textToSpeech.speak("Please Say, Exam Care and then your Email ID", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts2 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Email=""; // Store the Email over here using STT.
        int tts3=textToSpeech.speak("Please Say, Exam Care and then your Password", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts3 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String pwd=""; //Store Email over here using STT.

        int tts4=textToSpeech.speak("Please Say, Exam Care Log me In, Inorder to login", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts4 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        boolean profile=false;//Edit This Using STT
        if ( profile== true) {
            //loginUser(Email,pwd);
        }
    }

    public void PopulateList() {
        DatabaseReference JoiningReference = FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID);


        // Listener to retrieve the groups
        JoiningReference.child("Assignments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Assignment> AssignmentList = new ArrayList<>();
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
                                    Toast.makeText(StudentGroup.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(StudentGroup.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
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
}
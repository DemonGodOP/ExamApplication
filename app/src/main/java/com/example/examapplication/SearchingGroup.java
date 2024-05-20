package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.DialogInterface;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class SearchingGroup extends AppCompatActivity implements TextToSpeech.OnInitListener{
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching_group);
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        Intent intent=getIntent();
        Group_ID=intent.getStringExtra("GROUP_ID");

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

        DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");


        groupsRef.child(Group_ID).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    DatabaseReference Ref = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID);
                    Ref.child("Group Details").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot Snapshot) {
                                Group foundGroup = Snapshot.getValue(Group.class);
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

                                // Pass the unique key to the new activity
                                intent.putExtra("GROUP_ID", Group_ID);

                                // Start the new activity
                                startActivity(intent);
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
            int ttsResult=textToSpeech.speak("Hello, Welcome to the Searching Group Page of Exam Care, This page provides you with the facility, to " +
                    "search existing groups that you want to join, you just to say hello exam care,search group and enter group id.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Searching Group Page of Exam Care, This page provides you with the facility, to \" +\n" +
                "search existing groups that you want to join, you just to say hello exam care,search group and enter group id..", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int tts1=textToSpeech.speak("Let's, Begin the Search Group Process.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        int tts2=textToSpeech.speak("Please Say, Exam Care and Do you want to join the group?", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts2 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String joinGroup=""; // Store the Email over here using STT.

        boolean YesGroupId=false;//Edit This Using STT
        if (YesGroupId == true) {
            //loginUser(Email,pwd);
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
                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();
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
}
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

    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

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

        appstate = AState.AppState.TTS;
        if (hasRecordPermission()){
            wakeWordHelper=new WakeWordHelper(this,appstate,this);
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

    @Override //3
    protected void onResume() {
        super.onResume();
        // Reset the timer whenever the user interacts with the app
        resetToastTimer();
        isUserInteracted = false; // Reset user interaction flag
        if (textToSpeech != null) {
            int ttsResult=textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
            if (ttsResult == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
            //Enter the Condition Over here that is tts to take input from the user if they wants us to repeat the introduction and change r respectively.
            boolean r=false;
            if(r==true){
                StarUpRepeat();
            }else{
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();
            }// Restart the TTS when the activity is resumed

        }
    }

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
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        pauseToastTimer();
        if (textToSpeech != null) {
            textToSpeech.stop(); // Stop the TTS if the activity is no longer visible
        }
        if(appstate== AState.AppState.WAKEWORD) {
            wakeWordHelper.stopListening();
            appstate = AState.AppState.TTS;
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
            String voice;
            if(foundGroup!=null){
                voice="Hello, Welcome to the Searching Group Page of Exam Care The Group Id that you searched for belongs to the group named"+foundGroup.Group_Name+"with subject code"+foundGroup.Subject_Code+"Do You Want to Enter or join the group if so say Exam Care, Enter Group";
            }
            else{
                voice="Hello, Welcome to the Searching Group Page of Exam Care, No Groups are present with the group id that you provided please go back to the homepage and try searching for the group again. For Going back to the HomePage say Exam Care, HomePage";
            }
            int ttsResult=textToSpeech.speak(voice, TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
        String voice;
        if(foundGroup!=null){
            voice="Hello, Welcome to the Searching Group Page of Exam Care The Group Id that you searched for belongs to the group named"+foundGroup.Group_Name+"with subject code"+foundGroup.Subject_Code+"Do You Want to Enter or join the group if so say Exam Care, Enter Group";
        }
        else{
            voice="Hello, Welcome to the Searching Group Page of Exam Care, No Groups are present with the group id that you provided please go back to the homepage and try searching for the group again. For Going back to the HomePage say Exam Care, HomePage";
        }
        int ttsResult=textToSpeech.speak(voice, TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
        wakeWordHelper.stopListening();
        super.onDestroy();
        handler.removeCallbacks(toastRunnable);
    }//3

    public void Automate(String Temp){
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        appstate= AState.AppState.TTS;
        if(Temp.equals("Repeat Introduction")){
            StarUpRepeat();
        }
        else if(Temp.equals("HomePage")){
            Intent intent = new Intent(SearchingGroup.this, StudentHomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl","Student");

            // Start the new activity
            startActivity(intent);

            finish();
        }
        else if(Temp.equals("Enter Group")){
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
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

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
                                        int ttsResult=textToSpeech.speak("Joining Request Not Accepted yet. Please Wait Until the Group Owner Accepts your joining request. If You want to Go Back Say Exam Care, Home Page", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
                                        if (ttsResult == TextToSpeech.SUCCESS) {
                                            // Pause the timer until TTS completes
                                            pauseToastTimer();
                                        }
                                    } }else {
                                    int tts2=textToSpeech.speak("You have not Joined the Group Yet, Do you want to send a joining request. If So Say Yes.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
                                    if (tts2 == TextToSpeech.SUCCESS) {
                                        // Pause the timer until TTS completes
                                        pauseToastTimer();
                                    }
                                    String YN="";
                                    if(YN.equals("YES")) {
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
                                                    int tts3 = textToSpeech.speak("Joining Request Sent, Please Wait for the group owner to accept you. If you want to Go back, Say Exam Care, Home Page", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
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
                                                Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                    else{
                                        int tts4=textToSpeech.speak("Wrong input provided. Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
                                        if (tts4 == TextToSpeech.SUCCESS) {
                                            // Pause the timer until TTS completes
                                            pauseToastTimer();
                                        }
                                    }
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
        else{
            int tts1=textToSpeech.speak("Wrong input provided. Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        wakeWordHelper.startListening();
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
    @Override
    public void onWakeWordDetected() {
        Toast.makeText(this, "Wakeword Detected"+appstate, Toast.LENGTH_SHORT).show();
    }
}
package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.Objects;

public class StudentHomePage extends AppCompatActivity implements TextToSpeech.OnInitListener{

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
        SH_progressBar.setVisibility(View.VISIBLE);
        PopulateList(firebaseUser);

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
            int ttsResult=textToSpeech.speak("Hello, Welcome to the Student Home Page of Exam Care, This page provides you with the facility, to" +
                    " see your profile details, for this you have to say, hello Exam care, profile details," +
                    "you can also sign Out if you want, for this you have to say, hello Exam care, sign out, you can also search, existing groups for this," +
                    "that you want to join, you just to say, hello exam care,search group and enter group id, and finally you can check the groups," +
                    "that you have already joined, by saying, hello exam care,joined group names.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Student Home Page of Exam Care, This page provides you with the facility, to " +
                "see your profile details, for this you have to say, hello Exam care, profile details, " +
                "you can also sign Out if you want, for this you have to say, hello Exam care, sign out, you can also search, existing groups for this,"+
                " that you want to join, you just to say, hello exam care,search group and enter group id, and finally you can check the groups," +
                "that you have already joined, by saying, hello exam care,joined group names.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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

    public void Automate(String Temp){
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        if(Temp.equals("profile details")){
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
            int tts4=textToSpeech.speak("Please say your group Id now", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
            if (tts4 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
            String groupId="";
            Intent intent = new Intent(StudentHomePage.this, SearchingGroup.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            // Pass the unique key to the new activity
            intent.putExtra("GROUP_ID", groupId);
            // Start the new activity
            startActivity(intent);

        }
        else if(Temp.equals("joined groups name")){

             for(int i=0;i<groupsList.size();i++){
                 int tts5=textToSpeech.speak((i+1)+"Group name is"+groupsList.get(i).Group_Name+"and Group ID is"+groupsList.get(i).Subject_Code+"Do you want to enter the group please say yes or no", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
                 if (tts5 == TextToSpeech.SUCCESS) {
                     // Pause the timer until TTS completes
                     pauseToastTimer();
                 }
                 String YN="";
                 if(YN.equals("Yes")){
                     String selectedGroupId = groupsList.get(i).Group_ID; // Or however you store the ID in the Group class

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

             }
        }
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
}
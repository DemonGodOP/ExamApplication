package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import java.util.Locale;
import java.util.Objects;

public class Profile extends AppCompatActivity implements TextToSpeech.OnInitListener {
    Toolbar P_toolBar;

    TextView P_Name, P_Email, P_Phone, P_Institute, P_UserName, P_Role;
    ProgressBar P_progressBar;
    String name, email, phone, institute, username, role;
    FirebaseAuth authProfile;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1

    TextView PTH;

    String Rl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Intent intent = getIntent();

        Rl = intent.getStringExtra("Rl");

        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

        P_toolBar = findViewById(R.id.P_toolBar);
        setSupportActionBar(P_toolBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        P_Name = findViewById(R.id.P_Name);
        P_Email = findViewById(R.id.P_Email);
        P_Phone = findViewById(R.id.P_Phone);
        P_Institute = findViewById(R.id.P_Institute);
        P_UserName = findViewById(R.id.P_UserName);
        P_Role = findViewById(R.id.P_Role);
        P_progressBar = findViewById(R.id.P_progressBar);
        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(Profile.this, "Something went wrong!User's details are not available at the moment", Toast.LENGTH_SHORT).show();
        } else {
            checkifEmailVerified(firebaseUser);
            P_progressBar.setVisibility(View.VISIBLE);
            showUserProfile(firebaseUser);
        }

        PTH = findViewById(R.id.PTH);
        PTH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (role.equals("Teacher")) {
                    Intent intent = new Intent(Profile.this, TeacherHomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Rl", "Teacher");
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(Profile.this, StudentHomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("Rl", "Student");
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
                if (Rl.equals("Student"))
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
            } // Restart the TTS when the activity is resumed
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseToastTimer();
        if (textToSpeech != null) {
            textToSpeech.stop(); // Stop the TTS if the activity is no longer visible
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
    UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {

        @Override
        public void onStart(String utteranceId) {
            Log.d(TAG, "onStart ( utteranceId :" + utteranceId + " ) ");
        }

        @Override
        public void onError(String utteranceId) {
            Log.d(TAG, "onError ( utteranceId :" + utteranceId + " ) ");
        }

        @Override
        public void onDone(String utteranceId) {
            resetToastTimer();
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Rl.equals("Student")) {
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
    }


    @Override
    public void onInit(int status) {
        if (Rl.equals("Student")) {
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialization successful, set language and convert text to speech
                isTTSInitialized = true;
                textToSpeech.setLanguage(Locale.US);
                //Locale locale = new Locale("en","IN");
                //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
                //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
                //textToSpeech.setVoice(voice);
                int ttsResult = textToSpeech.speak("Hello, Welcome to the Profile Page of Exam Care, Would you like to listen to a Detailed introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
                if (ttsResult == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }

                String YN="";
                if(YN.equals("YES")){
                    StarUpRepeat();
                }
        } else {
                // TTS initialization failed, handle error
                Log.e("TTS", "Initialization failed");
            }
        }
    }

    // Repeat The Introduction if Repeat Method is Triggered.
    public void StarUpRepeat() {

        resetToastTimer();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult = textToSpeech.speak("Hello, Welcome to the Profile Page of Exam Care, This page provides you with the facility, to " +
                "see your profile details such as name, email, phone number, institute, username and role, you just have to say, Hello Exam care, profile details.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        Repeat();

    }

    public void Repeat() {
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

    public void Automate(String Temp) {
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        if(Temp.equals("back")){
            Intent intent=new Intent(Profile.this,StudentHomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Rl","Student");
            startActivity(intent);
            finish();
        }
        else if(Temp.equals("describe profile details")){
            int tts1=textToSpeech.speak("Your Name is"+name+"Your email address is"+email+"your phone number is"+phone+
                            "Your institute name is"+institute+"your username is"+username+"", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        } else if (Temp.equals("change password")) {
            Intent intent = new Intent(Profile.this, ChangePassword.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl", "Student");
            startActivity(intent);
            finish();
        } else if (Temp.equals("edit profile")) {
            Intent intent = new Intent(Profile.this, UpdateProfile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl", "Student");
            startActivity(intent);
            finish();
        } else if (Temp.equals("Delete profile")) {
            Intent intent = new Intent(Profile.this, DeleteUser.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl", "Student");
            startActivity(intent);
            finish();
        }
        else if (Temp.equals("change email")) {
            Intent intent = new Intent(Profile.this, ChangeEmail.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl", "Student");
            startActivity(intent);
            finish();
        }
    }



    private void checkifEmailVerified(FirebaseUser firebaseUser)
    {
        if(!firebaseUser.isEmailVerified()){
            showAlertDialog();
        }
    }

    private void showUserProfile(FirebaseUser firebaseUser){
        String userId=firebaseUser.getUid();

        //Extracting User Reference from Database for "Registered Users"
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                ReadWriteUserDetails readUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                if(readUserDetails != null){
                    name=readUserDetails.name;
                    email=readUserDetails.email;
                    phone=readUserDetails.phoneNo;
                    institute=readUserDetails.institute;
                    username=readUserDetails.userName;
                    role=readUserDetails.finalRole;
                    P_Name.setText(name);
                    P_Email.setText(email);
                    P_Phone.setText(phone);
                    P_UserName.setText(username);
                    P_Institute.setText(institute);
                    P_Role.setText(role);


                }else {
                    Toast.makeText(Profile.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
                P_progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(Profile.this, "Something went wrong!", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void showAlertDialog() {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(Profile.this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.M_CP) {
            Intent intent = new Intent(Profile.this, ChangePassword.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();
        }
        if (id == R.id.M_EP) {
            Intent intent = new Intent(Profile.this, UpdateProfile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();

        }
        if (id == R.id.M_DP) {
            Intent intent = new Intent(Profile.this, DeleteUser.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();

        }
        if(id==R.id.M_CE){
            Intent intent = new Intent(Profile.this, ChangeEmail.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();
        }
        // Handle other menu item clicks if needed

        return super.onOptionsItemSelected(item);
    }

}
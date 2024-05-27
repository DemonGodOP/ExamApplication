package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class DeleteUser extends AppCompatActivity implements TextToSpeech.OnInitListener, WakeWordListener{
    FirebaseAuth authProfile;
    FirebaseUser firebaseUser;
    EditText DU_Password;
    TextView Text,DUTP;
    Button DU_Authenticate, DU_Button;
    ProgressBar DU_progressBar;
    String userPwd,Email;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1

    String Rl;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_user);
        Intent in = getIntent();

        Rl= in.getStringExtra("Rl");

        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

        DU_progressBar=findViewById(R.id.DU_progressBar);
        DU_Password=findViewById(R.id.DU_Password);
        Text= findViewById(R.id.DU_Text);
        DU_Button=findViewById(R.id.DU_Button);
        DU_Authenticate=findViewById(R.id.DU_Authenticate);


        //disable delete user button until user is authenticated
        DU_Button.setEnabled(false);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                if (readUserDetails != null) {
                    Email=readUserDetails.email;
                } else {
                    Toast.makeText(DeleteUser.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled (@NonNull DatabaseError error){
                Toast.makeText(DeleteUser.this, "Something went wrong!", Toast.LENGTH_LONG).show();
            }
        });
        DUTP=findViewById(R.id.DUTP);
        DUTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(DeleteUser.this, Profile.class);
                intent.putExtra("Rl",Rl);
                startActivity(intent);
                finish();
            }
        });
        if(firebaseUser.equals("")){
            Toast.makeText(DeleteUser.this, "Something went wrong! User details are not available at the moment", Toast.LENGTH_SHORT).show();
            Intent intent=new Intent(DeleteUser.this, Profile.class);
            intent.putExtra("Rl",Rl);
            startActivity(intent);
            finish();
        }else {
            reAuthenticateUser(firebaseUser);
        }
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
                    if(Rl.equals("Student"))
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
            else{
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();
            }
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
            resetToastTimer();
            if(utteranceId.equals("UTTERANCE_DELETE")){
                deleteUser(firebaseUser);
            }
            if(utteranceId.equals("TTS_UTTERANCE_STARTWAKEWORD")){
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();

            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(Rl.equals("Student")) {
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
        if(Rl.equals("Student")) {
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialization successful, set language and convert text to speech
                isTTSInitialized = true;
                textToSpeech.setLanguage(Locale.US);
                //Locale locale = new Locale("en","IN");
                //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
                //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
                //textToSpeech.setVoice(voice);
                int ttsResult = textToSpeech.speak("Hello, Welcome to the Delete account Page of Exam Care,Would you like to listen to a " +
                        "Detailed introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
                if (ttsResult == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }

                String YN="";
                if(YN.equals("YES")){
                    StarUpRepeat();
                }
                else{
                    int tts1=textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction," +
                            " in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
                    if (tts1== TextToSpeech.SUCCESS) {
                        // Pause the timer until TTS completes
                        pauseToastTimer();
                    }
                }
            } else {
                // TTS initialization failed, handle error
                Log.e("TTS", "Initialization failed");
            }
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
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Delete account Page of Exam Care, This page provides you with the facility, to" +
                "to delete your existing account, if you want to delete your account, you just have to say, Hello" +
                "Exam Care Delete my account to start the delete functionality.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
        else{
            int tts1=textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1== TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
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




    public void Automate(String Temp) {
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        appstate= AState.AppState.TTS;
        if(Temp.equals("Repeat Introduction")){
            StarUpRepeat();
        }else if(Temp.equals("back")){
            Intent intent=new Intent(DeleteUser.this,Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Rl","Student");
            startActivity(intent);
            finish();
        }else if (Temp.equals("delete user")) {
            int tts1 = textToSpeech.speak("Please say your password", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
            String userPwdCur="";
            AuthCredential credential= EmailAuthProvider.getCredential(firebaseUser.getEmail(), userPwdCur);
            firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        AutomateDeleteUser();

                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            int tts2 = textToSpeech.speak("Wrong Password Entered", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                            if (tts2 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                            DU_Password.setError("Wrong Password Entered");
                            DU_Password.requestFocus();
                        } catch (Exception e) {
                            Toast.makeText(DeleteUser.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                String change = "";


            });
        }
        wakeWordHelper.startListening();
    }

    public void AutomateDeleteUser () {
        int ttsResult = textToSpeech.speak("The account along with all your data will be permanently deleted. Do you want to delete the account?", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }

        String YN="";
        if(YN.equals("YES")) {
            int tts2 = textToSpeech.speak("Your Profile deletion process has been started. After the deletion, you will be redirected " +
                    "to the login page.", TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_DELETE");
            if (tts2 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
        }
        else {
            int tts3 = textToSpeech.speak("Your profile deletion operation has been cancelled .", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
            if (tts3 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();

            }
        }
    }

    private void reAuthenticateUser(FirebaseUser firebaseUser) {

        DU_Authenticate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                DU_progressBar.setVisibility(View.VISIBLE);
                userPwd = DU_Password.getText().toString();

                if (TextUtils.isEmpty(userPwd)) {
                    Toast.makeText(DeleteUser.this, "password is needed", Toast.LENGTH_SHORT).show();
                    DU_Password.setError("Please enter your current password to authenticator");
                    DU_Password.requestFocus();
                } else {

                    //ReAuthenticate User now
                    AuthCredential credential = EmailAuthProvider.getCredential(Email, userPwd);

                    firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task){
                            if(task.isSuccessful()){
                                DU_progressBar.setVisibility(View.GONE);

                                DU_Password.setEnabled(false);
                                DU_Authenticate.setEnabled(false);
                                DU_Button.setEnabled(true);

                                //set TextView to show user is authenticated/verified
                                Text.setText("You are authenticated/verified. You can delete your profile now.");
                                Toast.makeText(DeleteUser.this,"Password has been verified"+ "Change password now", Toast.LENGTH_SHORT).show();

                                //update color of change password button
                                int color= ContextCompat.getColor(DeleteUser.this, R.color.dark_green);;
                                DU_Button.setBackgroundTintList(ColorStateList.valueOf(color));
                                DU_Button.setOnClickListener(new View.OnClickListener(){
                                    @Override
                                    public void onClick(View V){
                                        showAlertDialog();
                                    }
                                });
                            }else {
                                try{
                                    throw task.getException();
                                }catch (Exception e){
                                    Toast.makeText(DeleteUser.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            DU_progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(DeleteUser.this);
        builder.setTitle("Delete User and Related Data?");
        builder.setMessage("Do you really want to delete your profile and related data? This action is irreversible.");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteUser(firebaseUser);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(DeleteUser.this, Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl",Rl);
                startActivity(intent);
                finish();
            }
        });

        //create the AlertDialog
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

    private void deleteUser(FirebaseUser firebaseUser){
        deleteUserData();
        firebaseUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                   authProfile.signOut();
                    Toast.makeText(DeleteUser.this, "Profile Deleted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DeleteUser.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }else {
                    try{
                        throw task.getException();
                    }catch (Exception e){
                        Toast.makeText(DeleteUser.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                DU_progressBar.setVisibility(View.GONE);
            }
        });
    }

    public void deleteUserData(){
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(DeleteUser.this, "User Data Deleted", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(DeleteUser.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onWakeWordDetected() {
        Toast.makeText(this, "Wakeword Detected"+appstate, Toast.LENGTH_SHORT).show();
    }

}
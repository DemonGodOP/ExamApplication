package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class ChangePassword extends AppCompatActivity implements TextToSpeech.OnInitListener{
    FirebaseAuth authProfile;
    EditText CP_Password, CP_NewPassword, CP_ConfirmPassword;
    TextView CP_Text,CPTP;
    Button CP_Authenticate, CP_Button;
    ProgressBar CP_progressBar;
    String userPwdCurr;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted; // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1
    String Rl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        Rl=getIntent().getStringExtra("Rl");
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        CP_Password= findViewById(R.id.CP_Password);
        CP_NewPassword = findViewById(R.id.CP_NewPassword);
        CP_ConfirmPassword= findViewById(R.id.CP_ConfirmPassword);
        CP_Text= findViewById(R.id.CP_Text);
        CP_progressBar = findViewById(R.id.CP_progressBar);
        CP_Authenticate=findViewById(R.id.CP_Authenticate);
        CP_Button = findViewById(R.id.CP_Button);

        CP_NewPassword.setEnabled(false);
        CP_ConfirmPassword.setEnabled(false);
        CP_Button.setEnabled(false);
        CPTP=findViewById(R.id.CPTP);
        CPTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent= new Intent(ChangePassword.this, Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl",Rl);
                startActivity(intent);
                finish();
            }
        });
        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();

        if(firebaseUser.equals("")) {
            Toast.makeText(ChangePassword.this, "Something went wrong! User's details not available", Toast.LENGTH_SHORT).show();
            Intent intent= new Intent(ChangePassword.this, Profile.class);
            startActivity(intent);
            finish();
        }else{
            reAuthenticateUser(firebaseUser);
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
        if (status == TextToSpeech.SUCCESS) {
            // TTS initialization successful, set language and convert text to speech
            isTTSInitialized = true;
            textToSpeech.setLanguage(Locale.US);
            //Locale locale = new Locale("en","IN");
            //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
            //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
            //textToSpeech.setVoice(voice);
            int ttsResult=textToSpeech.speak("Hello, Welcome to the Change Password Page of Exam Care, This page provides you with the facility, to " +
                    "change your password, for that please say your registered email id and then say your old password, then new password," +
                    "and after that say hello exam care password. Then a link will be sent to your  " +
                    " registered email id, from there you can change your password.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Change Password Page of Exam Care, This page provides you with the facility, to " +
                "change your password, for that please say your registered email id and then say your old password, then new password," +
                "and after that say hello exam care password. Then a link will be sent to your " +
                "registered email id, from there you can change your password.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int tts1=textToSpeech.speak("Let's, Begin the Process to change your password.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        int tts2=textToSpeech.speak("Please Say, Exam Care and then your registered Email ID", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts2 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Email=""; // Store the Email over here using STT.
        int tts3=textToSpeech.speak("Please Say, Exam Care and then your old password", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts3 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String pwd=""; // Store the Email over here using STT.

        int tts4=textToSpeech.speak("Please Say, Exam Care and then your new password", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts4 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String pwd2=""; // Store the Email over here using STT.
        int tts5=textToSpeech.speak("Please Say, Exam Care Log me In, Inorder to login", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts5 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        boolean YesResetPassword=false;//Edit This Using STT
        if (YesResetPassword == true) {
            // reAuthenticate(firebaseuser);
            // updateEmail(Firebaseuser);
        }
    }
    private void reAuthenticateUser(FirebaseUser firebaseUser){
        CP_Authenticate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                userPwdCurr = CP_Password.getText().toString();

                if(TextUtils.isEmpty(userPwdCurr)){
                    Toast.makeText(ChangePassword.this, "password is needed", Toast.LENGTH_SHORT).show();
                    CP_Password.setError("Please enter your current password to authenticator");
                    CP_Password.requestFocus();
                }else {
                    CP_progressBar.setVisibility(View.VISIBLE);

                    //ReAuthenticate User now
                    AuthCredential credential= EmailAuthProvider.getCredential(firebaseUser.getEmail(), userPwdCurr);

                    firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task){
                            if(task.isSuccessful()){
                                CP_progressBar.setVisibility(View.GONE);

                                //Disable editText for current password, enable EditText for new password and confirm new password
                                CP_Password.setEnabled(false);
                                CP_ConfirmPassword.setEnabled(true);
                                CP_NewPassword.setEnabled(true);

                                //enable change Pwd button. Disable authenticate button
                                CP_Authenticate.setEnabled(false);
                                CP_Button.setEnabled(true);

                                //set TextView to show user is authenticated/verified
                                CP_Text.setText("You are authenticated/verified."+"You can changer password now!");
                                Toast.makeText(ChangePassword.this,"Password has been verified"+ "Change password now", Toast.LENGTH_SHORT).show();

                                //update color of change password button
                                int color=ContextCompat.getColor(ChangePassword.this, R.color.dark_green);;
                                CP_Button.setBackgroundTintList(ColorStateList.valueOf(color));
                                CP_Button.setOnClickListener(new View.OnClickListener(){
                                    @Override
                                    public void onClick(View V){
                                        changePwd(firebaseUser);
                                    }
                                });
                            }else {
                                try{
                                    throw task.getException();
                                }catch(FirebaseAuthInvalidCredentialsException e)
                                {
                                    CP_Password.setError("Wrong Password Entered");
                                    CP_Password.requestFocus();
                                } catch(Exception e){
                                    Toast.makeText(ChangePassword.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            CP_progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }
    private void changePwd(FirebaseUser firebaseUser){
        String userPwdNew = CP_NewPassword.getText().toString();
        String userPwdConfirmNew =  CP_ConfirmPassword.getText().toString();

        if(TextUtils.isEmpty(userPwdNew)){
            Toast.makeText(ChangePassword.this, "New password is needed", Toast.LENGTH_SHORT).show();
            CP_NewPassword.setError("Please enter your new password");
            CP_NewPassword.requestFocus();
        }else if(TextUtils.isEmpty(userPwdConfirmNew)){
            Toast.makeText(ChangePassword.this, "Please confirm your new password", Toast.LENGTH_SHORT).show();
            CP_ConfirmPassword.setError("Please re-enter your password");
            CP_ConfirmPassword.requestFocus();
        }else if(!userPwdNew.matches(userPwdConfirmNew)){
            Toast.makeText(ChangePassword.this, "Password did not match", Toast.LENGTH_SHORT).show();
            CP_ConfirmPassword.setError("Please re-enter same password");
            CP_ConfirmPassword.requestFocus();
        }
        else if(userPwdNew.matches(userPwdCurr)){
            Toast.makeText(ChangePassword.this, "New password cannot be same as old password", Toast.LENGTH_SHORT).show();
            CP_NewPassword.setError("Please enter a new password");
            CP_NewPassword.requestFocus();
        }else {
            CP_progressBar.setVisibility(View.VISIBLE);

            firebaseUser.updatePassword(userPwdNew).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task){
                    if(task.isSuccessful()){
                        Toast.makeText(ChangePassword.this, "Password has been changed", Toast.LENGTH_SHORT).show();
                        finish();
                    }else{
                        try{
                            throw task.getException();
                        }catch (Exception e){
                            Toast.makeText(ChangePassword.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    CP_progressBar.setVisibility(View.GONE);
                }
        });
        }
    }
}
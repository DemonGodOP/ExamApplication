package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;


public class Register extends AppCompatActivity implements TextToSpeech.OnInitListener {

    EditText R_Name,R_Email,R_PhoneNo,R_Institute,R_UserName,R_Password;
    Button Submit;
    TextView RTL;
    RadioGroup role;
    String FinalRole;
    ProgressBar R_PB;
    RadioButton Selected;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted;

    // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0
        R_Name=findViewById(R.id.R_Name);
        R_Email=findViewById(R.id.R_Email);
        R_PhoneNo=findViewById(R.id.R_PhoneNo);
        R_Institute=findViewById(R.id.R_Institute);
        R_UserName=findViewById(R.id.R_UserName);
        R_Password=findViewById(R.id.R_Password);
        RTL=findViewById(R.id.RTL);
        Submit=findViewById(R.id.R_Submit);
        role = findViewById(R.id.Role);
        R_PB=findViewById(R.id.R_PB);
        RTL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Register.this,Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedRole = role.getCheckedRadioButtonId();
                Selected = findViewById(selectedRole);
                String Name = R_Name.getText().toString();
                String Email = R_Email.getText().toString();
                String PhoneNo = R_PhoneNo.getText().toString();
                String Institute = R_Institute.getText().toString();
                String UserName = R_UserName.getText().toString();
                String Password = R_Password.getText().toString();
                String RS;
                if (TextUtils.isEmpty(Name)) {
                    R_Name.setError("Please enter your Name");
                    R_Name.requestFocus();
                } else if (TextUtils.isEmpty(Email)) {
                    R_Email.setError("Please enter your Email");
                    R_Email.requestFocus();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(Email).matches()) {
                    R_Email.setError("Valid Email is required");
                    R_Email.requestFocus();
                } else if (TextUtils.isEmpty(PhoneNo)) {
                    R_PhoneNo.setError("Please enter your PhoneNo");
                    R_PhoneNo.requestFocus();
                } else if(PhoneNo.length()!=10){
                    R_PhoneNo.setError("Valid PhoneNo. is required");
                    R_PhoneNo.requestFocus();
                } else if (TextUtils.isEmpty(Institute)) {
                    R_Institute.setError("Please enter your Institute");
                    R_Institute.requestFocus();
                } else if (TextUtils.isEmpty(UserName)) {
                    R_UserName.setError("Please enter your UserName");
                    R_UserName.requestFocus();
                } else if (TextUtils.isEmpty(Password)) {
                    R_Password.setError("Password is required");
                    R_Password.requestFocus();
                } else if(Password.length()<8){
                    R_Password.setError("Enter a Stronger Password");
                    R_Password.requestFocus();
                }else if(selectedRole==-1){
                    Toast.makeText(Register.this, "Please select a role", Toast.LENGTH_SHORT).show();
                }else {
                    FinalRole=Selected.getText().toString();
                    R_PB.setVisibility(View.VISIBLE);
                    registerUser(Name,Email,PhoneNo,Institute,UserName,Password,FinalRole);
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
        startToastTimer();

    //2
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
            int ttsResult=textToSpeech.speak("Hello, Welcome to the Registration Page of Exam Care, This page provides you with the facility, to " +
                    "register for a new account, for that you just have to say, Hello" +
                    " Exam Care Registration, Or if you have an account, you can also move on to the login page, for that you just say Exam Care" +
                    "login.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int ttsResult=textToSpeech.speak(" Hello, Welcome to the Registration Page of Exam Care, This page provides you with the facility, to " +
                "register for a new account, for that you just have to say, Hello"  +
                 "Exam Care Registration, Or if you have an account, you can also move on to the login page, for that you just say Exam Care" +
                "login.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
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
        int tts1=textToSpeech.speak("Let's, Begin the Registration Process.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        int tts2=textToSpeech.speak("Please Say, Exam Care and then your Name", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts2 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Name=""; // Store the Name over here using STT.
        int tts3=textToSpeech.speak("Please Say, Exam Care and then your Email ID", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts3 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Email=""; // Store the Email over here using STT.
        int tts4=textToSpeech.speak("Please Say, Exam Care and then your Phone no", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts4 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Phone=""; // Store the Phone No over here using STT.
        int tts5=textToSpeech.speak("Please Say, Exam Care and then your Institute name", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts5 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Institute=""; // Store the Institute name over here using STT.
        int tts6=textToSpeech.speak("Please Say, Exam Care and then your Username", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts6 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Username=""; // Store the Username over here using STT.
        int tts7=textToSpeech.speak("Please Say, Exam Care and then your Password", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts7 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Pwd=""; //Store password over here using STT.
        int tts8=textToSpeech.speak("Please Say, Exam Care and then your Role", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts8 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String Role=""; // Store the Email over here using STT.
        int tts9=textToSpeech.speak("Please Say, Exam Care Log me In, Inorder to login", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
        if (tts9 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        boolean YesRegister=false;//Edit This Using STT
        if (YesRegister == true) {
            registerUser(Name, Email,Phone,Institute, Username,Pwd,Role);
        }
    }

    private void registerUser(String name, String email, String phoneNo, String institute, String userName, String password, String finalRole) {
        FirebaseAuth auth= FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(email, name, phoneNo, institute, userName, finalRole);
                    DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                    assert firebaseUser != null;
                    referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails);
                    Toast.makeText(Register.this, "Account Created", Toast.LENGTH_SHORT).show();
                    assert firebaseUser != null;
                    firebaseUser.sendEmailVerification();
                    Intent intent = new Intent(Register.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                else{
                    try{
                        throw task.getException();
                    }catch(FirebaseAuthWeakPasswordException e){
                        R_Password.setError("Your Password is too weak.");
                        R_Password.requestFocus();
                    }catch(FirebaseAuthInvalidCredentialsException e){
                        R_Email.setError("Invalid Email");
                        R_Email.requestFocus();
                    }catch(FirebaseAuthUserCollisionException e){
                        R_Email.setError("User Already exists with this Email ID");
                        R_Email.requestFocus();
                    }catch(Exception e){
                        Toast.makeText(Register.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                R_PB.setVisibility(View.GONE);
            }
        });
    }
}
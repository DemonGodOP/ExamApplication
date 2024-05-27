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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChangeEmail extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener{
    FirebaseAuth authProfile;
    FirebaseUser firebaseUser;
    ProgressBar CE_progressBar;
    TextView CE_Text,CE_Email,CETP;
    Button CE_Button;
    EditText CE_NewEmail, CE_Password;

    String Old_Email,New_Email, Name, finalRole, Institute, Phone, Username,userPwd;
    TextToSpeech textToSpeech;//1

    Handler handler;
    Runnable toastRunnable;

    boolean isUserInteracted; // Flag to indicate if TextToSpeech engine is initialized
    boolean isTTSInitialized;//1
    Button buttonVerifyUser;
    String Rl;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);
        Intent intent = getIntent();

        Rl= intent.getStringExtra("Rl");

        Intent checkIntent = new Intent();//0
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, 1);//0

        buttonVerifyUser= findViewById(R.id.CE_Authenticate);
        CE_progressBar= findViewById(R.id.CE_progressBar);
        CE_Text=findViewById(R.id.CE_Text);
        CE_Email=findViewById(R.id.CE_Email);
        CE_NewEmail=findViewById(R.id.CE_NewEmail);
        CE_Button=findViewById(R.id.CE_Button);
        CE_Password=findViewById(R.id.CE_Password);
        authProfile=FirebaseAuth.getInstance();
        firebaseUser= authProfile.getCurrentUser();
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                if (readUserDetails != null) {
                    Name = readUserDetails.name;
                    Phone = readUserDetails.phoneNo;
                    Institute = readUserDetails.institute;
                    Username = readUserDetails.userName;
                    finalRole=readUserDetails.finalRole;
                    Old_Email=readUserDetails.email;
                    CE_Email.setText(Old_Email);
                } else {
                    Toast.makeText(ChangeEmail.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                }

            }
            @Override
            public void onCancelled (@NonNull DatabaseError error){
                Toast.makeText(ChangeEmail.this, "Something went wrong!", Toast.LENGTH_LONG).show();
            }
        });
        CETP=findViewById(R.id.CETP);
        CETP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 Intent intent=new Intent(ChangeEmail.this,Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl",Rl);
                startActivity(intent);
                finish();
            }
        });
        CE_Button.setEnabled(false);
        CE_NewEmail.setEnabled(false);



        if(firebaseUser.equals("")){
            Toast.makeText(ChangeEmail.this, "Something went wrong! User's details not available", Toast.LENGTH_SHORT).show();
        }else{
            reAuthenticate (firebaseUser);
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
            if(utteranceId.equals("TTS_UTTERANCE_STARTWAKEWORD")){
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();

            }
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
            int ttsResult=textToSpeech.speak("Hello, Welcome to the Change Email Page of Exam Care, Would you like to listen to a Detailed introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_ID");
            if (ttsResult == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }

            String YN="";
            if(YN.equals("YES")){
                StarUpRepeat();
            }
            else{
                int tts1=textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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

    // Repeat The Introduction if Repeat Method is Triggered.
    public void StarUpRepeat(){
        resetToastTimer();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult=textToSpeech.speak("Hello, Welcome to the Change Email Page of Exam Care, This page provides you with the facility, to " +
                "change your email, for that please say your registered email id and then say the new email id and after that say hello exam care reset email. Then a link will be sent to your " +
                "registered email id, from there you can reset your email.", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
        if(Temp.equals("Repeat Introduction")){
            StarUpRepeat();
        }else if(Temp.equals("back")){
            Intent intent=new Intent(ChangeEmail.this,Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Rl","Student");
            startActivity(intent);
            finish();
        }else if (Temp.equals("change email")) {
            int tts1 = textToSpeech.speak("Please say your password", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
            String userPwdCur = "";
            AuthCredential credential = EmailAuthProvider.getCredential(Old_Email, userPwd);

            firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        CE_progressBar.setVisibility(View.VISIBLE);
                        int tts2 = textToSpeech.speak("Password has been verified,You can update email now.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                        if (tts2 == TextToSpeech.SUCCESS) {
                            // Pause the timer until TTS completes
                            pauseToastTimer();
                        }

                        //set TextView to show the user is authenticated
                        CE_Text.setText("You are authenticated. You can update your email now");

                        //disable editText for password and enable editText for new email and update
                        CE_NewEmail.setEnabled(true);
                        CE_Password.setEnabled(false);
                        buttonVerifyUser.setEnabled(false);
                        CE_Button.setEnabled(true);

                        //change color of update Email button
                        CE_Button.setBackgroundTintList(ContextCompat.getColorStateList(ChangeEmail.this, R.color.dark_green));
                        AutomateChangeEmail ();
                    }
                    else {
                        try {
                            throw task.getException();
                        } catch(FirebaseAuthInvalidCredentialsException e){
                            CE_Password.setError("Incorrect Credentials");
                            CE_Password.requestFocus();
                        }catch(FirebaseAuthUserCollisionException e){
                            CE_NewEmail.setError("User Already exists with this Email ID");
                            CE_NewEmail.requestFocus();
                        }catch (Exception e) {
                            Toast.makeText(ChangeEmail.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
        wakeWordHelper.startListening();
    }

    public void AutomateChangeEmail () {
        int tts1 = textToSpeech.speak("Please say your new email", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
        if (tts1 == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
        String New_Email="";
        if (!Patterns.EMAIL_ADDRESS.matcher(New_Email).matches()) {
            int tts2 = textToSpeech.speak("Please provide valid email", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
            if (tts2 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
            CE_NewEmail.setError("Please provide valid email");
            CE_NewEmail.requestFocus();
        }
        else if (Old_Email.matches(New_Email)) {
            int tts3 = textToSpeech.speak("Please enter new email", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
            if (tts3 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
            CE_NewEmail.setError("Please enter new email");
            CE_NewEmail.requestFocus();
        } else {
            boolean res[]={false};
            DatabaseReference CheckEmail=FirebaseDatabase.getInstance().getReference().child("Registered Users");
            CheckEmail.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                        ReadWriteUserDetails readWriteUserDetails=dataSnapshot.child("User Details").getValue(ReadWriteUserDetails.class);
                        if(readWriteUserDetails!=null){
                            if(readWriteUserDetails.email.equals(New_Email)){
                                res[0]=true;
                                break;
                            }
                        }
                    }
                    if(res[0]==true){
                        int tts4 = textToSpeech.speak("Email Already Exists in the Database.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
                        if (tts4 == TextToSpeech.SUCCESS) {
                            // Pause the timer until TTS completes
                            pauseToastTimer();
                        }
                        CE_NewEmail.setError("Email Already Exists in the Database.");
                        CE_NewEmail.requestFocus();
                    }
                    else{
                        updateEmail(firebaseUser);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void reAuthenticate(FirebaseUser firebaseuser){
        buttonVerifyUser.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                //obtain password for authentication
                userPwd=CE_Password.getText().toString();

                if(TextUtils.isEmpty(userPwd)){
                    Toast.makeText(ChangeEmail.this, "Password is needed to continue", Toast.LENGTH_SHORT).show();
                    CE_Password.setError("Please enter your password for authentication");
                    CE_Password.requestFocus();
                }else {
                    CE_progressBar.setVisibility(View.VISIBLE);

                    AuthCredential credential= EmailAuthProvider.getCredential(Old_Email, userPwd);

                    firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                CE_progressBar.setVisibility(View.VISIBLE);

                                Toast.makeText(ChangeEmail.this, "Password has been verified." + "You can update email now.", Toast.LENGTH_SHORT).show();

                                //set TextView to show the user is authenticated
                                CE_Text.setText("You are authenticated. You can update your email now");

                                //disable editText for password and enable editText for new email and update
                                CE_NewEmail.setEnabled(true);
                                CE_Password.setEnabled(false);
                                buttonVerifyUser.setEnabled(false);
                                CE_Button.setEnabled(true);

                                //change color of update Email button
                                CE_Button.setBackgroundTintList(ContextCompat.getColorStateList(ChangeEmail.this, R.color.dark_green));

                                CE_Button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        New_Email = CE_NewEmail.getText().toString();
                                        if (TextUtils.isEmpty(New_Email)) {
                                            Toast.makeText(ChangeEmail.this, "New email is required", Toast.LENGTH_SHORT).show();
                                            CE_NewEmail.setError("Please enter new email");
                                            CE_NewEmail.requestFocus();
                                        } else if (!Patterns.EMAIL_ADDRESS.matcher(New_Email).matches()) {
                                            Toast.makeText(ChangeEmail.this, "Please enter valid email", Toast.LENGTH_SHORT).show();
                                            CE_NewEmail.setError("Please provide valid email");
                                            CE_NewEmail.requestFocus();
                                        } else if (Old_Email.matches(New_Email)) {
                                            Toast.makeText(ChangeEmail.this, "New email cannot be same as old email", Toast.LENGTH_SHORT).show();
                                            CE_NewEmail.setError("Please enter new email");
                                            CE_NewEmail.requestFocus();
                                        } else {
                                           boolean res[]={false};
                                           DatabaseReference CheckEmail=FirebaseDatabase.getInstance().getReference().child("Registered Users");
                                           CheckEmail.addValueEventListener(new ValueEventListener() {
                                               @Override
                                               public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                   for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                                                       ReadWriteUserDetails readWriteUserDetails=dataSnapshot.child("User Details").getValue(ReadWriteUserDetails.class);
                                                       if(readWriteUserDetails!=null){
                                                           if(readWriteUserDetails.email.equals(New_Email)){
                                                               res[0]=true;
                                                               break;
                                                           }
                                                       }
                                                   }
                                                   if(res[0]==true){
                                                       CE_NewEmail.setError("Email Already Exists in the Database.");
                                                       CE_NewEmail.requestFocus();
                                                   }
                                                   else{
                                                       updateEmail(firebaseuser);
                                                   }
                                               }

                                               @Override
                                               public void onCancelled(@NonNull DatabaseError error) {

                                               }
                                           });
                                        }

                                    }
                                });
                            } else {
                                try {
                                    throw task.getException();
                                } catch(FirebaseAuthInvalidCredentialsException e){
                                    CE_Password.setError("Incorrect Credentials");
                                    CE_Password.requestFocus();
                                }catch(FirebaseAuthUserCollisionException e){
                                    CE_NewEmail.setError("User Already exists with this Email ID");
                                    CE_NewEmail.requestFocus();
                                }catch (Exception e) {
                                    Toast.makeText(ChangeEmail.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            CE_progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void updateEmail(FirebaseUser firebaseUser){
        showAlertDialog();
        firebaseUser.verifyBeforeUpdateEmail(New_Email).addOnCompleteListener(new OnCompleteListener<Void>(){
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                            DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                            ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(New_Email, Name, Phone, Institute, Username, finalRole);
                            referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails);
                            CE_progressBar.setVisibility(View.GONE);
                            CE_Password.setError("Email Updated");
                            CE_Password.requestFocus();
                } else {
                    // Email verification failed, handle accordingly
                    Exception exception = task.getException();
                    if (exception != null) {
                        Toast.makeText(ChangeEmail.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(ChangeEmail.this);
        builder.setTitle("Email Not Verified");
        builder.setMessage("Please verify your email now.");

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
    public void onWakeWordDetected() {
        Toast.makeText(this, "Wakeword Detected"+appstate, Toast.LENGTH_SHORT).show();
    }
}
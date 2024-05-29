package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class Profile extends AppCompatActivity implements TextToSpeech.OnInitListener,WakeWordListener{
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
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    AState.AppState appstate;

    WakeWordHelper wakeWordHelper;

    String STTData;

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
        if(Rl.equals("Student")) {
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            appstate = AState.AppState.TTS;
            if (hasRecordPermission()) {
                wakeWordHelper = new WakeWordHelper(this, appstate, this);
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                speechRecognizer.setRecognitionListener(new Profile.SpeechListener());
            } else {
                // Permission already granted
                requestRecordPermission();
            }
        }
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



    @Override
    protected void onPause() {
        super.onPause();
        if(speechRecognizer!=null) {
            speechRecognizer.stopListening();
        }
        pauseToastTimer();
        if(wakeWordHelper!=null) {
            wakeWordHelper.stopListening();
            appstate= AState.AppState.TTS;
        }
        if(textToSpeech!=null) {
            textToSpeech.stop();
        }
    }

    @Override //3
    protected void onResume() {
        super.onResume();
        // Reset the timer whenever the user interacts with the app
        resetToastTimer();
        if(Rl.equals("Student")) {
            isUserInteracted = false; // Reset user interaction flag
            if (textToSpeech != null) {
                int ttsResult = textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care, Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                if (ttsResult == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
                }
                //Enter the Condition Over here that is tts to take input from the user if they wants us to repeat the introduction and change r respectively.
            /*boolean r=false;
            if(r==true){
                StarUpRepeat();
            } // Restart the TTS when the activity is resumed
            else{
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();
            }*/
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(Rl.equals("Student")) {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening(); // Destroy the speech recognizer when the app is no longer visible
            }
            if (textToSpeech != null) {
                textToSpeech.stop();
            }

            if (wakeWordHelper != null) {
                wakeWordHelper.stopListening();
            }
        }
        pauseToastTimer();
    }

    @Override
    protected void onDestroy() {
        // Release resources
        if(Rl.equals("Student")) {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
            if (speechRecognizer != null) {
                speechRecognizer.destroy(); // Destroy the speech recognizer when the app is no longer visible
            }
            if (wakeWordHelper != null) {
                wakeWordHelper.stopListening();
            }
        }
        handler.removeCallbacks(toastRunnable);
        super.onDestroy();
    }//3

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
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new Profile.SpeechListener());
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

    private class SpeechListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    Toast.makeText(Profile.this, "Error recording audio.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Toast.makeText(Profile.this, "Insufficient permissions.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                case SpeechRecognizer.ERROR_NETWORK:
                    Toast.makeText(Profile.this, "Network Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Toast.makeText(Profile.this, "No recognition result matched.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    return;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Toast.makeText(Profile.this, "Recognition service is busy.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Toast.makeText(Profile.this, "Server Error.", Toast.LENGTH_SHORT).show();
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Toast.makeText(Profile.this, "No speech input.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(Profile.this, "Something wrong occurred.", Toast.LENGTH_SHORT).show();
            }


        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if(appstate==AState.AppState.STT) {
                STTData = data.get(0).toLowerCase();
            }
            else if(appstate== AState.AppState.AUTOMATE){
                if(data.get(0).toLowerCase()!=null)
                    Automate(data.get(0).toLowerCase());
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speechRecognizer.startListening(speechRecognizerIntent);
                            Toast.makeText(Profile.this, "Listening", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
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
            if(utteranceId.equals("TTS_UTTERANCE_STARTWAKEWORD")){
                appstate= AState.AppState.WAKEWORD;
                wakeWordHelper.startListening();
                Toast.makeText(Profile.this, "Listening", Toast.LENGTH_SHORT).show();
            }
            else if(utteranceId.equals("TTS_UTTERANCE_ONINIT")){
                runOnUiThread(() -> {
                    try {
                        speechRecognizer.startListening(speechRecognizerIntent);
                        Log.d("STT", "Speech recognizer started listening.");
                    } catch (Exception e) {
                        Log.e("STT", "Exception starting speech recognizer", e);
                    }

                    // Ensure the Toast is shown on the main thread
                    Toast.makeText(Profile.this, "Listening", Toast.LENGTH_SHORT).show();
                });
                appstate = AState.AppState.STT;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                        String YN = STTData;
                        if (YN != null && YN.equals("yes")) {
                            StarUpRepeat();
                        } else {
                            int tts1 = textToSpeech.speak("No Input Detected, Starting WakeWord Engine, Please Say, Exam Care, Repeat Introduction, in order to listen to the introduction of the page.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
                            if (tts1 == TextToSpeech.SUCCESS) {
                                // Pause the timer until TTS completes
                                pauseToastTimer();
                            }
                        }
                    }
                }, 5000);
            }
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
                int ttsResult = textToSpeech.speak("Hello, Welcome to the Profile Page of Exam Care, Would you like to listen to a Detailed introduction of the page. Say Yes or No", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ONINIT");
                if (ttsResult == TextToSpeech.SUCCESS) {
                    // Pause the timer until TTS completes
                    pauseToastTimer();
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
                "see your profile details such as name, email, phone number, institute, username and role, you just have to say, Hello Exam care,describe profile details.", TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_STARTWAKEWORD");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            pauseToastTimer();
        }
    }

    public void Repeat() {
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        int ttsResult=textToSpeech.speak("If you want me to repeat the introduction of the page again please say, Exam Care Repeat Introduction", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
        if (ttsResult == TextToSpeech.SUCCESS) {
            // Pause the timer until TTS completes
            //appstate= AState.AppState.TTS;
            pauseToastTimer();
        }
    }


    public void Automate(String Temp) {
        wakeWordHelper.stopListening();
        textToSpeech.setLanguage(Locale.US);
        //Locale locale = new Locale("en","IN");
        //Name: en-in-x-end-network Locale: en_IN Is Network TTS: true
        //Voice voice = new Voice("en-in-x-end-network", locale, 400, 200, true, null); // Example voice
        //textToSpeech.setVoice(voice);
        if(Temp.equals("repeat introduction")){
            StarUpRepeat();
        }else if(Temp.equals("back")){
            Intent intent=new Intent(Profile.this,StudentHomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Rl","Student");
            startActivity(intent);
            finish();
        }
        else if(Temp.equals("describe profile details")){
            int tts1=textToSpeech.speak("Your Name is"+name+"Your email address is"+email+"your phone number is"+phone+
                            "Your institute name is"+institute+"your username is"+username+"", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
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
        } else if (Temp.equals("delete profile")) {
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
        else{
            Toast.makeText(this, Temp, Toast.LENGTH_SHORT).show();
            int tts1=textToSpeech.speak("Wrong input provided "+Temp+ " Please start the process from the beginning. Sorry for any inconvenience", TextToSpeech.QUEUE_FLUSH, null,"TTS_UTTERANCE_STARTWAKEWORD");
            if (tts1 == TextToSpeech.SUCCESS) {
                // Pause the timer until TTS completes
                pauseToastTimer();
            }
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
    @Override
    public void onWakeWordDetected() {
        Toast.makeText(this, "Wakeword Detected"+appstate, Toast.LENGTH_SHORT).show();
        if(speechRecognizerIntent!=null){
            appstate= AState.AppState.AUTOMATE;
            pauseToastTimer();
            speechRecognizer.startListening(speechRecognizerIntent);
        }
        else{
            Toast.makeText(this, "Null Speech 2", Toast.LENGTH_SHORT).show();
        }
    }
}
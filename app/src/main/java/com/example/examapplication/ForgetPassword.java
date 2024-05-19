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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import java.util.Locale;

public class ForgetPassword extends AppCompatActivity {
    ProgressBar FP_progressBar;
    EditText FP_Email;
    Button FP_Button;
    FirebaseAuth authProfile;
    TextView FPTL;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        FP_Email=findViewById(R.id.FP_Email);
        FP_Button=findViewById(R.id.FP_Button);
        FP_progressBar=findViewById(R.id.FP_progressBar);
        FPTL=findViewById(R.id.FPTL);

        FP_Button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                String email=FP_Email.getText().toString();

                if(TextUtils.isEmpty(email)) {
                    FP_Email.setError("Email is required");
                    FP_Email.requestFocus();
                }
                else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    FP_Email.setError("Email is required");
                    FP_Email.requestFocus();
                }else{
                    FP_progressBar.setVisibility(View.VISIBLE);
                    resetPassword(email);
                }
            }
        });

        FPTL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(ForgetPassword.this,Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

    }

    private void resetPassword(String email) {
        authProfile=FirebaseAuth.getInstance();
        authProfile.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(ForgetPassword.this, "Please check your inbox for password reset link",
                            Toast.LENGTH_SHORT).show();
                    Intent intent=new Intent(ForgetPassword.this,Login.class);
                    //Clear stack to prevent user coming back to forgotpasswordActivity
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }else{
                    try {
                        throw task.getException();
                    }catch(FirebaseAuthInvalidUserException e){
                        FP_Email.setError("User does not exists or is no longer valid.Please register again.");
                    }catch(Exception e){
                        Toast.makeText(ForgetPassword.this,e.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                    Toast.makeText(ForgetPassword.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
                FP_progressBar.setVisibility(View.GONE);
            }
        });
    }
}
package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
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

public class ChangePassword extends AppCompatActivity {
    FirebaseAuth authProfile;
    EditText CP_Password, CP_NewPassword, CP_ConfirmPassword;
    TextView CP_Text,CPTP;
    Button CP_Authenticate, CP_Button;
    ProgressBar CP_progressBar;
    String userPwdCurr;

    String Rl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        Rl=getIntent().getStringExtra("Rl");
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
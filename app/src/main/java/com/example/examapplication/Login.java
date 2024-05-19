package com.example.examapplication;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Login extends AppCompatActivity {
    EditText L_Email, L_Password;
    ProgressBar L_progressBar;
    TextView LTR,FR;
    FirebaseAuth authProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login);

            L_Email = findViewById(R.id.L_Email);
            L_Password = findViewById(R.id.L_Password);
            L_progressBar = findViewById(R.id.L_progressBar);

            authProfile = FirebaseAuth.getInstance();
            FR=findViewById(R.id.FR);
            FR.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    Intent intent=new Intent(Login.this,ForgetPassword.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();

                }
            });

            LTR=findViewById(R.id.LTR);
            LTR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent=new Intent(Login.this,Register.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
            L_Password.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ImageView imageViewShowHidePwd = findViewById(R.id.imageView_show_hide_pwd);
            imageViewShowHidePwd.setImageResource(R.drawable.ic_hide_pwd);
            imageViewShowHidePwd.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (L_Password.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())) {
                        //if password is visible then hide it
                        L_Password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        //change icon
                        imageViewShowHidePwd.setImageResource(R.drawable.ic_show_pwd);
                    } else {
                        L_Password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        imageViewShowHidePwd.setImageResource(R.drawable.ic_hide_pwd);
                    }
                }
            });



            //Login User
            Button L_Button = findViewById(R.id.L_Button);
            L_Button.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    String textEmail = L_Email.getText().toString();
                    String textPwd = L_Password.getText().toString();
                    if (TextUtils.isEmpty(textEmail)) {
                        L_Email.setError("Email is required");
                        L_Email.requestFocus();
                    } else if (!Patterns.EMAIL_ADDRESS.matcher(textEmail).matches()) {
                        L_Email.setError("Valid Email is required");
                        L_Email.requestFocus();
                    } else if (TextUtils.isEmpty(textPwd)) {
                        Toast.makeText(Login.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                        L_Password.setError("Password is required");
                        L_Password.requestFocus();
                    } else {
                        L_progressBar.setVisibility(View.VISIBLE);
                        loginUser(textEmail, textPwd);
                    }
                }
            });


        }

 

        private void loginUser(String email, String pwd) {
            authProfile.signInWithEmailAndPassword(email, pwd).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(Login.this, "You are logged in now", Toast.LENGTH_SHORT).show();
                        //Get instance of the current user
                        FirebaseUser firebaseUser = authProfile.getCurrentUser();
                        //Check if email is verified before user can access their profile

                        if (firebaseUser.isEmailVerified()) {
                            Toast.makeText(Login.this, "You are logged in now", Toast.LENGTH_SHORT).show();
                            DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                            referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                                    if (readUserDetails != null) {
                                        String r = readUserDetails.finalRole;
                                        if (r.equals("Teacher")) {
                                            Intent intent = new Intent(Login.this, TeacherHomePage.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.putExtra("Rl","Teacher");
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Intent intent = new Intent(Login.this, StudentHomePage.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            intent.putExtra("Rl","Student");
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(Login.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            firebaseUser.sendEmailVerification();
                            authProfile.signOut();//Sign out User
                            showAlertDialog();
                        }
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            L_Email.setError("User does not exists or is no longer valid.Please register again.");
                            L_Email.requestFocus();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            L_Email.setError("Invalid credentials.Kindly,check and re-enter");
                            L_Email.requestFocus();
                        } catch (Exception e) {
                            Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    L_progressBar.setVisibility(View.GONE);
                }
            });
        }
        private void showAlertDialog () {
            //Setup the Alert Builder
            AlertDialog.Builder builder=new AlertDialog.Builder(Login.this);
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
    }
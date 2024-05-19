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


public class Register extends AppCompatActivity  {

    EditText R_Name,R_Email,R_PhoneNo,R_Institute,R_UserName,R_Password;
    Button Submit;
    TextView RTL;
    RadioGroup role;
    String FinalRole;
    ProgressBar R_PB;
    RadioButton Selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
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
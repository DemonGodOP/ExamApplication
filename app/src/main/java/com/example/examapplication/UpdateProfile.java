package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UpdateProfile extends AppCompatActivity {
    EditText UP_Name, UP_Phone, UP_Institute,UP_UserName;
    String Name, Phone, Institute,  Username, finalRole, email;
    FirebaseAuth authProfile;
    ProgressBar UP_progressBar;

    TextView UPTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);
        UP_progressBar = findViewById(R.id.UP_progressBar);
        UP_Name = findViewById(R.id.UP_Name);
        UP_Phone= findViewById(R.id.UP_Phone);
        UP_Institute = findViewById(R.id.UP_Institute);
        UP_UserName=findViewById(R.id.UP_UserName);

        UPTH=findViewById(R.id.UPTH);
        UPTH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(finalRole.equals("Teacher")) {
                    Intent intent = new Intent(UpdateProfile.this, TeacherHomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                else{
                    Intent intent = new Intent(UpdateProfile.this, StudentHomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });
        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();


        showProfile(firebaseUser);

        Button buttonUpdateProfile = findViewById(R.id.UP_Button);
        buttonUpdateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile(firebaseUser);
            }
        });
    }
    private void showProfile(FirebaseUser firebaseUser) {
        String userIDofRegistered = firebaseUser.getUid();

//Extracting User Reference from Database for "Registered users"
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");

        UP_progressBar.setVisibility(View.VISIBLE);

        referenceProfile.child(userIDofRegistered).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                if (readUserDetails != null) {
                    Name = readUserDetails.name;
                    Phone = readUserDetails.phoneNo;
                    Institute = readUserDetails.institute;
                    Username = readUserDetails.userName;
                    finalRole=readUserDetails.finalRole;
                    email=firebaseUser.getEmail();
                    UP_Name.setHint(Name);
                    UP_Phone.setHint(Phone);
                    UP_Institute.setHint(Institute);
                    UP_UserName.setHint(Username);
                } else {
                    Toast.makeText(UpdateProfile.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                }
                UP_progressBar.setVisibility(View.GONE);
            }
                @Override
                public void onCancelled (@NonNull DatabaseError error){
                    Toast.makeText(UpdateProfile.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                    UP_progressBar.setVisibility(View.GONE);
                }
        });
    }

    public void updateProfile(FirebaseUser firebaseUser){
        Name=UP_Name.getText().toString();
        Phone=UP_Phone.getText().toString();
        Institute=UP_Institute.getText().toString();
        Username=UP_UserName.getText().toString();

        if (TextUtils.isEmpty(Name)) {
            Name=UP_Name.getHint().toString();
        }
        if (TextUtils.isEmpty(Phone)) {
            Phone=UP_Phone.getHint().toString();
        } else if(Phone.length()!=10){
            UP_Phone.setError("Valid PhoneNo. is required");
            UP_Phone.requestFocus();
        }
        if (TextUtils.isEmpty(Institute)) {
            Institute=UP_Institute.getHint().toString();
        }
        if (TextUtils.isEmpty(Username)) {
            Username=UP_UserName.getHint().toString();
        }
        ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(email, Name, Phone, Institute, Username, finalRole);
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        assert firebaseUser != null;
        referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(UpdateProfile.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(UpdateProfile.this, Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
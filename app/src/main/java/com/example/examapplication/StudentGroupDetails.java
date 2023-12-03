package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

public class StudentGroupDetails extends AppCompatActivity {
    TextView SGDTSG,SGD_GN,SGD_SN,SGD_SC,SGD_CB,SGD_GD;

    ProgressBar SGD_progressBar;
    String GN,SN,SC,CB,GD;
    FirebaseAuth authProfile;

    String Group_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_group_details);
        SGD_GN = findViewById(R.id.SGD_GN);
        SGD_SN = findViewById(R.id.SGD_SN);
        SGD_SC = findViewById(R.id.SGD_SC);
        SGD_CB = findViewById(R.id.SGD_CB);
        SGD_GD = findViewById(R.id.SGD_GD);
        SGD_progressBar = findViewById(R.id.SGD_progressBar);

        Intent intent = getIntent();
        Group_ID = intent.getStringExtra("GROUP_ID");

        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        SGD_progressBar.setVisibility(View.VISIBLE);
        showGroupDetails(firebaseUser);

        SGDTSG=findViewById(R.id.SGDTSG);
        SGDTSG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public void showGroupDetails(FirebaseUser firebaseUser){

        //Extracting User Reference from Database for "Registered Users"
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).child("Groups").child(Group_ID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                Group readUserDetails=snapshot.getValue(Group.class);
                if(readUserDetails != null){
                    GN=readUserDetails.Group_Name;
                    SN=readUserDetails.Subject_Name;
                    SC=readUserDetails.Subject_Code;
                    CB=readUserDetails.TeacherName;
                    GD=readUserDetails.Description;
                    SGD_GN.setText(GN);
                    SGD_SN.setText(SN);
                    SGD_SC.setText(SC);
                    SGD_CB.setText(CB);
                    SGD_GD.setText(GD);
                }else {
                    Toast.makeText(StudentGroupDetails.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
                SGD_progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(StudentGroupDetails.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
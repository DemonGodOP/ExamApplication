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

public class GroupDetailsActivity extends AppCompatActivity {
    TextView GD_GN,GD_SN,GD_SC,GD_GID,GDGD,GDTIG;

    ProgressBar GD_progressBar;
    String GN,SN,SC,GID,GD;
    FirebaseAuth authProfile;

    String Group_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);
        GD_GN = findViewById(R.id.GD_GN);
        GD_SN = findViewById(R.id.GD_SN);
        GD_SC = findViewById(R.id.GD_SC);
        GD_GID = findViewById(R.id.GD_GID);
        GDGD = findViewById(R.id.GDGD);
        GD_progressBar = findViewById(R.id.GD_progressBar);

        Intent intent = getIntent();
        Group_ID = intent.getStringExtra("GROUP_ID");

        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        GD_progressBar.setVisibility(View.VISIBLE);
        showGroupDetails(firebaseUser);

        GDTIG=findViewById(R.id.GDTIG);
        GDTIG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(GroupDetailsActivity.this, InsideGroup.class);

                // Pass the unique key to the new activity
                intent.putExtra("GROUP_ID", Group_ID);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
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
                    GID=readUserDetails.Group_ID;
                    GD=readUserDetails.Description;
                    GD_GN.setText(GN);
                    GD_SN.setText(SN);
                    GD_SC.setText(SC);
                    GD_GID.setText(GID);
                    GDGD.setText(GD);
                }else {
                    Toast.makeText(GroupDetailsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
                GD_progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error){
            }
        });
    }
}
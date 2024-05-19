package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InsideGroup extends AppCompatActivity {
    TextView IGTTH, IGTGD, IGTCNA, IGTA, IGTGM, IGTJR, IG_delete;
    String receivedGroupId;

    FirebaseAuth authProfile;

    FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inside_group);

        // Get the intent that started this activity
        Intent intent = getIntent();

        // Retrieve the data from the intent using the key
        receivedGroupId = intent.getStringExtra("GROUP_ID");

        IGTTH=findViewById(R.id.IGTTH);
        IGTGD=findViewById(R.id.IGTGD);
        IGTCNA=findViewById(R.id.IGTCNA);
        IGTA=findViewById(R.id.IGTA);
        IGTGM=findViewById(R.id.IGTGM);
        IGTJR=findViewById(R.id.IGTJR);
        IG_delete=findViewById(R.id.IG_delete);
        authProfile = FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        IGTTH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(InsideGroup.this, TeacherHomePage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl","Teacher");
                startActivity(intent);
                finish();
            }
        });

        IG_delete.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                 showAlertDialog();
            }
        });

        IGTGD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(InsideGroup.this, GroupDetailsActivity.class);

                // Pass the unique key to the new activity
                intent.putExtra("GROUP_ID", receivedGroupId);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                // Start the new activity
                startActivity(intent);
                finish();
            }
        });

        IGTCNA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InsideGroup.this, NewAssignment.class);

                intent.putExtra("GROUP_ID", receivedGroupId);

                // Start the new activity
                startActivity(intent);
            }
        });

        IGTJR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InsideGroup.this, GroupJoiningRequest.class);

                intent.putExtra("GROUP_ID", receivedGroupId);

                // Start the new activity
                startActivity(intent);
            }
        });

        IGTGM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InsideGroup.this, CurrentParticipants.class);

                intent.putExtra("GROUP_ID", receivedGroupId);

                // Start the new activity
                startActivity(intent);
            }
        });

        IGTA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InsideGroup.this, AvailableAssignments.class);

                intent.putExtra("GROUP_ID", receivedGroupId);

                // Start the new activity
                startActivity(intent);
            }
        });

    }

    private void showAlertDialog () {

        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(InsideGroup.this);
        builder.setTitle("Delete User and Related Data?");
        builder.setMessage("Do you really want to delete your profile and related data? This action is irreversible.");


        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteGroup();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog=builder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red));
            }
        });

        //show the alert dialog
        alertDialog.show();
    }

    public void deleteGroup(){
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("Groups").child(receivedGroupId);

        groupRef.child("Current Participants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ParticipantDetails participantDetails = snapshot.getValue(ParticipantDetails.class);
                    if(participantDetails!=null){
                        DatabaseReference deleteGroup=FirebaseDatabase.getInstance().getReference("Registered Users").child(participantDetails.UserID).child("Groups").child(receivedGroupId);
                        deleteGroup.removeValue();
                    }
                }
                groupRef.removeValue().addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(InsideGroup.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                });

                assert firebaseUser != null;
                DatabaseReference TgroupRef=FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("Groups").child(receivedGroupId);
                TgroupRef.removeValue().addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(InsideGroup.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                });

                Intent intent=new Intent(InsideGroup.this,TeacherHomePage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(InsideGroup.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
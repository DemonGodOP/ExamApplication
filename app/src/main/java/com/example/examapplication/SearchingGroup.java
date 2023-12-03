package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class SearchingGroup extends AppCompatActivity {
    TextView SRGTSHM,SRG_GroupName,SRG_GroupSubjectCode,SRG_NoText;
    Button SRG_Layout;
    String Group_ID,Username,Email;

    FirebaseAuth authProfile;

    FirebaseUser firebaseUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching_group);
        Intent intent=getIntent();
        Group_ID=intent.getStringExtra("GROUP_ID");

        SRGTSHM=findViewById(R.id.SRGTSHM);
        SRG_GroupName=findViewById(R.id.SRG_GroupName);
        SRG_GroupSubjectCode=findViewById(R.id.SRG_GroupSubjectCode);
        SRG_NoText=findViewById(R.id.SRG_NoText);
        SRG_Layout=findViewById(R.id.SRG_Layout);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        SRGTSHM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");


        groupsRef.child(Group_ID).addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    DatabaseReference Ref = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID);
                    Ref.child("Group Details").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot Snapshot) {
                                Group foundGroup = Snapshot.getValue(Group.class);
                                if (foundGroup != null) {
                                    SRG_NoText.setVisibility(View.GONE);
                                    SRG_GroupName.setVisibility(View.VISIBLE);
                                    SRG_GroupSubjectCode.setVisibility(View.VISIBLE);
                                    SRG_Layout.setVisibility(View.VISIBLE);
                                    String Group_Name = foundGroup.Group_Name;
                                    String Group_SubjectCode = foundGroup.Subject_Code;
                                    SRG_GroupName.setText(Group_Name);
                                    SRG_GroupSubjectCode.setText(Group_SubjectCode);
                                }
                            else {
                                Toast.makeText(SearchingGroup.this, "No Data", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(SearchingGroup.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    SRG_NoText.setVisibility(View.VISIBLE);
                    SRG_GroupName.setVisibility(View.GONE);
                    SRG_GroupSubjectCode.setVisibility(View.GONE);
                    SRG_Layout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SearchingGroup.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

        SRG_Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Current Participants").child(firebaseUser.getUid());
                database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){

                            ParticipantDetails participantDetails = snapshot.getValue(ParticipantDetails.class);
                            if (participantDetails != null) {
                                Intent intent = new Intent(SearchingGroup.this, StudentGroup.class);

                                // Pass the unique key to the new activity
                                intent.putExtra("GROUP_ID", Group_ID);

                                // Start the new activity
                                startActivity(intent);
                            } }else {
                                DatabaseReference database2 = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Joining Request").child(firebaseUser.getUid());
                                database2.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            ParticipantDetails participantDetails = snapshot.getValue(ParticipantDetails.class);
                                            if (participantDetails != null) {
                                                Toast.makeText(SearchingGroup.this, "Joining Request Not Accepted Yet", Toast.LENGTH_SHORT).show();
                                            } }else {
                                                showAlertDialog();
                                            }
                                    }
                                    @Override
                                    public void onCancelled (@NonNull DatabaseError error){
                                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();

                                    }
                                });
                            }

                    }
                    @Override
                    public void onCancelled (@NonNull DatabaseError error){
                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                        //UP_progressBar.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(SearchingGroup.this);
        builder.setTitle("Send Joining Request");
        builder.setMessage("Do you want to Send a Joining Request?");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                        if (readUserDetails != null) {
                            Username = readUserDetails.userName;
                            Email = readUserDetails.email;
                            ParticipantDetails participantDetails=new ParticipantDetails(Username,Email,firebaseUser.getUid());
                            DatabaseReference joiningRequest= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Joining Request");
                            joiningRequest.child(firebaseUser.getUid()).setValue(participantDetails);
                        } else {
                            Toast.makeText(SearchingGroup.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onCancelled (@NonNull DatabaseError error){
                        Toast.makeText(SearchingGroup.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //create the AlertDialog
        AlertDialog alertDialog=builder.create();

        //show the alert dialog
        alertDialog.show();
    }
}
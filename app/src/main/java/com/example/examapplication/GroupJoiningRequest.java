package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupJoiningRequest extends AppCompatActivity {
    ListView GJR_LV;

    TextView GJRTIG,GJR_Text;

    String Group_ID;

    ProgressBar GJRP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_joining_request);

        Intent intent=getIntent();

        Group_ID=intent.getStringExtra("GROUP_ID");

        GJRTIG=findViewById(R.id.GJRTIG);

        GJR_LV=findViewById(R.id.GJR_LV);

        GJR_Text=findViewById(R.id.GJR_Text);

        GJRP=findViewById(R.id.GJRP);

        GJRTIG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        GJRP.setVisibility(View.VISIBLE);
        PopulateList();


    }

    public void PopulateList(){
        DatabaseReference JoiningReference = FirebaseDatabase.getInstance()
                .getReference("Groups").child(Group_ID);


        // Listener to retrieve the groups
        JoiningReference.child("Joining Request").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ParticipantDetails> ParticipantList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ParticipantDetails Participants = snapshot.getValue(ParticipantDetails.class);
                    if (Participants != null) {
                        ParticipantList.add(Participants);
                    }
                }

                if (ParticipantList.isEmpty()) {
                    // If there are no groups, display the message TextView and hide the ListView
                    GJR_Text.setVisibility(View.VISIBLE);
                    GJRP.setVisibility(View.GONE);
                    GJR_LV.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    GJR_Text.setVisibility(View.GONE);
                    GJR_LV.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    GJRLV adapter = new GJRLV(GroupJoiningRequest.this, R.layout.gjrlv, ParticipantList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    GJR_LV.setAdapter(adapter);

                    GJRP.setVisibility(View.GONE);

                    GJR_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                            ParticipantDetails selectedParticipant = (ParticipantDetails) parent.getItemAtPosition(position);
                            showAlertDialog(JoiningReference,selectedParticipant);
                        }
                    });
                }
            }
            @Override
            public void onCancelled (@NonNull DatabaseError databaseError){

            }
        });
    }

    private void showAlertDialog (DatabaseReference JoiningReference,ParticipantDetails selectedParticipant) {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(GroupJoiningRequest.this);
        builder.setTitle("Add Participant");
        builder.setMessage("Do You want to Accept the given user into the group?");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                  JoiningReference.child("Joining Request").child(selectedParticipant.UserID).removeValue().addOnFailureListener(new OnFailureListener() {
                      @Override
                      public void onFailure(@NonNull Exception e) {
                          Toast.makeText(GroupJoiningRequest.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                      }
                  });
                  JoiningReference.child("Current Participants").child(selectedParticipant.UserID).setValue(selectedParticipant).addOnFailureListener(new OnFailureListener() {
                      @Override
                      public void onFailure(@NonNull Exception e) {
                          Toast.makeText(GroupJoiningRequest.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                      }
                  });

                  DatabaseReference GroupDetails=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Group Details");
                  GroupDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                      @Override
                      public void onDataChange(@NonNull DataSnapshot snapshot) {
                          if (snapshot.exists()) {
                              Group group = snapshot.getValue(Group.class);
                              if (group != null) {
                                  DatabaseReference newGroup = FirebaseDatabase.getInstance().getReference("Registered Users").child(selectedParticipant.UserID).child("Groups").child(Group_ID);
                                  newGroup.setValue(group);
                              } else {
                                  Toast.makeText(GroupJoiningRequest.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                              }
                          }
                      }
                      @Override
                      public void onCancelled (@NonNull DatabaseError error){

                      }
                  });
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                JoiningReference.child("Joining Request").child(selectedParticipant.UserID).removeValue().addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GroupJoiningRequest.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                });
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });

        //create the AlertDialog
        AlertDialog alertDialog=builder.create();

        //show the alert dialog
        alertDialog.show();
    }

}
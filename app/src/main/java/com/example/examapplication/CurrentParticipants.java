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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CurrentParticipants extends AppCompatActivity {
    ListView CP_LV;

    TextView CPTIG,CPA_Text;

    String Group_ID;

    ProgressBar CP_P;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_participants);

        Intent intent=getIntent();

        Group_ID=intent.getStringExtra("GROUP_ID");

        CPA_Text=findViewById(R.id.CPA_Text);

        CPTIG=findViewById(R.id.CPTIG);

        CP_P=findViewById(R.id.CP_P);

        CP_LV=findViewById(R.id.CP_LV);

        CPTIG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        CP_P.setVisibility(View.VISIBLE);
        PopulateList();

    }

    public void PopulateList(){
        DatabaseReference JoiningReference = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID);


        // Listener to retrieve the groups
        JoiningReference.child("Current Participants").addValueEventListener(new ValueEventListener() {
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
                    CPA_Text.setVisibility(View.VISIBLE);
                    CP_LV.setVisibility(View.GONE);
                    CP_P.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    CPA_Text.setVisibility(View.GONE);
                    CP_LV.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    GJRLV adapter = new GJRLV(CurrentParticipants.this, R.layout.gjrlv, ParticipantList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    CP_LV.setAdapter(adapter);

                    CP_P.setVisibility(View.GONE);

                    CP_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        AlertDialog.Builder builder=new AlertDialog.Builder(CurrentParticipants.this);
        builder.setTitle("Remove Participant");
        builder.setMessage("Do You want to Remove this Participant from the Group?");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                JoiningReference.child("Current Participants").child(selectedParticipant.UserID).removeValue().addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CurrentParticipants.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                });
                DatabaseReference newGroup = FirebaseDatabase.getInstance().getReference("Registered Users").child(selectedParticipant.UserID).child("Groups").child(Group_ID);
                newGroup.removeValue();
                Intent intent = getIntent();
                finish();
                startActivity(intent);

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //create the AlertDialog
        AlertDialog alertDialog=builder.create();

        //show the alert dialog
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red));
            }
        });

        alertDialog.show();
    }
}
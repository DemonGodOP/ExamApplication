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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssignmentDetails extends AppCompatActivity {
    TextView ASTIG,AD_Edit,AD_CA,AD_Details,AD_Text,AD_NoText;

    ListView AD_LV;

    String Group_ID,AssignmentID,Name,Timing,Duration;

    boolean Active;

    List<String> Questions;

    ProgressBar AD_P;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_details);

        Intent intent=getIntent();
        Group_ID=intent.getStringExtra("GROUP_ID");

        AssignmentID=intent.getStringExtra("Assignment_ID");

        ASTIG=findViewById(R.id.ADTAA);
        AD_Edit=findViewById(R.id.AD_Details);
        AD_CA=findViewById(R.id.AD_CA);
        AD_P=findViewById(R.id.AD_P);

        AD_LV=findViewById(R.id.AD_LV);

        AD_Details=findViewById(R.id.AD_Details);

        AD_Text=findViewById(R.id.AD_Text);
        AD_NoText=findViewById(R.id.AD_NoText);

        ASTIG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        AD_CA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        AD_Details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AssignmentDetails.this, AssignmentInfo.class);

                intent.putExtra("GROUP_ID", Group_ID);

                intent.putExtra("Assignment_ID",AssignmentID);

                // Start the new activity
                startActivity(intent);
            }
        });

        AD_P.setVisibility(View.VISIBLE);
        PopulateList();
    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(AssignmentDetails.this);
        builder.setTitle("Change Assignment State");
        builder.setMessage("Do you want to Change the State of the Assignment?");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(AssignmentID);
                database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Assignment assignment = snapshot.getValue(Assignment.class);
                        if (assignment != null) {
                            Questions=assignment.Questions;
                            Name=assignment.Name;
                            Timing=assignment.Timing;
                            Duration=assignment.Duration;
                            Active=assignment.Active;
                            Assignment temp=new Assignment(Questions,!Active,Name,Timing,AssignmentID,Duration);
                            database.setValue(temp).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    dialogInterface.dismiss();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(AssignmentDetails.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(AssignmentDetails.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                        }
                        //UP_progressBar.setVisibility(View.GONE);
                    }
                    @Override
                    public void onCancelled (@NonNull DatabaseError error){
                        //UP_progressBar.setVisibility(View.GONE);
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

    public void PopulateList() {
        DatabaseReference JoiningReference = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(AssignmentID);


        // Listener to retrieve the groups
        JoiningReference.child("Submissions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<SubmissionDetails> SubmissionList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    SubmissionDetails submission = snapshot.getValue(SubmissionDetails.class);
                    if (submission != null) {
                        SubmissionList.add(submission);
                    }
                }

                if (SubmissionList.isEmpty()) {
                    // If there are no groups, display the message TextView and hide the ListView
                    AD_NoText.setVisibility(View.VISIBLE);
                    AD_LV.setVisibility(View.GONE);
                    AD_P.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    AD_NoText.setVisibility(View.GONE);
                    AD_LV.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    SDLV adapter = new SDLV(AssignmentDetails.this, R.layout.sdlv, SubmissionList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    AD_LV.setAdapter(adapter);

                    AD_P.setVisibility(View.GONE);

                    AD_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                            SubmissionDetails selectedSubmission = (SubmissionDetails) parent.getItemAtPosition(position);
                            Intent intent = new Intent(AssignmentDetails.this, SubmissionInfo.class);

                            intent.putExtra("GROUP_ID", Group_ID);

                            intent.putExtra("Assignment_ID",AssignmentID);

                            intent.putExtra("Submission_ID",selectedSubmission.UserID);

                            // Start the new activity
                            startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
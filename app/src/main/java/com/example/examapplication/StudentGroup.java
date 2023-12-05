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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class StudentGroup extends AppCompatActivity {
    TextView SGTSHP,SG_GroupDetails,SG_Text,SG_NoText;
    ListView SG_LV;
    ProgressBar SG_P;
    String GROUP_ID;

    FirebaseAuth authProfile;

    FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_group);
        Intent intent=getIntent();
        GROUP_ID=intent.getStringExtra("GROUP_ID");
        SGTSHP=findViewById(R.id.SGTSHP);
        SG_GroupDetails=findViewById(R.id.SG_GroupDetails);
        SG_Text=findViewById(R.id.SG_Text);
        SG_NoText=findViewById(R.id.SG_NoText);
        SG_LV=findViewById(R.id.SG_LV);
        SG_P=findViewById(R.id.SG_P);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        SGTSHP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        SG_GroupDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StudentGroup.this, StudentGroupDetails.class);

                // Pass the unique key to the new activity
                intent.putExtra("GROUP_ID", GROUP_ID);

                // Start the new activity
                startActivity(intent);
            }
        });

        SG_P.setVisibility(View.VISIBLE);
        PopulateList();
    }

    public void PopulateList() {
        DatabaseReference JoiningReference = FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID);


        // Listener to retrieve the groups
        JoiningReference.child("Assignments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Assignment> AssignmentList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Assignment assignment = snapshot.getValue(Assignment.class);
                    if (assignment != null) {
                        AssignmentList.add(assignment);
                    }
                }

                if (AssignmentList.isEmpty()) {
                    // If there are no groups, display the message TextView and hide the ListView
                    SG_NoText.setVisibility(View.VISIBLE);
                    SG_LV.setVisibility(View.GONE);
                    SG_P.setVisibility(View.GONE);
                    SG_Text.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    SG_NoText.setVisibility(View.GONE);
                    SG_LV.setVisibility(View.VISIBLE);
                    SG_Text.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    ADLV adapter = new ADLV(StudentGroup.this, R.layout.adlv, AssignmentList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    SG_LV.setAdapter(adapter);

                    SG_P.setVisibility(View.GONE);

                    SG_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                            Assignment selectedAssignment = (Assignment) parent.getItemAtPosition(position);

                            DatabaseReference Checking=FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(selectedAssignment.Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                            Checking.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.exists()){
                                        Intent intent = new Intent(StudentGroup.this, StudentFeedBack.class);

                                        //intent.putExtra("GROUP_ID", GROUP_ID);
                                        intent.putExtra("Assignment_ID",selectedAssignment.Assignment_ID);

                                        intent.putExtra("Group_ID",GROUP_ID);

                                        // Start the new activity
                                        startActivity(intent);
                                    }
                                    else{
                                        showAlertDialog(selectedAssignment);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(StudentGroup.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(StudentGroup.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlertDialog (Assignment selectedAssignment) {

        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(StudentGroup.this);
        builder.setTitle("Start Exam?");
        builder.setMessage("Do you want to Start the Exam? Once Started You Can't Exit Before Submitting.");


        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference Active=FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(selectedAssignment.Assignment_ID);
                Active.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Assignment assignment=snapshot.getValue(Assignment.class);
                        if(assignment!=null){
                            boolean Active=assignment.Active;
                            if(Active==true){
                                Intent intent = new Intent(StudentGroup.this, AssignmentSubmission.class);

                                //intent.putExtra("GROUP_ID", GROUP_ID);
                                intent.putExtra("Assignment_ID",selectedAssignment.Assignment_ID);

                                intent.putExtra("Group_ID",GROUP_ID);

                                // Start the new activity
                                startActivity(intent);

                                finish();
                            }
                            else{
                                Toast.makeText(StudentGroup.this, "Assignment is Not Active Right Now", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
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
}
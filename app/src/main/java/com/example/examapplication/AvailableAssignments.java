package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AvailableAssignments extends AppCompatActivity {
    TextView AATIG,AA_Text;
    ListView AA_LV;
    ProgressBar AA_progressBar;
    String GROUP_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_assignments);
        Intent intent=getIntent();
        GROUP_ID=intent.getStringExtra("GROUP_ID");
        AATIG=findViewById(R.id.AATIG);
        AA_LV=findViewById(R.id.AA_LV);
        AA_progressBar=findViewById(R.id.AA_progressBar);
        AA_Text=findViewById(R.id.AA_Text);

        AATIG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        AA_progressBar.setVisibility(View.VISIBLE);
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
                    AA_Text.setVisibility(View.VISIBLE);
                    AA_LV.setVisibility(View.GONE);
                    AA_progressBar.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    AA_Text.setVisibility(View.GONE);
                    AA_LV.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    ADLV adapter = new ADLV(AvailableAssignments.this, R.layout.adlv, AssignmentList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    AA_LV.setAdapter(adapter);

                    AA_progressBar.setVisibility(View.GONE);

                    AA_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                             Assignment selectedAssignment = (Assignment) parent.getItemAtPosition(position);
                             Intent intent = new Intent(AvailableAssignments.this, AssignmentDetails.class);

                            intent.putExtra("GROUP_ID", GROUP_ID);
                            intent.putExtra("Assignment_ID",selectedAssignment.Assignment_ID);

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
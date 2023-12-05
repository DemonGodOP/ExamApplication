package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AssignmentInfo extends AppCompatActivity {
    TextView AITAD,AI_NameText,AI_Name,AI_TimingText,AI_Timing,AI_QN,AI_Q;
    Button AI_QB,AI_P,AI_N;

    String Name,Timing;

    int n=0;

    List<String> Questions;

    String GROUP_ID,Assignment_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_info);
        AITAD=findViewById(R.id.AITAD);
        AI_NameText=findViewById(R.id.AI_NameText);
        AI_Name=findViewById(R.id.AI_Name);
        AI_TimingText=findViewById(R.id.AI_TimingText);
        AI_Timing=findViewById(R.id.AI_Timing);
        AI_QN=findViewById(R.id.AI_QN);
        AI_Q=findViewById(R.id.AI_Q);
        AI_QB=findViewById(R.id.AI_QB);
        AI_P=findViewById(R.id.AI_P);
        AI_N=findViewById(R.id.AI_N);

        AITAD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        AI_QN.setVisibility(View.GONE);
        AI_Q.setVisibility(View.GONE);
        AI_P.setVisibility(View.GONE);
        AI_N.setVisibility(View.GONE);



        AI_P.setEnabled(false);
        Intent intent=getIntent();

        GROUP_ID=intent.getStringExtra("GROUP_ID");
        Assignment_ID=intent.getStringExtra("Assignment_ID");

        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(Assignment_ID);

        referenceProfile.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Assignment assignment = dataSnapshot.getValue(Assignment.class);
                    if (assignment != null) {
                        Name = assignment.Name;
                        Timing= assignment.Timing;
                        AI_Name.setText(Name);
                        AI_Timing.setText(Timing);
                        Questions=assignment.Questions;
                        AI_QN.setText("QN: "+(n+1));
                        AI_Q.setText(Questions.get(0));
                        if(n==Questions.size()-1){
                            AI_N.setEnabled(false);
                        }



                        AI_P.setEnabled(false);
                    } else {
                        Toast.makeText(AssignmentInfo.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                    }
                }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AssignmentInfo.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
            }
        });



        AI_N.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n++;
                AI_QN.setText("QN: "+(n+1));
                AI_Q.setText(Questions.get(n));
                if(n==Questions.size()-1){
                    AI_N.setEnabled(false);
                }
                AI_P.setEnabled(true);
            }
        });

        AI_P.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n--;
                AI_QN.setText("QN: "+(n+1));
                AI_Q.setText(Questions.get(n));
                if(n==0){
                    AI_P.setEnabled(false);
                }
                AI_N.setEnabled(true);
            }
        });

        AI_QB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(AI_QN.getVisibility()==View.GONE){
                    AI_QN.setVisibility(View.VISIBLE);
                    AI_Q.setVisibility(View.VISIBLE);
                    AI_P.setVisibility(View.VISIBLE);
                    AI_N.setVisibility(View.VISIBLE);

                    AI_Name.setVisibility(View.GONE);
                    AI_Timing.setVisibility(View.GONE);
                    AI_NameText.setVisibility(View.GONE);
                    AI_TimingText.setVisibility(View.GONE);

                    AI_QB.setText("Other Details");
                }
                else{
                    AI_QN.setVisibility(View.GONE);
                    AI_Q.setVisibility(View.GONE);
                    AI_P.setVisibility(View.GONE);
                    AI_N.setVisibility(View.GONE);

                    AI_Name.setVisibility(View.VISIBLE);
                    AI_Timing.setVisibility(View.VISIBLE);
                    AI_TimingText.setVisibility(View.VISIBLE);
                    AI_NameText.setVisibility(View.VISIBLE);

                    AI_QB.setText("Questions");
                }
            }
        });


    }
}
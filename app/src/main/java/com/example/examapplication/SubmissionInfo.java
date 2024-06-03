package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class SubmissionInfo extends AppCompatActivity {
    TextView SITAD,SI_QN,SI_Q,SI_FeedBackText,SI_A,SI_AnswerText;
    Button SI_P,SI_N,SI_FeedBack,SI_Submit;

    String GROUP_ID,Assignment_ID,Submission_ID;

    List<String> Questions;
    List<String>Answers;

    int n=0;

    EditText SI_F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_info);
        SITAD=findViewById(R.id.SITAD);
        SI_QN=findViewById(R.id.SI_QN);
        SI_Q=findViewById(R.id.SI_Q);
        SI_F=findViewById(R.id.SI_F);
        SI_FeedBackText=findViewById(R.id.SI_FeedBackText);
        SI_A=findViewById(R.id.SI_A);
        SI_AnswerText=findViewById(R.id.SI_AnswerText);



        SI_P=findViewById(R.id.SI_P);
        SI_N=findViewById(R.id.SI_N);
        SI_FeedBack=findViewById(R.id.SI_FeedBack);
        SI_Submit=findViewById(R.id.SI_Submit);

        SI_Submit.setVisibility(View.GONE);
        SI_F.setVisibility(View.GONE);
        SI_FeedBackText.setVisibility(View.GONE);



        Intent intent=getIntent();

        GROUP_ID=intent.getStringExtra("GROUP_ID");
        Assignment_ID=intent.getStringExtra("Assignment_ID");
        Submission_ID=intent.getStringExtra("Submission_ID");

        SI_P.setEnabled(false);

        DatabaseReference referenceQuestions = FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(Assignment_ID);

        referenceQuestions.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Assignment assignment = dataSnapshot.getValue(Assignment.class);
                if (assignment != null) {
                    Questions=assignment.Questions;
                    SI_QN.setText("QN: "+(n+1));
                    SI_Q.setText(Questions.get(0));
                    if(n==Questions.size()-1){
                        SI_N.setEnabled(false);
                    }
                } else {
                    Toast.makeText(SubmissionInfo.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference referenceAnswers = FirebaseDatabase.getInstance().getReference("Groups").child(GROUP_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(Submission_ID);

        referenceAnswers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                SubmissionDetails submissionDetails = dataSnapshot.getValue(SubmissionDetails.class);
                if (submissionDetails != null) {
                    Answers=submissionDetails.Answers;
                    SI_A.setText(Answers.get(0));
                } else {
                    Toast.makeText(SubmissionInfo.this, "Something went wrong!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        SI_N.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n++;
                SI_QN.setText("QN: "+(n+1));
                SI_Q.setText(Questions.get(n));
                SI_A.setText(Answers.get(n));
                if(n==Questions.size()-1){
                    SI_N.setEnabled(false);
                }
                SI_P.setEnabled(true);
            }
        });

        SI_P.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n--;
                SI_QN.setText("QN: "+(n+1));
                SI_Q.setText(Questions.get(n));
                SI_A.setText(Answers.get(n));
                if(n==0){
                    SI_P.setEnabled(false);
                }
                SI_N.setEnabled(true);
            }
        });

        SI_FeedBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SI_QN.setVisibility(View.GONE);
                SI_Q.setVisibility(View.GONE);
                SI_P.setVisibility(View.GONE);
                SI_N.setVisibility(View.GONE);
                SI_A.setVisibility(View.GONE);
                SI_AnswerText.setVisibility(View.GONE);
                SI_FeedBack.setVisibility(View.GONE);

                SI_F.setVisibility(View.VISIBLE);
                SI_Submit.setVisibility(View.VISIBLE);
                SI_FeedBackText.setVisibility(View.VISIBLE);

                referenceAnswers.child("FeedBack").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        FeedBackDetails feedBackDetails = dataSnapshot.getValue(FeedBackDetails.class);
                        if (feedBackDetails != null) {
                            String feedback=feedBackDetails.FeedBack;
                            SI_F.setText(feedback);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        SI_Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String FB=SI_F.getText().toString();
                if (TextUtils.isEmpty(FB)) {
                    SI_F.setError("Please Provide a FeedBack Before Continuing");
                    SI_F.requestFocus();
                }
                else{
                    FeedBackDetails feedBackDetails=new FeedBackDetails(FB);
                    referenceAnswers.child("FeedBack").setValue(feedBackDetails);
                    finish();
                }
            }
        });

        SITAD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });



    }
}
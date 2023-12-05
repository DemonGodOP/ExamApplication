package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class StudentFeedBack extends AppCompatActivity {
   TextView SFTSG,SF_QN,SN_Q,SF_FText,SN_F,SF_AnswerText,SN_A;
   Button SF_Prev,SF_Next,SF_FB;

   String Group_ID,Assignment_ID;

   List<String>Questions;
   List<String> Answers;

   FirebaseAuth authProfile;
   FirebaseUser firebaseUser;

   int n=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_feed_back);

        Intent intent=getIntent();
        Group_ID=intent.getStringExtra("Group_ID");
        Assignment_ID=intent.getStringExtra("Assignment_ID");

        SFTSG=findViewById(R.id.SFTSG);
        SF_QN=findViewById(R.id.SF_QN);
        SN_Q=findViewById(R.id.SN_Q);
        SF_FText=findViewById(R.id.SF_FText);
        SN_F=findViewById(R.id.SN_F);
        SF_AnswerText=findViewById(R.id.SF_AnswerText);
        SN_A=findViewById(R.id.SN_A);
        SF_Prev=findViewById(R.id.SF_Prev);
        SF_Next=findViewById(R.id.SF_Next);
        SF_FB=findViewById(R.id.SF_FB);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        SFTSG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        SF_FText.setVisibility(View.GONE);
        SN_F.setVisibility(View.GONE);

        DatabaseReference getQuestions= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID);
        getQuestions.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Assignment assignment=snapshot.getValue(Assignment.class);
                if(assignment!=null){
                    Questions=assignment.Questions;
                    SN_Q.setText(Questions.get(0));
                    SF_QN.setText("QN: "+(n+1));
                    if(n==Questions.size()-1){
                        SF_Next.setEnabled(false);
                    }
                }
                else{
                    Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

        DatabaseReference getAnswers= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
        getAnswers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SubmissionDetails submissionDetails=snapshot.getValue(SubmissionDetails.class);
                if(submissionDetails!=null){
                    Answers=submissionDetails.Answers;
                    SN_A.setText(Answers.get(0));
                }
                else{
                    Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentFeedBack.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

        SF_Prev.setEnabled(false);



        SF_Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n++;
                SF_QN.setText("QN: "+(n+1));
                SN_Q.setText(Questions.get(n));
                SN_A.setText(Answers.get(n));
                if(n==Questions.size()-1){
                    SF_Next.setEnabled(false);
                }
                SF_Prev.setEnabled(true);
            }
        });

        SF_Prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n--;
                SF_QN.setText("QN: "+(n+1));
                SN_Q.setText(Questions.get(n));
                SN_A.setText(Answers.get(n));
                if(n==0){
                    SF_Prev.setEnabled(false);
                }
                SF_Next.setEnabled(true);
            }
        });

        SF_FB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(SF_QN.getVisibility()==View.VISIBLE) {
                    SF_QN.setVisibility(View.GONE);
                    SN_Q.setVisibility(View.GONE);
                    SF_Prev.setVisibility(View.GONE);
                    SF_Next.setVisibility(View.GONE);
                    SN_A.setVisibility(View.GONE);
                    SF_AnswerText.setVisibility(View.GONE);

                    SN_F.setVisibility(View.VISIBLE);
                    SF_FText.setVisibility(View.VISIBLE);

                    SF_FB.setText("Answers");
                }
                else{
                    SF_QN.setVisibility(View.VISIBLE);
                    SN_Q.setVisibility(View.VISIBLE);
                    SF_Prev.setVisibility(View.VISIBLE);
                    SF_Next.setVisibility(View.VISIBLE);
                    SN_A.setVisibility(View.VISIBLE);
                    SF_AnswerText.setVisibility(View.VISIBLE);

                    SN_F.setVisibility(View.GONE);
                    SF_FText.setVisibility(View.GONE);

                    SF_FB.setText("FeedBack");
                }

                getAnswers.child("FeedBack").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        FeedBackDetails feedBackDetails = dataSnapshot.getValue(FeedBackDetails.class);
                        if (feedBackDetails != null) {
                            String feedback=feedBackDetails.FeedBack;
                            SN_F.setText(feedback);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(StudentFeedBack.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }
}
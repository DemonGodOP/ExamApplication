package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssignmentSubmission extends AppCompatActivity {
    TextView AS_QN,AS_Q,AS_A,AS_Time;

    Button AS_Prev,AS_Next,AS_S;

    String Group_ID,Assignment_ID;

    List<String> Answers;

    List<String>Questions;

    FirebaseAuth authProfile;

    FirebaseUser firebaseUser;

    int n=0;

    CountDownTimer countDownTimer;
    long timeLeftInMillis = 600000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_submission);

        Intent intent=getIntent();

        Group_ID=intent.getStringExtra("Group_ID");
        Assignment_ID=intent.getStringExtra("Assignment_ID");

        AS_QN=findViewById(R.id.AS_QN);
        AS_Q=findViewById(R.id.AS_Q);
        AS_A=findViewById(R.id.AS_A);
        AS_Prev=findViewById(R.id.AS_Prev);
        AS_Next=findViewById(R.id.AS_Next);
        AS_S=findViewById(R.id.AS_S);
        AS_Time=findViewById(R.id.AS_Time);

        AS_QN.setText(n+1+"");

        Answers=new ArrayList<>();

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        DatabaseReference database= FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID);

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Assignment assignment=snapshot.getValue(Assignment.class);
                if(assignment!=null){
                    Questions=assignment.Questions;
                    AS_Q.setText(Questions.get(0));
                    if(n==Questions.size()-1){
                        AS_Next.setEnabled(false);
                    }
                }
                else{
                    Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });

        AS_Prev.setEnabled(false);



        AS_Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Temp=AS_A.getText().toString();
                if(TextUtils.isEmpty(Temp)){
                    Temp="";
                }
                if(Answers.isEmpty()||n==Answers.size()-1){
                    Answers.add(Temp);
                }
                else{
                    Answers.set(n,Temp);
                }
                n++;
                AS_QN.setText(n+1+"");
                AS_Q.setText(Questions.get(n));
                if(n==Questions.size()-1){
                    AS_Next.setEnabled(false);
                }
                AS_Prev.setEnabled(true);
                if(n!=Answers.size()){
                    AS_A.setText(Answers.get(n));
                }
                AS_A.setText("");
            }
        });

        AS_Prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Temp=AS_A.getText().toString();
                if(TextUtils.isEmpty(Temp)){
                    Temp="";
                }
                if(Answers.isEmpty()||n==Answers.size()){
                    Answers.add(Temp);
                }
                else{
                    Answers.set(n,Temp);
                }
                n--;
                AS_QN.setText(n+1+"");
                AS_Q.setText(Questions.get(n));
                if(n==0){
                    AS_Prev.setEnabled(false);
                }
                AS_Next.setEnabled(true);
                AS_A.setText(Answers.get(n));
            }
        });

        AS_S.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

                userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                        if(readWriteUserDetails!=null){
                            String Temp=AS_A.getText().toString();
                            if(TextUtils.isEmpty(Temp)){
                                Temp="";
                            }
                            if(Answers.isEmpty()||n==Answers.size()){
                                Answers.add(Temp);
                            }
                            else {
                                Answers.set(n,Temp);
                            }
                            if(Answers.size()<Questions.size()){
                                while(Answers.size()<Questions.size()){
                                    Answers.add("");
                                }
                            }
                            String UserName=readWriteUserDetails.userName;
                            String Email=readWriteUserDetails.email;
                            DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                            SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                            newRef.setValue(submissionDetails);
                            Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("GROUP_ID",Group_ID);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        startCountdownTimer();

    }

    private boolean shouldAllowExit = false;

    @Override
    public void onBackPressed() {
        if (!shouldAllowExit) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Exit")
                    .setMessage("Your Assignment Will Be Submitted If You Exit Right Now?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

                            userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                                    if(readWriteUserDetails!=null){
                                        String Temp=AS_A.getText().toString();
                                        if(TextUtils.isEmpty(Temp)){
                                            Temp="";
                                        }
                                        if(Answers.isEmpty()||n==Answers.size()){
                                            Answers.add(Temp);
                                        }
                                        else {
                                            Answers.set(n,Temp);
                                        }
                                        if(Answers.size()<Questions.size()){
                                            while(Answers.size()<Questions.size()){
                                                Answers.add("");
                                            }
                                        }
                                        String UserName=readWriteUserDetails.userName;
                                        String Email=readWriteUserDetails.email;
                                        DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                                        SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                                        newRef.setValue(submissionDetails);
                                        Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtra("GROUP_ID",Group_ID);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else{
                                        Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                                }
                            });
                            shouldAllowExit = true;
                            onBackPressed();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }


    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                submitTest();
            }
        }.start();
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // Update a TextView with the remaining time
        AS_Time.setText(timeLeftFormatted);
    }

    private void submitTest() {
        DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                if(readWriteUserDetails!=null){
                    String Temp=AS_A.getText().toString();
                    if(TextUtils.isEmpty(Temp)){
                        Temp="";
                    }
                    if(Answers.isEmpty()||n==Answers.size()){
                        Answers.add(Temp);
                    }
                    else {
                        Answers.set(n,Temp);
                    }
                    if(Answers.size()<Questions.size()){
                        while(Answers.size()<Questions.size()){
                            Answers.add("");
                        }
                    }
                    String UserName=readWriteUserDetails.userName;
                    String Email=readWriteUserDetails.email;
                    DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                    SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                    newRef.setValue(submissionDetails);
                    Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("GROUP_ID",Group_ID);
                    startActivity(intent);
                    finish();
                }
                else{
                    Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void showAlertDialog () {

        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(AssignmentSubmission.this);
        builder.setTitle("Submit Exam?");
        builder.setMessage("Do you want to Submit the Exam?");


        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DatabaseReference userDetails= FirebaseDatabase.getInstance().getReference("Registered Users").child(firebaseUser.getUid()).child("User Details");

                userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ReadWriteUserDetails readWriteUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                        if(readWriteUserDetails!=null){
                            String Temp=AS_A.getText().toString();
                            if(TextUtils.isEmpty(Temp)){
                                Temp="";
                            }
                            if(Answers.isEmpty()||n==Answers.size()){
                                Answers.add(Temp);
                            }
                            else {
                                Answers.set(n,Temp);
                            }
                            if(Answers.size()<Questions.size()){
                                while(Answers.size()<Questions.size()){
                                    Answers.add("");
                                }
                            }
                            String UserName=readWriteUserDetails.userName;
                            String Email=readWriteUserDetails.email;
                            DatabaseReference newRef=FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments").child(Assignment_ID).child("Submissions").child(firebaseUser.getUid());
                            SubmissionDetails submissionDetails=new SubmissionDetails(UserName,firebaseUser.getUid(),Email,Answers);
                            newRef.setValue(submissionDetails);
                            Intent intent = new Intent(AssignmentSubmission.this, StudentGroup.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("GROUP_ID",Group_ID);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AssignmentSubmission.this, "SomeThing Went Wrong", Toast.LENGTH_SHORT).show();
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
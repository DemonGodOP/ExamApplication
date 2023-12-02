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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class NewAssignment extends AppCompatActivity {
    TextView NATIG,NA_QN;
    EditText NA_Q;
    Button NA_P,NA_N,NA_S;
    FirebaseAuth authProfile;
    FirebaseUser firebaseUser;
    String Group_ID;

    int n;

    List<String> Questions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_assignment);

        Intent intent = getIntent();

        // Retrieve the data from the intent using the key
        Group_ID = intent.getStringExtra("GROUP_ID");

        NATIG=findViewById(R.id.NATIG);
        NA_Q=findViewById(R.id.NA_Q);
        NA_QN=findViewById(R.id.NA_QN);
        NA_P=findViewById(R.id.NA_P);
        NA_N=findViewById(R.id.NA_N);
        NA_S=findViewById(R.id.NA_S);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        Questions=new ArrayList<>();

        n=0;

        NA_QN.setText(n+1+"");

        NA_P.setEnabled(false);

        NATIG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        NA_N.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String NQ=NA_Q.getText().toString();

                if(TextUtils.isEmpty(NQ)){
                    NA_Q.setError("Enter a Question in order to Proceed");
                    NA_Q.requestFocus();
                }
                else{
                    if(Questions.isEmpty()||n==Questions.size()){
                        Questions.add(NQ);
                    }
                    else {
                        Questions.set(n, NQ);
                    }
                    n++;
                    NA_P.setEnabled(true);
                    NA_QN.setText(n+1+"");
                    if(n==Questions.size()-1){
                        NA_N.setText("ADD QUESTION");
                    }
                    if(n<Questions.size()){
                        NA_Q.setText(Questions.get(n));
                    } else {
                        NA_Q.setText("");
                    }
                }
            }
        });

        NA_P.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                n--;
                if(n==0){
                    NA_P.setEnabled(false);
                }
                NA_QN.setText(n+1+"");
                NA_Q.setText(Questions.get(n));
                if(n<Questions.size()-1){
                    NA_N.setText("NEXT");
                }
            }
        });

        NA_S.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CreateNewAssignment();
            }
        });

    }

    public void CreateNewAssignment(){
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Groups").child(Group_ID).child("Assignments");
        String AssignmentID=database.push().getKey();
        Assignment assignment=new Assignment(Questions);
        assert AssignmentID != null;
        database.child(AssignmentID).setValue(assignment).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Intent intent = new Intent(NewAssignment.this, InsideGroup.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(NewAssignment.this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
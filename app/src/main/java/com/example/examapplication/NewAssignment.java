package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
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
    TextView NATIG,NA_QN,AS_NameText,AS_TimingText,AS_Text;
    EditText NA_Q,AS_Timing,AS_Name;
    Button NA_P,NA_N,NA_S,AS_Submit;
    FirebaseAuth authProfile;
    FirebaseUser firebaseUser;
    String Group_ID,Name,Timing;

    boolean temp;

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
        AS_NameText=findViewById(R.id.AS_NameText);
        AS_TimingText=findViewById(R.id.AS_TimingText);
        AS_Text=findViewById(R.id.AS_Text);
        AS_Timing=findViewById(R.id.AS_Timing);
        AS_Name=findViewById(R.id.AS_Name);
        AS_Submit=findViewById(R.id.AS_Submit);

        AS_NameText.setVisibility(View.GONE);
        AS_Timing.setVisibility(View.GONE);
        AS_TimingText.setVisibility(View.GONE);
        AS_Name.setVisibility(View.GONE);
        AS_Submit.setVisibility(View.GONE);

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

        NA_QN.setVisibility(View.GONE);
        NA_Q.setVisibility(View.GONE);
        AS_Text.setVisibility(View.GONE);
        NA_P.setVisibility(View.GONE);
        NA_N.setVisibility(View.GONE);
        NA_S.setVisibility(View.GONE);


        AS_NameText.setVisibility(View.VISIBLE);
        AS_Timing.setVisibility(View.VISIBLE);
        AS_TimingText.setVisibility(View.VISIBLE);
        AS_Name.setVisibility(View.VISIBLE);
        AS_Submit.setVisibility(View.VISIBLE);

        AS_Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Name=AS_Name.getText().toString();
                Timing=AS_Timing.getText().toString();



                if(TextUtils.isEmpty(Name)){
                    AS_Name.setError("Please enter your password for authentication");
                    AS_Name.requestFocus();
                }
                if(TextUtils.isEmpty(Timing)){
                    AS_Timing.setError("Please enter your password for authentication");
                    AS_Timing.requestFocus();
                }
                else{
                    showAlertDialog(AssignmentID,database);
                }
            }
        });
    }

    private void showAlertDialog (String AssignmentID,DatabaseReference database) {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(NewAssignment.this);
        builder.setTitle("Activate Assignment");
        builder.setMessage("Do you want to allow students to take the Assignment?");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String NQ=NA_Q.getText().toString();

                if(!TextUtils.isEmpty(NQ)&&n==0||n==Questions.size()){
                    Questions.add(NQ);
                }
                else{
                    Questions.set(n,NQ);
                }
                temp=true;
                Assignment assignment=new Assignment(Questions,temp,Name,Timing,AssignmentID);
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
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                temp=false;
                Assignment assignment=new Assignment(Questions,temp,Name,Timing,AssignmentID);
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
        });

        //create the AlertDialog
        AlertDialog alertDialog=builder.create();

        //show the alert dialog
        alertDialog.show();
    }
}
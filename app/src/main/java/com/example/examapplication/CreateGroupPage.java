package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.Objects;
import java.util.Random;

public class CreateGroupPage extends AppCompatActivity {
    EditText CG_GroupName,CG_SubjectName,CG_SubjectCode,CG_Institute,CG_GroupDescription;
    Button CG_Button;
    TextView GCTTH;
    ProgressBar CG_progressBar;

    FirebaseAuth authProfile;

    String GroupName, SubjectName, SubjectCode, Institute, GroupDescription, TeacherName;
    DatabaseReference database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group_page);
        CG_GroupName = findViewById(R.id.CG_GroupName);
        CG_SubjectName = findViewById(R.id.CG_SubjectName);
        CG_SubjectCode = findViewById(R.id.CG_SubjectCode);
        CG_Institute = findViewById(R.id.CG_Institute);
        CG_GroupDescription = findViewById(R.id.CG_GroupDescription);
        CG_Button = findViewById(R.id.CG_Button);
        GCTTH = findViewById(R.id.GCTTH);
        CG_progressBar = findViewById(R.id.CG_progressBar);
        authProfile=FirebaseAuth.getInstance();
        FirebaseUser firebaseUser=authProfile.getCurrentUser();
        database = FirebaseDatabase.getInstance().getReference("Groups");
        GCTTH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(CreateGroupPage.this, TeacherHomePage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("Rl","Teacher");
                startActivity(intent);
                finish();
            }
        });
        CG_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GroupName = CG_GroupName.getText().toString();
                SubjectName = CG_SubjectName.getText().toString();
                SubjectCode = CG_SubjectCode.getText().toString();
                Institute = CG_Institute.getText().toString();
                GroupDescription = CG_GroupDescription.getText().toString();
                if (TextUtils.isEmpty(GroupName)) {
                    CG_GroupName.setError("Please enter the group name");
                    CG_GroupName.requestFocus();
                } else if (TextUtils.isEmpty(SubjectName)) {
                    CG_SubjectName.setError("Please enter your subject name");
                    CG_SubjectName.requestFocus();
                } else if (TextUtils.isEmpty(SubjectCode)) {
                    CG_SubjectCode.setError("Please enter your subject code");
                    CG_SubjectCode.requestFocus();
                } else if (TextUtils.isEmpty(Institute)) {
                    CG_Institute.setError("Please enter the name of your Institute");
                    CG_Institute.requestFocus();
                } else if (TextUtils.isEmpty(GroupDescription)) {
                    CG_GroupDescription.setError("Please enter your Group Description");
                    CG_GroupDescription.requestFocus();
                } else {
                    //FinalRole = Selected.getText().toString();
                    CG_progressBar.setVisibility(View.VISIBLE);
                    CreateGroup(firebaseUser);
                }
            }
        });
    }

    // Helper function to generate a random alphanumeric string
    private void generateUniqueGroupId(int length, FirebaseCallback callback) {
        String tempGroupId = generateRandomString(length);
        database.child(tempGroupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // If the group ID exists, generate a new one recursively
                    generateUniqueGroupId(length, callback);
                } else {
                    // If the group ID does not exist, use this one
                    callback.onCallback(tempGroupId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Database error", databaseError.toException());
            }
        });
    }

    // Helper function to generate a random alphanumeric string
    private String generateRandomString(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    // Method to save data to Firebase using the generated group ID

    // Callback interface
    private interface FirebaseCallback {
        void onCallback(String uniqueGroupId);
    }

    public void CreateGroup(FirebaseUser firebaseUser){
        String teacherId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");

        referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                ReadWriteUserDetails readUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                if(readUserDetails != null){
                    TeacherName=readUserDetails.userName;
                    String groupId[] = {generateRandomString(7)}; // Generate a unique key for the group
                    generateUniqueGroupId(7, new FirebaseCallback() {
                        @Override
                        public void onCallback(String uniqueGroupId) {
                            groupId[0] = uniqueGroupId;
                            // Use the generated unique group ID in other parts of your code
                            Log.d("Firebase", "Generated unique group ID: " + groupId);

                        }
                    });

                    // Save group details in teacher's directory
                    assert groupId != null;
                    DatabaseReference teacherGroupsRef = FirebaseDatabase.getInstance().getReference()
                            .child("Registered Users") // Assuming this is where teachers are stored
                            .child(teacherId) // Use the teacher's user ID
                            .child("Groups") // Create a child node for groups
                            .child(groupId[0]);

                    // Creating a Group object with its details within Teacher User
                    Group newGroup = new Group(GroupName,SubjectName,SubjectCode,GroupDescription, groupId[0], Institute,TeacherName);
                    teacherGroupsRef.setValue(newGroup);

                    // Creating a Group Object Within the Main Group Directory
                    database.child(groupId[0]).child("Group Details").setValue(newGroup);
                }else {
                    Toast.makeText(CreateGroupPage.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error){

            }
        });


        CG_progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(CreateGroupPage.this, TeacherHomePage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }
}
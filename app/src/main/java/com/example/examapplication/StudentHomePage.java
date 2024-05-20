package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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
import java.util.Objects;

public class StudentHomePage extends AppCompatActivity {

    TextView SignOut,SHM_NoText;
    FirebaseAuth authProfile;

    TextView S_Profile;
    ProgressBar SH_progressBar;

    TextView SHM_SG;

    Button SHM_Search;

    ListView SHM_LV;

    String Rl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home_page);
        Intent intent = getIntent();

        Rl= intent.getStringExtra("Rl");

        SH_progressBar=findViewById(R.id.SH_progressBar);
        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        //Check if email is verified before user can access their profile

        SHM_LV=findViewById(R.id.SHM_LV);
        SHM_Search=findViewById(R.id.SHM_Search);
        SHM_SG=findViewById(R.id.SHM_SG);
        SHM_NoText=findViewById(R.id.SHM_NoText);

        if (!firebaseUser.isEmailVerified()) {
            firebaseUser.sendEmailVerification();
            showAlertDialog();
        }
        SignOut=findViewById(R.id.S_SignOut);
        SignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SH_progressBar.setVisibility(View.VISIBLE);
                authProfile.signOut();
                Intent intent=new Intent(StudentHomePage.this,Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                SH_progressBar.setVisibility(View.GONE);
                finish();
            }
        });

        S_Profile=findViewById(R.id.S_Profile);
        S_Profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(StudentHomePage.this,Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("Rl","Student");
                startActivity(intent);
                finish();
            }
        });

        SHM_Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Group_ID=SHM_SG.getText().toString();
                if (TextUtils.isEmpty(Group_ID)) {
                    SHM_SG.setError("Please Enter a Group ID to Search");
                    SHM_SG.requestFocus();
                }else{
                    Intent intent = new Intent(StudentHomePage.this, SearchingGroup.class);

                    // Pass the unique key to the new activity
                    intent.putExtra("GROUP_ID", Group_ID);



                    // Start the new activity
                    startActivity(intent);
                }
            }
        });

        SH_progressBar.setVisibility(View.VISIBLE);
        PopulateList(firebaseUser);

    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(StudentHomePage.this);
        builder.setTitle("Email Not Verified");
        builder.setMessage("Please verify your email now.You can not login without email verification.");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent=new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//to email app in new window and not within our app
                startActivity(intent);
            }
        });

        //create the AlertDialog
        AlertDialog alertDialog=builder.create();

        //show the alert dialog
        alertDialog.show();
    }

    public void PopulateList(FirebaseUser firebaseUser){
        //Group ListView
        String StudentId = firebaseUser.getUid();
        DatabaseReference teacherGroupsRef = FirebaseDatabase.getInstance()
                .getReference("Registered Users")
                .child(StudentId)
                .child("Groups");

        // Listener to retrieve the groups
        teacherGroupsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Group> groupsList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Group group = snapshot.getValue(Group.class);
                    if (group != null) {
                        groupsList.add(group);
                    }
                }

                if (groupsList.isEmpty()) {
                    // If there are no groups, display the message TextView and hide the ListView
                    SHM_NoText.setVisibility(View.VISIBLE);
                    SHM_LV.setVisibility(View.GONE);
                    SH_progressBar.setVisibility(View.GONE);
                } else {
                    // If there are groups, display the ListView and hide the message TextView
                    SHM_NoText.setVisibility(View.GONE);
                    SHM_LV.setVisibility(View.VISIBLE);

                    // Now, you have groupsList containing the groups data
                    // You can use this data to populate your ListView
                    // For instance, set up an ArrayAdapter with the ListView
                    TGRCL adapter = new TGRCL(StudentHomePage.this, R.layout.tgrcl, groupsList);

                    // Assuming you have a ListView with the ID listViewGroups in your layout
                    SHM_LV.setAdapter(adapter);

                    SH_progressBar.setVisibility(View.GONE);

                    SHM_LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Get the selected group from the adapter
                            Group selectedGroup = (Group) parent.getItemAtPosition(position);

                            // Retrieve the unique key of the selected group
                            String selectedGroupId = selectedGroup.Group_ID; // Or however you store the ID in the Group class

                            // Create an intent to start a new activity
                            Intent intent = new Intent(StudentHomePage.this, StudentGroup.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("Rl","Student");

                            // Pass the unique key to the new activity
                            intent.putExtra("GROUP_ID",selectedGroupId);

                            // Start the new activity
                            startActivity(intent);

                            finish();
                        }
                    });
                }
            }
            @Override
            public void onCancelled (@NonNull DatabaseError databaseError){
                Toast.makeText(StudentHomePage.this, "Something Went Wrong Please Restart The Application", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
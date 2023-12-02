package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

public class Profile extends AppCompatActivity {
    Toolbar P_toolBar;

    TextView P_Name,P_Email,P_Phone,P_Institute,P_UserName,P_Role;
    ProgressBar P_progressBar;
    String name,email,phone,institute,username,role;
    FirebaseAuth authProfile;

    TextView PTH;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        P_toolBar=findViewById(R.id.P_toolBar);
        setSupportActionBar(P_toolBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        P_Name = findViewById(R.id.P_Name);
        P_Email = findViewById(R.id.P_Email);
        P_Phone = findViewById(R.id.P_Phone);
        P_Institute = findViewById(R.id.P_Institute);
        P_UserName = findViewById(R.id.P_UserName);
        P_Role = findViewById(R.id.P_Role);
        P_progressBar = findViewById(R.id.P_progressBar);
        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(Profile.this, "Something went wrong!User's details are not available at the moment", Toast.LENGTH_SHORT).show();
        } else {
            checkifEmailVerified(firebaseUser);
            P_progressBar.setVisibility(View.VISIBLE);
            showUserProfile(firebaseUser);
        }

        PTH=findViewById(R.id.PTH);
        PTH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(role.equals("Teacher")){
                    Intent intent = new Intent(Profile.this, TeacherHomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                else{
                    Intent intent = new Intent(Profile.this, StudentHomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
    private void checkifEmailVerified(FirebaseUser firebaseUser)
    {
        if(!firebaseUser.isEmailVerified()){
            showAlertDialog();
        }
    }

    private void showUserProfile(FirebaseUser firebaseUser){
        String userId=firebaseUser.getUid();

        //Extracting User Reference from Database for "Registered Users"
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                ReadWriteUserDetails readUserDetails=snapshot.getValue(ReadWriteUserDetails.class);
                if(readUserDetails != null){
                    name=readUserDetails.name;
                    email=readUserDetails.email;
                    phone=readUserDetails.phoneNo;
                    institute=readUserDetails.institute;
                    username=readUserDetails.userName;
                    role=readUserDetails.finalRole;
                    P_Name.setText(name);
                    P_Email.setText(email);
                    P_Phone.setText(phone);
                    P_UserName.setText(username);
                    P_Institute.setText(institute);
                    P_Role.setText(role);


                }else {
                    Toast.makeText(Profile.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                }
                P_progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(Profile.this, "Something went wrong!", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void showAlertDialog() {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(Profile.this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.M_CP) {
            Intent intent = new Intent(Profile.this, ChangePassword.class);
            startActivity(intent);

        }
        if (id == R.id.M_EP) {
            Intent intent = new Intent(Profile.this, UpdateProfile.class);
            startActivity(intent);

        }
        if (id == R.id.M_DP) {
            Intent intent = new Intent(Profile.this, DeleteUser.class);
            startActivity(intent);

        }
        if(id==R.id.M_CE){
            Intent intent = new Intent(Profile.this, ChangeEmail.class);
            startActivity(intent);
        }
        // Handle other menu item clicks if needed

        return super.onOptionsItemSelected(item);
    }

}
package com.example.examapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class TeacherHomePage extends AppCompatActivity {
    TextView SignOut;
    FirebaseAuth authProfile;

    TextView T_Profile;
    ProgressBar TH_progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_home_page);
        TH_progressBar=findViewById(R.id.TH_progressBar);
        authProfile = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = authProfile.getCurrentUser();
        //Check if email is verified before user can access their profile

        if (!firebaseUser.isEmailVerified()) {
            firebaseUser.sendEmailVerification();
            showAlertDialog();
        }
        SignOut=findViewById(R.id.S_SignOut);
        SignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TH_progressBar.setVisibility(View.VISIBLE);
                authProfile.signOut();
                Intent intent=new Intent(TeacherHomePage.this,Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                TH_progressBar.setVisibility(View.GONE);
                finish();
            }
        });

        T_Profile=findViewById(R.id.T_Profile);
        T_Profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TH_progressBar.setVisibility(View.VISIBLE);
                Intent intent=new Intent(TeacherHomePage.this,Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                TH_progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }


    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(TeacherHomePage.this);
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


}
package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DeleteUser extends AppCompatActivity {
    FirebaseAuth authProfile;
    FirebaseUser firebaseUser;
    EditText DU_Password;
    TextView Text;
    Button DU_Authenticate, DU_Button;
    ProgressBar DU_progressBar;
    String userPwd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_user);
        DU_progressBar=findViewById(R.id.DU_progressBar);
        DU_Password=findViewById(R.id.DU_Password);
        Text= findViewById(R.id.DU_Text);
        DU_Button=findViewById(R.id.DU_Button);
        DU_Authenticate=findViewById(R.id.DU_Authenticate);

        //disable delete user button until user is authenticated
        DU_Button.setEnabled(false);

        authProfile=FirebaseAuth.getInstance();
        firebaseUser=authProfile.getCurrentUser();

        if(firebaseUser.equals("")){
            Toast.makeText(DeleteUser.this, "Something went wrong! User details are not available at the moment", Toast.LENGTH_SHORT).show();
            Intent intent=new Intent(DeleteUser.this, Profile.class);
            startActivity(intent);
            finish();
        }else {
            reAuthenticateUser(firebaseUser);
        }
    }
    private void reAuthenticateUser(FirebaseUser firebaseUser) {
        DU_Authenticate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userPwd = DU_Password.getText().toString();

                if (TextUtils.isEmpty(userPwd)) {
                    Toast.makeText(DeleteUser.this, "password is needed", Toast.LENGTH_SHORT).show();
                    DU_Password.setError("Please enter your current password to authenticator");
                    DU_Password.requestFocus();
                } else {
                    DU_progressBar.setVisibility(View.VISIBLE);

                    //ReAuthenticate User now
                    AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), userPwd);

                    firebaseUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task){
                            if(task.isSuccessful()){
                                DU_progressBar.setVisibility(View.GONE);

                                DU_Password.setEnabled(false);
                                DU_Authenticate.setEnabled(false);
                                DU_Button.setEnabled(true);

                                //set TextView to show user is authenticated/verified
                                Text.setText("You are authenticated/verified. You can delete your profile now.");
                                Toast.makeText(DeleteUser.this,"Password has been verified"+ "Change password now", Toast.LENGTH_SHORT).show();

                                //update color of change password button
                                int color= ContextCompat.getColor(DeleteUser.this, R.color.dark_green);;
                                DU_Button.setBackgroundTintList(ColorStateList.valueOf(color));
                                DU_Button.setOnClickListener(new View.OnClickListener(){
                                    @Override
                                    public void onClick(View V){
                                        showAlertDialog();
                                    }
                                });
                            }else {
                                try{
                                    throw task.getException();
                                }catch (Exception e){
                                    Toast.makeText(DeleteUser.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            DU_progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(DeleteUser.this);
        builder.setTitle("Delete User and Related Data?");
        builder.setMessage("Do you really want to delete your profile and related data? This action is irreversible.");

        //Open email apps i User clicks/taps Continue button
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteUser(firebaseUser);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(DeleteUser.this, Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        //create the AlertDialog
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

    private void deleteUser(FirebaseUser firebaseUser){
        deleteUserData();
        firebaseUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                   authProfile.signOut();
                    Toast.makeText(DeleteUser.this, "Profile Deleted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DeleteUser.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }else {
                    try{
                        throw task.getException();
                    }catch (Exception e){
                        Toast.makeText(DeleteUser.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                DU_progressBar.setVisibility(View.GONE);
            }
        });
    }

    public void deleteUserData(){
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(DeleteUser.this, "User Data Deleted", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(DeleteUser.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
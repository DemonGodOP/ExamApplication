package com.example.examapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChangeEmail extends AppCompatActivity {
    FirebaseAuth authProfile;
    FirebaseUser firebaseUser;
    ProgressBar CE_progressBar;
    TextView CE_Text,CE_Email,CETP;
    Button CE_Button;
    EditText CE_NewEmail, CE_Password;

    String Old_Email,New_Email, Name, finalRole, Institute, Phone, Username,userPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);
        CE_progressBar= findViewById(R.id.CE_progressBar);
        CE_Text=findViewById(R.id.CE_Text);
        CE_Email=findViewById(R.id.CE_Email);
        CE_NewEmail=findViewById(R.id.CE_NewEmail);
        CE_Button=findViewById(R.id.CE_Button);
        CE_Password=findViewById(R.id.CE_Password);
        authProfile=FirebaseAuth.getInstance();
        firebaseUser= authProfile.getCurrentUser();
        DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
        referenceProfile.child(firebaseUser.getUid()).child("User Details").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReadWriteUserDetails readUserDetails = snapshot.getValue(ReadWriteUserDetails.class);
                if (readUserDetails != null) {
                    Name = readUserDetails.name;
                    Phone = readUserDetails.phoneNo;
                    Institute = readUserDetails.institute;
                    Username = readUserDetails.userName;
                    finalRole=readUserDetails.finalRole;
                    Old_Email=readUserDetails.email;
                    CE_Email.setText(Old_Email);
                } else {
                    Toast.makeText(ChangeEmail.this , "Something went wrong!", Toast.LENGTH_LONG).show();
                }

            }
            @Override
            public void onCancelled (@NonNull DatabaseError error){
                Toast.makeText(ChangeEmail.this, "Something went wrong!", Toast.LENGTH_LONG).show();
            }
        });
        CETP=findViewById(R.id.CETP);
        CETP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 Intent intent=new Intent(ChangeEmail.this,Profile.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        CE_Button.setEnabled(false);
        CE_NewEmail.setEnabled(false);



        if(firebaseUser.equals("")){
            Toast.makeText(ChangeEmail.this, "Something went wrong! User's details not available", Toast.LENGTH_SHORT).show();
        }else{
            reAuthenticate (firebaseUser);
        }
    }
    private void reAuthenticate(FirebaseUser firebaseuser){
        Button buttonVerifyUser= findViewById(R.id.CE_Authenticate);
        buttonVerifyUser.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                //obtain password for authentication
                userPwd=CE_Password.getText().toString();

                if(TextUtils.isEmpty(userPwd)){
                    Toast.makeText(ChangeEmail.this, "Password is needed to continue", Toast.LENGTH_SHORT).show();
                    CE_Password.setError("Please enter your password for authentication");
                    CE_Password.requestFocus();
                }else {
                    CE_progressBar.setVisibility(View.VISIBLE);

                    AuthCredential credential= EmailAuthProvider.getCredential(Old_Email, userPwd);

                    firebaseuser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                CE_progressBar.setVisibility(View.VISIBLE);

                                Toast.makeText(ChangeEmail.this, "Password has been verified." + "You can update email now.", Toast.LENGTH_SHORT).show();

                                //set TextView to show the user is authenticated
                                CE_Text.setText("You are authenticated. You can update your email now");

                                //disable editText for password and enable editText for new email and update
                                CE_NewEmail.setEnabled(true);
                                CE_Password.setEnabled(false);
                                buttonVerifyUser.setEnabled(false);
                                CE_Button.setEnabled(true);

                                //change color of update Email button
                                CE_Button.setBackgroundTintList(ContextCompat.getColorStateList(ChangeEmail.this, R.color.dark_green));

                                CE_Button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        New_Email = CE_NewEmail.getText().toString();
                                        if (TextUtils.isEmpty(New_Email)) {
                                            Toast.makeText(ChangeEmail.this, "New email is required", Toast.LENGTH_SHORT).show();
                                            CE_NewEmail.setError("Please enter new email");
                                            CE_NewEmail.requestFocus();
                                        } else if (!Patterns.EMAIL_ADDRESS.matcher(New_Email).matches()) {
                                            Toast.makeText(ChangeEmail.this, "Please enter valid email", Toast.LENGTH_SHORT).show();
                                            CE_NewEmail.setError("Please provide valid email");
                                            CE_NewEmail.requestFocus();
                                        } else if (Old_Email.matches(New_Email)) {
                                            Toast.makeText(ChangeEmail.this, "New email cannot be same as old email", Toast.LENGTH_SHORT).show();
                                            CE_NewEmail.setError("Please enter new email");
                                            CE_NewEmail.requestFocus();
                                        } else {
                                            boolean e[]={true};
                                            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(New_Email)
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            List<String> signInMethods = task.getResult().getSignInMethods();
                                                            if (signInMethods != null && signInMethods.isEmpty()) {
                                                                e[0]=false;
                                                            } else {
                                                                e[0]=true;
                                                            }
                                                        } else {
                                                            // Handle task failure
                                                            Exception exception = task.getException();
                                                            if (exception != null) {
                                                                // Log or handle the exception
                                                            }
                                                        }
                                                    });
                                            if(e[0]==false) {
                                                CE_progressBar.setVisibility(View.VISIBLE);
                                                updateEmail(firebaseuser);
                                            }
                                            else{
                                                CE_NewEmail.setError("Email Already Exists in the Database");
                                                CE_NewEmail.requestFocus();
                                            }
                                        }

                                    }
                                });
                            } else {
                                try {
                                    throw task.getException();
                                } catch(FirebaseAuthInvalidCredentialsException e){
                                    CE_Password.setError("Incorrect Credentials");
                                    CE_Password.requestFocus();
                                }catch(FirebaseAuthUserCollisionException e){
                                    CE_NewEmail.setError("User Already exists with this Email ID");
                                    CE_NewEmail.requestFocus();
                                }catch (Exception e) {
                                    Toast.makeText(ChangeEmail.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            CE_progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void updateEmail(FirebaseUser firebaseuser){

        firebaseuser.verifyBeforeUpdateEmail(New_Email).addOnCompleteListener(new OnCompleteListener<Void>(){
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Email verification succeeded, show alert and update database
                    showAlertDialog();
                    DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");
                    ReadWriteUserDetails WriteUserDetails = new ReadWriteUserDetails(New_Email, Name, Phone, Institute, Username, finalRole);
                    referenceProfile.child(firebaseUser.getUid()).child("User Details").setValue(WriteUserDetails);
                    CE_progressBar.setVisibility(View.GONE);
                } else {
                    // Email verification failed, handle accordingly
                    Exception exception = task.getException();
                    if (exception != null) {
                        // Handle the exception (e.g., display an error message)
                    }
                }
            }
        });
    }
    private void showAlertDialog () {
        //Setup the Alert Builder
        AlertDialog.Builder builder=new AlertDialog.Builder(ChangeEmail.this);
        builder.setTitle("Email Not Verified");
        builder.setMessage("Please verify your email now.");

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
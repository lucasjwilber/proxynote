package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    private boolean loginShown = true;
    TextView firstNameLabel;
    EditText firstNameET;
    TextView lastNameLabel;
    EditText lastNameET;
    TextView usernameLabel;
    EditText usernameET;
    EditText emailET;
    EditText passwordET;
    TextView confirmPwLabel;
    EditText confirmPwET;
    Button forgotPwBtn;
    Button submitLoginBtn;
    Button submitSignupBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // set up view references
        {
            firstNameLabel = findViewById(R.id.loginActFirstNameLabel);
            firstNameET = findViewById(R.id.loginActFirstNameEditText);
            lastNameLabel = findViewById(R.id.loginActLastNameLabel);
            lastNameET = findViewById(R.id.loginActLastNameEditText);
            usernameLabel = findViewById(R.id.loginActUsernameLabel);
            usernameET = findViewById(R.id.loginActUsernameEditText);
            emailET = findViewById(R.id.loginActEmailEditText);
            passwordET = findViewById(R.id.loginActPasswordEditText);
            confirmPwLabel = findViewById(R.id.loginActConfirmPasswordLabel);
            confirmPwET = findViewById(R.id.loginActConfirmPasswordEditText);
            forgotPwBtn = findViewById(R.id.loginActForgotPwBtn);
            submitLoginBtn = findViewById(R.id.loginActSubmitLoginBtn);
            submitSignupBtn = findViewById(R.id.loginActSubmitSignupBtn);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void loginSignupToggle(View v) {
        // if login form is already shown and login is clicked or vice versa, return:
        if (v == findViewById(R.id.loginActLoginBtn) && loginShown) return;
        if (v == findViewById(R.id.loginActSignupBtn) && !loginShown) return;

        int loginDisplay;
        int signupDisplay;
        // depending on which form is shown now, determine which fields need to be shown/hidden:
        if (loginShown) {
            loginDisplay = View.GONE;
            signupDisplay = View.VISIBLE;
        } else {
            loginDisplay = View.VISIBLE;
            signupDisplay = View.GONE;
        }

        // show/hide stuff
        firstNameLabel.setVisibility(signupDisplay);
        firstNameET.setVisibility(signupDisplay);
        lastNameLabel.setVisibility(signupDisplay);
        lastNameET.setVisibility(signupDisplay);
        usernameLabel.setVisibility(signupDisplay);
        usernameET.setVisibility(signupDisplay);
        confirmPwLabel.setVisibility(signupDisplay);
        confirmPwET.setVisibility(signupDisplay);
        forgotPwBtn.setVisibility(loginDisplay);
        submitLoginBtn.setVisibility(loginDisplay);
        submitSignupBtn.setVisibility(signupDisplay);
        loginShown = !loginShown;
    }

    public void signupSubmit(View v) {
        String firstName = firstNameET.getText().toString();
        String lastName = lastNameET.getText().toString();
        String username = usernameET.getText().toString();
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.i("ljw", "createUserWithEmail:success");
                            user  = mAuth.getCurrentUser();

                            //add new user to DB
                            User newUser = new User(firstName, lastName, username, email, user.getUid());
                            db.collection("users")
                                    .add(newUser)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            Log.i("ljw", "added new user to firestore");
                                            // go to map:
                                            Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                                            intent.putExtra("newUser", true);
                                            startActivity(intent);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.i("ljw", "failed adding new user to firestore!\nnew user: "+newUser.toString());
                                        }
                                    });

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.i("ljw", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void loginSubmit(View v) {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.i("ljw", "signInWithEmail:success");
                            user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.i("ljw", "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}

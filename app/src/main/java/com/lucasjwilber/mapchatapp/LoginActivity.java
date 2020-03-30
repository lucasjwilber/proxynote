package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
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
    TextView confirmPwCLabel;
    TextView confirmPwPLabel;
    EditText confirmPwET;
    Button forgotPwBtn;
    Button submitBtn;
    Button toggleBtn;
    TextView toggleLabel;
    ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        firstNameLabel = findViewById(R.id.loginActFirstNameLabel);
        firstNameET = findViewById(R.id.loginActFirstNameEditText);
        lastNameLabel = findViewById(R.id.loginActLastNameLabel);
        lastNameET = findViewById(R.id.loginActLastNameEditText);
        usernameLabel = findViewById(R.id.loginActUsernameLabel);
        usernameET = findViewById(R.id.loginActUsernameEditText);
        emailET = findViewById(R.id.loginActEmailEditText);
        passwordET = findViewById(R.id.loginActPasswordEditText);
        confirmPwCLabel = findViewById(R.id.loginActConfirmPasswordCLabel);
        confirmPwPLabel = findViewById(R.id.loginActConfirmPasswordPLabel);
        confirmPwET = findViewById(R.id.loginActConfirmPasswordEditText);
//        forgotPwBtn = findViewById(R.id.loginActForgotPwBtn);
        submitBtn = findViewById(R.id.loginActSubmitButton);
        toggleBtn = findViewById(R.id.loginActToggleButton);
        toggleLabel = findViewById(R.id.loginToggleLabel);
        loadingSpinner = findViewById(R.id.loginProgressBar);
    }

    public void loginSignupToggle(View v) {
        String submitBtnText = loginShown ? "SIGN UP" : "LOGIN";
        String toggleBtnText = loginShown ? "LOGIN" : "SIGN UP";
        String toggleLabelText = loginShown ? "Already have an account?" : "Don't have an account?";
        submitBtn.setText(submitBtnText);
        toggleBtn.setText(toggleBtnText);
        toggleLabel.setText(toggleLabelText);

        int visibility = loginShown ? View.VISIBLE : View.GONE;
        firstNameLabel.setVisibility(visibility);
        firstNameET.setVisibility(visibility);
        lastNameLabel.setVisibility(visibility);
        lastNameET.setVisibility(visibility);
        usernameLabel.setVisibility(visibility);
        usernameET.setVisibility(visibility);
        confirmPwCLabel.setVisibility(visibility);
        confirmPwPLabel.setVisibility(visibility);
        confirmPwET.setVisibility(visibility);

        loginShown = !loginShown;
    }

    public void onLoginOrSignupSubmit(View v) {
        loadingSpinner.setVisibility(View.VISIBLE);
        if (loginShown) {
            loginSubmit();
        } else {
            signupSubmit();
        }
    }

    public void loginSubmit() {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i("ljw", "signInWithEmail:success");
                            user = mAuth.getCurrentUser();
                            Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                            startActivity(intent);
                            loadingSpinner.setVisibility(View.GONE);
                            finish();
                        } else {
                            Log.i("ljw", "signInWithEmail:failure", task.getException());
                            Utils.showToast(LoginActivity.this, "Authentication failed.");
                            loadingSpinner.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    public void signupSubmit() {
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
                            Log.i("ljw", "createUserWithEmail:success");
                            user  = mAuth.getCurrentUser();

                            // set the user's displayname/username. this can't be done when adding the user unfortunately.
                            UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                            user.updateProfile(userProfileChangeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.i("ljw", "updated user profile with username");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i("ljw", "failed adding username to user");
                                }
                            });

                            //add new user to firestore "users" collection
                            User newUser = new User(firstName, lastName, username, email, user.getUid());
                            db.collection("users")
                                    .document(user.getUid())
                                    .set(newUser)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.i("ljw", "added new user to firestore");
//                                            // go to map:
                                            Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                                            intent.putExtra("newUser", true);
                                            startActivity(intent);
                                            loadingSpinner.setVisibility(View.VISIBLE);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.i("ljw", "failed adding new user to firestore!\nnew user: "+newUser.toString());
                                            loadingSpinner.setVisibility(View.VISIBLE);
                                        }
                                    });

                        } else {
                            Log.i("ljw", "createUserWithEmail:failure", task.getException());
                            Utils.showToast(LoginActivity.this, "Authentication failed.");
                            loadingSpinner.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }
}

package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucasjwilber.mapchatapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    private boolean loginShown = true;
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void loginButtonClicked(View v) {
        loginShown = true;
        String submitBtnText = "LOGIN";
        binding.loginButton.setTextColor(getResources().getColor(R.color.colorAccent));
        binding.signupButton.setTextColor(getResources().getColor(R.color.gray));
        binding.loginActSubmitButton.setText(submitBtnText);
        binding.loginActFirstNameLabel.setVisibility(View.GONE);
        binding.loginActFirstNameEditText.setVisibility(View.GONE);
        binding.loginActLastNameLabel.setVisibility(View.GONE);
        binding.loginActLastNameEditText.setVisibility(View.GONE);
        binding.loginActUsernameLabel.setVisibility(View.GONE);
        binding.loginActUsernameEditText.setVisibility(View.GONE);
        binding.loginActConfirmPasswordCLabel.setVisibility(View.GONE);
        binding.loginActConfirmPasswordPLabel.setVisibility(View.GONE);
        binding.loginActConfirmPasswordEditText.setVisibility(View.GONE);
    }
    public void signupButtonClicked(View v) {
        loginShown = false;
        String submitBtnText = "SIGN UP";
        binding.loginActSubmitButton.setText(submitBtnText);
        binding.signupButton.setTextColor(getResources().getColor(R.color.colorAccent));
        binding.loginButton.setTextColor(getResources().getColor(R.color.gray));
        binding.loginActFirstNameLabel.setVisibility(View.VISIBLE);
        binding.loginActFirstNameEditText.setVisibility(View.VISIBLE);
        binding.loginActLastNameLabel.setVisibility(View.VISIBLE);
        binding.loginActLastNameEditText.setVisibility(View.VISIBLE);
        binding.loginActUsernameLabel.setVisibility(View.VISIBLE);
        binding.loginActUsernameEditText.setVisibility(View.VISIBLE);
        binding.loginActConfirmPasswordCLabel.setVisibility(View.VISIBLE);
        binding.loginActConfirmPasswordPLabel.setVisibility(View.VISIBLE);
        binding.loginActConfirmPasswordEditText.setVisibility(View.VISIBLE);
    }

    public void onSubmitButtonClicked(View v) {
        if (loginShown) {
            loginSubmit();
        } else {
            signupSubmit();
        }
    }

    public void loginSubmit() {
        String email = binding.loginActEmailEditText.getText().toString();
        String password = binding.loginActPasswordEditText.getText().toString();
        if (email.equals("") || email.length() == 0 || password.equals("") || password.length() == 0) {
            Utils.showToast(LoginActivity.this, "Please fill out your email and password.");
            return;
        }

        binding.loginProgressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i("ljw", "signInWithEmail:success");
                            user = mAuth.getCurrentUser();
                            Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                            startActivity(intent);
                            binding.loginProgressBar.setVisibility(View.GONE);
                            finish();
                        } else {
                            Log.i("ljw", "signInWithEmail:failure", task.getException());
                            Utils.showToast(LoginActivity.this, "Authentication failed.");
                            binding.loginProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void signupSubmit() {
        String firstName = binding.loginActFirstNameEditText.getText().toString();
        String lastName = binding.loginActLastNameEditText.getText().toString();
        String username = binding.loginActUsernameEditText.getText().toString();
        String email = binding.loginActEmailEditText.getText().toString();
        String password = binding.loginActPasswordEditText.getText().toString();
        String confirmedPassword = binding.loginActConfirmPasswordEditText.getText().toString();

        if (!password.equals(confirmedPassword)) {
            Utils.showToast(LoginActivity.this, "Passwords do not match.");
            return;
        }

        binding.loginProgressBar.setVisibility(View.VISIBLE);

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
                                            binding.loginProgressBar.setVisibility(View.GONE);
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.i("ljw", "failed adding new user to firestore!\nnew user: "+newUser.toString());
                                            binding.loginProgressBar.setVisibility(View.GONE);
                                        }
                                    });

                        } else {
                            Log.i("ljw", "createUserWithEmail:failure", task.getException());
                            Utils.showToast(LoginActivity.this, "Authentication failed.");
                            binding.loginProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }
}

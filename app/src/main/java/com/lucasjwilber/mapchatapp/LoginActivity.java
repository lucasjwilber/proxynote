package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucasjwilber.mapchatapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private boolean loginShown = true;
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;
    private final int EMAIL_VERIFICATION_CHECK_COOLDOWN = 2000;
    private Handler emailVerificationCheckRunnable;
    boolean waitingForEmailVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //autofill email ET if it was saved
        sharedPreferences = getApplicationContext().getSharedPreferences("mapchatPrefs", Context.MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString("email", "");
        binding.loginActEmailEditText.setText(savedEmail);

        if (mAuth.getCurrentUser() != null) {
            user = mAuth.getCurrentUser();
            if (user != null && !user.isEmailVerified()) {
                binding.emailVerificationModal.setVisibility(View.VISIBLE);
                binding.loginBaseLayout.setVisibility(View.GONE);
            }
        }

        emailVerificationCheckRunnable = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (waitingForEmailVerification) {
            waitForEmailVerification();
        }
    }
    @Override
    public void onPause() {
        super.onPause();
        emailVerificationCheckRunnable.removeCallbacksAndMessages(null);
    }
    @Override
    public void onStop() {
        super.onStop();
        emailVerificationCheckRunnable.removeCallbacksAndMessages(null);
    }

    public void loginButtonClicked(View v) {
        loginShown = true;
        String submitBtnText = "LOGIN";
        binding.loginButton.setTextColor(getResources().getColor(R.color.colorAccent));
        binding.signupButton.setTextColor(getResources().getColor(R.color.gray));
        binding.loginActSubmitButton.setText(submitBtnText);
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
            Utils.showToast(LoginActivity.this, "Please enter your email and password.");
            return;
        }

        binding.loginProgressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "signInWithEmail:success");
                            user = mAuth.getCurrentUser();
                            binding.loginProgressBar.setVisibility(View.GONE);
                            finish();
                        } else {
                            Log.i(TAG, "signInWithEmail:failure", task.getException());
                            Utils.showToast(LoginActivity.this, "Username or password incorrect.");
                            binding.loginProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void signupSubmit() {
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
                            Log.i(TAG, "createUserWithEmail:success");
                            user  = mAuth.getCurrentUser();

                            //save email for autofill next time
                            SharedPreferences.Editor editor = sharedPreferences.edit().putString("email", email);
                            editor.apply();

                            // set the user's displayname/username. this can't be done when adding the user unfortunately.
                            UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                            user.updateProfile(userProfileChangeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.i(TAG, "updated user profile with username");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "failed adding username to user");
                                }
                            });

                            //add new user to firestore "users" collection
                            User newUser = new User(username, email, user.getUid());
                            db.collection("users")
                                    .document(user.getUid())
                                    .set(newUser)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.i(TAG, "added new user to firestore");

                                            user.sendEmailVerification()
                                                    .addOnSuccessListener(r -> {
                                                        Log.i(TAG, "is user verified yet: " + user.isEmailVerified());
                                                        waitForEmailVerification();
                                                        binding.loginProgressBar.setVisibility(View.GONE);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "error checking for email verification: " + e.toString());
                                                        binding.loginProgressBar.setVisibility(View.GONE);
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.i(TAG, "failed adding new user to firestore!\nnew user: "+newUser.toString());
                                            binding.loginProgressBar.setVisibility(View.GONE);
                                        }
                                    });

                        } else {
                            Log.i(TAG, "createUserWithEmail:failure", task.getException());
                            Utils.showToast(LoginActivity.this, "This email is already in use.");
                            binding.loginProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void onGoToMapClick(View v) {
        finish();
    }

    public void onResendEmailVerificationClick(View v) {
        if (user != null) {
            user.sendEmailVerification()
            .addOnSuccessListener(r -> {
                Utils.showToast(LoginActivity.this, "Verification email sent.");
            })
            .addOnFailureListener(e -> Log.i(TAG, "error sending ver email: " + e.toString()));
        }
    }

    private void waitForEmailVerification() {
        binding.loginBackButton.setVisibility(View.GONE);
        binding.emailVerificationModal.setVisibility(View.VISIBLE);
        binding.loginBaseLayout.setVisibility(View.GONE);
        waitingForEmailVerification = true;

        emailVerificationCheckRunnable.postDelayed(new Runnable() {
            public void run() {
                if (mAuth.getCurrentUser() != null) {
                    mAuth.getCurrentUser().reload();
                }
                if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
                    emailVerificationCheckRunnable.removeCallbacksAndMessages(null);
                    Utils.showToast(LoginActivity.this, "Email verified!");
                    waitingForEmailVerification = false;
                    finish();
                } else {
                    Log.i(TAG, "user still not verified");
                    emailVerificationCheckRunnable.postDelayed(this, EMAIL_VERIFICATION_CHECK_COOLDOWN);
                }
            }
        }, EMAIL_VERIFICATION_CHECK_COOLDOWN);
    }

    public void onBackButtonClicked(View v) { finish(); }
}

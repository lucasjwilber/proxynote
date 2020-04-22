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

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
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

import org.json.JSONException;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private FirebaseAuth mAuth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;
    private boolean loginShown = true;
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;
    private final int EMAIL_VERIFICATION_CHECK_COOLDOWN = 2000;
    private Handler emailVerificationCheckRunnable;
    boolean waitingForEmailVerification;
    private LoginButton facebookLoginButton;
    private static final String EMAIL = "email";
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //autofill email ET if it was saved
        sharedPreferences = getApplicationContext().getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString("email", "");
        binding.loginActEmailEditText.setText(savedEmail);

//        if (mAuth.getCurrentUser() != null) {
//            firebaseUser = mAuth.getCurrentUser();
//            if (firebaseUser != null && !firebaseUser.isEmailVerified()) {
//                binding.emailVerificationModal.setVisibility(View.VISIBLE);
//                binding.loginBaseLayout.setVisibility(View.GONE);
//            }
//        }

        emailVerificationCheckRunnable = new Handler();

        //facebook oath
        facebookLoginButton = binding.facebookLoginButton;
        facebookLoginButton.setReadPermissions(Arrays.asList(EMAIL));
        callbackManager = CallbackManager.Factory.create();
        setupFacebookButtonCallback();
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
        binding.facebookLoginButton.setVisibility(View.VISIBLE);
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
        binding.facebookLoginButton.setVisibility(View.GONE);
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
                            firebaseUser = mAuth.getCurrentUser();
                            binding.loginProgressBar.setVisibility(View.GONE);
                            updateStoredUserInfo(
                                    "firebase",
                                    firebaseUser.getUid(),
                                    firebaseUser.getDisplayName(),
                                    firebaseUser.getEmail()
                            );
                            finish();
                        } else {
                            Log.e(TAG, "signInWithEmail:failure", task.getException());
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
                            firebaseUser = mAuth.getCurrentUser();

                            // set the user's display name (username). this can't be done while creating the user unfortunately.
                            UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                            firebaseUser.updateProfile(userProfileChangeRequest).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    updateStoredUserInfo(
                                            "firebase",
                                            firebaseUser.getUid(),
                                            username,
                                            firebaseUser.getEmail()
                                    );
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "failed adding username to user");
                                }
                            });

                            //add new user to firestore "users" collection
                            User newUser = new User(username, email, firebaseUser.getUid());
                            db.collection("users")
                                    .document(firebaseUser.getUid())
                                    .set(newUser)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            firebaseUser.sendEmailVerification()
                                                    .addOnSuccessListener(r -> {
                                                        waitForEmailVerification();
                                                        binding.loginProgressBar.setVisibility(View.GONE);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "error sending email verification: " + e.toString());
                                                        binding.loginProgressBar.setVisibility(View.GONE);
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "failed adding new user to firestore: " + e.toString());
                                            binding.loginProgressBar.setVisibility(View.GONE);
                                        }
                                    });

                        } else {
                            Log.e(TAG, "createUserWithEmail:failure", task.getException());
                            Utils.showToast(LoginActivity.this, "This email address is invalid or already in use.");
                            binding.loginProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void onGoToMapClick(View v) {
        finish();
    }

    public void onResendEmailVerificationClick(View v) {
        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification()
            .addOnSuccessListener(r -> {
                Utils.showToast(LoginActivity.this, "Verification email sent.");
            })
            .addOnFailureListener(e -> Log.e(TAG, "error sending ver email: " + e.toString()));
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
                    emailVerificationCheckRunnable.postDelayed(this, EMAIL_VERIFICATION_CHECK_COOLDOWN);
                }
            }
        }, EMAIL_VERIFICATION_CHECK_COOLDOWN);
    }

    public void onBackButtonClicked(View v) { finish(); }

    public void facebookLoginClicked(View v) {
        Log.i(TAG, "facebook button clicked");
    }

    private void setupFacebookButtonCallback() {
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {

                        db.collection("users")
                                .document(loginResult.getAccessToken().getUserId())
                                .get()
                                .addOnSuccessListener(res -> {
                                    User user = res.toObject(User.class);

                                    if (user != null) {
                                        Log.i(TAG, "existing user authenticated. userid is " + user.getUid() + ", username is " + user.getUsername());
                                        updateStoredUserInfo(
                                                "facebook",
                                                user.getUid(),
                                                user.getUsername(),
                                                user.getEmail()
                                        );
                                        finish();
                                    } else {
                                        Log.i(TAG, "creating a new user...");
                                        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), (object, response) -> {
                                            String name;
                                            String email;
                                            String id;
                                            try {
                                                name = object.getString("name");
                                                email = object.getString("email");
                                                id = object.getString("id");
                                                User newUser = new User(name, email, id);

                                                db.collection("users")
                                                        .document(id)
                                                        .set(newUser)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.i(TAG, "made new user object in firestore");
                                                                updateStoredUserInfo(
                                                                        "facebook",
                                                                        id,
                                                                        name,
                                                                        email
                                                                );
                                                                finish();
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.e(TAG, "failed adding new user to firestore: " + e.toString());
                                                            }
                                                        });

                                            } catch (JSONException e) {
                                                Log.e(TAG, "error calling graph: " + e.toString());
                                            }
                                        });

                                        Bundle parameters = new Bundle();
                                        parameters.putString("fields", "id, name, email");
                                        request.setParameters(parameters);
                                        request.executeAsync();
                                    }


                                });
                    }
                    @Override
                    public void onCancel() {
                        // App code
                    }
                    @Override
                    public void onError(FacebookException exception) {
                        Log.e(TAG, "error registering facebook callback: " + exception.toString());
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateStoredUserInfo(String loginType, String userId, String username, String userEmail) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("loginType", loginType);
        editor.putString("userId", userId);
        editor.putString("username", username);
        editor.putString("userEmail", userEmail);
        editor.apply();
        Log.i(TAG, "updated shared prefs");
    }
}

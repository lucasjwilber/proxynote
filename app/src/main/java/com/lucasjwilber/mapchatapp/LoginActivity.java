package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucasjwilber.mapchatapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private boolean loginShown = true;
    private static final int GOOGLE_SIGN_IN = 600613;
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;
    private final int EMAIL_VERIFICATION_CHECK_COOLDOWN = 2000;
    private Handler emailVerificationCheckRunnable;
    boolean waitingForEmailVerification;
    private CallbackManager mCallbackManager;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        //autofill email ET if it was saved
        sharedPreferences = getApplicationContext().getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString("email", "");
        binding.loginActEmailEditText.setText(savedEmail);

        emailVerificationCheckRunnable = new Handler();

        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = binding.facebookLoginButton;
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                // ...
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                // ...
            }
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        SignInButton googleLoginButton = binding.googleLoginButton;
        googleLoginButton.setStyle(SignInButton.SIZE_WIDE, SignInButton.COLOR_LIGHT);
        googleLoginButton.setOnClickListener(v -> googleSignIn());
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
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        user = mAuth.getCurrentUser();
                        binding.loginProgressBar.setVisibility(View.GONE);
                        sharedPreferences.edit().putString("loginType", "firebase").apply();
                        finish();
                    } else {
                        Log.e(TAG, "signInWithEmail:failure", task.getException());
                        Utils.showToast(LoginActivity.this, "Username or password incorrect.");
                        binding.loginProgressBar.setVisibility(View.GONE);
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
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        user = mAuth.getCurrentUser();

                        // set the user's display name (username). this can't be done while creating the user unfortunately.
                        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                        user.updateProfile(userProfileChangeRequest)
                                .addOnSuccessListener(aVoid -> {
                                    createNewUser(user.getDisplayName(), user.getEmail(), user.getUid());
                                    sharedPreferences.edit().putString("loginType", "firebase").apply();
                                })
                                .addOnFailureListener(e -> Log.i(TAG, "failed adding username to user"));

                        user.sendEmailVerification()
                            .addOnSuccessListener(r -> waitForEmailVerification())
                            .addOnFailureListener(e -> Log.e(TAG, "error sending email verification: " + e.toString()));

                    } else {
                        Log.e(TAG, "createUserWithEmail:failure", task.getException());
                        Utils.showToast(LoginActivity.this, "This email address is invalid or already in use.");
                        binding.loginProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    public void onGoToMapClick(View v) {
        finish();
    }

    public void onResendEmailVerificationClick(View v) {
        if (user != null) {
            user.sendEmailVerification()
            .addOnSuccessListener(r -> Utils.showToast(LoginActivity.this, "Verification email sent."))
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.e(TAG, "Google sign in failed", e);
            }
        } else {
            // If it wasn't Google, it's Facebook. Pass the activity result back to the Facebook SDK
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
        binding.loginProgressBar.setVisibility(View.VISIBLE);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.i(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        signInWithCredential(credential);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        signInWithCredential(credential);
    }

    private void signInWithCredential(AuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.i(TAG, "signInWithCredential:success");
                        user = mAuth.getCurrentUser();
                        Log.i(TAG, user.getDisplayName() + ", " + user.getUid() + ", " + user.getEmail() + ", " + user.isEmailVerified());
                        db.collection("users")
                                .document(user.getUid())
                                .get()
                                .addOnSuccessListener(res -> {
                                    User userObj = res.toObject(User.class);
                                    if (userObj != null) {
                                        Log.i(TAG, "existing user authenticated. userid is " + userObj.getUid() + ", username is " + userObj.getUsername());
                                        finish();
                                    } else {
                                        Log.i(TAG, "creating a new user...");
                                        createNewUser(user.getDisplayName(), user.getEmail(), user.getUid());
                                    }
                                });
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                    }
                });
    }

    private void createNewUser(String username, String email, String userId) {
        User newUser = new User(username, email, userId);
        db.collection("users")
                .document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "new user created in users collection");
                    binding.loginProgressBar.setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "failed adding new user to firestore: " + e.toString());
                    binding.loginProgressBar.setVisibility(View.GONE);
                });
    }

}

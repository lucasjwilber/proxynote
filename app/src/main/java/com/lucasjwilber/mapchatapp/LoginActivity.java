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
    private static final int GOOGLE_SIGN_IN = 6006;
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;
    private final int EMAIL_VERIFICATION_CHECK_COOLDOWN = 2000;
    private Handler emailVerificationCheckRunnable;
    private boolean waitingForEmailVerification;
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
        binding.loginEmailET.setText(savedEmail);

        emailVerificationCheckRunnable = new Handler();

        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = binding.loginWithFacebookBtn;
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.i(TAG, "facebook:onSuccess:" + loginResult);
                binding.loginPB.setVisibility(View.VISIBLE);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }
            @Override
            public void onCancel() {
                Log.i(TAG, "facebook:onCancel");
            }
            @Override
            public void onError(FacebookException error) {
                Log.i(TAG, "facebook:onError", error);
            }
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        SignInButton googleLoginButton = binding.loginWithGoogleBtn;
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
        binding.loginShowLoginBtn.setTextColor(getResources().getColor(R.color.white));
        binding.loginShowSignupBtn.setTextColor(getResources().getColor(R.color.whiteOpaque));
        binding.loginSubmitBtn.setText(submitBtnText);
//        binding.loginUsernameLabel.setVisibility(View.GONE);
        binding.loginUsernameET.setVisibility(View.GONE);
//        binding.loginConfirmPasswordCLabel.setVisibility(View.GONE);
//        binding.loginConfirmPasswordPLabel.setVisibility(View.GONE);
        binding.loginConfirmPasswordET.setVisibility(View.GONE);
    }
    public void signupButtonClicked(View v) {
        loginShown = false;
        String submitBtnText = "SIGN UP";
        binding.loginSubmitBtn.setText(submitBtnText);
        binding.loginShowSignupBtn.setTextColor(getResources().getColor(R.color.white));
        binding.loginShowLoginBtn.setTextColor(getResources().getColor(R.color.whiteOpaque));
//        binding.loginUsernameLabel.setVisibility(View.VISIBLE);
        binding.loginUsernameET.setVisibility(View.VISIBLE);
//        binding.loginConfirmPasswordCLabel.setVisibility(View.VISIBLE);
//        binding.loginConfirmPasswordPLabel.setVisibility(View.VISIBLE);
        binding.loginConfirmPasswordET.setVisibility(View.VISIBLE);
    }

    public void onSubmitButtonClicked(View v) {
        if (loginShown) {
            loginSubmit();
        } else {
            signupSubmit();
        }
    }

    public void loginSubmit() {
        String email = binding.loginEmailET.getText().toString();
        String password = binding.loginPasswordET.getText().toString();
        if (email.equals("") || email.length() == 0 || password.equals("") || password.length() == 0) {
            Utils.showToast(LoginActivity.this, "Please enter your email and password.");
            return;
        }

        binding.loginPB.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        user = mAuth.getCurrentUser();
                        binding.loginPB.setVisibility(View.GONE);
                        sharedPreferences.edit().putString("loginType", "firebase").apply();
                        finish();
                    } else {
                        Log.e(TAG, "signInWithEmail:failure", task.getException());
                        Utils.showToast(LoginActivity.this, "Username or password incorrect.");
                        binding.loginPB.setVisibility(View.GONE);
                    }
                });
    }

    public void signupSubmit() {
        String username = binding.loginUsernameET.getText().toString();
        String email = binding.loginEmailET.getText().toString();
        String password = binding.loginPasswordET.getText().toString();
        String confirmedPassword = binding.loginConfirmPasswordET.getText().toString();

        if (username.equals("") || username.length() == 0 ||
        email.equals("") || email.length() == 0 ||
        password.equals("") || password.length() == 0 ||
        confirmedPassword.equals("") || confirmedPassword.length() == 0
        ) {
            Utils.showToast(LoginActivity.this, "Please fill out all fields.");
            return;
        } else if (!password.equals(confirmedPassword)) {
            Utils.showToast(LoginActivity.this, "Passwords do not match.");
            return;
        }

        binding.loginPB.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        user = mAuth.getCurrentUser();

                        // set the user's display name (username). this can't be done while creating the user unfortunately.
                        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                        user.updateProfile(userProfileChangeRequest)
                                .addOnSuccessListener(aVoid -> {
                                    createNewUser(user.getDisplayName(), user.getUid(), false);
                                    sharedPreferences.edit().putString("loginType", "firebase").apply();
                                })
                                .addOnFailureListener(e -> Log.i(TAG, "failed adding username to user"));

                        user.sendEmailVerification()
                            .addOnSuccessListener(r -> waitForEmailVerification())
                            .addOnFailureListener(e -> Log.e(TAG, "error sending email verification: " + e.toString()));

                    } else {
                        Log.e(TAG, "createUserWithEmail:failure", task.getException());
                        Utils.showToast(LoginActivity.this, "This email address is invalid or already in use.");
                        binding.loginPB.setVisibility(View.GONE);
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
        binding.loginBackBtn.setVisibility(View.GONE);
        binding.loginEmailVerificationModal.setVisibility(View.VISIBLE);
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
        binding.loginPB.setVisibility(View.VISIBLE);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.i(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        signInWithCredential(credential, "facebook");
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        signInWithCredential(credential, "google");
    }

    private void signInWithCredential(AuthCredential credential, String loginType) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.i(TAG, "signInWithCredential:success");
                        sharedPreferences.edit().putString("loginType", loginType).apply();
                        user = mAuth.getCurrentUser();
                        db.collection("users")
                                .document(user.getUid())
                                .get()
                                .addOnSuccessListener(res -> {
                                    User userObj = res.toObject(User.class);
                                    if (userObj != null) {
                                        Log.i(TAG, "existing user authenticated. userid is " + userObj.getUid() + ", username is " + userObj.getUsername());
                                        binding.loginPB.setVisibility(View.GONE);
                                        finish();
                                    } else {
                                        Log.i(TAG, "creating a new user...");
                                        showChooseUsernameModal();
                                        binding.loginPB.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "error adding new user to db: " + e.toString());
                                    binding.loginPB.setVisibility(View.GONE);
                                });
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Utils.showToast(LoginActivity.this, "There was a problem signing in.");
                        binding.loginPB.setVisibility(View.GONE);
                    }
                });
    }

    private void showChooseUsernameModal() {
        binding.loginChooseUsernameET.setText(user.getDisplayName());
        binding.loginChooseUsernameModal.setVisibility(View.VISIBLE);
    }

    public void onUsernameSelected(View v) {
        binding.loginChooseUsernamePB.setVisibility(View.VISIBLE);
        String username = binding.loginChooseUsernameET.getText().toString();

        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
        user.updateProfile(userProfileChangeRequest)
                .addOnSuccessListener(aVoid -> {
                    createNewUser(username, user.getUid(), true);
                    sharedPreferences.edit().putString("loginType", "firebase").apply();
                })
                .addOnFailureListener(e -> Log.i(TAG, "failed adding username to user"));
    }

    private void createNewUser(String username, String userId, boolean finishOnComplete) {
        User newUser = new User(username, userId);
        db.collection("users")
                .document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "new user created in users collection");
                    binding.loginPB.setVisibility(View.GONE);
                    binding.loginChooseUsernamePB.setVisibility(View.GONE);
                    if (finishOnComplete) finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "failed adding new user to firestore: " + e.toString());
                    binding.loginPB.setVisibility(View.GONE);
                    binding.loginChooseUsernamePB.setVisibility(View.GONE);
                });
    }


}

package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReportActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private String postId;
    private String postUserId;
    private String postTitle;
    private String postText;
    private String postMediaStorageId;
    private double postLat;
    private double postLng;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        loadingSpinner = findViewById(R.id.reportProgressBar);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        postUserId = intent.getStringExtra("postUserId");
        postTitle = intent.getStringExtra("postTitle");
        postText = intent.getStringExtra("postText");
        postMediaStorageId = intent.getStringExtra("postMediaStorageId");
        postLat = intent.getDoubleExtra("postLat", postLat);
        postLng = intent.getDoubleExtra("postLng", postLng);
    }

    public void onReportSubmit(View v) {
        if (!Utils.isUserAuthorized()) {
            Utils.showToast(ReportActivity.this, "Please log in or verify your email first.");
            return;
        }

        RadioGroup reportTypeRG = findViewById(R.id.reportTypeRG);
        String reason;
        switch (reportTypeRG.getCheckedRadioButtonId()) {
            case R.id.reportRBspam:
                reason = "spam";
                break;
            case R.id.reportRBpersonalInfo:
                reason = "personal info";
                break;
            case R.id.reportRBthreat:
                reason = "threat";
                break;
            case R.id.reportRBvulgar:
                reason = "vulgar";
                break;
            case R.id.reportRBother:
                reason = "other";
                break;
            case -1:
            default:
                Utils.showToast(ReportActivity.this, "Please select a reason.");
                return;
        }

        loadingSpinner.setVisibility(View.VISIBLE);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);
        EditText additionalInfoTV = findViewById(R.id.reportAdditionalText);
        String additionalInfo = additionalInfoTV.getText().toString();
        String userId = user == null ? "unknown user" : user.getUid();
        String reportId = postId + "|" + userId;
        Report report = new Report(reportId, reason, additionalInfo, postId, postUserId, postTitle, postText, postMediaStorageId, postLat, postLng);

        db.collection("reports")
                .document(reportId)
                .set(report)
                .addOnSuccessListener(success -> {
                    Utils.showToast(ReportActivity.this, "Report submitted. Thank you!");
                    loadingSpinner.setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "error uploading report: " + e.toString());
                    loadingSpinner.setVisibility(View.GONE);
                });
    }

    public void onBackButtonClicked(View v) {
        finish();
    }
}

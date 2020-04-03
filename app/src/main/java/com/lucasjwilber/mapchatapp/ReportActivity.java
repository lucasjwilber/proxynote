package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

    String postId;
    FirebaseUser currentUser;
    FirebaseFirestore db;
    ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        loadingSpinner = findViewById(R.id.reportProgressBar);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        Log.i("ljw", "on report page for post " + postId);
    }

    public void onReportSubmit(View v) {
        loadingSpinner.setVisibility(View.VISIBLE);
        RadioGroup reportTypeRG = findViewById(R.id.reportTypeRG);
        String reason;
        Log.i("ljw", "checked rb is " + reportTypeRG.getCheckedRadioButtonId());
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
                Utils.showToast(ReportActivity.this, "Please select a reason for reporting.");
                return;
        }
        EditText additionalInfoTV = findViewById(R.id.reportAdditionalText);
        String additionalInfo = additionalInfoTV.getText().toString();
        String reportId = postId + "|" + currentUser.getUid();
        Report report = new Report(reportId, reason, additionalInfo, postId);

        db.collection("reports")
                .document(reportId)
                .set(report)
                .addOnSuccessListener(success -> {
                    Log.i("ljw", "successfully added report to db");
                    Utils.showToast(ReportActivity.this, "Report submitted");
                    loadingSpinner.setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.i("ljw", "error uploading report: " + e.toString());
                    loadingSpinner.setVisibility(View.GONE);
                });
    }

    public void onBackButtonClicked(View v) {
        finish();
    }
}

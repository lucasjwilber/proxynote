package com.lucasjwilber.proxynote;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReportActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private String postId;
    private String commentId;
    private String userId;
    private String title;
    private String text;
    private String mediaStorageId;
    private double lat;
    private double lng;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private ProgressBar loadingSpinner;
    private EditText additionalInfoET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        loadingSpinner = findViewById(R.id.reportPB);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        additionalInfoET = findViewById(R.id.reportAdditionalInfoET);
        TextView additionalInfoETcounter = findViewById(R.id.reportAdditionalInfoETcounter);
        additionalInfoET.addTextChangedListener(Utils.makeTextWatcher(
                additionalInfoET,
                additionalInfoETcounter,
                200
        ));

        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        commentId = intent.getStringExtra("commentId");
        userId = intent.getStringExtra("postUserId");
        title = intent.getStringExtra("postTitle");
        text = intent.getStringExtra("postText");
        mediaStorageId = intent.getStringExtra("postMediaStorageId");
        lat = intent.getDoubleExtra("postLat",lat);
        lng = intent.getDoubleExtra("postLng", lng);
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

        String additionalInfo = additionalInfoET.getText().toString();
        String currentUserId = user == null ? "unknown user" : user.getUid();
        //report id is [post/comment that is being reported's id]|[reporting user's id]
        String reportId = commentId != null ? (commentId + "|" + currentUserId) : postId + "|" + currentUserId;
        Report report = new Report(reportId, reason, additionalInfo, postId, commentId, userId, title, text, mediaStorageId, lat, lng);

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

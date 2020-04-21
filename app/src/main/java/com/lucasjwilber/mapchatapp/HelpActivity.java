package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucasjwilber.mapchatapp.databinding.ActivityHelpBinding;

public class HelpActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private ActivityHelpBinding binding;
    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        if (user != null) binding.questionCommentModal.setVisibility(View.VISIBLE);

        binding.helpRv.setLayoutManager(new LinearLayoutManager(this));
        binding.helpRv.setAdapter(new HelpRvAdapter());
    }

    public void onSubmitQuestionOrCommentButtonClicked(View v) {
        String text = binding.helpCommentBox.getText().toString();
        String userId = user != null ? user.getUid() : "unknown";
        String userEmail = user != null ? user.getEmail() : "unknown";
        QuestionOrComment qoc = new QuestionOrComment(text, userId, userEmail);

        db.collection("questionsAndComments")
                .add(qoc)
                .addOnSuccessListener(s -> {
                    binding.helpCommentBox.setText("");
                    Utils.showToast(this, "Submitted! Thank you.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "error submitting question/comment: " + e.toString()));
    }

    public void onBackButtonClicked(View v) {
        finish();
    }



    public class HelpRvAdapter extends RecyclerView.Adapter<HelpRvAdapter.HelpViewHolder> {

        int[] helpStringIds = new int[]{
                R.string.general_information,
                R.string.gi1,
                R.string.gi2,
                R.string.gi4,
                R.string.gi5,
                R.string.gi6,
                R.string.gi7,
                R.string.faq,
                R.string.q1,
                R.string.a1,
                R.string.q3,
                R.string.a3, //email verification cl
                R.string.q2,
                R.string.a2,
        };

        int positionOfResendEmailCL = 11;

        // this is going to be entirely hardcoded so there's no need to parameterize the adapter
        HelpRvAdapter() {}

        class HelpViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ConstraintLayout constraintLayout;
            HelpViewHolder(TextView tv) {
                super(tv);
                textView = tv;
            }
            HelpViewHolder(ConstraintLayout cl) {
                super(cl);
                constraintLayout = cl;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position != positionOfResendEmailCL)
                return 0;
            else
                return 1;
        }

        @NonNull
        @Override
        public HelpRvAdapter.HelpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                TextView tv = (TextView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.help_textview, parent, false);

                return new HelpViewHolder(tv);
            } else {
                ConstraintLayout cl = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.help_textview_with_button, parent, false);
                return new HelpViewHolder(cl);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull HelpViewHolder holder, int position) {
            if (position != positionOfResendEmailCL) holder.textView.setText(getString(helpStringIds[position]));

            if (position == 0 || position == 7) {
                holder.textView.setTextSize(20);
            //bold FAQ questions:
            } else if (position != positionOfResendEmailCL && position > 7 && position % 2 == 0) {
                holder.textView.setTypeface(holder.textView.getTypeface(), Typeface.BOLD_ITALIC);
            }

            //the resend email textview/button:
            if (position == positionOfResendEmailCL) {
                TextView tv = holder.constraintLayout.findViewById(R.id.helpResendEmailTv);
                tv.setText(getString(helpStringIds[position]));
                Button b = holder.constraintLayout.findViewById(R.id.helpResendEmailButton);
                b.setOnClickListener(x -> resendVerificationEmail());
            }
        }

        @Override
        public int getItemCount() {
            return helpStringIds.length;
        }
    }

    private void resendVerificationEmail() {
        if (user != null) user.reload();

        if (user != null && !user.isEmailVerified()) {
            user.sendEmailVerification()
                    .addOnSuccessListener(r -> {
                        Utils.showToast(HelpActivity.this, "Verification email sent.");
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "error sending ver email: " + e.toString()));
        } else if (user == null) {
            Utils.showToast(HelpActivity.this, "Please sign up or log in first.");
        } else {
            Utils.showToast(HelpActivity.this, "Your account is already verified.");
        }
    }
}

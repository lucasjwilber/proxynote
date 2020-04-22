package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
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
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private static final int TEXT_VIEW = 0;
    private static final int CONSTRAINT_LAYOUT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getApplicationContext().getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);

        if (Utils.checkUserAuthorization()) {
            binding.questionCommentModal.setVisibility(View.VISIBLE);
        }

        binding.helpRv.setLayoutManager(new LinearLayoutManager(this));
        binding.helpRv.setAdapter(new HelpRvAdapter());
    }

    public void onSubmitQuestionOrCommentButtonClicked(View v) {
        if (!Utils.checkUserAuthorization()) {
            Utils.showToast(HelpActivity.this, "Please log in or verify your email first.");
        }

        String text = binding.helpCommentBox.getText().toString();
        String userId = sharedPreferences.getString("userId", "user id unknown");
        String userEmail = sharedPreferences.getString("userEmail", "user email unknown");
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

        int[] helpStringIds;
        boolean showResendEmailTip;
        int positionOfResendEmailCL = 11;

        // this is going to be entirely hardcoded so there's no need to parameterize the adapter
        HelpRvAdapter() {
            //firebase user that hasn't verified their email:
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (sharedPreferences.getString("loginType", "none").equals("firebase") &&
                    firebaseUser != null &&
                    !firebaseUser.isEmailVerified()
            ) {
                showResendEmailTip = true;
                helpStringIds = new int[]{
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
                        R.string.q3, //email verification question textview
                        R.string.a3, //email verification answer constraintLayout
                        R.string.q2,
                        R.string.a2,
                };
            } else {
                helpStringIds = new int[]{
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
                        R.string.q2,
                        R.string.a2,
                };
            }
        }

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
            if (position == positionOfResendEmailCL && showResendEmailTip)
                return CONSTRAINT_LAYOUT;
            else
                return TEXT_VIEW;
        }

        @NonNull
        @Override
        public HelpRvAdapter.HelpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TEXT_VIEW) {
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
            if (showResendEmailTip) {
                if (position != positionOfResendEmailCL) {
                    holder.textView.setText(getString(helpStringIds[position]));
                } else {
                    TextView tv = holder.constraintLayout.findViewById(R.id.helpResendEmailTv);
                    tv.setText(getString(helpStringIds[position]));
                    Button b = holder.constraintLayout.findViewById(R.id.helpResendEmailButton);
                    b.setOnClickListener(x -> resendVerificationEmail());
                }
            } else {
                holder.textView.setText(getString(helpStringIds[position]));
            }

            //"General Information" and "FAQ" headers:
            if (position == 0 || position == 7) {
                holder.textView.setTextSize(20);
            //bold the questions in the FAQ section:
            } else if (position != positionOfResendEmailCL && position > 7 && position % 2 == 0) {
                holder.textView.setTypeface(holder.textView.getTypeface(), Typeface.BOLD_ITALIC);
            }
        }

        @Override
        public int getItemCount() {
            return helpStringIds.length;
        }
    }

    private void resendVerificationEmail() {
        if (Utils.checkUserAuthorization()) {
            Utils.showToast(HelpActivity.this, "Your account is already verified.");
        } else {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null) {
                Log.e(TAG, "user's firebase auth instance is null, but the 'resend verification email' button was shown");
            } else {
                firebaseUser.sendEmailVerification()
                        .addOnSuccessListener(r -> {
                            Utils.showToast(HelpActivity.this, "Verification email sent.");
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "error sending ver email: " + e.toString()));
            }
        }
    }
}

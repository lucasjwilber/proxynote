package com.lucasjwilber.proxynote;

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
import com.lucasjwilber.proxynote.databinding.ActivityHelpBinding;

public class HelpActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private ActivityHelpBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser user;
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
        user = FirebaseAuth.getInstance().getCurrentUser();
        sharedPreferences = getApplicationContext().getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);

        if (Utils.isUserAuthorized()) {
            binding.helpQuestionOrCommentContainer.setVisibility(View.VISIBLE);
        }

        binding.helpRV.setLayoutManager(new LinearLayoutManager(this));
        binding.helpRV.setAdapter(new HelpRvAdapter());
        binding.helpRV.setHasFixedSize(true);

        binding.helpQuestionOrCommentET.addTextChangedListener(Utils.makeTextWatcher(
                binding.helpQuestionOrCommentET,
                binding.helpQuestionOrCommentETcounter,
                200
        ));
    }

    public void onSubmitQuestionOrCommentButtonClicked(View v) {
        if (!Utils.isUserAuthorized()) {
            Utils.showToast(HelpActivity.this, "Please log in or verify your email first.");
            return;
        }

        String text = binding.helpQuestionOrCommentET.getText().toString();
        if (text.equals("") || text.length() == 0) {
            Utils.showToast(HelpActivity.this, "Please write something first.");
            return;
        }
        String userId = user.getUid();
        String userEmail = user.getEmail();
        QuestionOrComment qoc = new QuestionOrComment(text, userId, userEmail);

        db.collection("questionsAndComments")
                .add(qoc)
                .addOnSuccessListener(s -> {
                    binding.helpQuestionOrCommentET.setText("");
                    Utils.showToast(this, "Submitted! Thank you.");
                    binding.helpQuestionOrCommentContainer.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> Log.e(TAG, "error submitting question/comment: " + e.toString()));
    }

    public void onBackButtonClicked(View v) {
        finish();
    }



    public class HelpRvAdapter extends RecyclerView.Adapter<HelpRvAdapter.HelpViewHolder> {

        int[] helpStringIds;
        boolean showResendEmailTip;
        int positionOfResendEmailCL = 9;

        // this is going to be entirely hardcoded so there's no need to parameterize the adapter
        HelpRvAdapter() {
            //firebase user that hasn't verified their email:
            if (sharedPreferences.getString("loginType", "none").equals("firebase") &&
                    user != null &&
                    !user.isEmailVerified()
            ) {
                showResendEmailTip = true;
                helpStringIds = new int[]{
                        R.string.general_information,
                        R.string.gi1,
                        R.string.gi2,
                        R.string.gi4,
                        R.string.gi5,
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
                    TextView tv = holder.constraintLayout.findViewById(R.id.helpResendEmailTV);
                    tv.setText(getString(helpStringIds[position]));
                    Button b = holder.constraintLayout.findViewById(R.id.helpResendEmailBtn);
                    b.setOnClickListener(x -> resendVerificationEmail());
                }
            } else {
                holder.textView.setText(getString(helpStringIds[position]));
            }

            //"General Information" and "FAQ" headers:
            if (position == 0 || position == 5) {
                holder.textView.setTextSize(20);
            //bold the questions in the FAQ section:
            } else if (position != positionOfResendEmailCL && position > 5 && position % 2 == 0) {
                holder.textView.setTypeface(holder.textView.getTypeface(), Typeface.BOLD_ITALIC);
            }
        }

        @Override
        public int getItemCount() {
            return helpStringIds.length;
        }
    }

    private void resendVerificationEmail() {
        if (user != null && user.isEmailVerified()) {
            Utils.showToast(HelpActivity.this, "Your account is already verified.");
        } else {
            if (user == null || user.isAnonymous()) {
                Log.e(TAG, "user's firebase auth instance is null, but the 'resend verification email' button was shown");
            } else {
                user.sendEmailVerification()
                        .addOnSuccessListener(r -> {
                            Utils.showToast(HelpActivity.this, "Verification email sent.");
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "error sending ver email: " + e.toString()));
            }
        }
    }
}

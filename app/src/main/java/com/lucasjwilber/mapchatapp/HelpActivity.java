package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucasjwilber.mapchatapp.databinding.ActivityHelpBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HelpActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private ActivityHelpBinding binding;
    private FirebaseUser user;
    private FirebaseFirestore db;
//    private RecyclerView helpRv;
//    private RecyclerView.Adapter helpRvAdapter;


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
                    Log.i(TAG, "question/comment submitted");
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
                R.string.tip1,
                R.string.tip2,
                R.string.tip3,
        };

        // this is going to be entirely hardcoded so there's no need to parameterize the adapter
        HelpRvAdapter() {}

        class HelpViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            HelpViewHolder(TextView tv) {
                super(tv);
                textView = tv;
            }
        }

        @NonNull
        @Override
        public HelpRvAdapter.HelpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.help_textview, parent, false);

            return new HelpViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull HelpViewHolder holder, int position) {
            holder.textView.setText(getString(helpStringIds[position]));
            if (position == 0) holder.textView.setTextSize(20);
        }

        @Override
        public int getItemCount() {
            return helpStringIds.length;
        }

    }
}

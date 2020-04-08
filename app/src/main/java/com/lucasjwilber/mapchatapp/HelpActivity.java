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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucasjwilber.mapchatapp.databinding.ActivityHelpBinding;

import java.util.List;
import java.util.UUID;

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

//        binding.helpRv.setLayoutManager(new LinearLayoutManager(this));
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

//    public class HelpRvAdapter extends RecyclerView.Adapter<HelpRvAdapter.HelpRvSectionViewHolder> {
//
//        ConstraintLayout[] layouts = new ConstraintLayout[]{
//
//        };
//
//        public HelpRvAdapter() {
//
//        }
//
//        public class HelpRvSectionViewHolder extends RecyclerView.ViewHolder {
//
//            HelpRvSectionViewHolder(@NonNull View itemView) {
//                super(itemView);
//            }
//        }
//
//        @NonNull
//        @Override
//        public HelpRvSectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            ConstraintLayout l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.post_descriptor_layout, parent, false);
//
//            return new HelpRvAdapter.HelpRvSectionViewHolder(l);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull HelpRvSectionViewHolder holder, int position) {
//
//        }
//
//        @Override
//        public int getItemCount() {
//            return 0;
//        }
//
//    }
}

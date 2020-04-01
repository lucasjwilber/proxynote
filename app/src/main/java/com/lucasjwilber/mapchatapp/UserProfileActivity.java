package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lucasjwilber.mapchatapp.databinding.ActivityUserProfileBinding;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private String thisProfileOwnerId;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private ActivityUserProfileBinding binding;
    private RecyclerView.Adapter postDescriptorsRvAdapter;
    private RecyclerView.LayoutManager postDescriptorsRvLayoutManager;
    private RecyclerView.Adapter postRvAdapter;
    private ConstraintLayout selectedDV;
    private Button selectedDVdelBtn;
    private String selectedPostId;
    private boolean userIsOnTheirOwnProfile;
    private boolean aboutMeBeingEdited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.profileOnePostRv.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        thisProfileOwnerId = intent.getStringExtra("userId");
        Log.i("ljw", "userId is " + thisProfileOwnerId);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && thisProfileOwnerId.equals(currentUser.getUid())) {
            userIsOnTheirOwnProfile = true;
            binding.aboutMeEditBtn.setVisibility(View.VISIBLE);
        }

        if (thisProfileOwnerId != null) {
            binding.postDescRvProgressBar.setVisibility(View.VISIBLE);
            db.collection("users")
                    .document(thisProfileOwnerId)
                    .get()
                    .addOnSuccessListener(result -> {
                        Log.i("ljw", "successfully got user:\n" + result.toString());
                        User user = result.toObject(User.class);
                        assert user != null;
                        binding.profileUsername.setText(user.getUsername());
                        String userScoreText = "(" +
                                user.getTotalScore() +
                                (user.getTotalScore() == 1 || user.getTotalScore() == -1 ? " point)" : " points)");
                        binding.profileScore.setText(userScoreText);
                        binding.aboutMeTV.setText(user.getAboutme());
                        List<PostDescriptor> userPostDescriptors = user.getPostDescriptors();

                        if (userPostDescriptors == null || userPostDescriptors.size() == 0) {
                            Log.i("ljw", "user hasn't made any posts, or possibly doesn't have a postDescriptors list");
                            String noPostsText = user.getUsername() + " hasn't made any posts yet.";
                            binding.profileNoCommentsYet.setText(noPostsText);
                            binding.profileNoCommentsYet.setVisibility(View.VISIBLE);
                        } else {
                            postDescriptorsRvLayoutManager = new LinearLayoutManager(this);
                            binding.profileAllPostsRv.setLayoutManager(postDescriptorsRvLayoutManager);
                            postDescriptorsRvAdapter = new PostSelectAdapter(userPostDescriptors);
                            binding.profileAllPostsRv.setAdapter(postDescriptorsRvAdapter);
                        }
                        binding.postDescRvProgressBar.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        Log.i("ljw", "error getting user: " + e.toString());
                        binding.postDescRvProgressBar.setVisibility(View.GONE);
                    });
        }
    }

    //list of post descriptors RV:
    public class PostSelectAdapter extends RecyclerView.Adapter<PostSelectAdapter.PostTitleViewholder> {

        List<PostDescriptor> userPostDescriptors;

        public PostSelectAdapter(List<PostDescriptor> userPostDescriptors) {
            this.userPostDescriptors = userPostDescriptors;
        }


        public class PostTitleViewholder extends RecyclerView.ViewHolder {
            ConstraintLayout constraintLayout;

            PostTitleViewholder(ConstraintLayout view) {
                super(view);
                constraintLayout = view;
            }

        }

        @Override
        public PostTitleViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
            ConstraintLayout l;

            l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.post_descriptor_layout, parent, false);
            return new PostSelectAdapter.PostTitleViewholder(l);
        }

        @Override
        public void onBindViewHolder(PostTitleViewholder holder, int position) {

            PostDescriptor data = userPostDescriptors.get(position);
            int score = data.getScore();
            String title = data.getTitle();
            int icon = data.getIcon();
            long time = data.getTimestamp();
            String location = data.getLocation();


            ImageView iconView = holder.constraintLayout.findViewById(R.id.postdescriptorIcon);
            TextView scoreView = holder.constraintLayout.findViewById(R.id.postdescriptorScore);
            TextView timeAndLocationView = holder.constraintLayout.findViewById(R.id.postdescriptorTimeAndLocation);
            TextView titleView = holder.constraintLayout.findViewById(R.id.postdescriptorTitle);
            Button deleteButton = holder.constraintLayout.findViewById(R.id.deletePostButton);
            ConstraintLayout cl = holder.constraintLayout;
            holder.constraintLayout.setOnClickListener(v -> onPostDescriptorClicked(cl, deleteButton));

            iconView.setImageBitmap(Utils.getPostIconBitmap(icon, getApplicationContext()));
            if (score >= 20) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_red));
            } else if (score >= 15) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_orangered));
            } else if (score >= 10) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_orange));
            } else if (score >= 5) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_yelloworange));
            } else if (score <= -5) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_brown));
            } else {
                iconView.setBackground(getDrawable(R.drawable.postoutline_yellow));
            }

            scoreView.setText(Integer.toString(score));
            // location removed pending geocode implementation
//            String timeAndLocationText = new Date(time) + ", " + location;
            String timeAndLocationText = new Date(time).toString();
            timeAndLocationView.setText(timeAndLocationText);
            titleView.setText(title);

            holder.constraintLayout.setTag(data.getId());
        }

        @Override
        public int getItemCount() {
            return userPostDescriptors.size();
        }

        public void onPostDescriptorClicked(ConstraintLayout cl, Button b) {
            Log.i("ljw", "clicked on post " + cl.getTag());
            selectedPostId = cl.getTag().toString();
            binding.profileOnePostRv.setVisibility(View.GONE);
            binding.postRvProgressBar.setVisibility(View.VISIBLE);

            if (selectedDV != null) {
                selectedDV.setBackground(null);
                selectedDVdelBtn.setVisibility(View.GONE);
            }
            selectedDV = cl;
            selectedDVdelBtn = b;
            cl.setBackgroundColor(getResources().getColor(R.color.lightgray));

            if (userIsOnTheirOwnProfile) b.setVisibility(View.VISIBLE);
                db.collection("posts")
                        .document(selectedPostId)
                        .get()
                        .addOnSuccessListener(response -> {
                            Log.i("ljw", "got post!");
                            Post post = response.toObject(Post.class);
                            assert post != null;
                            Log.i("ljw", "found post " + post.getId());
                            ArrayList list = (ArrayList) response.getData().get("comments");
                            post.setComments(Utils.turnMapsIntoListOfComments(list));

//                            cachedPosts.put(selectedPostId, post);
                            postRvAdapter = new PostRvAdapter(
                                    post,
                                    UserProfileActivity.this,
                                    currentUser != null ? currentUser.getUid() : null,
                                    currentUser != null ? currentUser.getDisplayName() : null,
//                                    binding.profileOnePostRv,
                                    db);
                            binding.profileOnePostRv.setAdapter(postRvAdapter);
                            binding.profileOnePostRv.setBackground(getResources().getDrawable(R.drawable.rounded_square_black));
                            binding.profileOnePostRv.setVisibility(View.VISIBLE);
                            binding.postRvProgressBar.setVisibility(View.GONE);

                        })
                        .addOnFailureListener(e -> {
                            Log.i("ljw", "error getting post: " + e.toString());
                            binding.postRvProgressBar.setVisibility(View.GONE);
                        });
//            }
        }

    }

    public void onDeleteButtonClick(View v) {
        if (!userIsOnTheirOwnProfile) return;
        binding.deletePostModal.setVisibility(View.VISIBLE);
    }
    public void yesDelete(View v) {
        if (!userIsOnTheirOwnProfile) return;
        v.setEnabled(false);
        binding.deletePostProgressBar.setVisibility(View.VISIBLE);

        db.collection("posts")
                .document(selectedPostId)
                .delete()
                .addOnSuccessListener(result -> {
                    Log.i("ljw", "post deleted!");

                    //delete post descriptor
                    db.collection("users")
                            .document(thisProfileOwnerId)
                            .get()
                            .addOnSuccessListener(result2 -> {
                                Log.i("ljw", "got user to delete their postdescriptor");
                                User user = result2.toObject(User.class);
                                List<PostDescriptor> usersNewPDs = new ArrayList<>();
                                int postScore = 0;
                                assert user != null;
                                for (PostDescriptor pd : user.getPostDescriptors()) {
                                    if (!pd.getId().equals(selectedPostId)) {
                                        usersNewPDs.add(pd);
                                    } else {
                                        postScore = pd.getScore();
                                    }
                                }

                                selectedDV.removeAllViews();
                                selectedDV.setVisibility(View.GONE);
                                binding.profileOnePostRv.setAdapter(null);
                                binding.profileOnePostRv.setBackground(null);

                                db.collection("users")
                                        .document(thisProfileOwnerId)
                                        .update("postDescriptors", usersNewPDs,
                                                "totalScore", user.getTotalScore() - postScore)
                                        .addOnSuccessListener(result3 -> {
                                            Log.i("ljw", "successfully removed the deleted post's PD");
                                            binding.deletePostProgressBar.setVisibility(View.GONE);
                                            Utils.showToast(UserProfileActivity.this, "Post deleted.");
                                            v.setEnabled(true);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.i("ljw", "error removing the deleted post's PD: " + e.toString());
                                            binding.deletePostProgressBar.setVisibility(View.GONE);
                                            v.setEnabled(true);
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Log.i("ljw", "error getting user to delete this PD");
                                binding.deletePostProgressBar.setVisibility(View.GONE);
                                v.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.i("ljw", "error deleting post: " + e.toString());
                    binding.deletePostProgressBar.setVisibility(View.GONE);
                    v.setEnabled(true);
                });

        binding.deletePostModal.setVisibility(View.GONE);
    }
    public void noDelete(View v) {
        binding.deletePostModal.setVisibility(View.GONE);
    }

    public void onEditAboutmeButtonClicked(View v) {
        TextView aboutMeTV = binding.aboutMeTV;
        EditText aboutMeET = binding.aboutMeET;

        if (!userIsOnTheirOwnProfile) return;
        aboutMeBeingEdited = !aboutMeBeingEdited;
        aboutMeTV.setVisibility(aboutMeBeingEdited ? View.GONE : View.VISIBLE);
        aboutMeET.setVisibility(aboutMeBeingEdited ? View.VISIBLE : View.GONE);
        String buttonText = aboutMeBeingEdited ? "SAVE" : "EDIT";
        binding.aboutMeEditBtn.setText(buttonText);

        //on "edit" click:
        if (aboutMeBeingEdited) {
            aboutMeET.setText(aboutMeTV.getText().toString());
        }

        // on "save" click, if the text was changed:
        if (!aboutMeBeingEdited && !aboutMeET.getText().toString().equals(aboutMeTV.getText().toString())) {
            String newAboutmeText = aboutMeET.getText().toString();
            aboutMeTV.setText(newAboutmeText);
            db.collection("users")
                    .document(currentUser.getUid())
                    .update("aboutme", newAboutmeText)
                    .addOnSuccessListener(success -> {
                        Log.i("ljw", "successfully updated aboutme");
                    })
                    .addOnFailureListener(e -> {
                        Log.i("ljw", "failed updating aboutme: " + e.toString());
                    });
        }
    }

}

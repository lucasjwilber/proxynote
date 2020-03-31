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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class UserProfileActivity extends AppCompatActivity {

    User thisProfileOwner;
    String thisProfileOwnerId;
    FirebaseUser currentUser;
//    HashMap<String, Post> cachedPosts;
    FirebaseFirestore db;


    private TextView userScoreView;
    private TextView usernameView;
    private RecyclerView postDescriptorsRv;
    private RecyclerView.Adapter postDescriptorsRvAdapter;
    private RecyclerView.LayoutManager postDescriptorsRvLayoutManager;
    private RecyclerView postRv;
    private RecyclerView.Adapter postRvAdapter;
    private RecyclerView.LayoutManager postRvLayoutManager;
    ConstraintLayout selectedDV;
    Button selectedDVdelBtn;
    ConstraintLayout cl;
    String selectedPostId;
    boolean userIsOnTheirOwnProfile;
    ProgressBar postLoadingSpinner;
    ProgressBar deleteLoadingSpinner;
    TextView userProfAboutme;
    EditText userProfAboutmeEdit;
    Button userProfAboutmeEditBtn;
    boolean aboutmeBeingEdited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);


//        cachedPosts = new HashMap<>();


        postRv = findViewById(R.id.profileOnePostRv);
        postRvLayoutManager = new LinearLayoutManager(this);
        postRv.setLayoutManager(postRvLayoutManager);
        usernameView = findViewById(R.id.profileUsername);
        userScoreView = findViewById(R.id.profileScore);
        cl = findViewById(R.id.profileDeletePostModal);
        postLoadingSpinner = findViewById(R.id.profPostProgressBar);
        deleteLoadingSpinner = findViewById(R.id.profDeleteProgressBar);
        userProfAboutme = findViewById(R.id.userProfAboutme);
        userProfAboutmeEdit = findViewById(R.id.userProfAboutmeEdit);
        userProfAboutmeEditBtn = findViewById(R.id.userProfEditAboutmeEditBtn);

        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        thisProfileOwnerId = intent.getStringExtra("userId");
        Log.i("ljw", "userId is " + thisProfileOwnerId);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && thisProfileOwnerId.equals(currentUser.getUid())) {
            userIsOnTheirOwnProfile = true;
            userProfAboutmeEditBtn.setVisibility(View.VISIBLE);
        }


        if (thisProfileOwnerId != null) {
            db.collection("users")
                    .document(thisProfileOwnerId)
                    .get()
                    .addOnSuccessListener(result -> {
                        Log.i("ljw", "successfully got user:\n" + result.toString());
                        User user = result.toObject(User.class);
                        assert user != null;
                        thisProfileOwner = user;
                        usernameView.setText(user.getUsername());
                        String userScoreText = "(" +
                                user.getTotalScore() +
                                (user.getTotalScore() == 1 || user.getTotalScore() == -1 ? " point)" : " points)");
                        userScoreView.setText(userScoreText);
                        userProfAboutme.setText(user.getAboutme());
                        List<PostDescriptor> userPostDescriptors = user.getPostDescriptors();

                        if (userPostDescriptors == null || userPostDescriptors.size() == 0) {
                            Log.i("ljw", "user hasn't made any posts, or possibly doesn't have a postDescriptors list");
                            TextView noPosts = findViewById(R.id.profileNoCommentsYet);
                            String noPostsText = user.getUsername() + " hasn't made any posts yet.";
                            noPosts.setText(noPostsText);
                            noPosts.setVisibility(View.VISIBLE);
                        } else {
                            postDescriptorsRv = findViewById(R.id.profileAllPostsRv);
                            postDescriptorsRvLayoutManager = new LinearLayoutManager(this);
                            postDescriptorsRv.setLayoutManager(postDescriptorsRvLayoutManager);
                            postDescriptorsRvAdapter = new PostSelectAdapter(userPostDescriptors);
                            postDescriptorsRv.setAdapter(postDescriptorsRvAdapter);
                        }
                    })
                    .addOnFailureListener(e -> Log.i("ljw", "error getting user: " + e.toString()));
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
//                view.setOnClickListener(PostSelectAdapter.this::onClick);
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
            String timeAndLocationText = new Date(time) + ", " + location;
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
            postLoadingSpinner.setVisibility(View.VISIBLE);

            if (selectedDV != null) {
                selectedDV.setBackground(null);
                selectedDVdelBtn.setVisibility(View.GONE);
            }
            selectedDV = cl;
            selectedDVdelBtn = b;
            cl.setBackgroundColor(getResources().getColor(R.color.lightgray));

            if (userIsOnTheirOwnProfile) b.setVisibility(View.VISIBLE);
//
////            if (cachedPosts.containsKey(selectedPostId)) {
////                Log.i("ljw", "getting post from cache instead of firestore");
////                postRvAdapter = new PostRvAdapter(
////                        Objects.requireNonNull(cachedPosts.get(selectedPostId)),
////                        getApplicationContext(),
////                        currentUser != null ? currentUser.getUid() : null,
////                        currentUser != null ? currentUser.getDisplayName() : null,
////                        postRv,
////                        db);
////                postRv.setAdapter(postRvAdapter);
////                postRvAdapter.notifyDataSetChanged();
////                postLoadingSpinner.setVisibility(View.GONE);
//            } else {
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
                                    postRv,
                                    db);
                            postRv.setAdapter(postRvAdapter);
                            postDescriptorsRv.setMinimumHeight(100);
                            postLoadingSpinner.setVisibility(View.GONE);

                        })
                        .addOnFailureListener(e -> {
                            Log.i("ljw", "error getting post: " + e.toString());
                            postLoadingSpinner.setVisibility(View.GONE);
                        });
//            }
        }

    }

    public void onDeleteButtonClick(View v) {
        if (!userIsOnTheirOwnProfile) return;
        cl.setVisibility(View.VISIBLE);
    }
    public void yesDelete(View v) {
        if (!userIsOnTheirOwnProfile) return;

        v.setEnabled(false);
        deleteLoadingSpinner.setVisibility(View.VISIBLE);

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
                                postRv.setAdapter(null);
                                postRv.setBackground(null);

                                db.collection("users")
                                        .document(thisProfileOwnerId)
                                        .update("postDescriptors", usersNewPDs,
                                                "totalScore", user.getTotalScore() - postScore)
                                        .addOnSuccessListener(result3 -> {
                                            Log.i("ljw", "successfully removed the deleted post's PD");
                                            Utils.showToast(UserProfileActivity.this, "Post deleted.");
                                            deleteLoadingSpinner.setVisibility(View.GONE);
                                            v.setEnabled(true);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.i("ljw", "error removing the deleted post's PD: " + e.toString());
                                            deleteLoadingSpinner.setVisibility(View.GONE);
                                            v.setEnabled(true);
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Log.i("ljw", "error getting user to delete this PD");
                                deleteLoadingSpinner.setVisibility(View.GONE);
                                v.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.i("ljw", "error deleting post: " + e.toString());
                    deleteLoadingSpinner.setVisibility(View.GONE);
                    v.setEnabled(true);
                });

        cl.setVisibility(View.GONE);
    }
    public void noDelete(View v) {
        cl.setVisibility(View.GONE);
    }

    public void onEditAboutmeButtonClicked(View v) {
        if (!userIsOnTheirOwnProfile) return;
        aboutmeBeingEdited = !aboutmeBeingEdited;
        userProfAboutme.setVisibility(aboutmeBeingEdited ? View.GONE : View.VISIBLE);
        userProfAboutmeEdit.setVisibility(aboutmeBeingEdited ? View.VISIBLE : View.GONE);
        String buttonText = aboutmeBeingEdited ? "SAVE" : "EDIT";
        userProfAboutmeEditBtn.setText(buttonText);

        //on "edit" click:
        if (aboutmeBeingEdited) {
            userProfAboutmeEdit.setText(userProfAboutme.getText().toString());
        }

        // on "save" click, if the text was changed:
        if (!aboutmeBeingEdited && !userProfAboutmeEdit.getText().toString().equals(userProfAboutme.getText().toString())) {
            String newAboutmeText = userProfAboutmeEdit.getText().toString();
            userProfAboutme.setText(newAboutmeText);
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

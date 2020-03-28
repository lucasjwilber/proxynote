package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class UserProfileActivity extends AppCompatActivity {

    User currentUser;
    String userId;
    HashMap<String, Post> cachedPosts;

    private TextView userScoreView;
    private TextView usernameView;
    private RecyclerView postDescriptorsRv;
    private RecyclerView.Adapter postDescriptorsRvAdapter;
    private RecyclerView.LayoutManager postDescriptorsRvLayoutManager;
    FirebaseFirestore db;
    private RecyclerView postRv;
    private RecyclerView.Adapter postRvAdapter;
    private RecyclerView.LayoutManager postRvLayoutManager;
    View selectedDescriptorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        cachedPosts = new HashMap<>();

        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        Log.i("ljw", "userId is " + userId);

        postRv = findViewById(R.id.profileOnePostRv);
        postRvLayoutManager = new LinearLayoutManager(this);
        postRv.setLayoutManager(postRvLayoutManager);
        usernameView = findViewById(R.id.profileUsername);
        userScoreView = findViewById(R.id.profileScore);

        db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(result -> {
                    Log.i("ljw", "successfully got user:\n" + result.toString());
                    User user = result.toObject(User.class);

                    currentUser = user;

                    assert user != null;
                    usernameView.setText(user.getUsername());
                    String userScoreText = "(" +
                            user.getTotalScore() +
                            (user.getTotalScore() == 1 || user.getTotalScore() == -1 ? " point)" : " points)");
                    userScoreView.setText(userScoreText);
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

    //list of post descriptors RV:
    public class PostSelectAdapter extends RecyclerView.Adapter<PostSelectAdapter.PostTitleViewholder>
            implements View.OnClickListener {

        List<PostDescriptor> userPostDescriptors;

        public PostSelectAdapter(List<PostDescriptor> userPostDescriptors) {
            this.userPostDescriptors = userPostDescriptors;
        }

        @Override
        public void onClick(View v) {
            Log.i("ljw", "clicked on post " + v.getTag());
            String postId = v.getTag().toString();

            if (selectedDescriptorView != null) selectedDescriptorView.setBackground(null);
            selectedDescriptorView = v;
            v.setBackground(getDrawable(R.drawable.rounded_square_filled_accentcolor));

            if (cachedPosts.containsKey(postId)) {
                Log.i("ljw", "getting post from cache instead of firestore");
                postRvAdapter = new PostRvAdapter(Objects.requireNonNull(cachedPosts.get(postId)), getApplicationContext(), userId, postRv);
                postRv.setAdapter(postRvAdapter);
                postRvAdapter.notifyDataSetChanged();
            } else {
                db.collection("posts")
                        .document(postId)
                        .get()
                        .addOnSuccessListener(response -> {
                            Log.i("ljw", "got post!");
                            Post post = response.toObject(Post.class);
                            if (post == null) {
                                Log.i("ljw", "post not found. may have been deleted from the post collection but not the user object");
                                return;
                            }
                            Log.i("ljw", "found post " + post.getId());
                            ArrayList list = (ArrayList) response.getData().get("comments");
                            post.setComments(Utils.turnMapsIntoListOfComments(list));

                            cachedPosts.put(postId, post);
                            postRvAdapter = new PostRvAdapter(post, getApplicationContext(), userId, postRv);
                            postRv.setAdapter(postRvAdapter);
                            postDescriptorsRv.setMinimumHeight(100);

                        })
                        .addOnFailureListener(e -> Log.i("ljw", "error getting post: " + e.toString()));
            }
        }

        public class PostTitleViewholder extends RecyclerView.ViewHolder {
            ConstraintLayout constraintLayout;

            PostTitleViewholder(ConstraintLayout view) {
                super(view);
                constraintLayout = view;
                view.setOnClickListener(PostSelectAdapter.this::onClick);
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

    }

}

package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class UserProfileActivity extends AppCompatActivity {

    User currentUser;
    String userId;
    HashMap<String, Post> cachedPosts;

    private RecyclerView stringsRv;
    private RecyclerView.Adapter stringsRvAdapter;
    private RecyclerView.LayoutManager stringsRvLayoutManager;
    FirebaseFirestore db;
    private RecyclerView postRv;
    private RecyclerView.Adapter postRvAdapter;
    private RecyclerView.LayoutManager postRvLayoutManager;

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

        db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(result -> {
                    Log.i("ljw", "successfully got user:\n" + result.toString());
                    User user = result.toObject(User.class);

                    currentUser = user;

                    assert user != null;
                    Log.i("ljw", user.toString());
                    Log.i("ljw", user.getPostDescriptors().toString());
                    List<PostDescriptor> userPosts = user.getPostDescriptors();

                    stringsRv = findViewById(R.id.profileAllPostsRv);
                    stringsRvLayoutManager = new LinearLayoutManager(this);
                    stringsRv.setLayoutManager(stringsRvLayoutManager);
                    stringsRvAdapter = new PostSelectAdapter(userPosts);
                    stringsRv.setAdapter(stringsRvAdapter);
                })
                .addOnFailureListener(e -> Log.i("ljw", "error getting user: " + e.toString()));

    }

    //list of post titles RV:
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

            if (cachedPosts.containsKey(postId)) {
                Log.i("ljw", "getting post from cache instead of firestore");
                postRvAdapter = new PostRvAdapter(Objects.requireNonNull(cachedPosts.get(postId)), getApplicationContext(), userId);
                postRv.setAdapter(postRvAdapter);
                postRvAdapter.notifyDataSetChanged();
            } else {
                db.collection("posts")
                        .document(postId)
                        .get()
                        .addOnSuccessListener(response -> {
                            Log.i("ljw", "got post!");
                            Post post = response.toObject(Post.class);
                            Log.i("ljw", "found post " + post.getId());

                            cachedPosts.put(postId, post);

                            //set postRv to this post
                            postRvAdapter = new PostRvAdapter(post, getApplicationContext(), userId);
                            postRv.setAdapter(postRvAdapter);
                            postRvAdapter.notifyDataSetChanged();

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
                    .inflate(R.layout.postrv_comment, parent, false);
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

            TextView dateAndLocation = holder.constraintLayout.findViewById(R.id.postRvCommentHeader);
            StringBuilder dalText = new StringBuilder();
            dalText.append(time + location);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dateAndLocation.setText(Html.fromHtml(dalText.toString(), Html.FROM_HTML_MODE_COMPACT));
            } else {
                dateAndLocation.setText(Html.fromHtml(dalText.toString()));
            }

            TextView scoreEmojiAndTitle = holder.constraintLayout.findViewById(R.id.postRvCommentText);
            String seatText = score + title + icon;
            scoreEmojiAndTitle.setText(seatText);

            holder.constraintLayout.setTag(data.getId());
        }

        @Override
        public int getItemCount() {
            return userPostDescriptors.size();
        }

    }
}

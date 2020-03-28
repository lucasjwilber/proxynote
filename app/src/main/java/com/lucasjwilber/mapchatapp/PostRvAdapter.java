package com.lucasjwilber.mapchatapp;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

//import com.squareup.picasso.Picasso;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;


public class PostRvAdapter extends RecyclerView.Adapter<PostRvAdapter.PostViewHolder> {

    private Post post;
    private String userId;
    private Context context;
    private RecyclerView recyclerView;
    private boolean userIsSignedIn;
    private String distanceType;
    private Drawable upArrowColored;
    private Drawable downArrowColored;
    private Button upvoteButton;
    private Button downvoteButton;
    private TextView postScore;
    private Drawable upArrow;
    private Drawable downArrow;
    private FirebaseFirestore db;

    private static final int POST_HEADER = 0;
    private static final int POST_COMMENT = 1;

    PostRvAdapter(Post post, Context context, String userId, RecyclerView recyclerView, FirebaseFirestore db) {
        this.post = post;
        this.context = context;
        this.userId = userId;
        this.recyclerView = recyclerView;
        this.db = db;
        if (userId != null) {
            userIsSignedIn = true;
            this.userId = userId;
        }
        upArrowColored = context.getDrawable(R.drawable.arrow_up_colored);
        downArrowColored = context.getDrawable(R.drawable.arrow_down_colored);
        upArrow = context.getDrawable(R.drawable.arrow_up);
        downArrow = context.getDrawable(R.drawable.arrow_down);

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        distanceType = prefs.getString("distanceType", "imperial");

        //set border color of this based on the post score
        if (post.getScore() >= 20) {
            recyclerView.setBackground(context.getDrawable(R.drawable.rounded_square_red));
        } else if (post.getScore() >= 15) {
            recyclerView.setBackground(context.getDrawable(R.drawable.rounded_square_orangered));
        } else if (post.getScore() >= 10) {
            recyclerView.setBackground(context.getDrawable(R.drawable.rounded_square_orange));
        } else if (post.getScore() >= 5) {
            recyclerView.setBackground(context.getDrawable(R.drawable.rounded_square_yelloworange));
        } else if (post.getScore() <= -5) {
            recyclerView.setBackground(context.getDrawable(R.drawable.rounded_square_brown));
        } else {
            recyclerView.setBackground(context.getDrawable(R.drawable.rounded_square_primarycolor));
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout constraintLayout;

        PostViewHolder(ConstraintLayout l) {
            super(l);
            constraintLayout = l;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return POST_HEADER;
        else
            return POST_COMMENT;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PostRvAdapter.PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ConstraintLayout l;

//        if (parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Log.i("ljw", "parent is " + parent.getId());
//            Log.i("ljw", "map is " + R.id.mapLayout);
//            if (parent == parent.getLayoutlayout.activity_map) {
//                RecyclerView rv = parent.findViewById(R.id.postRecyclerView);
//                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) rv.getLayoutParams();
//                marginLayoutParams.setMargins(250, 50, 250, 50);
//            } else if (parent.getId() == R.id.userProfileLayout) {
//                RecyclerView rv = parent.findViewById(R.id.profileOnePostRv);
//                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) rv.getLayoutParams();
//                marginLayoutParams.setMargins(250, 50, 250, 50);
//            }
//        }

        switch (viewType) {
            case POST_HEADER:
                l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.post_layout, parent, false);
                upvoteButton = l.findViewById(R.id.postRvHeaderVoteUpBtn);
                upvoteButton.setOnClickListener(v -> onVoteButtonClick(upvoteButton));
                downvoteButton = l.findViewById(R.id.postRvHeaderVoteDownBtn);
                downvoteButton.setOnClickListener(v -> onVoteButtonClick(downvoteButton));
                postScore = l.findViewById(R.id.postRvHeaderScore);
                TextView postInfo = l.findViewById(R.id.postRvPostInfo);
                TextView postTitle = l.findViewById(R.id.postRvHeaderTitle);
                Button reportButton = l.findViewById(R.id.postRvHeaderReportBtn);
                ImageView postImage = l.findViewById(R.id.postRvPostImage);
                TextView postText = l.findViewById(R.id.postRvPostText);
                TextView commentCount = l.findViewById(R.id.postCommentCount);

                postTitle.setText(post.getTitle());
                String postScoreText = Long.toString(post.getScore());
                postScore.setText(postScoreText);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    postInfo.setText(Html.fromHtml(getHtmlHeaderString(post.getTimestamp()), Html.FROM_HTML_MODE_COMPACT));
                } else {
                    postInfo.setText(Html.fromHtml(getHtmlHeaderString(post.getTimestamp())));
                }

                if (post.getImageUrl() != null && post.getImageUrl().length() > 0) {
                    Glide.with(parent).load(post.getImageUrl()).into(postImage);
                }

                postText.setText(post.getText());
                int numComments = post.getComments().size();
                String commentCountText = "";
                if (numComments == 1) {
                    commentCountText = numComments + " comment:";
                } else if (numComments > 1) {
                    commentCountText = numComments + " comments:";
                }
                commentCount.setText(commentCountText);

                Log.i("ljw", "post votes: " + post.getVotes().size());

                if (userIsSignedIn) {
                    if (post.getVotes().containsKey(userId)) {
                        if (post.getVotes().get(userId) > 0) {
                            upvoteButton.setBackground(upArrowColored);
                        } else if (post.getVotes().get(userId) < 0) {
                            downvoteButton.setBackground(downArrowColored);
                        }
                    }
                }
                return new PostViewHolder(l);
            case POST_COMMENT:
            default:
                Log.i("ljw", "view created for viewtype " + viewType);
                l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.postrv_comment, parent, false);
                return new PostViewHolder(l);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        // only if it's a comment are we recycling the same view type:
        if (position >= 1) {
            Comment comment = post.getComments().get(position - 1);
            TextView commentHeader = holder.constraintLayout.findViewById(R.id.postRvCommentHeader);
            StringBuilder headerText = new StringBuilder();
            headerText.append(getHtmlHeaderString(comment.getTimestamp()));
            headerText.append(", <i>");
            headerText.append(Utils.getHowFarAway(comment.getDistanceFromPost(), distanceType));
            headerText.append("</i>");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                commentHeader.setText(Html.fromHtml(headerText.toString(), Html.FROM_HTML_MODE_COMPACT));
            } else {
                commentHeader.setText(Html.fromHtml(headerText.toString()));
            }

            // body of comment:
            TextView commentText = holder.constraintLayout.findViewById(R.id.postRvCommentText);
            commentText.setText(comment.getText());
        }
    }

    @Override
    public int getItemCount() {
        return 1 + post.getComments().size();
    }

    public String getHtmlHeaderString(long timestamp) {
        return "<i><b>" + post.getUsername() + "</b>, " + Utils.getHowLongAgo(timestamp) + "</i>";
    }

    public void onVoteButtonClick(Button b) {
        if (!userIsSignedIn) {
            //TODO: modal with "sign in to vote"
            return;
        }
        // need to disable the button until the firestore transaction is complete, otherwise users
        // could cast multiple votes by spamming the button
        b.setEnabled(false);
        int usersPreviousVote = 0;
        int currentScore = post.getScore();
        HashMap<String, Integer> voteMap = post.getVotes();
        if (voteMap.containsKey(userId)) {
            usersPreviousVote = voteMap.get(userId);
        }

//        Button up = findViewById(R.id.postRvHeaderVoteUpBtn);
//        Button down = findViewById(R.id.postRvHeaderVoteDownBtn);
        int usersNewVote = 0;
        int scoreChange = 0;

//        if (b.getId() == R.id.postRvHeaderVoteDownBtn) {
        if (b == downvoteButton) {
            if (usersPreviousVote == -1) {
                usersNewVote = 0;
                scoreChange = 1;
                downvoteButton.setBackground(context.getDrawable(R.drawable.arrow_down));
            } else if (usersPreviousVote == 0) {
                usersNewVote = -1;
                scoreChange = -1;
                downvoteButton.setBackground(context.getDrawable(R.drawable.arrow_down_colored));
            } else { //if (usersPreviousVote == 1)
                usersNewVote = -1;
                scoreChange = -2;
                downvoteButton.setBackground(context.getDrawable(R.drawable.arrow_down_colored));
                upvoteButton.setBackground(context.getDrawable(R.drawable.arrow_up));
            }
        }
//        if (v.getId() == R.id.postRvHeaderVoteUpBtn) {
        if (b == upvoteButton) {
            if (usersPreviousVote == -1) {
                usersNewVote = 1;
                scoreChange = 2;
                downvoteButton.setBackground(context.getDrawable(R.drawable.arrow_down));
                upvoteButton.setBackground(context.getDrawable(R.drawable.arrow_up_colored));
            } else if (usersPreviousVote == 0) {
                usersNewVote = 1;
                scoreChange = 1;
                upvoteButton.setBackground(context.getDrawable(R.drawable.arrow_up_colored));
            } else { //if (usersPreviousVote == 1)
                usersNewVote = 0;
                scoreChange = -1;
                upvoteButton.setBackground(context.getDrawable(R.drawable.arrow_up));
            }
        }


        post.setScore(currentScore + scoreChange);
        String scoreViewText = Long.toString(currentScore + scoreChange);
        postScore.setText(scoreViewText);

        voteMap.put(userId, usersNewVote);
        int finalScoreChange = scoreChange;

        // get the current score in firestore first
        int finalScoreChange1 = scoreChange;
        db.collection("posts")
                .document(post.getId())
                .get()
                .addOnCompleteListener(task -> {
                    Post post = task.getResult().toObject(Post.class);

                    db.collection("posts")
                            .document(post.getId())
                            .update("score", post.getScore() + finalScoreChange,
                                    "votes", voteMap)
                            .addOnCompleteListener(task1 -> {
                                Log.i("ljw", "successfully updated score");

                                //update the post creator's total score field:
                                db.collection("users")
                                        .document(post.getUserId())
                                        .get()
                                        .addOnCompleteListener(task2 -> {
                                            Log.i("ljw", "got post creator for score update");
                                            User user = task2.getResult().toObject(User.class);
                                            int userScore = user.getTotalScore();

                                            //update the postDescriptor for this post:
                                            List<PostDescriptor> postDescriptors = user.getPostDescriptors();
                                            for (PostDescriptor pd : postDescriptors) {
                                                if (pd.id.equals(post.getId())) {
                                                    pd.setScore(pd.getScore() + finalScoreChange);
                                                }
                                            }

                                            db.collection("users")
                                                    .document(post.getUserId())
                                                    .update("totalScore", userScore + finalScoreChange,
                                                            "postDescriptors", postDescriptors)
                                                    .addOnCompleteListener(task3 -> {
                                                        Log.i("ljw", "updated post user's score and the postDescriptor successfully");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.i("ljw", "couldn't update user's score: " + e.toString());
                                                    });


                                        })
                                        .addOnFailureListener(e -> {
                                            Log.i("ljw", "failed getting user: " + e.toString());
                                        });


                                b.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.i("ljw", "failed updating score: " + e.toString());
                                b.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.i("ljw", "error getting post to update its score: " + e.toString());
                    b.setEnabled(true);
                });
    }

}
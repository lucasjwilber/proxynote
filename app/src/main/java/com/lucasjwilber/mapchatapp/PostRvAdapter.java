package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;

public class PostRvAdapter extends RecyclerView.Adapter<PostRvAdapter.PostViewHolder> {

    private final String TAG = "ljw";
    private FirebaseFirestore db;
    private FirebaseUser user;
    private Context context;
    private Post post;
    private String currentUserId;
    private String currentUserUsername;
    private String profileOwnerId;
    private String distanceType;
    private Drawable upArrowColored;
    private Drawable downArrowColored;
    private Button upvoteButton;
    private Button downvoteButton;
    private TextView postScore;
    private EditText addCommentBox;
    private ProgressBar replyLoadingSpinner;
    private static final int POST_HEADER = 0;
    private static final int POST_COMMENT = 1;

    PostRvAdapter(
            Post post,
            Context context,
            String currentUserId,
            String currentUserUsername,
            String profileOwnerId,
            FirebaseFirestore db) {
        this.post = post;
        this.context = context;
        this.currentUserId = currentUserId;
        this.currentUserUsername = currentUserUsername;
        this.profileOwnerId = profileOwnerId;
        this.db = db;

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        SharedPreferences prefs = context.getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);
        distanceType = prefs.getString("distanceType", "imperial");
        upArrowColored = context.getDrawable(R.drawable.arrow_up_colored);
        downArrowColored = context.getDrawable(R.drawable.arrow_down_colored);
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

    @Override
    public PostRvAdapter.PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ConstraintLayout l;
        switch (viewType) {
            case POST_HEADER:
                l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.post_layout, parent, false);

                addCommentBox = l.findViewById(R.id.postRvPostReplyBox);
                replyLoadingSpinner = l.findViewById(R.id.replySubmitProgressBar);

                postScore = l.findViewById(R.id.postRvHeaderScore);
                String postScoreText = Long.toString(post.getScore());
                postScore.setText(postScoreText);

                TextView postUsername = l.findViewById(R.id.postRvUsername);
                postUsername.setText(post.isAnonymous() ? "Anonymous" : post.getUsername());

                if (post.isAnonymous()) {
                    postUsername.setTextColor(context.getResources().getColor(R.color.black));
                } else {
                    postUsername.setOnClickListener(v -> onUsernameClicked(post.getUserId()));
                }

                String place = post.getLocation();
                String timeAndPlace = place == null ?
                        Utils.getHowLongAgo(post.getTimestamp()) :
                        Utils.getHowLongAgo(post.getTimestamp()) + " @ " + place;
                TextView postTimeAndPlace = l.findViewById(R.id.postRvTimeAndPlace);
                postTimeAndPlace.setText(timeAndPlace);

                TextView postTitle = l.findViewById(R.id.postRvHeaderTitle);
                postTitle.setText(post.getTitle());

                Button reportButton = l.findViewById(R.id.postRvHeaderReportBtn);
                reportButton.setOnClickListener(v -> onReportButtonClicked());

                TextView postText = l.findViewById(R.id.postRvPostText);
                postText.setText(post.getText());

                Button addCommentButton = l.findViewById(R.id.postRvPostReplyButton);
                addCommentButton.setOnClickListener(v -> addCommentToPost(addCommentBox.getText().toString()));

                TextView commentCount = l.findViewById(R.id.postCommentCount);
                int numComments = post.getComments().size();
                String commentCountText = "";
                if (numComments == 1) {
                    commentCountText = numComments + " comment:";
                } else if (numComments > 1) {
                    commentCountText = numComments + " comments:";
                }
                commentCount.setText(commentCountText);

                //if there's an image or video create the thumbnail
                if ((post.getImageUrl() != null && post.getImageUrl().length() > 0) ||
                        (post.getVideoUrl() != null && post.getVideoUrl().length() > 0)) {
                    ImageView postImage = l.findViewById(R.id.postRvPostImage);
                    postImage.setVisibility(View.VISIBLE);

                    Glide.with(parent)
                            .load(post.getImageUrl() != null ? post.getImageUrl() : post.getVideoThumbnailUrl())
                            .thumbnail(.25f)
                            .into(postImage);

                    if (post.getVideoUrl() != null) {
                        ImageView playButton = l.findViewById(R.id.postRvPlayButton);
                        playButton.setVisibility(View.VISIBLE);
                    }

                    String type = post.getImageUrl() != null ? "image" : "video";
                    String url = post.getImageUrl() != null ? post.getImageUrl() : post.getVideoUrl();
                    postImage.setOnClickListener(v -> goToFullScreenMedia(
                            type,
                            url,
                            post.getTitle(),
                            post.getVideoThumbnailUrl()
                    ));
                }

                upvoteButton = l.findViewById(R.id.postRvHeaderVoteUpBtn);
                upvoteButton.setOnClickListener(v -> onVoteButtonClick(upvoteButton));
                downvoteButton = l.findViewById(R.id.postRvHeaderVoteDownBtn);
                downvoteButton.setOnClickListener(v -> onVoteButtonClick(downvoteButton));
                if (post.getVotes().containsKey(currentUserId)) {
                    if (post.getVotes().get(currentUserId) > 0) {
                        upvoteButton.setBackground(upArrowColored);
                    } else if (post.getVotes().get(currentUserId) < 0) {
                        downvoteButton.setBackground(downArrowColored);
                    }
                }

                return new PostViewHolder(l);

            case POST_COMMENT:
            default:
                l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.postrv_comment, parent, false);
                return new PostViewHolder(l);
        }
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        // only if it's a comment are we recycling the same view type:
        if (getItemViewType(position) == POST_COMMENT) {
            Comment comment = post.getComments().get(position - 1);

            TextView commentUsername = holder.constraintLayout.findViewById(R.id.commentUsername);
            commentUsername.setText(comment.getUsername());
            commentUsername.setOnClickListener(v -> onUsernameClicked(comment.getUserId()));

            TextView commentTimeAndPlace = holder.constraintLayout.findViewById(R.id.postRvCommentHeader);
            String commentTimeAndPlaceText = Utils.getHowLongAgo(comment.getTimestamp()) +
                    ", " +
                    Utils.getHowFarAway(comment.getDistanceFromPost(), distanceType);
            commentTimeAndPlace.setText(commentTimeAndPlaceText);

            // body of comment:
            TextView commentText = holder.constraintLayout.findViewById(R.id.postRvCommentText);
            commentText.setText(comment.getText());
        }
    }

    @Override
    public int getItemCount() {
        return 1 + post.getComments().size();
    }

    public void onVoteButtonClick(Button b) {
        if (user == null) {
            Utils.showToast(context, "You must be logged in to vote.");
        } else {
            //reload and check again first in case user logged out or verified their email while this RV is open
            user.reload()
                    .addOnSuccessListener(r -> {
                        if (user == null) {
                            Utils.showToast(context, "You must be logged in to vote.");
                        } else if (!user.isEmailVerified()) {
                            Utils.showToast(context, "Please verify your email first.");
                        } else {
                            castVote(b);
                        }
                    });
        }
    }

    private void castVote(Button b) {
        // need to disable the button until the firestore transaction is complete, otherwise users
        // could cast multiple votes by spamming the button
        b.setEnabled(false);
        int usersPreviousVote = 0;
        int currentScore = post.getScore();
        HashMap<String, Integer> voteMap = post.getVotes();
        if (voteMap.containsKey(currentUserId)) {
            usersPreviousVote = voteMap.get(currentUserId);
        }

        int usersNewVote = 0;
        int scoreChange = 0;

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

        voteMap.put(currentUserId, usersNewVote);
        int finalScoreChange = scoreChange;
        db.collection("posts")
                .document(post.getId())
                .get()
                .addOnCompleteListener(task -> {
                    Post post = task.getResult().toObject(Post.class);

                    if (post == null) {
                        Utils.showToast(context, "This post no longer exists.");
                        return;
                    }

                    db.collection("posts")
                            .document(post.getId())
                            .update("score", post.getScore() + finalScoreChange,
                                    "votes", voteMap)
                            .addOnCompleteListener(task1 -> {
                                //update the post creator's total score field:
                                db.collection("users")
                                        .document(post.getUserId())
                                        .get()
                                        .addOnCompleteListener(task2 -> {
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
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "couldn't update user's score: " + e.toString());
                                                    });

                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "failed getting user: " + e.toString());
                                        });

                                b.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "failed updating score: " + e.toString());
                                b.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "error getting post to update its score: " + e.toString());
                    b.setEnabled(true);
                });
    }

    private void onUsernameClicked(String userId) {
        if (post.getUserId().equals(profileOwnerId)) {
            Utils.showToast(context, "You're already viewing this profile.");
        } else {
            Intent goToProfile = new Intent(context, UserProfileActivity.class);
            goToProfile.putExtra("userId", userId);
            context.startActivity(goToProfile);
        }
    }

    private void onReportButtonClicked() {
        if (user == null) {
            Utils.showToast(context, "You must be signed in to report a post.");
            return;
        } else if (!user.isEmailVerified()) {
            //reload and check again first
            user.reload()
                .addOnSuccessListener(r -> {
                    if (!user.isEmailVerified()) {
                        Utils.showToast(context, "Please verify your email first.");
                        return;
                    }
                });
        }
        Intent goToReportActivity = new Intent(context, ReportActivity.class);
        goToReportActivity.putExtra("postId", post.getId());
        context.startActivity(goToReportActivity);
    }

    public void addCommentToPost(String commentText) {
        Log.i(TAG, commentText);
        if (user == null) {
            Utils.showToast(context, "You must be logged in to comment.");
            return;
        } else if (!user.isEmailVerified()) {
            //reload and check again first
            user.reload()
                    .addOnSuccessListener(r -> {
                        if (!user.isEmailVerified()) {
                            Utils.showToast(context, "Please verify your email first.");
                            return;
                        }
                    });
        } else if (commentText.equals("") || commentText.length() == 0) {
            Utils.showToast(context, "Please write a comment first.");
            return;
        }
        replyLoadingSpinner.setVisibility(View.VISIBLE);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    double userLat;
                    double userLng;
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();

                        double distanceFromPost = Utils.getDistance(userLat, userLng, post.getLat(), post.getLng());

                        Comment comment = new Comment(
                                currentUserId,
                                currentUserUsername != null ? currentUserUsername : "someone",
                                commentText,
                                userLat,
                                userLng,
                                distanceFromPost);

                        List<Comment> comments = post.getComments();
                        comments.add(comment);

                        //get post by id from firestore
                        db.collection("posts")
                                .document(post.getId())
                                .update("comments", comments)
                                .addOnCompleteListener(task -> {
                                    addCommentBox.setText("");
                                    replyLoadingSpinner.setVisibility(View.GONE);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "failed adding a comment: " + e.toString());
                                    Utils.showToast(context, "This post no longer exists.");
                                    replyLoadingSpinner.setVisibility(View.GONE);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Utils.showToast(context, "Unable to get your location.");
                    Log.e(TAG, "error getting location: " + e.toString());
                    replyLoadingSpinner.setVisibility(View.GONE);
                });
    }

    private void goToFullScreenMedia(String type, String url, String title, String videoThumbnailUrl) {
        Intent goToFullScreenMedia = new Intent(context, FullScreenMediaActivity.class);
        goToFullScreenMedia.putExtra("type", type);
        goToFullScreenMedia.putExtra("url", url);
        goToFullScreenMedia.putExtra("title", title);
        goToFullScreenMedia.putExtra("videoThumbnailUrl", videoThumbnailUrl);
        context.startActivity(goToFullScreenMedia);
    }

}
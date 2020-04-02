package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PostRvAdapter extends RecyclerView.Adapter<PostRvAdapter.PostViewHolder> {

    private Post post;
    private String currentUserId;
    private String currentUserUsername;
    private Context context;
//    private RecyclerView recyclerView;
//    private boolean userIsSignedIn;
    private String distanceType;
    private Drawable upArrowColored;
    private Drawable downArrowColored;
    private Button upvoteButton;
    private Button downvoteButton;
    private TextView postScore;
    private ImageView postImage;
//    private Drawable upArrow;
//    private Drawable downArrow;
    private FirebaseFirestore db;
    private EditText addCommentBox;
    private ProgressBar replyLoadingSpinner;

    private static final int POST_HEADER = 0;
    private static final int POST_COMMENT = 1;

    PostRvAdapter(
            Post post,
            Context context,
            String currentUserId,
            String currentUserUsername,
//            RecyclerView recyclerView,
            FirebaseFirestore db) {
        this.post = post;
        this.context = context;
        this.currentUserId = currentUserId;
        this.currentUserUsername = currentUserUsername;
//        this.recyclerView = recyclerView;
        this.db = db;

        upArrowColored = context.getDrawable(R.drawable.arrow_up_colored);
        downArrowColored = context.getDrawable(R.drawable.arrow_down_colored);
//        upArrow = context.getDrawable(R.drawable.arrow_up);
//        downArrow = context.getDrawable(R.drawable.arrow_down);

        SharedPreferences prefs = context.getSharedPreferences("mapchatPrefs", Context.MODE_PRIVATE);
        distanceType = prefs.getString("distanceType", "imperial");
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
                TextView postUsername = l.findViewById(R.id.postRvUsername);
                TextView postTimeAndPlace = l.findViewById(R.id.postRvTimeAndPlace);
                TextView postTitle = l.findViewById(R.id.postRvHeaderTitle);
                Button reportButton = l.findViewById(R.id.postRvHeaderReportBtn);
                postImage = l.findViewById(R.id.postRvPostImage);
                postImage.setOnClickListener(v -> goToFullSizeImage(post.getImageUrl()));
                TextView postText = l.findViewById(R.id.postRvPostText);
                addCommentBox = l.findViewById(R.id.postRvPostReplyBox);
                Button addCommentButton = l.findViewById(R.id.postRvPostReplyButton);
                addCommentButton.setOnClickListener(v -> addCommentToPost(addCommentBox.getText().toString()));
                TextView commentCount = l.findViewById(R.id.postCommentCount);
                replyLoadingSpinner = l.findViewById(R.id.replySubmitProgressBar);

                if (!post.getUserId().equals(currentUserId)) {
                    postUsername.setText(post.getUsername());
                    postUsername.setOnClickListener(v -> onUsernameClicked(post.getUserId()));
                } else {
                    String me = "me";
                    postUsername.setText(me);
                    postUsername.setTextColor(context.getResources().getColor(R.color.black));
                }
                postTimeAndPlace.setText(Utils.getHowLongAgo(post.getTimestamp()));
                reportButton.setOnClickListener(v -> onReportButtonClicked());
                postTitle.setText(post.getTitle());
                String postScoreText = Long.toString(post.getScore());
                postScore.setText(postScoreText);

                if (post.getImageUrl() != null && post.getImageUrl().length() > 0) {
                    Glide.with(parent).load(post.getImageUrl()).into(postImage);
                }

                Log.i("ljw", "comments: " + post.getComments());
//                ArrayList list = (ArrayList) post.getComments();
//                post.setComments(Utils.turnMapsIntoListOfComments(list));

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

//                if (userIsSignedIn && post.getVotes().containsKey(currentUserId)) {
//                    if (post.getVotes().get(currentUserId) > 0) {
//                        upvoteButton.setBackground(upArrowColored);
//                    } else if (post.getVotes().get(currentUserId) < 0) {
//                        downvoteButton.setBackground(downArrowColored);
//                    }
//                }
                //todo: use above if NPE:
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
            TextView commentUsername = holder.constraintLayout.findViewById(R.id.commentUsername);
            TextView commentTimeAndPlace = holder.constraintLayout.findViewById(R.id.postRvCommentHeader);
            if (!comment.getUserId().equals(currentUserId)) {
                commentUsername.setText(comment.getUsername());
                commentUsername.setOnClickListener(v -> onUsernameClicked(comment.getUserId()));
            } else {
                String me = "me";
                commentUsername.setText(me);
                commentUsername.setTextColor(context.getResources().getColor(R.color.black));
            }
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
        if (currentUserId == null) {
            Utils.showToast(context, "You must be logged in to vote.");
            return;
        }
        // need to disable the button until the firestore transaction is complete, otherwise users
        // could cast multiple votes by spamming the button
        b.setEnabled(false);
        int usersPreviousVote = 0;
        int currentScore = post.getScore();
        HashMap<String, Integer> voteMap = post.getVotes();
        if (voteMap.containsKey(currentUserId)) {
            usersPreviousVote = voteMap.get(currentUserId);
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

        voteMap.put(currentUserId, usersNewVote);
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

    private void onUsernameClicked(String userId) {
        Intent goToProfile = new Intent(context, UserProfileActivity.class);
        goToProfile.putExtra("userId", userId);
        context.startActivity(goToProfile);
    }

    private void onReportButtonClicked() {
        if (currentUserId == null) {
            Utils.showToast(context, "You must be signed in to report a post.");
            return;
        }
        Intent goToReportActivity = new Intent(context, ReportActivity.class);
        goToReportActivity.putExtra("postId", post.getId());
        context.startActivity(goToReportActivity);
    }

    public void addCommentToPost(String commentText) {
        Log.i("ljw", commentText);
        if (currentUserId == null) {
            Utils.showToast(context, "You must be logged in to comment.");
            return;
        } else if (commentText.equals("") || commentText.length() == 0) {
            Utils.showToast(context, "Please write a comment first.");
            return;
        }
        replyLoadingSpinner.setVisibility(View.VISIBLE);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    Log.i("ljw", "successfully got location");
                    // Got last known location. In some rare situations this can be null.
                    double userLat;
                    double userLng;
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        Log.i("ljw", "lat: " + userLat + "\nlong: " + userLng);

//                        //get user from "users"
//                        db.collection("users")
//                                .document(postOwnerId)
//                                .get()
//                                .addOnSuccessListener(result -> {
//                                    Log.i("ljw", "got user from db");
//                                    User user = result.toObject(User.class);
//                                    assert user != null;

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
                                    Log.i("ljw", "adding comment " + comments.toString());

                                    //get post by id from firestore
                                    db.collection("posts")
                                            .document(post.getId())
                                            .update("comments", comments)
                                            .addOnCompleteListener(task -> {
                                                Log.i("ljw", "successfully added a comment");
                                                addCommentBox.setText("");
                                                replyLoadingSpinner.setVisibility(View.GONE);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.i("ljw", "failed adding a comment because " + e.toString());
                                                replyLoadingSpinner.setVisibility(View.GONE);
                                            });
//                                })
//                                .addOnFailureListener(e -> {
//                                    Log.i("ljw", "error getting user from db: " + e.toString());
//                                    replyLoadingSpinner.setVisibility(View.GONE);
//                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Utils.showToast(context, "Unable to get your location.");
                    Log.i("ljw", "error getting location: " + e.toString());
                    replyLoadingSpinner.setVisibility(View.GONE);
                });
    }

    private void goToFullSizeImage(String imageUrl) {
        Intent goToFullSizeImage = new Intent(context, FullSizeImageActivity.class);
        goToFullSizeImage.putExtra("imageUrl", imageUrl);
        context.startActivity(goToFullSizeImage);
    }


}
package com.lucasjwilber.proxynote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class PostRvAdapter extends RecyclerView.Adapter<PostRvAdapter.PostViewHolder> implements PopupMenu.OnMenuItemClickListener {

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
    private ImageButton upvoteButton;
    private ImageButton downvoteButton;
    private TextView postScore;
    private EditText addCommentBox;
    private ProgressBar replyLoadingSpinner;
    private static final int POST_HEADER = 0;
    private static final int POST_COMMENT = 1;
    private long timeOfLastComment;
    private Comment selectedComment;
    private TextView selectedCommentText;
    private List<String> deletedCommentIds;

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

        user = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences prefs = context.getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);
        distanceType = prefs.getString("distanceType", "imperial");
        upArrowColored = context.getResources().getDrawable(R.drawable.arrow_up_colored);
        downArrowColored = context.getResources().getDrawable(R.drawable.arrow_down_colored);
        timeOfLastComment = new Date().getTime() - 2000;
        deletedCommentIds = new LinkedList<>();
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

                addCommentBox = l.findViewById(R.id.postRvCommentET);
                TextView commentCounter = l.findViewById(R.id.postRvCommentETcounter);
                addCommentBox.addTextChangedListener(Utils.makeTextWatcher(addCommentBox, commentCounter,300));
                replyLoadingSpinner = l.findViewById(R.id.replySubmitPB);

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
                String timeAndPlace = (place == null || place.length() == 0) ?
                        Utils.getHowLongAgo(post.getTimestamp()) :
                        Utils.getHowLongAgo(post.getTimestamp()) + " @ " + place;
                TextView postTimeAndPlace = l.findViewById(R.id.postRvTimeAndPlace);
                postTimeAndPlace.setText(timeAndPlace);

                TextView postTitle = l.findViewById(R.id.postRvHeaderTitle);
                postTitle.setText(post.getTitle());

                Button reportButton = l.findViewById(R.id.postRvHeaderReportBtn);
                reportButton.setOnClickListener(v -> onPostReportButtonClicked());

                TextView postText = l.findViewById(R.id.postRvPostText);
                postText.setText(post.getText());

                Button addCommentButton = l.findViewById(R.id.postRvPostReplyBtn);
                addCommentButton.setOnClickListener(v -> addCommentToPost(addCommentBox.getText().toString()));

                //if there's an image or video create the thumbnail
                if ((post.getImageUrl() != null && post.getImageUrl().length() > 0) ||
                        (post.getVideoUrl() != null && post.getVideoUrl().length() > 0)) {
                    ImageView postImage = l.findViewById(R.id.postRvPostImage);
                    postImage.setVisibility(View.VISIBLE);

                    Glide.with(parent)
                            .load(post.getImageUrl() != null ? post.getImageUrl() : post.getVideoThumbnailUrl())
                            .thumbnail(.25f)
                            .centerCrop()
                            .into(postImage);

                    if (post.getVideoUrl() != null || post.getVideoThumbnailUrl() != null) {
                        ImageView videoIndicator = l.findViewById(R.id.postRvVideoIndicator);
                        videoIndicator.setVisibility(View.VISIBLE);
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

            TextView commentUsername = holder.constraintLayout.findViewById(R.id.postRvCommentUsername);
            commentUsername.setText(comment.getUsername());
            commentUsername.setOnClickListener(v -> onUsernameClicked(comment.getUserId()));

            TextView commentTimeAndPlace = holder.constraintLayout.findViewById(R.id.postRvCommentHeader);
            String commentTimeAndPlaceText = Utils.getHowLongAgo(comment.getTimestamp()) +
                    ", " +
                    Utils.getHowFarAway(comment.getDistanceFromPost(), distanceType);
            commentTimeAndPlace.setText(commentTimeAndPlaceText);

            // body of comment:
            TextView commentTextView = holder.constraintLayout.findViewById(R.id.postRvCommentText);
            String commentText;
            if (deletedCommentIds.contains(comment.getId())) {
                commentText = context.getResources().getString(R.string.deleted);
            } else {
                commentText = comment.getText();
            }
            commentTextView.setText(commentText);

            Button optionsButton = holder.constraintLayout.findViewById(R.id.commentOptionsButton);
            if (Utils.isUserAuthorized()) {
                optionsButton.setVisibility(View.VISIBLE);
                optionsButton.setOnClickListener(c -> showCommentMenu(optionsButton, comment, commentTextView));
            }
        }
    }

    @Override
    public int getItemCount() {
        return 1 + post.getComments().size();
    }

    public void onVoteButtonClick(ImageButton b) {
        if (!Utils.isUserAuthorized()) {
            Utils.showToast(context, "Please log in or verify your email address.");
        } else {
            castVote(b);
        }
    }

    private void castVote(ImageButton b) {
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
                downvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_down));
            } else if (usersPreviousVote == 0) {
                usersNewVote = -1;
                scoreChange = -1;
                downvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_down_colored));
            } else { //if (usersPreviousVote == 1)
                usersNewVote = -1;
                scoreChange = -2;
                downvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_down_colored));
                upvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_up));
            }
        }
        if (b == upvoteButton) {
            if (usersPreviousVote == -1) {
                usersNewVote = 1;
                scoreChange = 2;
                downvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_down));
                upvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_up_colored));
            } else if (usersPreviousVote == 0) {
                usersNewVote = 1;
                scoreChange = 1;
                upvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_up_colored));
            } else { //if (usersPreviousVote == 1)
                usersNewVote = 0;
                scoreChange = -1;
                upvoteButton.setBackground(context.getResources().getDrawable(R.drawable.arrow_up));
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
                .addOnSuccessListener(result -> {
                    Post post = result.toObject(Post.class);

                    if (post == null) {
                        Utils.showToast(context, "This post no longer exists.");
                        return;
                    }

                    db.collection("posts")
                            .document(post.getId())
                            .update("score", post.getScore() + finalScoreChange,
                                    "votes", voteMap)
                            .addOnSuccessListener(task1 -> {
                                //update the post creator's total score field:
                                db.collection("users")
                                        .document(post.getUserId())
                                        .get()
                                        .addOnSuccessListener(task2 -> {
                                            User user = task2.toObject(User.class);
                                            if (user != null) {
                                                int userScore = user.getTotalScore();

                                                //update the postDescriptor for this post:
                                                List<PostDescriptor> postDescriptors = user.getPostDescriptors();
                                                for (PostDescriptor pd : postDescriptors) {
                                                    if (pd.getId().equals(post.getId())) {
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
                                            }

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
        if (userId.equals(profileOwnerId)) {
            Utils.showToast(context, "You're already viewing this profile.");
        } else {
            Intent goToProfile = new Intent(context, UserProfileActivity.class);
            goToProfile.putExtra("userId", userId);
            context.startActivity(goToProfile);
        }
    }

    private void onPostReportButtonClicked() {
        if (user == null || user.isAnonymous()) {
            Utils.showToast(context, "You must be signed in to report a post.");
        } else if (!user.isEmailVerified()) {
            //reload and check again first
            user.reload()
                    .addOnSuccessListener(r -> {
                        if (!user.isEmailVerified()) {
                            Utils.showToast(context, "Please verify your email first.");
                        }
                    });
        } else {
            initReport(
                    post.getId(),
                    null,
                    post.getUserId(),
                    post.getTitle(),
                    post.getText(),
                    post.getLat(),
                    post.getLng(),
                    post.getMediaStorageId()
            );
        }
    }

    private void initReport(String postId, String commentId, String userId, String title, String text, double lat, double lng, String mediaStorageId) {
        Intent goToReportActivity = new Intent(context, ReportActivity.class);
        goToReportActivity.putExtra("postId", postId);
        goToReportActivity.putExtra("commentId", commentId);
        goToReportActivity.putExtra("postUserId", userId);
        goToReportActivity.putExtra("postTitle", title);
        goToReportActivity.putExtra("postText", text);
        goToReportActivity.putExtra("postLat", lat);
        goToReportActivity.putExtra("postLng", lng);
        goToReportActivity.putExtra("postMediaStorageId", mediaStorageId);
        context.startActivity(goToReportActivity);
    }

    public void addCommentToPost(String commentText) {
        if (!Utils.isUserAuthorized()) {
            Utils.showToast(context, "Please log in or verify your email first.");
            return;
        } else if (commentText.equals("") || commentText.length() == 0) {
            Utils.showToast(context, "Please write a comment first.");
            return;
        //2 second cooldown between comments
        } else if (!(new Date().getTime() > timeOfLastComment + 2000)) {
            Utils.showToast(context, "You're commenting too fast");
            return;
        }
        
        timeOfLastComment = new Date().getTime();
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
                                .addOnSuccessListener(task -> {
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

    private void showCommentMenu(View v, Comment comment, TextView commentText) {
        PopupMenu popup = new PopupMenu(context, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.comment_menu, popup.getMenu());
        selectedComment = comment;
        selectedCommentText = commentText;

        // show the delete option for a user's own comments
        if (currentUserId.equals(comment.getUserId())) {
            popup.getMenu().getItem(1).setVisible(true);
        }
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.commentMenuReport:
                initReport(
                        post.getId(),
                        selectedComment.getId(),
                        selectedComment.getUserId(),
                        null,
                        selectedComment.getText(),
                        selectedComment.getLat(),
                        selectedComment.getLng(),
                        null
                );
                return true;
            case R.id.commentMenuYesDelete:
                if (user.getUid().equals(selectedComment.getUserId())) {
                    //get the latest comments list
                    db.collection("posts")
                            .document(post.getId())
                            .get()
                            .addOnSuccessListener(result -> {
                                Post thisPost = result.toObject(Post.class);
                                if (thisPost == null) {
                                    Utils.showToast(context, "Couldn't delete post");
                                    return;
                                }
                                ArrayList oldCommentMaps = (ArrayList) result.getData().get("comments");
                                ArrayList<Comment> oldComments = Utils.turnMapsIntoListOfComments(oldCommentMaps);

                                ArrayList<Comment> updatedComments = new ArrayList<>();

                                for (Comment comment : oldComments) {
                                    if (!comment.getId().equals(selectedComment.getId())) {
                                        updatedComments.add(comment);
                                    }
                                }

                                db.collection("posts")
                                        .document(post.getId())
                                        .update("comments", updatedComments)
                                        .addOnSuccessListener(success -> {
                                            selectedCommentText.setText(R.string.deleted);
                                            deletedCommentIds.add(selectedComment.getId());
                                            Utils.showToast(context, "Comment deleted");
                                        })
                                        .addOnFailureListener(e -> {
                                            Utils.showToast(context, "Couldn't delete post");
                                            Log.e(TAG, "couldn't get post");
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Utils.showToast(context, "Couldn't delete post");
                                Log.e(TAG, "couldn't get post");
                            });
                }
                return true;
            default:
                return false;
        }
    }

}
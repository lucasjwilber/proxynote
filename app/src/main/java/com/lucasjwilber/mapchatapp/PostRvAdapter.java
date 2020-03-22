package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PostRvAdapter extends RecyclerView.Adapter<PostRvAdapter.PostViewHolder> {

    private Post post;
    private String userId;
    private String distanceType;
    Drawable upArrowColored;
    Drawable downArrowColored;
    Drawable upArrow;
    Drawable downArrow;

    private static final int POST_HEADER = 0;
    private static final int POST_COMMENT = 1;

    PostRvAdapter(Post post, Context context, String userId) {
        this.post = post;
        this.userId = userId;
        upArrowColored = context.getDrawable(R.drawable.up_arrow_colored);
        downArrowColored = context.getDrawable(R.drawable.down_arrow_colored);
        upArrow = context.getDrawable(R.drawable.up_arrow);
        downArrow = context.getDrawable(R.drawable.down_arrow);

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        distanceType = prefs.getString("distanceType", "imperial");

        Log.i("ljw", "post comments = " + post.getComments().toString());
        Log.i("ljw", "made PostRvAdapter with post that has title " + post.getTitle());
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

        switch (viewType) {
            case POST_HEADER:
                l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.post_layout, parent, false);
                TextView postTitle = l.findViewById(R.id.postRvHeaderTitle);
                TextView postScore = l.findViewById(R.id.postRvHeaderScore);
                TextView postInfo = l.findViewById(R.id.postRvPostInfo);
                ImageView postImage = l.findViewById(R.id.postRvPostImage);
                TextView postText = l.findViewById(R.id.postRvPostText);
                Button upvoteButton = l.findViewById(R.id.postRvHeaderVoteUpBtn);
                Button downvoteButton = l.findViewById(R.id.postRvHeaderVoteDownBtn);
                TextView commentCount = l.findViewById(R.id.postCommentCount);

                postTitle.setText(post.getTitle());
                String postScoreText = Long.toString(post.getScore());
                postScore.setText(postScoreText);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    postInfo.setText(Html.fromHtml(getHtmlHeaderString(post.getTimestamp()), Html.FROM_HTML_MODE_COMPACT));
                } else {
                    postInfo.setText(Html.fromHtml(getHtmlHeaderString(post.getTimestamp())));
                }
                postImage.setImageURI(post.getLink());
                postText.setText(post.getText());
                int numComments = post.getComments().size();
                String commentCountText = numComments + (numComments == 1 ?" comment:" : " comments:");
                commentCount.setText(commentCountText);

                Log.i("ljw", "post votes: " + post.getVotes().size());

                if (post.getVotes().containsKey(userId)) {
                    Log.i("ljw", "post contains a user vote");
                    if (post.getVotes().get(userId) > 0) {
                        Log.i("ljw", "upvote should be colored");
                        upvoteButton.setBackground(upArrowColored);
                    } else if (post.getVotes().get(userId) < 0) {
                        Log.i("ljw", "downvote should be colored");
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

}
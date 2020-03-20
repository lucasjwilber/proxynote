package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PostRvAdapter extends RecyclerView.Adapter<PostRvAdapter.PostViewHolder> {

    private Post post;
    private double distanceMultiplier = 0.8684;
    private String distanceType = " miles";

    private static final int POST_HEADER = 0;
    private static final int POST_COMMENT = 1;

    PostRvAdapter(Post post, Context context) {
        this.post = post;

        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String distanceType = prefs.getString("distanceType", "imperial");
        if (distanceType.equals("metric"))  {
            distanceMultiplier = 1.609344;
            distanceType = " km";
        }

        Log.i("ljw", "post comments = " + post.getComments().toString());
        Log.i("ljw", "made PostRvAdapter with post that has title " + post.getTitle());
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;

        PostViewHolder(LinearLayout l) {
            super(l);
            linearLayout = l;
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

        LinearLayout l;

        switch (viewType) {
            case POST_HEADER:
                l = (LinearLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.post_layout, parent, false);
                TextView postTitle = l.findViewById(R.id.postRvHeaderTitle);
                TextView postScore = l.findViewById(R.id.postRvHeaderScore);
                TextView postInfo = l.findViewById(R.id.postRvPostInfo);
                ImageView postImage = l.findViewById(R.id.postRvPostImage);
                TextView postText = l.findViewById(R.id.postRvPostText);
                postTitle.setText(post.getTitle());
                String postScoreText = Long.toString(post.getScore());
                postScore.setText(postScoreText);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    postInfo.setText(Html.fromHtml(getHtmlDateString(post.getTimestamp()), Html.FROM_HTML_MODE_COMPACT));
                } else {
                    postInfo.setText(Html.fromHtml(getHtmlDateString(post.getTimestamp())));
                }
                postImage.setImageURI(post.getLink());
                postText.setText(post.getText());
                return new PostViewHolder(l);
            case POST_COMMENT:
            default:
                Log.i("ljw", "view created for viewtype " + viewType);
                l = (LinearLayout) LayoutInflater.from(parent.getContext())
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
            // header of comment:
            Comment comment = post.getComments().get(position - 1);
            TextView commentHeader = holder.linearLayout.findViewById(R.id.postRvCommentHeader);
            double distance = comment.getDistanceFromPost() * distanceMultiplier;
            String text = getHtmlDateString(comment.getTimestamp()) + ", " + distance + distanceType + " away";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                commentHeader.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
            } else {
                commentHeader.setText(Html.fromHtml(text));
            }

            // body of comment:
            TextView commentText = holder.linearLayout.findViewById(R.id.postRvCommentText);
            commentText.setText(comment.getText());
        }
    }

    @Override
    public int getItemCount() {
        return 1 + post.getComments().size();
    }

    public String getHtmlDateString(long timestamp) {
        Date date = new java.util.Date(timestamp);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-M-yyyy hh:mm:ss", Locale.US);
        String formattedDate = sdf.format(date);
        return "<i><b>" + post.getUsername() + "</b>, " + formattedDate + "</i>";
    }

}
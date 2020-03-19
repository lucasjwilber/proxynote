package com.lucasjwilber.mapchatapp;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class PostRvAdapter extends RecyclerView.Adapter<PostRvAdapter.PostViewHolder> {

    private Post post;

    // score++ button, score, score-- button, title, report button, 'X' to close button:
    private static final int POST_HEADER = 0;
    // "by [username] at [location], [timestamp]:
    private static final int POST_INFO = 1;
    // attached image or video:
    private static final int POST_MEDIA = 2;
    // actual text of the post:
    private static final int POST_TEXT = 3;
    private static final int POST_REPLY_FORM = 4;
    private static final int POST_REPLY_BUTTON = 5;
    // 6 and above are comments
    private static final int POST_COMMENT = 6;

    PostRvAdapter(Post post) {
        this.post = post;
        Log.i("ljw", "post comments = " + post.getComments().toString());
        Log.i("ljw", "made PostRvAdapter with post that has title " + post.getTitle());
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        LinearLayout linearLayout;
        ImageView imageView;
        EditText editText;
        Button button;

        PostViewHolder(TextView v) {
            super(v);
            textView = v;
        }
        PostViewHolder(LinearLayout l) {
            super(l);
            linearLayout = l;
        }
        PostViewHolder(ImageView i) {
            super(i);
            imageView = i;
        }
        PostViewHolder(EditText e) {
            super(e);
            editText = e;
        }
        PostViewHolder(Button b) {
            super(b);
            button = b;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return POST_HEADER;
        else if (position == 1)
            return POST_INFO;
        else if (position == 2)
            return POST_MEDIA;
        else if (position == 3)
            return POST_TEXT;
        else if (position == 4)
            return POST_REPLY_FORM;
        else if (position == 5)
            return POST_REPLY_BUTTON;
        else
            return POST_COMMENT;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PostRvAdapter.PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView v;
        LinearLayout l;
        ImageView i;
        PostViewHolder vh;
        EditText e;
        Button b;

        switch (viewType) {
            case POST_HEADER:
                l = (LinearLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.postrv_header, parent, false);
                TextView title = l.findViewById(R.id.postRvHeaderTitle);
                title.setText(post.getTitle());
                TextView scoreView = l.findViewById(R.id.postRvHeaderScore);
                scoreView.setText(Integer.toString(post.getScore()));
                vh = new PostViewHolder(l);
                return vh;
            case POST_INFO:
                v = (TextView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.postrv_info, parent, false);

                String postInfo = "Posted by " + post.getUsername() + " from " + post.getLocation() + " at " + post.getTimestamp();
                v.setText(postInfo);
                vh = new PostViewHolder(v);
                return vh;
            case POST_MEDIA:
                i = (ImageView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.postrv_media, parent, false);
                i.setImageURI(post.getLink());
                vh = new PostViewHolder(i);
                return vh;
            case POST_TEXT:
                v = (TextView) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.postrv_text, parent, false);
                v.setText(post.getText());
                vh = new PostViewHolder(v);
                return vh;
            case POST_REPLY_FORM:
                e = new EditText(parent.getContext());
                vh = new PostViewHolder(e);
                return vh;
            case POST_REPLY_BUTTON:
                b = new Button(parent.getContext());
                b.setText("Reply");
                vh = new PostViewHolder(b);
                return vh;
            case POST_COMMENT:
            default:
//                l = (LinearLayout) LayoutInflater.from(parent.getContext())
//                        .inflate(R.layout.postrv_comment, parent, false);
//                TextView commentHeader = l.findViewById(R.id.postRvCommentHeader);
//                commentHeader.setText(post.getComments().get);
//                TextView commentText = l.findViewById(R.id.postRvCommentText);
                v = new TextView(parent.getContext());
                vh = new PostViewHolder(v);
                return vh;
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Log.i("ljw", "there should be " + post.getComments().size() + " comments");

        if (position >= 6) {
//            holder.textView.setText(post.getComments().get(position - 6).getText());
//            Comment comment = post.getComments().get(position - 6);
            String commentText = post.getComments().get(position - 6).getText();
            holder.textView.setText(commentText);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        // should be minimum stuff + length of post.comments
        Log.i("ljw", "there are this many comments: " + post.getComments().size());
        return 6 + post.getComments().size();
    }

}
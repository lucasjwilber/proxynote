package com.lucasjwilber.mapchatapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lucasjwilber.mapchatapp.databinding.ActivityFullScreenMediaBinding;

public class FullScreenMediaActivity extends AppCompatActivity {
    private ActivityFullScreenMediaBinding binding;
    private VideoView vv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenMediaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MediaController controller = new MediaController(this);
        vv = binding.fullScreenVideo;
        vv.setMediaController(controller);

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String url = intent.getStringExtra("url");
        String title = intent.getStringExtra("title");
        String videoThumbnailUrl = intent.getStringExtra("videoThumbnailUrl");

        binding.fullScreenTitle.setText(title);

        // load the image, or if it's a video load the thumbnail initially
        Glide.with(this)
                .load(type.equals("image") ? url : videoThumbnailUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (type.equals("image")) binding.fullScreenPB.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(binding.fullScreenImage);

        // if it's a video load it
        if (type.equals("video")) {
            Uri video = Uri.parse(url);
            vv.setVideoURI(video);
            vv.setVisibility(View.VISIBLE);
            vv.setOnPreparedListener(mp -> {
                binding.fullScreenPB.setVisibility(View.GONE);
                binding.fullScreenImage.setVisibility(View.GONE);
                vv.start();
            });
            //loop video
            vv.setOnCompletionListener(mp -> vv.start());
        }
    }

    public void onBackButtonClicked(View v) {
        finish();
    }

}

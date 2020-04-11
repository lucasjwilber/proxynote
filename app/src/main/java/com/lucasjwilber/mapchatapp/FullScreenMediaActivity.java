package com.lucasjwilber.mapchatapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.lucasjwilber.mapchatapp.databinding.ActivityFullScreenMediaBinding;

public class FullScreenMediaActivity extends AppCompatActivity {
    ActivityFullScreenMediaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenMediaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String url = intent.getStringExtra("url");
        String title = intent.getStringExtra("title");
        String videoThumbnailUrl = intent.getStringExtra("videoThumbnailUrl");

        binding.fullSizeImageTitle.setText(title);

        // load the image, or if it's a video load the thumbnail initially
        Glide.with(this)
                .load((type != null && type.equals("image")) ? url : videoThumbnailUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        binding.fullScreenMediaPB.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(binding.fullSizeImage);

        // if it's a video load the video
        if (type != null && type.equals("video")) {
            VideoView vv = binding.fullScreenVideo;
            vv.setVideoURI(Uri.parse(url));
            vv.setOnInfoListener((mp, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    binding.fullScreenMediaPB.setVisibility(View.GONE);
                    return true;
                }
                else if(what == MediaPlayer.MEDIA_INFO_BUFFERING_START){
                    binding.fullScreenMediaPB.setVisibility(View.VISIBLE);
                    return true;
                }
                return false;
            });
            binding.playVideoButton.setVisibility(View.VISIBLE);
            binding.fullScreenMediaPB.setVisibility(View.GONE);
        }
    }

    public void onBackButtonClicked(View v) {
        finish();
    }

    public void onPlayVideoClicked(View v) {
        VideoView vv = binding.fullScreenVideo;
        binding.fullSizeImage.setVisibility(View.GONE);
        vv.setVisibility(View.VISIBLE);
        binding.playVideoButton.setVisibility(View.GONE);
        binding.pauseVideoButton.setVisibility(View.VISIBLE);
        vv.start();
        vv.setOnCompletionListener(mp -> {
            binding.pauseVideoButton.setVisibility(View.GONE);
            binding.playVideoButton.setVisibility(View.VISIBLE);
        });
    }
    public void onPauseVideoClicked(View v) {
        binding.playVideoButton.setVisibility(View.VISIBLE);
        binding.pauseVideoButton.setVisibility(View.GONE);
        binding.fullScreenVideo.pause();
    }
}

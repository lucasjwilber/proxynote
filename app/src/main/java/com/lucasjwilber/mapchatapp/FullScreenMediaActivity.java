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

        binding.fullSizeImageTitle.setText(title);

        if (type != null && type.equals("image")) {
            Glide.with(this)
                    .load(url)
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
        } else if (type != null && type.equals("video")) {
            binding.fullSizeImage.setVisibility(View.GONE);
            VideoView vv = binding.fullScreenVideo;
            vv.setVisibility(View.VISIBLE);
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
        binding.playVideoButton.setVisibility(View.GONE);
        binding.pauseVideoButton.setVisibility(View.VISIBLE);
        binding.fullScreenVideo.start();
        binding.fullScreenVideo.setOnCompletionListener(mp -> {
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

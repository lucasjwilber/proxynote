package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.bumptech.glide.Glide;

public class FullScreenMediaActivity extends AppCompatActivity {
    ImageView imageView;
    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_media);
        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String url = intent.getStringExtra("url");
        String title = intent.getStringExtra("title");
        TextView titleView = findViewById(R.id.fullSizeImageTitle);
        titleView.setText(title);
        imageView = findViewById(R.id.fullSizeImage);
        videoView = findViewById(R.id.fullScreenVideo);
        if (type != null && type.equals("image")) {
            Glide.with(this).load(url).into(imageView);
        } else if (type != null && type.equals("video")) {
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            Button playVideoButton = findViewById(R.id.playVideoButton);
            playVideoButton.setVisibility(View.VISIBLE);
            videoView.setVideoURI(Uri.parse(url));
        }
    }

    public void onBackButtonClicked(View v) {
        finish();
    }

    public void onPlayVideoClicked(View v) {
        videoView.start();
    }
}

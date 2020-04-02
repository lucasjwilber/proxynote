package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class FullSizeImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_size_image);
        Intent intent = getIntent();
        String imageUrl = intent.getStringExtra("imageUrl");
        ImageView imageView = findViewById(R.id.fullSizeImage);
        Glide.with(this).load(imageUrl).into(imageView);
    }
}

package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onStart() {
        super.onStart();

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
            finish();
        },300);
    }

}
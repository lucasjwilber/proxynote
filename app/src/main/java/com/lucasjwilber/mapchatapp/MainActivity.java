package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get user's current location, then

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                Intent goToMap = new Intent(MainActivity.this, MapActivity.class);
//                goToMap.putExtra("userLocation", userLocation);
                startActivity(goToMap);
                finish();
            }
        },500);
    }

}
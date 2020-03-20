package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.i("ljw", "current user is null");
        } else {
            Log.i("ljw", "current user is" + currentUser.getEmail());
        }

//        SharedPreferences prefs = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit().putString("distanceType", "imperial");
//        editor.apply();

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                Intent intent = new Intent(MainActivity.this,
                        currentUser == null ? LoginActivity.class : MapActivity.class);
                startActivity(intent);
                finish();
            }
        },300);
    }

}
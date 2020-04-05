package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lucasjwilber.mapchatapp.databinding.ActivityMapBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
        PopupMenu.OnMenuItemClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraIdleListener {

    private ActivityMapBinding mapBinding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    FirebaseFirestore db;
    long postQueryLimit = 500;
    FirebaseUser currentUser;
    public static final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 69;
    private static final int LOCATION_UPDATE_COOLDOWN = 15000;
    private static final int POST_QUERY_COOLDOWN = 1750;
    static boolean postQueryIsOnCooldown;
    double userLat;
    double userLng;
    Marker userMarker;
    LatLngBounds cameraBounds;
    public String userCurrentAddress = "somewhere";
    BitmapDescriptor userMarkerIcon;
    BitmapDescriptor postOutlineYellow;
    BitmapDescriptor postOutlineYellowOrange;
    BitmapDescriptor postOutlineOrange;
    BitmapDescriptor postOutlineOrangeRed;
    BitmapDescriptor postOutlineRed;
    BitmapDescriptor postOutlineBrown;
    String currentSelectedPostId;
    private RecyclerView postRv;
    private RecyclerView.Adapter postRvAdapter;
    boolean mapHasBeenSetUp;
    SharedPreferences sharedPreferences;
    List<Marker> postMarkers;
    Handler periodicLocationUpdateHandler;
    private boolean userIsEmailVerified;
    private Handler emailVerificationCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapBinding = ActivityMapBinding.inflate(getLayoutInflater());
        View view = mapBinding.getRoot();
        setContentView(view);

        // post recyclerview
        postRv = mapBinding.postRecyclerView;
        postRv.setLayoutManager(new LinearLayoutManager(this));
        postMarkers = new LinkedList<>();

        userMarkerIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.user_location_pin));
        postOutlineYellow = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_yellow));
        postOutlineYellowOrange = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_yelloworange));
        postOutlineOrange = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_orange));
        postOutlineOrangeRed = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_orangered));
        postOutlineRed = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_red));
        postOutlineBrown = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_brown));

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getApplicationContext().getSharedPreferences("mapchatPrefs", Context.MODE_PRIVATE);
        periodicLocationUpdateHandler = new Handler();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get location permission if necessary, then get location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserLatLng(true);
        }

        emailVerificationCheckRunnable = new Handler();

    }

    @Override
    public void onResume() {
        super.onResume();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            userIsEmailVerified = true;
        }

        startGetLocationLooper();
        if (mMap != null) getPostsFromDbAndCreateMapMarkers();
    }

    @Override
    public void onPause() {
        super.onPause();
        periodicLocationUpdateHandler.removeCallbacksAndMessages(null);   //aka stopGetLocationLooper
    }

    private void startGetLocationLooper() {
        if (mMap != null) {
            periodicLocationUpdateHandler.removeCallbacksAndMessages(null);

            periodicLocationUpdateHandler.postDelayed(new Runnable() {
                public void run() {
                    getUserLatLng(false);
                    Log.i("ljw", "updated location from the runnable: " + userLat + "/" + userLng);
                    //this is nulled on activity pause, in which case we don't want to start another one
                    if (periodicLocationUpdateHandler != null) {
                        periodicLocationUpdateHandler.postDelayed(this, LOCATION_UPDATE_COOLDOWN);
                    }
                }
            }, LOCATION_UPDATE_COOLDOWN);
        }
    }

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this::onMenuItemClick);
        popup.inflate(R.menu.hamburger_menu);

        Menu menu = popup.getMenu();
        if (currentUser == null) {
            menu.getItem(0).setVisible(false);
        } else {
            menu.getItem(3).setTitle("Log Out");
        }
        menu.getItem(0).setIcon(getDrawable(R.drawable.posticon_128064));
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        SharedPreferences.Editor editor;

        switch (item.getItemId()) {
            case R.id.menuProfile:
                hideAllModals();
                Intent goToProfile = new Intent(this, UserProfileActivity.class);
                goToProfile.putExtra("userId", currentUser.getUid());
                startActivity(goToProfile);
                return true;
            case R.id.menuHelp:
                Intent goToHelp = new Intent(MapActivity.this, HelpActivity.class);
                startActivity(goToHelp);
                return true;
            case R.id.menuLoginLogout:
                if (currentUser == null) { //go to login/signup page
                    postRv.setVisibility(View.GONE);
                    Intent i = new Intent(this, LoginActivity.class);
                    startActivity(i);
                } else { //log out
                    FirebaseAuth.getInstance().signOut();
                    Log.i("ljw", "user logged out");
                    Utils.showToast(MapActivity.this, "You are now logged out.");
                    currentUser = null;
                }
                return true;
            case R.id.mapTypeNormal:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                editor = sharedPreferences.edit().putInt("mapType", GoogleMap.MAP_TYPE_NORMAL);
                editor.apply();
                return true;
            case R.id.mapTypeSatellite:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                editor = sharedPreferences.edit().putInt("mapType", GoogleMap.MAP_TYPE_SATELLITE);
                editor.apply();
                return true;
            case R.id.mapTypeHybrid:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                editor = sharedPreferences.edit().putInt("mapType", GoogleMap.MAP_TYPE_HYBRID);
                editor.apply();
                return true;
            case R.id.mapTypeTerrain:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                editor = sharedPreferences.edit().putInt("mapType", GoogleMap.MAP_TYPE_TERRAIN);
                editor.apply();
                return true;
            case R.id.distanceTypeImperial:
                editor = sharedPreferences.edit().putString("distanceType", "imperial");
                editor.apply();
                return true;
            case R.id.distanceTypeMetric:
                editor = sharedPreferences.edit().putString("distanceType", "metric");
                editor.apply();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //remove the directions/gps buttons
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerClickListener(this::onMarkerClick);
        mMap.setOnMapClickListener(this::onMapClick);
        mMap.setOnCameraIdleListener(this::onCameraIdle);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //location permission
        if (requestCode == FINE_LOCATION_PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission granted:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.i("ljw", "location permission granted, getting location...");
                    getUserLatLng(true);
                }
            } else {
                Log.i("ljw", "location permission denied");
            }
        }
    }

    public void getUserLatLng(boolean centerOnUser) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    Log.i("ljw", "successfully got location");
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        Log.i("ljw", "lat: " + userLat + "\nlong: " + userLng);

                        if (!mapHasBeenSetUp) mapViewSetup();
                        if (centerOnUser) mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(userLat, userLng)));

                        if (userMarker != null) userMarker.remove();
                        userMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(userLat, userLng))
                                .title("My Location")
                                .icon(userMarkerIcon));

                        startGetLocationLooper();
                    }
                })
                .addOnFailureListener(this, error -> {
                    Log.i("ljw", "error getting location:\n" + error.toString());
                });
    }

    public void mapViewSetup() {
        Intent intent = getIntent();
        double lat = intent.getDoubleExtra("lat", 0d);
        double lng = intent.getDoubleExtra("lng", 0d);
        Log.i("ljw", "from intent: " + lat + "/" + lng);
        Log.i("ljw", "user: " + userLat + "/" + userLng);

        //if lat/lng is 0/0, either by the db field being null or an error with the intent,
        //just center on the user instead of an incorrect location
        if (lat == 0d && lng == 0d) {
            lat = userLat;
            lng = userLng;
        }

        double finalLat = lat;
        double finalLng = lng;
        Log.i("ljw", "going to " + finalLat + "/" + finalLng);
        AsyncTask.execute(() -> {
            //update map on main thread
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message input) {

                    //if we got here from a click on the "show on map" button in a user profile,
                    //center on that marker, else center on the user's location.
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(finalLat, finalLng)));
                    cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    //https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap#setMapType(int)
                    mMap.setMapType(sharedPreferences.getInt("mapType", GoogleMap.MAP_TYPE_HYBRID));
                    // mMap.setMinZoomPreference(10f);

                    if (userMarker != null) userMarker.remove();
                    userMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(userLat, userLng))
                            .title("My Location")
                            .icon(userMarkerIcon));

                    //sleep for a moment because zooming and centering the camera at the same time
                    // causes an issue where the camera ends up zoomed on the wrong location.
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15f));
                }
            };
            handler.obtainMessage().sendToTarget();

            mapHasBeenSetUp = true;
        });
    }

    public void onCreatePostButtonClick(View v) {
        hideAllModals();

        if (currentUser == null) {
            String text = "You must be logged in to post.";
            mapBinding.mapLoginSuggestion.setText(text);
            mapBinding.mapLoginSuggestionModal.setVisibility(View.VISIBLE);
            return;
        } else if (!userIsEmailVerified) {
            mapBinding.verifyEmailReminder.setVisibility(View.VISIBLE);
            startVerificationListener();
            return;
        } else {
            Intent goToCreatePostAct = new Intent(this, CreatePostActivity.class);
            startActivity(goToCreatePostAct);
        }
    }

    @Override
    public void onCameraIdle() {
        cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Log.i("ljw", "camera bounds: " + cameraBounds.toString());

        // query db for posts near the user
//        getPostsFromDbAndCreateMapMarkers();

        if (!postQueryIsOnCooldown) {
            getPostsFromDbAndCreateMapMarkers();
            postQueryIsOnCooldown = true;
            Log.i("ljw", "post query is on cooldown");

            Handler postQueryCooldownHandler = new Handler();
            postQueryCooldownHandler.postDelayed(new Runnable() {
                public void run() {
                    postQueryIsOnCooldown = false;
                    Log.i("ljw", "post query is off cooldown");
                }
            }, POST_QUERY_COOLDOWN);
        }
    }

    public void getPostsFromDbAndCreateMapMarkers() {
        // get only posts within a certain radius of the user
        Double latZone = Math.round(userLat * 10) / 10.0;

        // remove old markers
        for (Marker m : postMarkers) m.remove();

        Log.i("ljw", "getting posts from " + cameraBounds.southwest.longitude + " to " + cameraBounds.northeast.longitude);

        db.collection("posts")
                .whereLessThan("lng", cameraBounds.northeast.longitude)
                .whereGreaterThan("lng", cameraBounds.southwest.longitude)
                // firestore only allows one range query, so latitude is broken into zones and we get the closest ones with a logical OR
                // the max is 10, we use 9 to make it symmetrical
                // the range of 0.9 latitude is about 54 miles
                .whereIn("latZone", Arrays.asList(
                        latZone - 0.4f,
                        latZone - 0.3f,
                        latZone - 0.2f,
                        latZone - 0.1f,
                        latZone,
                        latZone + 0.1f,
                        latZone + 0.2f,
                        latZone + 0.3f,
                        latZone + 0.4f))
                .orderBy("lng")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(postQueryLimit)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                Post post = Objects.requireNonNull(document.toObject(Post.class));

                                // for whatever reason the Post created by document.toObject doesn't include the comments list
                                // they are however present as an ArrayList of HashMaps
                                ArrayList list = (ArrayList) document.getData().get("comments");
                                post.setComments(Utils.turnMapsIntoListOfComments(list));
                                createMarkerWithPost(post);
                            }
                        } else {
                            Log.i("ljw", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public void createMarkerWithPost(Post post) {
        // this is used to prevent marker overlap
        float zIndex = (float) Math.random();

        Marker borderMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(post.getLat(), post.getLng()))
                .anchor(0, 1)
                .zIndex(zIndex)
        );

        int score = post.getScore();
        if (score >= 20) {
            borderMarker.setIcon(postOutlineRed);
        } else if (score >= 15) {
            borderMarker.setIcon(postOutlineOrangeRed);
        } else if (score >= 10) {
            borderMarker.setIcon(postOutlineOrange);
        } else if (score >= 5) {
            borderMarker.setIcon(postOutlineYellowOrange);
        } else if (score <= -5) {
            borderMarker.setIcon(postOutlineBrown);
        } else {
            borderMarker.setIcon(postOutlineYellow);
        }

        Marker iconMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(post.getLat(), post.getLng()))
                .anchor(-0.4f, 1.575f)
                .zIndex(zIndex + 0.0001f)
                .icon(Utils.getPostIconBitmapDescriptor(post.getIcon(), this))
        );
        iconMarker.setTag(post.getId());

        postMarkers.add(borderMarker);
        postMarkers.add(iconMarker);
    }

    public void onMapClick(LatLng latlng) {
        hideAllModals();
        emailVerificationCheckRunnable.removeCallbacksAndMessages(null);
    }

    private void hideAllModals() {
        postRv.setVisibility(View.GONE);
        postRv.setAdapter(null);
        mapBinding.mapLoginSuggestionModal.setVisibility(View.GONE);
        mapBinding.verifyEmailReminder.setVisibility(View.GONE);
    }

    private Bitmap getBitmap(int drawableRes) {
        Drawable drawable = getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i("ljw", "clicked on a marker");

        if (marker.getTag() == null) {
            Log.i("ljw", "no post tagged to the clicked on marker, hmm");
            return true;
        }

        currentSelectedPostId = (String) marker.getTag();
        setPostRvAdapter(currentSelectedPostId);
        postRv.setVisibility(View.VISIBLE);

        // returning true instead would prevent the camera centering/info window opening
        return false;
    }

    private void setPostRvAdapter(String postId) {
        mapBinding.mapPostRvProgressBar.setVisibility(View.VISIBLE);

        if (postId != null) {
            db.collection("posts")
                    .document(postId)
                    .get()
                    .addOnSuccessListener(result -> {
                        Log.i("ljw", "got post " + result.getId());
                        Post post = result.toObject(Post.class);
                        if (post != null) {
                            ArrayList list = (ArrayList) result.getData().get("comments");
                            post.setComments(Utils.turnMapsIntoListOfComments(list));
                        }

                        postRvAdapter = new PostRvAdapter(
                                post,
                                MapActivity.this,
                                currentUser != null ? currentUser.getUid() : null,
                                currentUser != null ? currentUser.getDisplayName() : null,
                                null,
                                db
                        );
                        postRv.setAdapter(postRvAdapter);
                        //set border color of this based on the post score
                        if (post.getScore() >= 20) {
                            postRv.setBackground(getDrawable(R.drawable.rounded_square_red));
                        } else if (post.getScore() >= 15) {
                            postRv.setBackground(getDrawable(R.drawable.rounded_square_orangered));
                        } else if (post.getScore() >= 10) {
                            postRv.setBackground(getDrawable(R.drawable.rounded_square_orange));
                        } else if (post.getScore() >= 5) {
                            postRv.setBackground(getDrawable(R.drawable.rounded_square_yelloworange));
                        } else if (post.getScore() <= -5) {
                            postRv.setBackground(getDrawable(R.drawable.rounded_square_brown));
                        } else {
                            postRv.setBackground(getDrawable(R.drawable.rounded_square_yellow));
                        }
                        mapBinding.mapPostRvProgressBar.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> Log.i("ljw", "error getting post: " + e.toString()));
        }
    }

    public void onLoginSuggestionButtonClick(View v) {
        Intent goToLogin = new Intent(MapActivity.this, LoginActivity.class);
        postRv.setVisibility(View.GONE);
        mapBinding.mapLoginSuggestionModal.setVisibility(View.GONE);
        startActivity(goToLogin);
    }

    public void onResendEmailVerificationClick(View v) {
        if (currentUser != null) {
            currentUser.sendEmailVerification()
                    .addOnSuccessListener(r -> {
                        Utils.showToast(MapActivity.this, "Verification email sent.");
                    });
        }
    }

    private void startVerificationListener() {
        emailVerificationCheckRunnable.postDelayed(new Runnable() {
            public void run() {
                if (currentUser != null) {
                    currentUser.reload();
                }
                if (currentUser != null && currentUser.isEmailVerified()) {
                    emailVerificationCheckRunnable.removeCallbacksAndMessages(null);
                    userIsEmailVerified = true;
                    Utils.showToast(MapActivity.this, "Email verified!");
                    mapBinding.verifyEmailReminder.setVisibility(View.GONE);
                } else {
                    Log.i("ljw", "user still not verified");
                    emailVerificationCheckRunnable.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }

}

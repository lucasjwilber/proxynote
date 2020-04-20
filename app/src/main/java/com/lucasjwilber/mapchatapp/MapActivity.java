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
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

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
import com.google.android.gms.maps.model.MapStyleOptions;
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
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
        PopupMenu.OnMenuItemClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraIdleListener {

    private final String TAG = "ljw";
    private ActivityMapBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private long POSTS_PER_ZONE_LIMIT = 20; //this * 1.5 is roughly how many posts are queried per square mile. update the Help string when changing this
    private FirebaseUser currentUser;
    private final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 69;
    private final int LOCATION_UPDATE_COOLDOWN = 30000;
    private double userLat;
    private double userLng;
    private Marker userMarker;
    private LatLngBounds cameraBounds;
    private BitmapDescriptor userLocationIcon;
    private BitmapDescriptor postOutlineYellow;
    private BitmapDescriptor postOutlineYellowOrange;
    private BitmapDescriptor postOutlineOrange;
    private BitmapDescriptor postOutlineOrangeRed;
    private BitmapDescriptor postOutlineRed;
    private BitmapDescriptor postOutlineBrown;
    private String currentSelectedPostId;
    private RecyclerView postRv;
    private RecyclerView.Adapter postRvAdapter;
    private boolean mapHasBeenSetUp;
    SharedPreferences sharedPreferences;
    private boolean areMarkersShown;
    private Handler periodicLocationUpdateHandler;
    private List<Marker> markerList;
    private HashSet<String> zoneCacheTiny;
    private HashSet<String> zoneCacheSmall;
    private HashSet<String> zoneCacheMedium;
    private HashSet<String> zoneCacheLarge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postRv = binding.postRecyclerView;
        postRv.setHasFixedSize(true);
        postRv.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getApplicationContext().getSharedPreferences("mapchatPrefs", Context.MODE_PRIVATE);
        periodicLocationUpdateHandler = new Handler();

//        markerListTiny = new LinkedList<>();
//        markerListSmall = new LinkedList<>();
//        markerListMedium = new LinkedList<>();
//        markerListLarge = new LinkedList<>();
        markerList = new LinkedList<>();

        userLocationIcon = Utils.getBitmapDescriptorFromSvg(R.drawable.user_location_pin, MapActivity.this);
        postOutlineYellow = Utils.getBitmapDescriptorFromSvg(R.drawable.postoutline_yellow, MapActivity.this);
        postOutlineYellowOrange = Utils.getBitmapDescriptorFromSvg(R.drawable.postoutline_yelloworange, MapActivity.this);
        postOutlineOrange = Utils.getBitmapDescriptorFromSvg(R.drawable.postoutline_orange, MapActivity.this);
        postOutlineOrangeRed = Utils.getBitmapDescriptorFromSvg(R.drawable.postoutline_orangered, MapActivity.this);
        postOutlineRed = Utils.getBitmapDescriptorFromSvg(R.drawable.postoutline_red, MapActivity.this);
        postOutlineBrown = Utils.getBitmapDescriptorFromSvg(R.drawable.postoutline_brown, MapActivity.this);



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

    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

//        createTestPosts();

        //post data may have changed. delete all stored markers, clear the caches, and get posts
        refreshMapData(null);

        startGetLocationLooper();

        // if the postRv is open, refresh it to prevent vote manipulation via out-of-date scores
        if (postRv.getVisibility() == View.VISIBLE && mMap != null) {
            postRv.setVisibility(View.GONE);
            postRvAdapter = null;

            // make sure it wasn't just deleted before showing it
            db.collection("posts")
                    .document(currentSelectedPostId)
                    .get()
                    .addOnSuccessListener(result -> {
                        if (result.exists()) {
                            setPostRvAdapter(currentSelectedPostId);
                            postRv.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        periodicLocationUpdateHandler.removeCallbacksAndMessages(null);   //aka stopGetLocationLooper
    }

    private void startGetLocationLooper() {
        if (mMap != null) {
            periodicLocationUpdateHandler.removeCallbacksAndMessages(null);

            //update userLat and userLng on a loop:
            periodicLocationUpdateHandler.postDelayed(new Runnable() {
                public void run() {
                    getUserLatLng(false);
                    Log.i(TAG, "updated location from the runnable: " + userLat + "/" + userLng);
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
            menu.getItem(4).setTitle("Log Out");
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
                    Log.i(TAG, "user logged out");
                    Utils.showToast(MapActivity.this, "You are now logged out.");
                    currentUser = null;
                }
                return true;

            // map styles //
            case R.id.mapStyleDay:
            case R.id.mapStyleNight:
            case R.id.mapStyleLightGray:
            case R.id.mapStyleDarkGray:
            case R.id.mapStyleCobalt:
            case R.id.mapStyleSatellite:
                return setMapStyle(item.getItemId());

            //distance types
            case R.id.distanceTypeImperial:
                editor = sharedPreferences.edit().putString("distanceType", "imperial");
                editor.apply();
                return true;
            case R.id.distanceTypeMetric:
                editor = sharedPreferences.edit().putString("distanceType", "metric");
                editor.apply();
                return true;

            //filter options
//            case R.id.filterAgeNone:
//                editor = sharedPreferences.edit().putLong("postMaxAge", 0L);
//                editor.apply();
//                refreshMapData(null);
//                return true;
//            case R.id.filterAgeOneDay:
//                editor = sharedPreferences.edit().putLong("postMaxAge", 86400000L);
//                editor.apply();
//                refreshMapData(null);
//                return true;
//            case R.id.filterAgeThreeDays:
//                editor = sharedPreferences.edit().putLong("postMaxAge", 259200000L);
//                editor.apply();
//                refreshMapData(null);
//                return true;
//            case R.id.filterAgeOneWeek:
//                editor = sharedPreferences.edit().putLong("postMaxAge", 604800000L);
//                editor.apply();
//                refreshMapData(null);
//                return true;
            default:
                return false;
        }
    }

    private boolean setMapStyle(int id) {
        SharedPreferences.Editor editor;
        switch (id) {
            default:
            case R.id.mapStyleDay:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                try {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.nature));
                    editor = sharedPreferences.edit().putInt("mapStyle", R.id.mapStyleDay);
                    editor.apply();
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Can't find style. Error: ", e);
                }
                return true;
            case R.id.mapStyleNight:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                try {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.simple_night_vision));
                    editor = sharedPreferences.edit().putInt("mapStyle", R.id.mapStyleNight);
                    editor.apply();
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Can't find style. Error: ", e);
                }
                return true;
            case R.id.mapStyleLightGray:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                try {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.grayscale));
                    editor = sharedPreferences.edit().putInt("mapStyle", R.id.mapStyleLightGray);
                    editor.apply();
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Can't find style. Error: ", e);
                }
                return true;
            case R.id.mapStyleDarkGray:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                try {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.lunar_landscape));
                    editor = sharedPreferences.edit().putInt("mapStyle", R.id.mapStyleDarkGray);
                    editor.apply();
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Can't find style. Error: ", e);
                }
                return true;
            case R.id.mapStyleCobalt:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                try {
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.cobalt));
                    editor = sharedPreferences.edit().putInt("mapStyle", R.id.mapStyleCobalt);
                    editor.apply();
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Can't find style. Error: ", e);
                }
                return true;
            case R.id.mapStyleSatellite:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                editor = sharedPreferences.edit().putInt("mapStyle", R.id.mapStyleSatellite);
                editor.apply();
                return true;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //remove the directions/gps/compass buttons
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);

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
                    Log.i(TAG, "location permission granted, getting location...");
                    getUserLatLng(true);
                }
            } else {
                Log.i(TAG, "location permission denied");
            }
        }
    }

    public void getUserLatLng(boolean centerOnUser) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    Log.i(TAG, "successfully got location");
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        Log.i(TAG, "lat: " + userLat + "\nlong: " + userLng);

                        if (!mapHasBeenSetUp) mapViewSetup();
                        if (centerOnUser) mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(userLat, userLng)));

                        if (userMarker != null) userMarker.remove();
                        userMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(userLat, userLng))
                                .title("My Location")
                                .icon(userLocationIcon));

                        startGetLocationLooper();
                    }
                })
                .addOnFailureListener(this, error -> {
                    Log.i(TAG, "error getting location:\n" + error.toString());
                });
    }

    public void mapViewSetup() {
        Intent intent = getIntent();
        double lat = intent.getDoubleExtra("lat", 0d);
        double lng = intent.getDoubleExtra("lng", 0d);
        Log.i(TAG, "from intent: " + lat + "/" + lng);
        Log.i(TAG, "user: " + userLat + "/" + userLng);

        //if lat/lng is 0/0, either by the db field being null or an error with the intent,
        //just center on the user instead of an incorrect location
        if (lat == 0d && lng == 0d) {
            lat = userLat;
            lng = userLng;
        }

        double finalLat = lat;
        double finalLng = lng;
        Log.i(TAG, "going to " + finalLat + "/" + finalLng);
        AsyncTask.execute(() -> {
            //update map on main thread
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message input) {

                    //if we got here from a click on the "show on map" button in a user profile,
                    //center on that marker, else center on the user's location.
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(finalLat, finalLng)));
                    cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;

                    setMapStyle(sharedPreferences.getInt("mapStyle", R.id.mapStyleDay));

                    mMap.setMinZoomPreference(5f);

                    if (userMarker != null) userMarker.remove();
                    userMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(userLat, userLng))
                            .title("My Location")
                            .icon(userLocationIcon));

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
            binding.mapLoginSuggestion.setText(text);
            binding.mapLoginSuggestionModal.setVisibility(View.VISIBLE);
        } else if (!currentUser.isEmailVerified()) {
            //reload and check again first
            currentUser.reload()
                .addOnSuccessListener(r -> {
                    if (!currentUser.isEmailVerified()) {
                        Utils.showToast(MapActivity.this, "Please verify your email first.");
                    }
                });
        } else {
            Intent goToCreatePostAct = new Intent(this, CreatePostActivity.class);
            startActivity(goToCreatePostAct);
        }
    }

    @Override
    public void onCameraIdle() {
        cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        if (Utils.getZoneType(cameraBounds).equals("tinyZone")) {
            getPosts(zoneCacheTiny);
        } else if (Utils.getZoneType(cameraBounds).equals("smallZone")) {
            getPosts(zoneCacheSmall);
        } else if (Utils.getZoneType(cameraBounds).equals("mediumZone")) {
            getPosts(zoneCacheMedium);
        } else {
            getPosts(zoneCacheLarge);
        }
    }

    private void getPosts(HashSet<String> cache) {
        List<String> zonesOnScreen = Utils.getZonesOnScreen(cameraBounds);
        Log.i(TAG, "the " + zonesOnScreen.toString() + " zones are on screen");
        Log.i(TAG, "zoneType is " + Utils.getZoneType(cameraBounds));

        binding.postQueryRefreshButton.setVisibility(View.GONE);
        binding.postQueryPB.setVisibility(View.VISIBLE);

        //check the zones on screen against the cache
        for (String zone : zonesOnScreen) {
            if (!cache.contains(zone)) {

                db.collection("posts")
                        .whereEqualTo(Utils.getZoneType(cameraBounds), zone)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(POSTS_PER_ZONE_LIMIT)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                        Post post = Objects.requireNonNull(document.toObject(Post.class));
                                        ArrayList list = (ArrayList) document.getData().get("comments");
                                        post.setComments(Utils.turnMapsIntoListOfComments(list));
                                        createMarkerWithPost(post);
                                    }
                                    Log.i(TAG, "got " + task.getResult().size() + " posts from zone " + zone);

                                    cache.add(zone);

                                    binding.postQueryPB.setVisibility(View.GONE);
                                    binding.postQueryRefreshButton.setVisibility(View.VISIBLE);
                                } else {
                                    Log.i(TAG, "Error getting documents.", task.getException());
                                    binding.postQueryPB.setVisibility(View.GONE);
                                    binding.postQueryRefreshButton.setVisibility(View.VISIBLE);
                                }
                            }
                        });


                Log.i(TAG, "cached and queried zone " + zone);
            }
        }
    }

    public void createMarkerWithPost(Post post) {
        float zIndex = (float) post.getTimestamp();

        Marker borderMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(post.getLat(), post.getLng()))
                .anchor(0, 1)
                .zIndex(zIndex)
                .visible(areMarkersShown)
        );
        borderMarker.setTag(post.getId());

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
                .zIndex(zIndex + 1f)
                .icon(BitmapDescriptorFactory.fromBitmap(Utils.getPostIconBitmap(post.getIcon(), this)))
                .visible(areMarkersShown)
        );
        iconMarker.setTag(post.getId());

        markerList.add(borderMarker);
        markerList.add(iconMarker);
    }

    public void onMapClick(LatLng latlng) {
        hideAllModals();
    }

    private void hideAllModals() {
        postRv.setVisibility(View.GONE);
        postRv.setAdapter(null);
        binding.mapLoginSuggestionModal.setVisibility(View.GONE);
        binding.verifyEmailReminder.setVisibility(View.GONE);
        binding.mapPostRvProgressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() == null) return true;

        currentSelectedPostId = (String) marker.getTag();
        setPostRvAdapter(currentSelectedPostId);
        postRv.setVisibility(View.VISIBLE);

        // returning false would center the camera on the marker and open the info window if there is one attached
        return true;
    }

    private void setPostRvAdapter(String postId) {
        binding.mapPostRvProgressBar.setVisibility(View.VISIBLE);

        if (postId != null) {
            db.collection("posts")
                    .document(postId)
                    .get()
                    .addOnSuccessListener(result -> {
                        Post post = result.toObject(Post.class);
                        if (post == null) {
                            Utils.showToast(MapActivity.this, "This post no longer exists.");
                            hideAllModals();
                            return;
                        }

                        ArrayList list = (ArrayList) result.getData().get("comments");
                        post.setComments(Utils.turnMapsIntoListOfComments(list));

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
                        binding.mapPostRvProgressBar.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> Log.i(TAG, "error getting post: " + e.toString()));
        }
    }

    public void onLoginSuggestionButtonClick(View v) {
        Intent goToLogin = new Intent(MapActivity.this, LoginActivity.class);
        hideAllModals();
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

//    private void createTestPosts() {
//
//        //50/50
//        //start at lat - 25(x), lng -25(x)
//        // lat/lng += x in a for(for
//        //x is random
//        //every 0.005 * (random + 1)
//
//        for (double i = userLng - 0.25; i < userLng + 0.25; i+= 0.01 * (Math.random() + 1)) {
//            for (double j = userLat - 0.25; j < userLat + 0.25; j+= 0.01 * (Math.random() + 1)) {
//                Post post = new Post(
//                        UUID.randomUUID().toString(),
//                        "KfMLnuMWyxbAsKAJt812LLWwXEu2",
//                        "tester",
//                        "test",
//                        "testststsetest",
//                        "somewhere",
//                        j,
//                        i);
//                db.collection("posts").add(post);
//            }
//        }
//    }

    public void toggleMarkerVisibility(View v) {
        for (Marker marker : markerList) marker.setVisible(areMarkersShown);
        v.setBackground(areMarkersShown ? getDrawable(R.drawable.visibility) : getDrawable(R.drawable.visibility_off));
        areMarkersShown = !areMarkersShown;
    }

    public void refreshMapData(View v) {
        for (Marker marker : markerList) marker.remove();
        zoneCacheTiny = new HashSet<>();
        zoneCacheSmall = new HashSet<>();
        zoneCacheMedium = new HashSet<>();
        zoneCacheLarge = new HashSet<>();

        if (mMap != null && cameraBounds != null) {
            if (Utils.getZoneType(cameraBounds).equals("tinyZone")) {
                getPosts(zoneCacheTiny);
            } else if (Utils.getZoneType(cameraBounds).equals("smallZone")) {
                getPosts(zoneCacheSmall);
            } else if (Utils.getZoneType(cameraBounds).equals("mediumZone")) {
                getPosts(zoneCacheMedium);
            } else {
                getPosts(zoneCacheLarge);
            }
        }
    }

}

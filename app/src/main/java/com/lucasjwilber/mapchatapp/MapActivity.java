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
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    FirebaseUser user;
    public static final int FINE_LOCATION_PERMISSION_REQUEST_CODE = 69;
    double postsRadius = 0.05;
    String currentUsername = "someone";
    String currentUserId = "some id";
    double userLat;
    double userLng;
    Marker userMarker;
    LatLngBounds cameraBounds;
    public String userCurrentAddress = "somewhere";
    TextView userLocationTV;
    BitmapDescriptor userMarkerIcon;
    BitmapDescriptor postOutlineYellow;
    BitmapDescriptor postOutlineYellowOrange;
    BitmapDescriptor postOutlineOrange;
    BitmapDescriptor postOutlineOrangeRed;
    BitmapDescriptor postOutlineRed;
    BitmapDescriptor postOutlineBrown;
    Post currentSelectedPost;
    Marker currentSelectedMarker;
    private RecyclerView postRv;
    private RecyclerView.Adapter postRvAdapter;
    private RecyclerView.LayoutManager postRvLayoutManager;
    boolean mapHasBeenSetUp;
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapBinding = ActivityMapBinding.inflate(getLayoutInflater());
        View view = mapBinding.getRoot();
        setContentView(view);


        // post recyclerview
        postRv = mapBinding.postRecyclerView;
        postRvLayoutManager = new LinearLayoutManager(this);
        postRv.setLayoutManager(postRvLayoutManager);

        userMarkerIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.user_location_pin));
        postOutlineYellow = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_yellow));
        postOutlineYellowOrange = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_yelloworange));
        postOutlineOrange = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_orange));
        postOutlineOrangeRed = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_orangered));
        postOutlineRed = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_red));
        postOutlineBrown = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.postoutline_brown));

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        sharedPreferences = getApplicationContext().getSharedPreferences("mapchatPrefs", Context.MODE_PRIVATE);


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

        getUserLatLng(false);
    }

    public void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this::onMenuItemClick);
        popup.inflate(R.menu.hamburger_menu);

        Menu menu = popup.getMenu();
        if (user == null) {
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
                Intent goToProfile = new Intent(this, UserProfileActivity.class);
                goToProfile.putExtra("userId", user.getUid());
                startActivity(goToProfile);
                return true;
            case R.id.menuHelp:
                return true;
            case R.id.menuLoginLogout:
                if (user == null) { //go to login/signup page
                    Intent i = new Intent(this, LoginActivity.class);
                    startActivity(i);
                } else { //log out
                    FirebaseAuth.getInstance().signOut();
                    Log.i("ljw", "user logged out");
                    user = null;
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
                    }
                })
                .addOnFailureListener(this, error -> {
                    Log.i("ljw", "error getting location:\n" + error.toString());
                });
    }

    public void mapViewSetup() {
        AsyncTask.execute(() -> {
            //update map on main thread
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message input) {
                    //center the map on the user
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(userLat, userLng)));
                    cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    //https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap#setMapType(int)
                    mMap.setMapType(sharedPreferences.getInt("mapType", GoogleMap.MAP_TYPE_SATELLITE));
                    // mMap.setMinZoomPreference(10f);

                    if (userMarker != null) userMarker.remove();
                    userMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(userLat, userLng))
                            .title("My Location")
                            .icon(userMarkerIcon)
                            .snippet(userCurrentAddress));

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
        if (user == null) {
            //TODO: modal with "sign in to post"
            return;
        }

        postRv.setVisibility(View.GONE);
        Intent goToCreatePostAct = new Intent(this, CreatePostActivity.class);
        goToCreatePostAct.putExtra("userLat", userLat);
        goToCreatePostAct.putExtra("userLng", userLng);
        goToCreatePostAct.putExtra("userCurrentAddress", userCurrentAddress);
        startActivity(goToCreatePostAct);
    }

    @Override
    public void onCameraIdle() {
        cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Log.i("ljw", "camera bounds: " + cameraBounds.toString());

        // query db for posts near the user
        getPostsFromDbAndCreateMapMarkers();
    }

    public void getPostsFromDbAndCreateMapMarkers() {
        // get only posts within a certain radius of the user
        Double latZone = Math.round(userLat * 10) / 10.0;

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

                                Log.i("ljw", "found post \"" + post.getTitle() + "/" + post.getText() + "\" with id " + post.getId() + "from document with id " + document.getId());
                            }
                        } else {
                            Log.i("ljw", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    public Marker createMarkerWithPost(Post post) {
        Marker borderMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(post.getLat(), post.getLng()))
                .anchor(0, 1)
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
                .zIndex(1.0f)
        );
        iconMarker.setIcon(Utils.getPostIconBitmapDescriptor(post.getIcon(), this));
        iconMarker.setTag(post);


        return borderMarker;
    }

    public void getUsersFormattedAddress() {
        Log.i("ljw", "calling geocode api...");

        try {
            URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + userLat + "," + userLng + "&key=AIzaSyDEcxFt2-EK-4UN2IBzj0gTkegHKzyxHpk");

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            Log.i("ljw", "called api, reading response...");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = in.readLine()) != null) {
                content.append(line);
                if (line.contains("formatted_address")) {
                    userCurrentAddress = line.split("\" : \"")[1];
                    userCurrentAddress = userCurrentAddress.substring(0, userCurrentAddress.length() - 2);
                    Log.i("ljw", "found formatted addresss: " + userCurrentAddress);
                    break;
                }
            }
            in.close();
            con.disconnect();

            String postingFromString = "Posting from " + userCurrentAddress;
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message input) {
                    userLocationTV.setText(postingFromString);
                }
            };
            handler.obtainMessage().sendToTarget();

        } catch (MalformedURLException e) {
            Log.i("ljw", "malformedURLexception:\n" + e.toString());
        } catch (ProtocolException e) {
            Log.i("ljw", "protocol exception:\n" + e.toString());
        } catch (IOException e) {
            Log.i("ljw", "IO exception:\n" + e.toString());
        }
    }

    public void onMapClick(LatLng arg0) {
        postRv.setVisibility(View.GONE);
    }

    public void addCommentToPost(View v) {
        if (user == null) {
            //TODO: modal with "sign in to comment"
            return;
        }

        Log.i("ljw", "commentbutton clicked");
        EditText commentEditText = findViewById(R.id.postRvPostReplyBox);

        if (commentEditText.getText().toString().equals("") || commentEditText.getText().toString().length() == 0) {
            Log.i("ljw", "empty comment, not gonna add it to the post");
            return;
        } else if (currentSelectedPost.getId() == null) {
            Log.i("ljw", "post has a null id so a DB query won't work");
            return;
        }

        double distanceFromPost = Utils.getDistance(userLat, userLng, currentSelectedPost.getLat(), currentSelectedPost.getLng());

        Comment comment = new Comment(user.getUid(),
                user.getDisplayName(),
                commentEditText.getText().toString(),
                userLat,
                userLng,
                distanceFromPost);

        List<Comment> comments = currentSelectedPost.getComments();
        comments.add(comment);
        Log.i("ljw", comments.toString());

        //get post by id from firestore
        db.collection("posts")
                .document(currentSelectedPost.getId())
                .update("comments", comments)
                .addOnCompleteListener(task -> {
                    Log.i("ljw", "successfully added a comment");
                    postRvAdapter.notifyDataSetChanged();
                    commentEditText.setText("");
                })
                .addOnFailureListener(e -> Log.i("ljw", "failed adding a comment because " + e.toString()));
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

        Post post = (Post) marker.getTag();
        if (post == null) {
            Log.i("ljw", "no post tagged to the clicked on marker, hmm");
            return true;
        }

        currentSelectedMarker = marker;
        currentSelectedPost = (Post) marker.getTag();
        postRvAdapter = new PostRvAdapter(post, MapActivity.this, (user != null ? user.getUid() : null), postRv, db);
        postRv.setAdapter(postRvAdapter);
        postRv.setVisibility(View.VISIBLE);

        // returning true instead would prevent the camera centering/info window opening
        return false;
    }

    public void onPostVoteClicked(View v) {
//        if (user == null) {
//            //TODO: modal with "sign in to vote"
//            return;
//        }
//
//        // need to disable the button until the firestore transaction is complete, otherwise users
//        // could cast multiple votes by spamming the button
//        // TODO: move this into Utils
//        v.setEnabled(false);
//        int usersPreviousVote = 0;
//        int currentScore = currentSelectedPost.getScore();
//        HashMap<String, Integer> voteMap = currentSelectedPost.getVotes();
//        if (voteMap.containsKey(user.getUid())) {
//            usersPreviousVote = voteMap.get(user.getUid());
//        }
//
//        Button up = findViewById(R.id.postRvHeaderVoteUpBtn);
//        Button down = findViewById(R.id.postRvHeaderVoteDownBtn);
//        int usersNewVote = 0;
//        int scoreChange = 0;
//
//        if (v.getId() == R.id.postRvHeaderVoteDownBtn) {
//            if (usersPreviousVote == -1) {
//                usersNewVote = 0;
//                scoreChange = 1;
//                down.setBackground(getDrawable(R.drawable.arrow_down));
//            } else if (usersPreviousVote == 0) {
//                usersNewVote = -1;
//                scoreChange = -1;
//                down.setBackground(getDrawable(R.drawable.arrow_down_colored));
//            } else { //if (usersPreviousVote == 1)
//                usersNewVote = -1;
//                scoreChange = -2;
//                down.setBackground(getDrawable(R.drawable.arrow_down_colored));
//                up.setBackground(getDrawable(R.drawable.arrow_up));
//            }
//        }
//        if (v.getId() == R.id.postRvHeaderVoteUpBtn) {
//            if (usersPreviousVote == -1) {
//                usersNewVote = 1;
//                scoreChange = 2;
//                down.setBackground(getDrawable(R.drawable.arrow_down));
//                up.setBackground(getDrawable(R.drawable.arrow_up_colored));
//            } else if (usersPreviousVote == 0) {
//                usersNewVote = 1;
//                scoreChange = 1;
//                up.setBackground(getDrawable(R.drawable.arrow_up_colored));
//            } else { //if (usersPreviousVote == 1)
//                usersNewVote = 0;
//                scoreChange = -1;
//                up.setBackground(getDrawable(R.drawable.arrow_up));
//            }
//        }
//
//        TextView scoreView = findViewById(R.id.postRvHeaderScore);
//        currentSelectedPost.setScore(currentScore + scoreChange);
//        String scoreViewText = Long.toString(currentScore + scoreChange);
//        scoreView.setText(scoreViewText);
//
//        voteMap.put(user.getUid(), usersNewVote);
//        int finalScoreChange = scoreChange;
//
//        // get the current score in firestore first
//        int finalScoreChange1 = scoreChange;
//        db.collection("posts")
//                .document(currentSelectedPost.getId())
//                .get()
//                .addOnCompleteListener(task -> {
//                    Post post = task.getResult().toObject(Post.class);
//
//                    db.collection("posts")
//                            .document(currentSelectedPost.getId())
//                            .update("score", post.getScore() + finalScoreChange,
//                                    "votes", voteMap)
//                            .addOnCompleteListener(task1 -> {
//                                Log.i("ljw", "successfully updated score");
//
//                                //update the post creator's total score field:
//                                db.collection("users")
//                                        .document(currentSelectedPost.getUserId())
//                                        .get()
//                                        .addOnCompleteListener(task2 -> {
//                                            Log.i("ljw", "got post creator for score update");
//                                            User user = task2.getResult().toObject(User.class);
//                                            int userScore = user.getTotalScore();
//
//                                            //update the postDescriptor for this post:
//                                            List<PostDescriptor> postDescriptors = user.getPostDescriptors();
//                                            for (PostDescriptor pd : postDescriptors) {
//                                                if (pd.id.equals(post.getId())) {
//                                                    pd.setScore(pd.getScore() + finalScoreChange);
//                                                }
//                                            }
//
//                                            db.collection("users")
//                                                    .document(currentSelectedPost.getUserId())
//                                                    .update("totalScore", userScore + finalScoreChange,
//                                                            "postDescriptors", postDescriptors)
//                                                    .addOnCompleteListener(task3 -> {
//                                                        Log.i("ljw", "updated post user's score and the postDescriptor successfully");
//                                                    })
//                                                    .addOnFailureListener(e -> {
//                                                        Log.i("ljw", "couldn't update user's score: " + e.toString());
//                                                    });
//
//
//                                        })
//                                        .addOnFailureListener(e -> {
//                                            Log.i("ljw", "failed getting user: " + e.toString());
//                                        });
//
//
//                                v.setEnabled(true);
//                            })
//                            .addOnFailureListener(e -> {
//                                Log.i("ljw", "failed updating score: " + e.toString());
//                                v.setEnabled(true);
//                            });
//                })
//                .addOnFailureListener(e -> {
//                    Log.i("ljw", "error getting post to update its score: " + e.toString());
//                    v.setEnabled(true);
//                });
    }


}

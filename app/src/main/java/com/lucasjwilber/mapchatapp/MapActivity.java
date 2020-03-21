package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
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
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lucasjwilber.mapchatapp.databinding.ActivityMapBinding;
import com.lucasjwilber.mapchatapp.databinding.PostLayoutBinding;

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
    private PostLayoutBinding postViewBinding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    FirebaseFirestore db;
    long postQueryLimit = 100;
    FirebaseUser user;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 69;
    double postsRadius = 0.05;
    String currentUsername = "someone";
    String currentUserId = "some id";
    double userLat;
    double userLng;
    LatLngBounds cameraBounds;
    public String userCurrentAddress = "somewhere";
    LinearLayout createPostForm;
    TextView userLocationTV;
    BitmapDescriptor postMarkerIcon;
    BitmapDescriptor userMarkerIcon;
    Post currentSelectedPost;
    Marker currentSelectedMarker;
    private RecyclerView postRv;
    private RecyclerView.Adapter postRvAdapter;
    private RecyclerView.LayoutManager postRvLayoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapBinding = ActivityMapBinding.inflate(getLayoutInflater());
        View view = mapBinding.getRoot();
        setContentView(view);

        postViewBinding = PostLayoutBinding.inflate(getLayoutInflater());

        // get location permission if necessary, then get location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            getUserLatLng();
        }

        createPostForm = mapBinding.createPostForm;
        userLocationTV = mapBinding.postLocationTextView;

        // post recyclerview
        postRv = mapBinding.postRecyclerView;
        postRvLayoutManager = new LinearLayoutManager(this);
        postRv.setLayoutManager(postRvLayoutManager);
//        Post testPost = new Post("fakeid", "lucas", "faketitle", "faketext", "6969 420 avenue", userLat, userLng);
//        postRvAdapter = new PostRvAdapter(testPost, );
//        postRv.setAdapter(postRvAdapter);

        postMarkerIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.post_icon));
        userMarkerIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.user_location_pin));

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUserId = user.getUid();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //    pop up method to show hamburger
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.hamburger_menu_contents);
        popup.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //remove the directions/gps buttons
        mMap.getUiSettings().setMapToolbarEnabled(false);
//        PostInfoWindowAdapter windowAdapter = new PostInfoWindowAdapter(getApplicationContext());
//        mMap.setInfoWindowAdapter(windowAdapter);
        mMap.setOnMarkerClickListener(this::onMarkerClick);
        mMap.setOnMapClickListener(this::onMapClick);
        mMap.setOnCameraIdleListener(this::onCameraIdle);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        //location permission
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission granted:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    Log.i("ljw", "location permission granted, getting location...");
                    getUserLatLng();
                }

            } else {
                Log.i("ljw", "location permission denied");
            }
        }
    }

    public void getUserLatLng() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    Log.i("ljw", "successfully got location");
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        Log.i("ljw", "lat: " + userLat + "\nlong: " + userLng);

                        AsyncTask.execute(() -> {

                            // call geocode to get formatted address
//                            getUsersFormattedAddress();

                            //update map on main thread
                            Handler handler = new Handler(Looper.getMainLooper()) {
                                @Override
                                public void handleMessage(Message input) {
                                    Log.i("ljw", "lat/lng for user is " + userLat + "/" + userLng);

                                    //add a marker to display the user's location:
                                    mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(userLat, userLng))
                                            .title("My Location")
                                            .icon(userMarkerIcon)
                                            .snippet(userCurrentAddress));

                                    //center the map on the user
                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(userLat, userLng)));
                                    cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                                    //https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap#setMapType(int)
                                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                    mMap.setMinZoomPreference(10f);
                                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15f));
                                }
                            };
                            handler.obtainMessage().sendToTarget();
                        });
                    }
                })
                .addOnFailureListener(this, error -> {
                    Log.i("ljw", "error getting location:\n" + error.toString());
                });
    }

    public void toggleFormVisibility(View v) {
        if (createPostForm.getVisibility() == View.VISIBLE) {
            createPostForm.setVisibility(View.GONE);
        } else {
            createPostForm.setVisibility(View.VISIBLE);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(userLat, userLng)));
        }
        postRv.setVisibility(View.GONE);
    }

    @Override
    public void onCameraIdle() {
        cameraBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Log.i("ljw", "camera bounds: " + cameraBounds.toString());

        // query db for posts near the user
        getPostsFromDbAndCreateMapMarkers();
    }

    public void createPost(View v) {
        //gather form data
        EditText postTitleForm = mapBinding.postTitleEditText;
        String postTitle = postTitleForm.getText().toString();
        EditText postBodyForm = mapBinding.postBodyEditText;
        String postBody = postBodyForm.getText().toString();

        //create a Post object
        Post post = new Post(
                user.getUid(),
                user.getDisplayName(),
                postTitle,
                postBody,
                userCurrentAddress,
                userLat,
                userLng);
        //set icon as required here
        //set link as required here
        //set any other post attributes here:

        Log.i("ljw", "new post created: " + post.toString());
//
//        //push it to DB
        db.collection("posts")
                .document(post.getId())
                .set(post)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("ljw", "successfully added new post to DB");
                        //add the new post to the map now that it's in the db
                        Marker marker = createMarkerWithPost(post);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("ljw", "Error adding post to db: " + e);
                    }
                });

//      // hide form
        createPostForm.setVisibility(View.GONE);
        postTitleForm.setText("");
        postBodyForm.setText("");
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
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(post.getLat(), post.getLng()))
                .anchor(0, 1)
                .icon(postMarkerIcon)
        );
        marker.setTag(post);
        return marker;
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
        createPostForm.setVisibility(View.GONE);
        postRv.setVisibility(View.GONE);
    }

    public void addCommentToPost(View v) {
        Log.i("ljw", "commentbutton clicked");
        EditText commentEditText = findViewById(R.id.postRvPostReplyBox);

        if (currentSelectedPost.getId() == null) {
            Log.i("ljw", "post has a null id so a DB query won't work");
            return;
        } else if (commentEditText.getText().equals("")) {
            Log.i("ljw", "comment box is empty");
            // TODO: toast
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
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.i("ljw", "successfully added a comment");
                        postRvAdapter.notifyDataSetChanged();
                        commentEditText.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("ljw", "failed adding a comment because " + e.toString());
                    }
                });
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
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.i("ljw", "clicked on a marker");

        Post post = (Post) marker.getTag();
        if (post == null) {
            Log.i("ljw", "no post tagged to the clicked on marker, hmm");
            return true;
        }
        createPostForm.setVisibility(View.GONE);

        currentSelectedMarker = marker;
        currentSelectedPost = (Post) marker.getTag();
        postRvAdapter = new PostRvAdapter(post, getApplicationContext(), currentUserId);
        postRv.setAdapter(postRvAdapter);
        postRv.setVisibility(View.VISIBLE);

        // returning true instead would prevent the camera centering/info window opening
        return false;
    }

    public void onPostVoteClicked(View v) {
        // need to disable the button until the firestore transaction is complete, otherwise users
        // could cast multiple votes by spamming the button
        v.setEnabled(false);
        int usersPreviousVote = 0;
        int usersNewVote = 0;
        HashMap<String, Integer> voteMap = currentSelectedPost.getVotes();
        if (voteMap.containsKey(user.getUid())) {
            usersPreviousVote = voteMap.get(user.getUid());
        }
        Button up = findViewById(R.id.postRvHeaderVoteUpBtn);
        Button down = findViewById(R.id.postRvHeaderVoteDownBtn);

        //on downvote
        if (v.getId() == R.id.postRvHeaderVoteDownBtn) {
            if (usersPreviousVote != -1) {
                usersNewVote = -1;
                down.setBackground(getDrawable(R.drawable.down_arrow_colored));
                if (usersPreviousVote == 1) {
                    up.setBackground(getDrawable(R.drawable.up_arrow));
                }
            } else {
                down.setBackground(getDrawable(R.drawable.down_arrow));
            }
        //on upvote
        } else {
            if (usersPreviousVote != 1) {
                usersNewVote = 1;
                up.setBackground(getDrawable(R.drawable.up_arrow_colored));
                if (usersPreviousVote == -1) {
                    down.setBackground(getDrawable(R.drawable.down_arrow));
                }
            } else {
                up.setBackground(getDrawable(R.drawable.up_arrow));
            }
        }
        int scoreChange = usersNewVote - usersPreviousVote;
        voteMap.put(user.getUid(), usersNewVote);

        db.collection("posts")
                .document(currentSelectedPost.getId())
                .get()
                .addOnCompleteListener(task -> {
                    Post post = task.getResult().toObject(Post.class);

                    db.collection("posts")
                            .document(currentSelectedPost.getId())
                            .update("score", post.getScore() + scoreChange,
                                    "votes", voteMap)
                            .addOnCompleteListener(task1 -> {
                                Log.i("ljw", "successfully updated score");
                                postRvAdapter.notifyDataSetChanged();
                                v.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.i("ljw", "failed updating score: " + e.toString());
                                v.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.i("ljw", "error getting post to update its score: " + e.toString());
                    v.setEnabled(true);
                });
    }


}

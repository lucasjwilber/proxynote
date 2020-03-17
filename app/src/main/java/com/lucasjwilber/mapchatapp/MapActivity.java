package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
        PopupMenu.OnMenuItemClickListener,
        GoogleMap.OnInfoWindowLongClickListener,
        GoogleMap.OnCameraIdleListener {

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
    LinearLayout addCommentForm;
    TextView userLocationTV;
    BitmapDescriptor postMarkerIcon;
    BitmapDescriptor userMarkerIcon;
    Post currentSelectedPost;
    String currentSelectedPostId;
    Marker currentSelectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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

        createPostForm = findViewById(R.id.createPostForm);
        addCommentForm = findViewById(R.id.addCommentForm);
        userLocationTV = findViewById(R.id.postLocationTextView);
        postMarkerIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.yellow_chat_icon));
        userMarkerIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.user_location_pin));

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

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
        PostInfoWindowAdapter windowAdapter = new PostInfoWindowAdapter(getApplicationContext());
        mMap.setInfoWindowAdapter(windowAdapter);
        mMap.setOnInfoWindowLongClickListener(this::onInfoWindowLongClick);
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
                            getUsersFormattedAddress();

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
                                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
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
        //toggle visibility
        createPostForm.setVisibility(createPostForm.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
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
        EditText postTitleForm = findViewById(R.id.postTitleEditText);
        String postTitle = postTitleForm.getText().toString();
        EditText postBodyForm = findViewById(R.id.postBodyEditText);
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
        //set icon
        //set link
        //set any other post attributes here:

        Log.i("ljw", "new post created: " + post.toString());
//
//        //push it to DB
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.i("vik", "DocumentSnapshot added with ID: " + documentReference.getId());
                        Log.i("ljw", "successfully added new post to DB");

                        //set the post's id to whatever id it was given by firestore
                        post.setId(documentReference.getId());

                        //add the new post to the map now that it's in the db
                        Marker marker = createMarkerWithPost(post);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("vik", "Error adding document", e);
                    }
                });

//        //hide form
        createPostForm.setVisibility(View.INVISIBLE);
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

                                createMarkerWithPost(post);

                                Log.i("ljw", "found post \"" + post.getTitle() + "/" + post.getText() + "\" with id " + post.getId());
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
        addCommentForm.setVisibility(View.INVISIBLE);
        createPostForm.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        Log.i("ljw", marker.getId() + " long pressed");
        // so that you can't comment on your user pin:
        if (marker.getId().equals("m0")) return;

        addCommentForm.setVisibility(View.VISIBLE);
        Post c = (Post) marker.getTag();
        if (c != null) {
            currentSelectedPost = c;
            if (c.getId() == null) Log.i("ljw", "id is null");
            currentSelectedPostId = c.getId();
        }
        currentSelectedMarker = marker;
    }

    public void addCommentToPost(View v) {
        Log.i("ljw", "commentbutton clicked");
        EditText commentEditText = findViewById(R.id.commentEditText);
        Comment comment = new Comment("fakeuserid", "fakeusername", "faketext", userLat, userLng);

        if (currentSelectedPostId == null) {
            Log.i("ljw", "post has a null id so a DB query won't work");
            addCommentForm.setVisibility(View.INVISIBLE);
            return;
        }

        //get post by id from firestore
        db.collection("posts")
                .document(currentSelectedPostId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        Log.i("ljw", "query successful");

                        Post c = Objects.requireNonNull(task.getResult()).toObject(Post.class);
                        //these are to prevent NPEs on old posts that didn't have IDs or instantiated LLs:
                        if (c == null) return;
                        Log.i("ljw", "post currently has " + c.getComments().size() + " comments already:");
                        Log.i("ljw", c.getComments().toString());
                        c.getComments().add(comment);

                        //update post in firestore
                        db.collection("posts")
                                .document(currentSelectedPostId)
                                .set(c)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.i("ljw", "successfully updated post with new comment");
                                        addCommentForm.setVisibility(View.INVISIBLE);
                                        commentEditText.setText("");

                                        //refresh marker
                                        currentSelectedMarker.remove();
                                        Marker marker = mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(c.getLat(), c.getLng()))
                                                .anchor(0, 1)
                                                .icon(postMarkerIcon)
                                                .title(c.getTitle())
                                                .snippet(c.getText()));
                                        marker.setTag(c);
                                        marker.showInfoWindow();
                                        currentSelectedMarker = marker;
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.i("ljw", "failed updating post with new comment:\n", e);
                                    }
                                });
                    }
                });
    }

    public void addTestPostAtLatLng(Double lat, Double lng) {
        Post post = new Post("fakeid", "lucas", "faketitle", "faketext", "6969 420 avenue", userLat, userLng);
        Log.i("ljw", "new post created: " + post.toString());
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(userLat, userLng))
                                .anchor(0, 1)
                                .icon(postMarkerIcon)
                                .title(post.getTitle())
                                .snippet(post.getText()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("vik", "Error adding document", e);
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
}

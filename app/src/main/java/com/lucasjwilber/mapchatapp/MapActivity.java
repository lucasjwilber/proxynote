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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, PopupMenu.OnMenuItemClickListener, GoogleMap.OnInfoWindowLongClickListener  {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 69;
    public double userLat;
    public double userLng;
    public String userCurrentAddress = "somewhere";
    FirebaseFirestore db;
    LinearLayout createPostForm;
    LinearLayout addReplyForm;
    TextView userLocationTV;
    BitmapDescriptor postIcon;
    BitmapDescriptor userIcon;
    Post currentSelectedPost;
    String currentSelectedPostId;
    Marker currentSelectedMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        createPostForm = findViewById(R.id.createPostForm);
        addReplyForm = findViewById(R.id.addReplyForm);
        userLocationTV = findViewById(R.id.postLocationTextView);

        postIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.yellow_chat_icon));
        userIcon = BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.user_location_pin));

        db = FirebaseFirestore.getInstance();
        getPostsFromDbAndCreateMapMarkers();

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        PostInfoWindowAdapter windowAdapter = new PostInfoWindowAdapter(getApplicationContext());
        mMap.setInfoWindowAdapter(windowAdapter);
        mMap.setOnInfoWindowLongClickListener(this::onInfoWindowLongClick);
        mMap.setOnMapClickListener(this::onMapClick);

        // get location permission if necessary, then get location
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            getUserLatLng();
        }
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

                            //call geocode to get formatted address
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
                                            .icon(userIcon)
                                            .snippet(userCurrentAddress));

                                    //center the map on the user
                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(userLat, userLng)));

                                    //this zooms in on the user's location by restricting how far you can zoom out:
                                    //TODO: set the default zoom but somehow still allow users to zoom out farther than that
                                    mMap.setMinZoomPreference((float) 15.0);

                                    //using map type 2 to remove clutter, so only our markers are displayed:
                                    //https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap#setMapType(int)
                                    mMap.setMapType(2);
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

    public void createPost(View v) {
        //gather form data
        EditText postTitleView = findViewById(R.id.postTitleEditText);
        String postTitle = postTitleView.getText().toString();
        EditText postBodyView = findViewById(R.id.postBodyEditText);
        String postBody = postBodyView.getText().toString();

        //create a Post object
        Post post = new Post("fakeid", "lucas", "faketitle", "faketext", "6969 420 avenue", userLat, userLng);
        Log.i("ljw", "new post created: " + post.toString());
//
//        //push it to DB
//        db.collection("posts")
//                .add(post)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Log.i("vik", "DocumentSnapshot added with ID: " + documentReference.getId());
//                        Log.i("ljw", "successfully added new post to DB");
//
//                        //set the post's id to whatever id it was given by firestore
//                        post.setId(documentReference.getId());
//
//                        //add the new post to the map now that it's in the db
                        Marker marker = createMarkerWithPost(post);
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.i("vik", "Error adding document", e);
//                    }
//                });
//
//        //hide form
        createPostForm.setVisibility(View.INVISIBLE);
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

    public void getPostsFromDbAndCreateMapMarkers() {
//        db.collection("posts")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
//                                Post post = Objects.requireNonNull(document.toObject(Post.class));
//
//                                createMarkerWithPost(post);
//
//                                Log.i("ljw", "found post \"" + post.getTitle() + "/" + post.getText() + "\" with id " + post.getId());
//                            }
//                        } else {
//                            Log.i("ljw", "Error getting documents.", task.getException());
//                        }
//                    }
//                });
    }

    public Marker createMarkerWithPost(Post post) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(post.getLat(), post.getLng()))
                .anchor(0, 1)
                .icon(postIcon)
//                .title(post.getTitle())  // TODO: are these still necessary with HTML? test
//                .snippet(post.getText())
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

//            String postingFromString = "Posting from " + userCurrentAddress;
//            Handler handler = new Handler(Looper.getMainLooper()) {
//                @Override
//                public void handleMessage(Message input) {
//                    userLocationTV.setText(postingFromString);
//                }
//            };
//            handler.obtainMessage().sendToTarget();

        } catch (MalformedURLException e) {
            Log.i("ljw", "malformedURLexception:\n" + e.toString());
        } catch (ProtocolException e) {
            Log.i("ljw", "protocol exception:\n" + e.toString());
        } catch (IOException e) {
            Log.i("ljw", "IO exception:\n" + e.toString());
        }
    }

    public void onMapClick(LatLng arg0) {
        addReplyForm.setVisibility(View.INVISIBLE);
        createPostForm.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        Log.i("ljw", marker.getId() + " long pressed");
        // so that you can't reply to your user pin:
        if (marker.getId().equals("m0")) return;

        addReplyForm.setVisibility(View.VISIBLE);
        Post c = (Post) marker.getTag();
        if (c != null) {
            currentSelectedPost = c;
            if (c.getId() == null) Log.i("ljw", "id is null");
            currentSelectedPostId = c.getId();
        }
        currentSelectedMarker = marker;
    }

    public void addReplyToPost(View v) {
        Log.i("ljw", "reply button clicked");
        EditText replyEditText = findViewById(R.id.replyEditText);
//        Reply reply = new Reply("user", replyEditText.getText().toString(), new Date().getTime());
//
//        if (currentSelectedPostId == null) {
//            Log.i("ljw", "post has a null id so a DB query won't work");
//            addReplyForm.setVisibility(View.INVISIBLE);
//            return;
//        }
//
//        //get post by id from firestore
//        db.collection("posts")
//                .document(currentSelectedPostId)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                        Log.i("ljw", "query successful");
//
//                        Post c = Objects.requireNonNull(task.getResult()).toObject(Post.class);
//                        //these are to prevent NPEs on old posts that didn't have IDs or instantiated LLs:
//                        if (c == null) return;
//                        if (c.replies == null) c.replies = new LinkedList<>();
//                        Log.i("ljw", "post currently has " + c.replies.size() + " replies already:");
//                        Log.i("ljw", c.replies.toString());
//                        c.replies.add(reply);
//
//                        //update post in firestore
//                        db.collection("posts")
//                                .document(currentSelectedPostId)
//                                .set(c)
//                                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                    @Override
//                                    public void onSuccess(Void aVoid) {
//                                        Log.i("ljw", "successfully updated post with new reply");
//                                        addReplyForm.setVisibility(View.INVISIBLE);
//                                        replyEditText.setText("");
//
//                                        //refresh marker
//                                        currentSelectedMarker.remove();
//                                        Marker marker = mMap.addMarker(new MarkerOptions()
//                                                .position(new LatLng(c.getLat(), c.getLng()))
//                                                .anchor(0, 1)
//                                                .icon(postIcon)
//                                                .title(c.getTitle())
//                                                .snippet(c.getText()));
//                                        marker.setTag(c);
//                                        marker.showInfoWindow();
//                                        currentSelectedMarker = marker;
//                                    }
//                                })
//                                .addOnFailureListener(new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        Log.i("ljw", "failed updating post with new reply:\n", e);
//                                    }
//                                });
//                    }
//                });
    }

    public void deleteDocumentByID(String collection, String id) {
        db.collection(collection).document(id)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("ljw", "successfully deleted " + id + " from " + collection);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("ljw", "Error deleting document", e);
                    }
                });
    }

    public void addTestPostAtLatLng(Double lat, Double lng) {
        Post post = new Post("fakeid", "lucas", "faketitle", "faketext", "6969 420 avenue", userLat, userLng);
        Log.i("ljw", "new post created: " + post.toString());
//        db.collection("posts")
//                .add(post)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        mMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(userLat, userLng))
//                                .anchor(0, 1)
//                                .icon(postIcon)
//                                .title(post.getTitle())
//                                .snippet(post.getText()));
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.i("vik", "Error adding document", e);
//                    }
//                });
    }
}

package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.lucasjwilber.mapchatapp.databinding.ActivityCreatePostBinding;
import com.lucasjwilber.mapchatapp.databinding.ActivityMapBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 69;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ActivityCreatePostBinding binding;
    FirebaseFirestore db;
    FirebaseUser user;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef;
    double userLat;
    double userLng;
    public String userCurrentAddress;
    ImageView createPostImage;
    String currentImageUUID;
    Bitmap currentImageThumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        //"gs://mapchatapp-b83bc.appspot.com"
        storageRef = storage.getReference();
        createPostImage = binding.createPostImage;

        Intent intent = getIntent();
        userLat = intent.getDoubleExtra("userLat", userLat);
        userLng = intent.getDoubleExtra("userLng", userLng);
        userCurrentAddress = intent.getStringExtra("userCurrentAddress");
    }

    public void createPost(View v) {
        //gather form data
        EditText postTitleForm = binding.postTitleEditText;
        String postTitle = postTitleForm.getText().toString();
        EditText postBodyForm = binding.postBodyEditText;
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
        //TODO: set icon as required here
        //TODO: set link as required here

        Log.i("ljw", "new post created: " + post.toString());
//
//        //push it to DB
        //TODO: confirm user is signed in
        db.collection("posts")
                .document(post.getId())
                .set(post)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("ljw", "successfully added new post to DB");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("ljw", "Error adding post to db: " + e);
                    }
                });

        //TODO: toast;
        finish();
    }

    public void cameraButtonClicked(View v) {
        Log.i("ljw", "camera button clicked");

        PackageManager pm = getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Log.i("ljw", "package manager says this device has a camera. going to try to use it...");
            checkVersionLaunchCamera();
        } else {
            //TODO: toast
            Log.i("ljw", "this device doesn't have a camera to use.");
        }
    }

    private void checkVersionLaunchCamera() {
        //if device API requires permission first, get it and use the camera, else just use the camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                // permission already granted. launch camera
                dispatchTakePictureIntent();
            }
        } else {
            //permission request not required. launch camera
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission granted:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {

                    Log.i("ljw", "camera permission granted");

                    //launch camera
                    dispatchTakePictureIntent();
                }
            } else {
                Log.i("ljw", "camera permission denied");
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            currentImageThumbnail = imageBitmap;
            createPostImage.setImageBitmap(imageBitmap);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();

            String randId = UUID.randomUUID().toString();
            StorageReference imageRef = storageRef.child(randId);
            Log.i("ljw", randId);

            UploadTask uploadTask = imageRef.putBytes(imageData);

            uploadTask.addOnSuccessListener(result -> {
                        Log.i("ljw", "success! \n" + result.toString());
                    })
                    .addOnFailureListener(failure -> {
                        Log.i("ljw", "failure! :" + failure.toString());
                    });

        }



    }

//    private File createImageFile() throws IOException {
////        // Create an image file name
////        String imageFileName = UUID.randomUUID().toString();
////        File image = File.createTempFile(
////                UUID.randomUUID().toString(),  /* prefix */
////                ".jpg",         /* suffix */
////                storageDir      /* directory */
////        );
////
////        // Save a file: path for use with ACTION_VIEW intents
////        currentPhotoPath = image.getAbsolutePath();
////        return image;
////    }

}

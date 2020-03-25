package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.lucasjwilber.mapchatapp.databinding.ActivityCreatePostBinding;

import java.io.ByteArrayOutputStream;
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
    Bitmap currentImage;
    private RecyclerView iconRv;
    private RecyclerView.Adapter iconRvAdapter;
    private RecyclerView.LayoutManager iconRvLayoutManager;
    LinearLayoutManager HorizontalLayout;
    private int selectedIcon = 0;
    ImageView selectedIconView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        storageRef = storage.getReference();
        createPostImage = binding.createPostImage;

        iconRv = findViewById(R.id.postIconRv);
        HorizontalLayout = new LinearLayoutManager(
                CreatePostActivity.this,
                LinearLayoutManager.HORIZONTAL,
                false);
        iconRv.setLayoutManager(HorizontalLayout);
        iconRvAdapter = new IconSelectAdapter(this);
        iconRv.setAdapter(iconRvAdapter);

        Intent intent = getIntent();
        userLat = intent.getDoubleExtra("userLat", userLat);
        userLng = intent.getDoubleExtra("userLng", userLng);
        userCurrentAddress = intent.getStringExtra("userCurrentAddress");
    }

    public void createPost(View v) {
        EditText postTitleForm = binding.postTitleEditText;
        String postTitle = postTitleForm.getText().toString();
        EditText postBodyForm = binding.postBodyEditText;
        String postBody = postBodyForm.getText().toString();

        Post post = new Post(
                user.getUid(),
                user.getDisplayName(),
                postTitle,
                postBody,
                userCurrentAddress,
                userLat,
                userLng);

        if (selectedIcon != 0) post.setIcon(selectedIcon);

        if (currentImage == null) {
            uploadPost(post);
        } else {
            uploadImageAndPost(post);
        }
    }

    private void uploadImageAndPost(Post post) {
        String imageUUID = UUID.randomUUID().toString();
        post.setImageUUID(imageUUID);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();
        StorageReference imageRef = storageRef.child(imageUUID);
        Log.i("ljw", "storage ref is " + imageRef + "\nurl is + " + imageRef.getDownloadUrl());

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(result -> {
            Log.i("ljw", "uploaded image! \n" + result.toString());
            imageRef.getDownloadUrl().addOnSuccessListener(url -> {
                Log.i("ljw", "got image url: " + url.toString());
                post.setImageUrl(url.toString());
                uploadPost(post);
            })
            .addOnFailureListener(e -> {
                Log.i("ljw", "error: " + e.toString());
            });
        }).addOnFailureListener(failure -> {
            Log.i("ljw", "failure! :" + failure.toString());
        });
    }

    public void uploadPost(Post post) {
        db.collection("posts")
                .document(post.getId())
                .set(post)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i("ljw", "successfully added new post to DB");
                        //TODO: toast;
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("ljw", "Error adding post to db: " + e);
                    }
                });
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
            currentImage = imageBitmap;
            createPostImage.setImageBitmap(imageBitmap);
        }
    }

    public void onIconClick(View v) {
        Log.i("ljw", "selected icon code is " + v.getTag().toString());
        if (selectedIconView != null) selectedIconView.setBackground(null);
        selectedIconView = (ImageView) v;
        selectedIcon = (int) v.getTag();
        v.setBackground(getDrawable(R.drawable.selected_icon_bg));
    }

}

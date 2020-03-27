package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.lucasjwilber.mapchatapp.databinding.ActivityCreatePostBinding;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
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
    boolean iconSelected;
    int selectedPosition;

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

        post.setIcon(selectedIcon);

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
                .addOnSuccessListener(result -> {
                    Log.i("ljw", "successfully added new post to DB");
                    //TODO: toast?

                    //add post to user's list and +1 their total score
                    db.collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(userData -> {
                                User user = userData.toObject(User.class);
                                assert user != null;
                                if (user.getPostDescriptors() == null) {
                                    Log.i("ljw", "user doesn't have a postDescriptors field!");
                                } else {
                                    List<PostDescriptor> postDescriptors = user.getPostDescriptors();
                                    postDescriptors.add(new PostDescriptor(
                                            post.getId(),
                                            post.getTitle(),
                                            post.getTimestamp(),
                                            post.getScore(),
                                            post.getIcon(),
                                            post.getLocation()
                                    ));

                                    db.collection("users")
                                            .document(user.getUid())
                                            .update("postDescriptors", postDescriptors,
                                                    "totalScore", user.getTotalScore() + 1)
                                            .addOnSuccessListener(result2 -> {
                                                Log.i("ljw", "successfully updated user's post descriptors list");
                                            })
                                            .addOnFailureListener(e -> Log.i("ljw", "Error updating user's post descriptors list " + e));
                                }

                            })
                            .addOnFailureListener(e -> Log.i("ljw", "Error getting user: " + e));

                    finish();

                })
                .addOnFailureListener(e -> Log.i("ljw", "Error adding post to db: " + e));
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




    public void onIconClick(View v, int position) {
        Log.i("ljw", "selected icon code is " + v.getTag().toString());
        if (selectedIconView != null) selectedIconView.setBackground(null);
        selectedIconView = (ImageView) v;
        selectedIcon = (int) v.getTag();
        v.setBackground(getDrawable(R.drawable.postoutline_accent));
        selectedPosition = position;
        iconSelected = true;
    }

    public class IconSelectAdapter extends RecyclerView.Adapter<IconSelectAdapter.IconViewholder> {

        private Drawable[] icons;
        int selectedIcon;

        public IconSelectAdapter(Context context) {

            //when adding new icons be sure to update onBindViewholder() below and getPostIcon() in Utils
            icons = new Drawable[]{
                    context.getDrawable(R.drawable.posticon_127867),
                    context.getDrawable(R.drawable.posticon_127881),
                    context.getDrawable(R.drawable.posticon_128064),
                    context.getDrawable(R.drawable.posticon_128076),
                    context.getDrawable(R.drawable.posticon_128077),
                    context.getDrawable(R.drawable.posticon_128078),
                    context.getDrawable(R.drawable.posticon_128293),
                    context.getDrawable(R.drawable.posticon_128405),
                    context.getDrawable(R.drawable.posticon_128514),
                    context.getDrawable(R.drawable.posticon_128517),
                    context.getDrawable(R.drawable.posticon_128521),
                    context.getDrawable(R.drawable.posticon_128522),
                    context.getDrawable(R.drawable.posticon_128525),
                    context.getDrawable(R.drawable.posticon_128526),
                    context.getDrawable(R.drawable.posticon_128528),
                    context.getDrawable(R.drawable.posticon_128557),
                    context.getDrawable(R.drawable.posticon_128580),
                    context.getDrawable(R.drawable.posticon_128591),
                    context.getDrawable(R.drawable.posticon_129300),
                    context.getDrawable(R.drawable.posticon_129314),
                    context.getDrawable(R.drawable.posticon_129315),
                    context.getDrawable(R.drawable.posticon_9996),
            };
        }

        public class IconViewholder extends RecyclerView.ViewHolder {
            ImageView imageView;

            IconViewholder(ImageView view) {
                super(view);
                imageView = view;
            }

        }

        //TODO: do i need the viewType parameter here?
        @Override
        public IconSelectAdapter.IconViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView iconView = (ImageView) LayoutInflater.from(parent.getContext()).inflate(R.layout.posticon_imageview, parent, false);
            return new IconSelectAdapter.IconViewholder(iconView);
        }

        @Override
        public void onBindViewHolder(IconSelectAdapter.IconViewholder holder, int position) {
            holder.imageView.setImageDrawable(icons[position]);

            holder.imageView.setOnClickListener(v -> onIconClick(v, holder.getAdapterPosition()));
            if (selectedPosition != position) {
                holder.imageView.setBackground(null);
            }
            if (iconSelected && selectedPosition == position) {
                holder.imageView.setBackground(getDrawable(R.drawable.postoutline_accent));
            }

            switch (position) {
                case 0:
                    holder.imageView.setTag(127867);
                    break;
                case 1:
                    holder.imageView.setTag(127881);
                    break;
                case 2:
                    holder.imageView.setTag(128064);
                    break;
                case 3:
                    holder.imageView.setTag(128076);
                    break;
                case 4:
                    holder.imageView.setTag(128077);
                    break;
                case 5:
                    holder.imageView.setTag(128078);
                    break;
                case 6:
                    holder.imageView.setTag(128293);
                    break;
                case 7:
                    holder.imageView.setTag(128405);
                    break;
                case 8:
                    holder.imageView.setTag(128514);
                    break;
                case 9:
                    holder.imageView.setTag(128517);
                    break;
                case 10:
                    holder.imageView.setTag(128521);
                    break;
                case 11:
                    holder.imageView.setTag(128522);
                    break;
                case 12:
                    holder.imageView.setTag(128525);
                    break;
                case 13:
                    holder.imageView.setTag(128526);
                    break;
                case 14:
                    holder.imageView.setTag(128528);
                    break;
                case 15:
                    holder.imageView.setTag(128557);
                    break;
                case 16:
                    holder.imageView.setTag(128580);
                    break;
                case 17:
                    holder.imageView.setTag(128591);
                    break;
                case 18:
                    holder.imageView.setTag(129300);
                    break;
                case 19:
                    holder.imageView.setTag(129314);
                    break;
                case 20:
                    holder.imageView.setTag(129315);
                    break;
                case 21:
                    holder.imageView.setTag(9996);
                    break;
                default:
                    holder.imageView.setTag(0);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return icons.length;
        }

    }

}

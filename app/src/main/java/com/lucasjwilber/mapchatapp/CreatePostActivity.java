package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.lucasjwilber.mapchatapp.databinding.ActivityCreatePostBinding;
import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private static final int REQUEST_IMAGE_CAPTURE = 69;
    private static final int REQUEST_VIDEO_CAPTURE = 70;
    private static final int MAX_VIDEO_DURATION = 20;
    private String currentFilePath;
    private ActivityCreatePostBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef;
//    private String userCurrentAddress;
    private Bitmap currentImage;
    private Uri currentVideo;
    private Bitmap currentVideoThumbnail;
    private int selectedIcon = -1;  // this is the code for the default icon
    private ImageView selectedIconView;
    private boolean iconSelected;
    private int selectedPosition;
    private ProgressBar loadingSpinner;
    private String postAndImageId;
    File photoFile = null;
    File videoFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        storageRef = storage.getReference();
        loadingSpinner = findViewById(R.id.createPostProgressBar);

        RecyclerView iconRv = findViewById(R.id.postIconRv);
        LinearLayoutManager horizontalLayout = new LinearLayoutManager(
                CreatePostActivity.this,
                LinearLayoutManager.HORIZONTAL,
                false);
        iconRv.setLayoutManager(horizontalLayout);
        iconRv.setAdapter(new IconSelectAdapter());

    }

    public void createPost(View v) {
        EditText postTitleForm = binding.postTitleEditText;
        String postTitle = postTitleForm.getText().toString();
        EditText postBodyForm = binding.postBodyEditText;
        String postBody = postBodyForm.getText().toString();

        if (postTitle.equals("") || postTitle.length() == 0) {
            Utils.showToast(CreatePostActivity.this, "A post title is required.");
            return;
        } else if (postBody.equals("") || postBody.length() == 0) {
            Utils.showToast(CreatePostActivity.this, "Post text is required.");
            return;
        }

        loadingSpinner.setVisibility(View.VISIBLE);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(CreatePostActivity.this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(CreatePostActivity.this, location -> {
                    // Got last known location. In some rare situations this can be null.

                    double userLat;
                    double userLng;
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        Log.i(TAG, "lat: " + userLat + "\nlong: " + userLng);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                postAndImageId = UUID.randomUUID().toString();

                                Post post = new Post(
                                        postAndImageId,
                                        user.getUid(),
                                        user.getDisplayName(),
                                        postTitle,
                                        postBody,
                                        "somewhere",
                                        userLat,
                                        userLng);
                                post.setIcon(selectedIcon);

                                if (currentImage != null || currentVideo != null) {
                                    try {
                                        uploadMediaThenPost(post);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    uploadPost(post);
                                }
                            }
                        }).start();
                    }
                })
                .addOnFailureListener(e -> {
                    Utils.showToast(CreatePostActivity.this, "Unable to get your location.");
                    Log.e(TAG, "failed getting location: " + e.toString());
                });
    }

    private void uploadMediaThenPost(Post post) throws IOException {
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Utils.showToast(CreatePostActivity.this, "Uploading...");
            }
        };
        handler.obtainMessage().sendToTarget();

        StorageReference mediaRef = storageRef.child(postAndImageId);
        post.setMediaStorageId(postAndImageId);

        //  UPLOAD IMAGE //
        if (currentImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            currentImage.compress(Bitmap.CompressFormat.JPEG, 10, baos);
            byte[] imageData = baos.toByteArray();
            UploadTask uploadImage = mediaRef.putBytes(imageData);

            uploadImage.addOnSuccessListener(result -> {
                mediaRef.getDownloadUrl().addOnSuccessListener(url -> {
                    post.setImageUrl(url.toString());
                    uploadPost(post);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "error: " + e.toString());
                });
            }).addOnFailureListener(failure -> {
                Log.e(TAG, "failure! :" + failure.toString());
            });

        // UPLOAD VIDEO //
        } else {

            //temp file to transcode the video into, then upload
            File transcodedVideoFile = File.createTempFile(
                    "t" + user.getDisplayName(),  /* prefix */
                    ".mp4",         /* suffix */
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES)      /* directory */
            );

            Transcoder.into(transcodedVideoFile.getAbsolutePath())
                    .addDataSource(CreatePostActivity.this, currentVideo)
                    .setListener(new TranscoderListener() {
                        public void onTranscodeProgress(double progress) {}
                        public void onTranscodeCompleted(int successCode) {
                            Uri transcodedVideoUri = Uri.fromFile(transcodedVideoFile);
                            UploadTask uploadVideo = mediaRef.putFile(transcodedVideoUri);
                            uploadVideo.addOnSuccessListener(result -> {
                                mediaRef.getDownloadUrl().addOnSuccessListener(url -> {
                                    post.setVideoUrl(url.toString());

                                    //now upload the video thumbnail
                                    StorageReference thumbnailRef = storageRef.child("thumbnail" + postAndImageId);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    currentVideoThumbnail.compress(Bitmap.CompressFormat.JPEG, 10, baos);
                                    byte[] thumbnailData = baos.toByteArray();

                                    UploadTask uploadThumbnail = thumbnailRef.putBytes(thumbnailData);
                                    uploadThumbnail.addOnSuccessListener(thumbnail -> {
                                        Log.i(TAG, "uploaded video thumbnail successfully");
                                        thumbnailRef.getDownloadUrl().addOnSuccessListener(thumbnailUrl -> {
                                                    post.setVideoThumbnailUrl(thumbnailUrl.toString());
                                                    uploadPost(post);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "error getting video thumbnail: " + e.toString());
                                                });
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "error: " + e.toString());
                                });
                            }).addOnFailureListener(failure -> {
                                Log.e(TAG, "failure! :" + failure.toString());
                            });
                        }
                        public void onTranscodeCanceled() {}
                        public void onTranscodeFailed(@NonNull Throwable exception) {}
                    }).transcode();
        }

    }

    public void uploadPost(Post post) {
        db.collection("posts")
                .document(post.getId())
                .set(post)
                .addOnSuccessListener(result -> {
                    Log.i(TAG, "successfully added new post to DB");

                    //add post to user's list and +1 their total score
                    db.collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(userData -> {
                                User user = userData.toObject(User.class);
                                if (user.getPostDescriptors() == null) {
                                    Log.i(TAG, "user doesn't have a postDescriptors field!");
                                } else {
                                    List<PostDescriptor> postDescriptors = user.getPostDescriptors();
                                    postDescriptors.add(new PostDescriptor(
                                            post.getId(),
                                            post.getTitle(),
                                            post.getTimestamp(),
                                            post.getScore(),
                                            post.getIcon(),
                                            post.getLocation(),
                                            post.getLat(),
                                            post.getLng()
                                    ));

                                    db.collection("users")
                                            .document(user.getUid())
                                            .update("postDescriptors", postDescriptors,
                                                    "totalScore", user.getTotalScore() + 1)
                                            .addOnSuccessListener(result2 -> {
                                                Log.i(TAG, "successfully updated user's post descriptors list");
                                                loadingSpinner.setVisibility(View.VISIBLE);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error updating user's post descriptors list " + e);
                                                loadingSpinner.setVisibility(View.VISIBLE);
                                            });
                                }

                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error getting user: " + e);
                                loadingSpinner.setVisibility(View.VISIBLE);
                            });

                    finish();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding post to db: " + e);
                    loadingSpinner.setVisibility(View.VISIBLE);
                });
    }

    public void onMediaButtonClicked(View v) {
        PackageManager pm = getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Log.i(TAG, "package manager says this device has a camera. going to try to use it...");
            if (v.getId() == R.id.cameraButton) {
                checkVersionBeforeLaunchingCamera(REQUEST_IMAGE_CAPTURE);
            } else if (v.getId() == R.id.videoButton) {
                checkVersionBeforeLaunchingCamera(REQUEST_VIDEO_CAPTURE);
            }
        } else {
            Utils.showToast(CreatePostActivity.this, "This device doesn't have a compatible camera.");
            Log.i(TAG, "this device doesn't have a camera to use.");
        }

    }

    private void checkVersionBeforeLaunchingCamera(int requestCode) {
        //if we don't have camera or storage permissions ask for both
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    requestCode);
            } else {
            dispatchRecordMediaIntent(requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_VIDEO_CAPTURE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission granted:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "camera permission granted");

                    //launch camera
                    dispatchRecordMediaIntent(requestCode);
                }
            } else {
                Log.i(TAG, "camera permission denied");
            }
        }
    }

    private void dispatchRecordMediaIntent(int requestCode) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
//                File photoFile = null;
                try {
                    photoFile = createMediaFile(requestCode);
                } catch (IOException ex) {
                    Log.e(TAG, "error making file: " + ex.toString());
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.lucasjwilber.mapchatapp.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {

                // Create the File where the photo should go
                try {
                    videoFile = createMediaFile(requestCode);
                } catch (IOException ex) {
                    Log.e(TAG, "error making file: " + ex.toString());
                }
                // Continue only if the File was successfully created
                if (videoFile != null) {
                    Uri videoURI = FileProvider.getUriForFile(this,
                            "com.lucasjwilber.mapchatapp.fileprovider",
                            videoFile);
                    takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                    takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, MAX_VIDEO_DURATION);
                    startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {

                //read image exif data to get orientation, and rotate the image to match that
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(currentFilePath, bounds);

                BitmapFactory.Options opts = new BitmapFactory.Options();
                Bitmap bm = BitmapFactory.decodeFile(currentFilePath, opts);
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(currentFilePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                int orientation = orientString != null ? Integer.parseInt(orientString) : ExifInterface.ORIENTATION_NORMAL;

                int rotationAngle = 0;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
                    rotationAngle = 180;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
                    rotationAngle = 270;

                Matrix matrix = new Matrix();
                matrix.setRotate(rotationAngle, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true);

                currentVideo = null;
                currentVideoThumbnail = null;
                binding.createPostVideo.setVisibility(View.GONE);
                currentImage = rotatedBitmap;
                binding.createPostImage.setImageBitmap(rotatedBitmap);
                binding.createPostImage.setVisibility(View.VISIBLE);

            } else if (requestCode == REQUEST_VIDEO_CAPTURE) {

                binding.createPostImage.setVisibility(View.GONE);
                currentImage = null;
                Uri videoUri = intent.getData();
                MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
                mMMR.setDataSource(CreatePostActivity.this, videoUri);
                currentVideoThumbnail = mMMR.getFrameAtTime();
                currentVideo = videoUri;
                VideoView vv = binding.createPostVideo;
                vv.setVisibility(View.VISIBLE);
                vv.setVideoURI(videoUri);
                vv.setOnCompletionListener(complete -> {
                    vv.start();
                });
                vv.start();
            }
        }
    }


    private File createMediaFile(int requestCode) throws IOException {
        String timestamp = Long.toString(new Date().getTime());
        String fileName = user.getDisplayName() + timestamp;
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    fileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            currentFilePath = image.getAbsolutePath();
            return image;
        } else {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            File video = File.createTempFile(
                    fileName,  /* prefix */
                    ".mp4",         /* suffix */
                    storageDir      /* directory */
            );
            currentFilePath = video.getAbsolutePath();
            return video;
        }
    }

    public void onIconClick(View v, int position) {
        Log.i(TAG, "selected icon code is " + v.getTag().toString());
        if (selectedIconView != null) selectedIconView.setBackground(null);
        selectedIconView = (ImageView) v;
        selectedIcon = (int) v.getTag();
        v.setBackground(getDrawable(R.drawable.postoutline_yellow));
        selectedPosition = position;
        iconSelected = true;
    }

    public class IconSelectAdapter extends RecyclerView.Adapter<IconSelectAdapter.IconViewholder> {

        //when adding new icons be sure to update onBindViewholder() below and getPostIcon() in Utils
        private int[] icons = new int[]{
                R.drawable.posticon_127867,
                R.drawable.posticon_127881,
                R.drawable.posticon_128021,
                R.drawable.posticon_128064,
                R.drawable.posticon_128076,
                R.drawable.posticon_128077,
                R.drawable.posticon_128078,
                R.drawable.posticon_128293,
                R.drawable.posticon_128405,
                R.drawable.posticon_128514,
                R.drawable.posticon_128517,
                R.drawable.posticon_128521,
                R.drawable.posticon_128522,
                R.drawable.posticon_128525,
                R.drawable.posticon_128526,
                R.drawable.posticon_128528,
                R.drawable.posticon_128557,
                R.drawable.posticon_128580,
                R.drawable.posticon_128591,
                R.drawable.posticon_129300,
                R.drawable.posticon_129314,
                R.drawable.posticon_129315,
                R.drawable.posticon_9996,
        };

        public IconSelectAdapter() { }

        public class IconViewholder extends RecyclerView.ViewHolder {
            ImageView imageView;
            IconViewholder(ImageView view) {
                super(view);
                imageView = view;
            }
        }

        @Override
        public IconSelectAdapter.IconViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView iconView = (ImageView) LayoutInflater.from(parent.getContext()).inflate(R.layout.posticon_imageview, parent, false);
            return new IconSelectAdapter.IconViewholder(iconView);
        }

        @Override
        public void onBindViewHolder(IconSelectAdapter.IconViewholder holder, int position) {
            holder.imageView.setImageDrawable(getDrawable(icons[position]));

            holder.imageView.setOnClickListener(v -> onIconClick(v, holder.getAdapterPosition()));

            if (selectedPosition != position) {
                holder.imageView.setBackground(null);
            }
            if (iconSelected && selectedPosition == position) {
                holder.imageView.setBackground(getDrawable(R.drawable.postoutline_accent));
            }

            holder.imageView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return icons.length;
        }
    }

    public void onBackButtonClicked(View v) {
        finish();
    }

}

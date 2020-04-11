package com.lucasjwilber.mapchatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
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
import com.otaliastudios.transcoder.source.DataSource;

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
    private String currentFilePath;
    private ActivityCreatePostBinding binding;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef;
    private String userCurrentAddress;
    private Bitmap currentImage;
    private Uri currentVideo;
    private Bitmap currentVideoThumbnail;
    private int selectedIcon = 0;
    private ImageView selectedIconView;
    private boolean iconSelected;
    private int selectedPosition;
    private ProgressBar loadingSpinner;
    private String postAndImageId;

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
        RecyclerView.Adapter iconRvAdapter = new IconSelectAdapter(this);
        iconRv.setAdapter(iconRvAdapter);

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

                                //get formatted address
                                String formattedAddress = "somewhere";
                                {
//                                Log.i(TAG, "calling geocode api...");
//                                try {
//                                    URL url = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
//                                            userLat +
//                                            "," +
//                                            userLng +
//                                            "&key=" +
//                                            getResources().getString(R.string.google_geocode_key));
//
//                                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
//                                    con.setRequestMethod("GET");
//                                    Log.i(TAG, "called api, reading response...");
//                                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//                                    String line;
//                                    StringBuilder content = new StringBuilder();
//                                    while ((line = in.readLine()) != null) {
//                                        content.append(line);
//                                        if (line.contains("formatted_address")) {
//                                            formattedAddress = line.split("\" : \"")[1];
//                                            formattedAddress = formattedAddress.substring(0, formattedAddress.length() - 2);
//                                            Log.i(TAG, "found formatted addresss: " + formattedAddress);
//                                            break;
//                                        }
//                                    }
//                                    Log.i(TAG, content.toString());
//                                    Log.i(TAG, "formatted address is " + formattedAddress);
//                                    in.close();
//                                    con.disconnect();
//
//                                } catch (MalformedURLException e) {
//                                    Log.e(TAG, "malformedURLexception:\n" + e.toString());
//                                } catch (ProtocolException e) {
//                                    Log.e(TAG, "protocol exception:\n" + e.toString());
//                                } catch (IOException e) {
//                                    Log.e(TAG, "IO exception:\n" + e.toString());
//                                }
                                }
                                postAndImageId = UUID.randomUUID().toString();

                                Post post = new Post(
                                        postAndImageId,
                                        user.getUid(),
                                        user.getDisplayName(),
                                        postTitle,
                                        postBody,
                                        formattedAddress,
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
            currentImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();
            UploadTask uploadImage = mediaRef.putBytes(imageData);

            uploadImage.addOnSuccessListener(result -> {
                mediaRef.getDownloadUrl().addOnSuccessListener(url -> {
                    post.setImageUrl(url.toString());
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
            /* prefix */
            /* suffix */
            /* directory */
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
                                    currentVideoThumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
                File photoFile = null;
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
                File videoFile = null;
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
                    startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            //allocate a filepath
            File file = new File(currentFilePath);
            if (file.exists()) {
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    Bitmap imageBitmap = BitmapFactory.decodeFile(currentFilePath);
                    currentVideo = null;
                    currentVideoThumbnail = null;
                    binding.createPostVideo.setVisibility(View.GONE);
                    currentImage = imageBitmap;
                    binding.createPostImage.setImageBitmap(imageBitmap);
                    binding.createPostImage.setVisibility(View.VISIBLE);
                } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
                    binding.createPostImage.setVisibility(View.GONE);
                    currentImage = null;
                    Uri videoUri = intent.getData();
                    MediaMetadataRetriever mMMR = new MediaMetadataRetriever();
                    mMMR.setDataSource(CreatePostActivity.this, videoUri);
                    currentVideoThumbnail = mMMR.getFrameAtTime();
                    currentVideo = videoUri;
                    binding.createPostVideo.setVisibility(View.VISIBLE);
                    binding.createPostVideo.setVideoURI(videoUri);
                    binding.createPostVideo.start();
                }
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

            // Save a file: path for use with ACTION_VIEW intents
            currentFilePath = image.getAbsolutePath();
            return image;
        } else {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            File video = File.createTempFile(
                    fileName,  /* prefix */
                    ".mp4",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
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

    public void onBackButtonClicked(View v) {
        finish();
    }

}

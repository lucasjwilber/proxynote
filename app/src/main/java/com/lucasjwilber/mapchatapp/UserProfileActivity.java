package com.lucasjwilber.mapchatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.SharedElementCallback;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lucasjwilber.mapchatapp.databinding.ActivityUserProfileBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private final String TAG = "ljw";
    private String thisProfileOwnerId;
    private String userId;
    private String username;
    private FirebaseFirestore db;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private ActivityUserProfileBinding binding;
    private RecyclerView.Adapter postDescriptorsRvAdapter;
    private RecyclerView.LayoutManager postDescriptorsRvLayoutManager;
    private RecyclerView.Adapter postRvAdapter;
    private ConstraintLayout selectedDV;
    private String selectedPostId;
    private LatLng selectedPostLatLng;
    private boolean userIsOnTheirOwnProfile;
    private boolean aboutMeBeingEdited;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.profileOnePostRv.setLayoutManager(new LinearLayoutManager(this));
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getApplicationContext().getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", null);
        username = sharedPreferences.getString("username", null);

        Intent intent = getIntent();
        thisProfileOwnerId = intent.getStringExtra("userId");

        if (thisProfileOwnerId != null) {
            if (thisProfileOwnerId.equals(userId)) userIsOnTheirOwnProfile = true;

            binding.postDescRvProgressBar.setVisibility(View.VISIBLE);
            db.collection("users")
                    .document(thisProfileOwnerId)
                    .get()
                    .addOnSuccessListener(result -> {
                        User thisProfileOwner = result.toObject(User.class);

                        if (thisProfileOwner == null) {
                            Utils.showToast(UserProfileActivity.this, "This user account has been deleted.");
                            finish();
                            return;
                        }

                        binding.profileUsername.setText(thisProfileOwner.getUsername());
                        String userScoreText = "(" +
                                thisProfileOwner.getTotalScore() +
                                (thisProfileOwner.getTotalScore() == 1 || thisProfileOwner.getTotalScore() == -1 ? " point)" : " points)");
                        binding.profileScore.setText(userScoreText);
                        binding.aboutMeTV.setText(thisProfileOwner.getAboutme());

                        List<PostDescriptor> visiblePostDescriptors = new ArrayList<>();
                        if (userIsOnTheirOwnProfile) {
                            visiblePostDescriptors = thisProfileOwner.getPostDescriptors();
                        } else { //remove anonymous posts from users that aren't on their own profile
                            for (PostDescriptor pd : thisProfileOwner.getPostDescriptors()) {
                                if (!pd.isAnonymous()) visiblePostDescriptors.add(pd);
                            }
                        }

                        if (visiblePostDescriptors == null || visiblePostDescriptors.size() == 0) {
                            String noPostsText = thisProfileOwner.getUsername() + " hasn't made any posts yet.";
                            binding.profileNoCommentsYet.setText(noPostsText);
                            binding.profileNoCommentsYet.setVisibility(View.VISIBLE);
                        } else {
                            postDescriptorsRvLayoutManager = new LinearLayoutManager(this);
                            binding.profileAllPostsRv.setLayoutManager(postDescriptorsRvLayoutManager);
                            postDescriptorsRvAdapter = new PostSelectAdapter(visiblePostDescriptors);
                            binding.profileAllPostsRv.setAdapter(postDescriptorsRvAdapter);
                        }
                        binding.postDescRvProgressBar.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "error getting the profile owner's user object: " + e.toString());
                        binding.postDescRvProgressBar.setVisibility(View.GONE);
                    });
        }
    }

    //list of post descriptors RV:
    public class PostSelectAdapter extends RecyclerView.Adapter<PostSelectAdapter.PostTitleViewholder> {
        List<PostDescriptor> userPostDescriptors;

        public PostSelectAdapter(List<PostDescriptor> userPostDescriptors) {
            this.userPostDescriptors = userPostDescriptors;
        }

        public class PostTitleViewholder extends RecyclerView.ViewHolder {
            ConstraintLayout constraintLayout;
            PostTitleViewholder(ConstraintLayout view) {
                super(view);
                constraintLayout = view;
            }
        }

        @Override
        public PostTitleViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
            ConstraintLayout l;

            l = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.post_descriptor_layout, parent, false);
            return new PostSelectAdapter.PostTitleViewholder(l);
        }

        @Override
        public void onBindViewHolder(PostTitleViewholder holder, int position) {

            PostDescriptor pd = userPostDescriptors.get(position);
            int score = pd.getScore();
            String title = pd.getTitle();
            int icon = pd.getIcon();
            long time = pd.getTimestamp();
            String place = pd.getLocation() == null ? "" : " @ " + pd.getLocation();

            ImageView iconView = holder.constraintLayout.findViewById(R.id.postdescriptorIcon);
            TextView scoreView = holder.constraintLayout.findViewById(R.id.postdescriptorScore);
            TextView timeAndPlaceView = holder.constraintLayout.findViewById(R.id.postdescriptorTimeAndLocation);
            TextView titleView = holder.constraintLayout.findViewById(R.id.postdescriptorTitle);
            ConstraintLayout cl = holder.constraintLayout;
            holder.constraintLayout.setOnClickListener(v -> onPostDescriptorClicked(cl));

            iconView.setImageBitmap(Utils.getPostIconBitmap(icon, getApplicationContext()));
            if (score >= 20) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_red));
            } else if (score >= 15) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_orangered));
            } else if (score >= 10) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_orange));
            } else if (score >= 5) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_yelloworange));
            } else if (score <= -5) {
                iconView.setBackground(getDrawable(R.drawable.postoutline_brown));
            } else {
                iconView.setBackground(getDrawable(R.drawable.postoutline_yellow));
            }

            scoreView.setText(Integer.toString(score));
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, h:mm a", Locale.US);
            String timeAndPlace = sdf.format(new Date(time)) + place;
            timeAndPlaceView.setText(timeAndPlace);
            titleView.setText(title);

            holder.constraintLayout.setTag(pd.getId());
        }

        @Override
        public int getItemCount() {
            return userPostDescriptors.size();
        }

        public void onPostDescriptorClicked(ConstraintLayout cl) {
            selectedPostId = cl.getTag().toString();
            binding.profileOnePostRv.setAdapter(null);
            binding.profPostRvContainer.setVisibility(View.VISIBLE);
            binding.profPostRvButtons.setVisibility(View.VISIBLE);
            binding.postRvProgressBar.setVisibility(View.VISIBLE);

            binding.aboutMeLayout.setVisibility(View.INVISIBLE);
            binding.allPostsLabel.setVisibility(View.INVISIBLE);
            binding.aboutMeEditBtn.setVisibility(View.INVISIBLE);
            binding.closeUserProfile.setVisibility(View.INVISIBLE);

            if (selectedDV != null) {
                selectedDV.setBackgroundColor(getResources().getColor(R.color.white));
            }
            selectedDV = cl;
            cl.setBackgroundColor(getResources().getColor(R.color.lightgray));

            if (userIsOnTheirOwnProfile) {
                binding.profDeletePostBtn.setVisibility(View.VISIBLE);
            }
            db.collection("posts")
                    .document(selectedPostId)
                    .get()
                    .addOnSuccessListener(response -> {
                        Post post = response.toObject(Post.class);

                        if (post == null) {
                            Utils.showToast(UserProfileActivity.this, "This post no longer exists.");
                            hidePostRv(null);
                            selectedDV.removeAllViews();
                            selectedDV.setVisibility(View.GONE);
                            binding.profileOnePostRv.setAdapter(null);
                            binding.profileOnePostRv.setBackground(null);
                            return;
                        }

                        ArrayList list = (ArrayList) response.getData().get("comments");
                        post.setComments(Utils.turnMapsIntoListOfComments(list));
                        selectedPostLatLng = new LatLng(post.getLat(), post.getLng());

                        postRvAdapter = new PostRvAdapter(
                                post,
                                UserProfileActivity.this,
                                userId,
                                username,
                                thisProfileOwnerId,
                                db);

                        if (post.getScore() >= 20) {
                            binding.profileOnePostRv.setBackground(getDrawable(R.drawable.rounded_square_red));
                        } else if (post.getScore() >= 15) {
                            binding.profileOnePostRv.setBackground(getDrawable(R.drawable.rounded_square_orangered));
                        } else if (post.getScore() >= 10) {
                            binding.profileOnePostRv.setBackground(getDrawable(R.drawable.rounded_square_orange));
                        } else if (post.getScore() >= 5) {
                            binding.profileOnePostRv.setBackground(getDrawable(R.drawable.rounded_square_yelloworange));
                        } else if (post.getScore() <= -5) {
                            binding.profileOnePostRv.setBackground(getDrawable(R.drawable.rounded_square_brown));
                        } else {
                            binding.profileOnePostRv.setBackground(getDrawable(R.drawable.rounded_square_yellow));
                        }

                        binding.profileOnePostRv.setAdapter(postRvAdapter);
                        binding.profileOnePostRv.setVisibility(View.VISIBLE);
                        binding.postRvProgressBar.setVisibility(View.GONE);

                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "error getting post: " + e.toString());
                        binding.postRvProgressBar.setVisibility(View.GONE);
                    });
        }

    }

    public void onDeleteButtonClick(View v) {
        if (!userIsOnTheirOwnProfile) return;
        binding.deletePostModal.setVisibility(View.VISIBLE);
    }
    public void yesDelete(View v) {
        if (!userIsOnTheirOwnProfile) return;
        v.setEnabled(false);
        binding.deletePostProgressBar.setVisibility(View.VISIBLE);

        db.collection("posts")
                .document(selectedPostId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Post post = documentSnapshot.toObject(Post.class);

                        //delete the post from "posts", then delete the post descriptor from "users"
                        db.collection("posts")
                                .document(selectedPostId)
                                .delete()
                                .addOnSuccessListener(result -> {
                                    hidePostRv(null);

                                    //delete post descriptor
                                    db.collection("users")
                                            .document(thisProfileOwnerId)
                                            .get()
                                            .addOnSuccessListener(result2 -> {
                                                User user = result2.toObject(User.class);
                                                List<PostDescriptor> usersNewPDs = new ArrayList<>();
                                                int postScore = 0;
                                                assert user != null;
                                                for (PostDescriptor pd : user.getPostDescriptors()) {
                                                    if (!pd.getId().equals(selectedPostId)) {
                                                        usersNewPDs.add(pd);
                                                    } else {
                                                        postScore = pd.getScore();
                                                    }
                                                }

                                                selectedDV.removeAllViews();
                                                selectedDV.setVisibility(View.GONE);
                                                binding.profileOnePostRv.setAdapter(null);
                                                binding.profileOnePostRv.setBackground(null);

                                                db.collection("users")
                                                        .document(thisProfileOwnerId)
                                                        .update("postDescriptors", usersNewPDs,
                                                                "totalScore", user.getTotalScore() - postScore)
                                                        .addOnSuccessListener(result3 -> {
                                                            binding.deletePostProgressBar.setVisibility(View.GONE);
                                                            Utils.showToast(UserProfileActivity.this, "Post deleted.");
                                                            v.setEnabled(true);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "error removing the deleted post's PD: " + e.toString());
                                                            binding.deletePostProgressBar.setVisibility(View.GONE);
                                                            v.setEnabled(true);
                                                        });

                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "error getting user to delete this PD");
                                                binding.deletePostProgressBar.setVisibility(View.GONE);
                                                v.setEnabled(true);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "error deleting post: " + e.toString());
                                    binding.deletePostProgressBar.setVisibility(View.GONE);
                                    v.setEnabled(true);
                                });


                    }
                });

        //delete the post's image/video from storage. the storage id is the same as the post id
        StorageReference mediaRef = storage.getReference().child(selectedPostId);
        mediaRef.delete()
                .addOnFailureListener(e -> {
                    Log.e(TAG, "failed deleting the media: " + e.toString());
                });

        //delete the post's video thumbnail from storage. its id is "thumbnail[postid]"
        StorageReference thumbnailRef = storage.getReference().child("thumbnail" + selectedPostId);
        thumbnailRef.delete()
                .addOnFailureListener(e -> {
                    Log.e(TAG, "failed deleting the video thumbnail: " + e.toString());
                });

        binding.deletePostModal.setVisibility(View.GONE);
    }
    public void noDelete(View v) {
        binding.deletePostModal.setVisibility(View.GONE);
    }

    public void onEditAboutmeButtonClicked(View v) {
        TextView aboutMeTV = binding.aboutMeTV;
        EditText aboutMeET = binding.aboutMeET;

        if (!userIsOnTheirOwnProfile) return;
        aboutMeBeingEdited = !aboutMeBeingEdited;
        aboutMeTV.setVisibility(aboutMeBeingEdited ? View.GONE : View.VISIBLE);
        aboutMeET.setVisibility(aboutMeBeingEdited ? View.VISIBLE : View.GONE);
        String buttonText = aboutMeBeingEdited ? "SAVE" : "EDIT";
        binding.aboutMeEditBtn.setText(buttonText);

        //on "edit" click:
        if (aboutMeBeingEdited) {
            aboutMeET.setText(aboutMeTV.getText().toString());
        }

        // on "save" click, if the text was changed:
        if (!aboutMeBeingEdited && !aboutMeET.getText().toString().equals(aboutMeTV.getText().toString())) {
            String newAboutmeText = aboutMeET.getText().toString();
            aboutMeTV.setText(newAboutmeText);
            db.collection("users")
                    .document(userId)
                    .update("aboutme", newAboutmeText)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "failed updating aboutme: " + e.toString());
                    });
        }
    }

    public void onViewLocationClicked(View v) {
        Intent goToMapOnLocation = new Intent(UserProfileActivity.this, MapActivity.class);
        goToMapOnLocation.putExtra("lat", selectedPostLatLng.latitude);
        goToMapOnLocation.putExtra("lng", selectedPostLatLng.longitude);
        startActivity(goToMapOnLocation);
    }

    public void hidePostRv(View v) {
        binding.profPostRvContainer.setVisibility(View.GONE);
        binding.profPostRvButtons.setVisibility(View.GONE);
        binding.aboutMeLayout.setVisibility(View.VISIBLE);
        binding.aboutMeEditBtn.setVisibility(View.VISIBLE);
        binding.closeUserProfile.setVisibility(View.VISIBLE);
        binding.allPostsLabel.setVisibility(View.VISIBLE);
    }

    public void onBackButtonClicked(View v) {
        finish();
    }

}

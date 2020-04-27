package com.lucasjwilber.proxynote;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

public class Utils {

    private static final String TAG = "ljw";

    // the name is the unicode translated to decimal. this was originally done to allow me to store these names as primitives
    // but i've since then refactored the Post icon field value to represent the index of the icon in this array, which makes
    // re-ordering the recyclerview in the create post activity easier (they'll appear in the same order that they are in in
    // this array:
    // WARNING: re-ordering these will change the icons for all posts retroactively! add new icons to the end only.
    private static int[] iconIds = new int[] {
            R.drawable.posticon_128514,
            R.drawable.posticon_128515,
            R.drawable.posticon_128521,
            R.drawable.posticon_128580,
            R.drawable.posticon_128522,
            R.drawable.posticon_129315,
            R.drawable.posticon_128525,
            R.drawable.posticon_128526,
            R.drawable.posticon_128527,
            R.drawable.posticon_128528,
            R.drawable.posticon_128529,
            R.drawable.posticon_128530,
            R.drawable.posticon_128531,
            R.drawable.posticon_128536,
            R.drawable.posticon_128540,
            R.drawable.posticon_128544,
            R.drawable.posticon_128546,
            R.drawable.posticon_128548,
            R.drawable.posticon_128553,
            R.drawable.posticon_128517,
            R.drawable.posticon_128556,
            R.drawable.posticon_128557,
            R.drawable.posticon_128558,
            R.drawable.posticon_128561,
            R.drawable.posticon_128568,
            R.drawable.posticon_128564,
            R.drawable.posticon_128565,
            R.drawable.posticon_128519,
            R.drawable.posticon_128566,
            R.drawable.posticon_129324,
            R.drawable.posticon_129326,
            R.drawable.posticon_129392,
            R.drawable.posticon_129297,
            R.drawable.posticon_129300,
            R.drawable.posticon_129396,
            R.drawable.posticon_2639,
            R.drawable.posticon_129313,
            R.drawable.posticon_129314,
            R.drawable.posticon_129320,
            R.drawable.posticon_129321,
            R.drawable.posticon_129323,
            R.drawable.posticon_128567,
            R.drawable.posticon_128578,
            R.drawable.posticon_128064,
            R.drawable.posticon_128169,
            R.drawable.posticon_128076,
            R.drawable.posticon_128077,
            R.drawable.posticon_128078,
            R.drawable.posticon_128075,
            R.drawable.posticon_128405,
            R.drawable.posticon_128079,
            R.drawable.posticon_129311,
            R.drawable.posticon_129305,
            R.drawable.posticon_9994,
            R.drawable.posticon_9996,
            R.drawable.posticon_128591,
            R.drawable.posticon_2764,
            R.drawable.posticon_127867,
            R.drawable.posticon_127873,
            R.drawable.posticon_127881,
            R.drawable.posticon_128293,
            R.drawable.posticon_127926,
            R.drawable.posticon_128008,
            R.drawable.posticon_128021,
            R.drawable.posticon_11088,
            R.drawable.posticon_127379,
            R.drawable.posticon_128680,
            R.drawable.posticon_128761,
            R.drawable.posticon_128286,
            R.drawable.posticon_128139,
            R.drawable.posticon_128175,
            R.drawable.posticon_128178,
            R.drawable.posticon_2615,
            R.drawable.posticon_2620,
            R.drawable.posticon_127752,
            R.drawable.posticon_2753,
            R.drawable.posticon_2049,
            R.drawable.posticon_8252,
            R.drawable.posticon_127790,
            R.drawable.posticon_9203,
            R.drawable.posticon_9762,
            R.drawable.posticon_9888,
    };

    static int[] getIcons() { return iconIds; }

    // since ImageViews require a bitmap while map markers require a bitmap descriptor, this returns a bitmap
    // if a bitmap descriptor is needed it is translated inline rather than using a separate function
    static Bitmap getPostIconBitmap(int code, Context context) {
        Drawable drawable;
        if (code < 0 || code > iconIds.length) {
            drawable = context.getResources().getDrawable(R.drawable.posticon_default);
        } else {
            drawable = context.getResources().getDrawable(iconIds[code]);
        }
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    //this is used primarily to get the post outlines and user-location pin
    static BitmapDescriptor getBitmapDescriptorFromSvg(int resourceId, Context context) {
        Drawable drawable = context.getResources().getDrawable(resourceId);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    static ArrayList<Comment> turnMapsIntoListOfComments(ArrayList<HashMap> mapList) {

        ArrayList<Comment> comments = new ArrayList<>();

        for (HashMap map : mapList) {
            String id = (String) map.get("id");
            String userId = (String) map.get("userId");
            String username = (String) map.get("username");
            String text = (String) map.get("text");
            long timestamp = (long) map.get("timestamp");
            double lat = (double) map.get("lat");
            double lng = (double) map.get("lng");
            double distance;
            if (map.get("distanceFromPost").getClass() == Long.class) {
                Long d = (Long) map.get("distanceFromPost");
                distance = d.doubleValue();
            } else {
                distance = (double) map.get("distanceFromPost");
            }
            long score = (long) map.get("score");
            HashMap<String, Integer> votes = (HashMap<String, Integer>) map.get("votes");

            comments.add(new Comment(id, userId, username, text, timestamp, lat, lng, distance, score, votes));
        }
        return comments;
    }

    static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        if ((lat1 == lat2) && (lng1 == lng2)) {
            return 0;
        }
        else {
            double theta = lng1 - lng2;
            double distance = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            distance = Math.acos(distance);
            distance = Math.toDegrees(distance);
            distance = distance * 60 * 1.1515;
            return distance;
        }
    }

    // using casting instead of formatting methods here because it's faster
    static String getHowFarAway(double d, String type) {
        double dDistance;
        int iDistance;
        if (type.equals("metric")) {
            dDistance = d * 1.609344;
            if (dDistance < 0.1) {
                iDistance = (int) Math.round(dDistance * 1000);
                return iDistance + (iDistance != 1 ? " meters away" : " meter away");
            } else if (dDistance < 10){
                //round the first decimal and remove the others
                dDistance = Math.round(dDistance * 10);
                dDistance = dDistance / 10;
                return dDistance + " km away";
            } else { // if (dDistance >= 10)
                // round it and remove decimals
                dDistance = Math.round(dDistance * 10);
                dDistance = dDistance / 10;
                iDistance = (int) dDistance;
                return iDistance + " km away";
            }
        } else { //if (type.equals("imperial")) {
            dDistance = d * 0.8684;
            if (dDistance < 0.18939393939) { //dDistance < 1000 feet
                iDistance = (int) Math.round(dDistance * 5280);
                return iDistance + (iDistance != 1 ? " feet away" : " foot away");
            } else if (dDistance < 10){
                //round the first decimal and remove the others
                dDistance = Math.round(dDistance * 10);
                dDistance = dDistance / 10;
                return dDistance + (dDistance != 1 ? " miles away" : " mile away");
            } else { // if (dDistance >= 10)
                // round it and remove decimals
                dDistance = Math.round(dDistance * 10);
                dDistance = dDistance / 10;
                iDistance = (int) dDistance;
                return iDistance + " miles away";
            }
        }
    }

    static String getHowLongAgo(long timestamp) {
        long seconds = (new Date().getTime() - timestamp) / 1000;
        long number;
        String unit;
        if (seconds >= 86400) {
            number = seconds/86400;
            unit = "day";
        } else if (seconds >= 3600) {
            number = seconds/3600;
            unit = "hour";
        } else if (seconds >= 60) {
            number = seconds/60;
            unit = "minute";
        } else {
            number = seconds;
            unit = "second";
        }

        if (number == 1) {
            return number + " " + unit + " ago";
        } else {
            return number + " " + unit + "s ago";
        }
    }

    static void showToast(Context context, String message) {
        Toast toast = Toast.makeText(context,
                message,
                Toast.LENGTH_SHORT);
        View toastView = toast.getView();
        toastView.setBackground(context.getResources().getDrawable(R.drawable.rounded_square_accentcolor));
        toast.show();
    }

    static String getTinyZone(double lat, double lng) {
        return (Math.floor(lat * 100) / 100.0) + "/" + (Math.floor(lng * 100) / 100.0);
    }
    static String getSmallZone(double lat, double lng) {
        return (Math.floor(lat * 10) / 10.0) + "/" + (Math.floor(lng * 10) / 10.0);
    }
    static String getMediumZone(double lat, double lng) {
        return Math.floor(lat) + "/" + Math.floor(lng);
    }
    static String getLargeZone(double lat, double lng) {
        return (Math.floor(lat / 10) * 10.0) + "/" + (Math.floor(lng / 10) * 10.0);
    }

    static String getZoneType(LatLngBounds cameraBounds) {
        double left = cameraBounds.southwest.longitude;
        double right = cameraBounds.northeast.longitude;
        double top = cameraBounds.northeast.latitude;
        double bottom = cameraBounds.southwest.latitude;

        //account for antimeridian
        if (right < left) right += 360;

        //get the longer side of the screen's distance:
        double longSideDistance = Math.max(Math.abs(left - right), Math.abs(top - bottom));

        if (longSideDistance < 0.1) {
            return "tinyZone";
        } else if (longSideDistance < 1) {
            return "smallZone";
        } else if (longSideDistance < 10) {
            return "mediumZone";
        } else {
            return "largeZone";
        }
    }

    static List<String> getZonesOnScreen(LatLngBounds cameraBounds, HashSet<String> cache) {
        double left = cameraBounds.southwest.longitude;
        double right = cameraBounds.northeast.longitude;
        double top = cameraBounds.northeast.latitude;
        double bottom = cameraBounds.southwest.latitude;

        List<String> zonesOnScreen = new ArrayList<>();

        double leftCounter = left;
        double rightCounter = right;
        double bottomCounter = bottom;

        //account for antimeridian:
        if (leftCounter > rightCounter) rightCounter += 360;

        String zoneType = getZoneType(cameraBounds);
        double increment;
        if (zoneType.equals("tinyZone")) {
            increment = 0.01;
        } else if (zoneType.equals("smallZone")) {
            increment = 0.1;
        } else if (zoneType.equals("mediumZone")) {
            increment = 1;
        } else {
            increment = 10;
        }

        while (leftCounter <= rightCounter + increment) {
            while (bottomCounter <= top) {
                //tinyZone by default
                String zone = getTinyZone(bottomCounter, left);
                if (zoneType.equals("smallZone")) {
                    zone = getSmallZone(bottomCounter, left);
                } else if (zoneType.equals("mediumZone")) {
                    zone = getMediumZone(bottomCounter, left);
                } else if (zoneType.equals("largeZone")){
                    zone = getLargeZone(bottomCounter, left);
                }
                if (!cache.contains(zone)) zonesOnScreen.add(zone);
                bottomCounter += increment;
            }
            bottomCounter = bottom;
            left += increment;
            leftCounter += increment;

            //account for antimeridian:
            if (left >= 180.0) left = -180.0;
        }
        return zonesOnScreen;
    }

    static boolean isUserAuthorized() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("proxyNotePrefs", Context.MODE_PRIVATE);
        String loginType = sharedPreferences.getString("loginType", null);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            return false;
        } else {
            user.reload();
            return loginType == null || !loginType.equals("firebase") || user.isEmailVerified();
        }
    }

    // limits an EditText's length and number of lines, and updates the alpha of a provided TextView to
    // show the user when they're approaching the max char limit
    static TextWatcher makeTextWatcher(EditText editText, TextView counter, int maxLength) {
        return new TextWatcher() {
            String currentText;
            int cursorPosition;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.i(TAG, "before: " + s.toString() + ", " + editText.getText().toString());

                currentText = editText.getText().toString();
                cursorPosition = editText.getSelectionStart() - 1;
            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.i(TAG, "on: " + s.toString() + ", " + editText.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i(TAG, "after: " + s.toString() + ", " + editText.getText().toString());

                if (editText.getLineCount() > editText.getMaxLines() ||
                        (editText.length() -1) >= maxLength) {
                    editText.setText(currentText);
                    editText.setSelection(cursorPosition);
                } else {
                    String counterText = (editText.length()) + "/" + maxLength;
                    counter.setText(counterText);
                }

                float alpha = ( (float) editText.length() ) / maxLength;
                counter.setAlpha(alpha);
            }
        };
    }

}

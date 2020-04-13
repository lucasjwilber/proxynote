package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
            R.drawable.posticon_128517,
            R.drawable.posticon_128519,
            R.drawable.posticon_128521,
            R.drawable.posticon_128522,
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
            R.drawable.posticon_128556,
            R.drawable.posticon_128557,
            R.drawable.posticon_128558,
            R.drawable.posticon_128561,
            R.drawable.posticon_128568,
            R.drawable.posticon_128564,
            R.drawable.posticon_128565,
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
            R.drawable.posticon_129315,
            R.drawable.posticon_129320,
            R.drawable.posticon_129321,
            R.drawable.posticon_129323,
            R.drawable.posticon_128567,
            R.drawable.posticon_128578,
            R.drawable.posticon_128580,
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
            R.drawable.posticon_128684,
            R.drawable.posticon_128761,
            R.drawable.posticon_128286,
            R.drawable.posticon_128139,
            R.drawable.posticon_128175,
            R.drawable.posticon_128178,
            R.drawable.posticon_128138,
            R.drawable.posticon_2615,
            R.drawable.posticon_2620,
            R.drawable.posticon_127752,
            R.drawable.posticon_2753,
            R.drawable.posticon_2049,
            R.drawable.posticon_8252,
            R.drawable.posticon_127790,
            R.drawable.posticon_127814,
            R.drawable.posticon_127825,
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
            drawable = context.getDrawable(R.drawable.posticon_default);
        } else {
            drawable = context.getDrawable(iconIds[code]);
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
        Drawable drawable = context.getDrawable(resourceId);
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

    static String getLatZoneSize(double top, double bottom) {
        double latHeight = Math.abs(top - bottom);
        if (latHeight < 1) {
            return "smallLatZone";
        } else if (latHeight >= 1 && latHeight < 10) {
            return "mediumLatZone";
        } else {
            return "largeLatZone";
        }
    }

    static List<Double> getLatQueryRange(double top, double bottom) {
        double centerOfScreenLat = Math.max(top, bottom)
                - (Math.abs(top - bottom) / 2);
        Log.i(TAG, "screen is centered on latitude " + centerOfScreenLat);

        List<Double> latZonesToQuery = new ArrayList<>();

        double latHeight = Math.abs(top - bottom);

        // by default add the latZone we're in and the ones above and below,
        // then add more based on the latitude boundaries of the screen
        if (latHeight < 1) {
            double smallLatZone = Math.round(centerOfScreenLat * 10) / 10.0;
            latZonesToQuery.add(smallLatZone);
            latZonesToQuery.add(smallLatZone - 0.1f);
            latZonesToQuery.add(smallLatZone + 0.1f);

            if (latHeight >= 0.2) {
                latZonesToQuery.add(smallLatZone - 0.2f);
                latZonesToQuery.add(smallLatZone + 0.2f);
            }
            if (latHeight >= 0.3) {
                latZonesToQuery.add(smallLatZone - 0.3f);
                latZonesToQuery.add(smallLatZone + 0.3f);
            }
            if (latHeight >= 0.4) {
                latZonesToQuery.add(smallLatZone - 0.4f);
                latZonesToQuery.add(smallLatZone + 0.4f);
            }
            Log.i(TAG, "using small latZones");
        } else if (latHeight >= 1 && latHeight < 10) {
            double mediumLatZone = Math.round(centerOfScreenLat);

            latZonesToQuery.add(mediumLatZone);
            latZonesToQuery.add(mediumLatZone - 1);
            latZonesToQuery.add(mediumLatZone + 1);

            if (latHeight >= 2) {
                latZonesToQuery.add(mediumLatZone - 2);
                latZonesToQuery.add(mediumLatZone + 2);
            }
            if (latHeight >= 3) {
                latZonesToQuery.add(mediumLatZone - 3);
                latZonesToQuery.add(mediumLatZone + 3);
            }
            if (latHeight >= 4) {
                latZonesToQuery.add(mediumLatZone - 4);
                latZonesToQuery.add(mediumLatZone + 4);
            }
            Log.i(TAG, "using medium latZones");
        } else {
            double largeLatZone = Math.round(centerOfScreenLat / 10) * 10;

            latZonesToQuery.add(largeLatZone);
            latZonesToQuery.add(largeLatZone - 10);
            latZonesToQuery.add(largeLatZone + 10);

            if (latHeight >= 20) {
                latZonesToQuery.add(largeLatZone - 20);
                latZonesToQuery.add(largeLatZone + 20);
            }
            if (latHeight >= 30) {
                latZonesToQuery.add(largeLatZone - 30);
                latZonesToQuery.add(largeLatZone + 30);
            }
            if (latHeight >= 40) {
                latZonesToQuery.add(largeLatZone - 40);
                latZonesToQuery.add(largeLatZone + 40);
            }
            Log.i(TAG, "using large latZones");
        }

        Log.i(TAG, "querying " + latZonesToQuery.size() + " latZones");
        return latZonesToQuery;
    }

}

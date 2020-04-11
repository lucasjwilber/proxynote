package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Utils {

    private static int[] iconIds = new int[] {
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

    static Bitmap getBitmap(int drawableRes, Context context) {
        Drawable drawable = context.getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    static BitmapDescriptor getPostIconBitmapDescriptor(int code, Context context) {
        if (code < 0 || code >= iconIds.length) {
            return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_default, context));
        } else {
            return BitmapDescriptorFactory.fromBitmap(getBitmap(iconIds[code], context));
        }
    }

    static Bitmap getPostIconBitmap(int code, Context context) {
        if (code < 0 || code >= iconIds.length) {
            return getBitmap(R.drawable.posticon_default, context);
        } else {
            return getBitmap(iconIds[code], context);
        }
    }

    public static void showToast(Context context, String message) {
        Toast toast = Toast.makeText(context,
                message,
                Toast.LENGTH_SHORT);
        View toastView = toast.getView();
        toastView.setBackground(context.getResources().getDrawable(R.drawable.rounded_square_accentcolor));
        toast.show();
    }

}

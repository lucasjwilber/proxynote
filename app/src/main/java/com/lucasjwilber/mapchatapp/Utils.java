package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Utils {

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
//            int reports;
//            if (map.get("reports") == null) {
//                reports = 0;
//            } else if (map.get("reports").getClass() == Long.class) {
//                Long r = (Long) map.get("reports");
//                reports = r.intValue();
//            } else {
//                reports = (int) map.get("reports");
//            }

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
                return iDistance + " meters away";
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
                return iDistance + " feet away";
            } else if (dDistance < 10){
                //round the first decimal and remove the others
                dDistance = Math.round(dDistance * 10);
                dDistance = dDistance / 10;
                return dDistance + " miles away";
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

    static String getFormattedTime(long timestamp) {
        return new java.util.Date(timestamp).toString();
    }

    private static Bitmap getBitmap(int drawableRes, Context context) {
        Drawable drawable = context.getResources().getDrawable(drawableRes);
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    static BitmapDescriptor getPostIconBitmapDescriptor(int code, Context context) {
        switch (code) {
            case 127867:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_127867, context));
            case 127881:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_127881, context));
            case 128064:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128064, context));
            case 128076:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128076, context));
            case 128077:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128077, context));
            case 128078:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128078, context));
            case 128293:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128293, context));
            case 128405:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128405, context));
            case 128514:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128514, context));
            case 128517:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128517, context));
            case 128521:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128521, context));
            case 128522:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128522, context));
            case 128525:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128525, context));
            case 128526:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128526, context));
            case 128528:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128528, context));
            case 128557:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128557, context));
            case 128580:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128580, context));
            case 128591:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_128591, context));
            case 129300:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_129300, context));
            case 129314:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_129314, context));
            case 129315:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_129315, context));
            case 9996:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_9996, context));
            case 0:
            default:
                return BitmapDescriptorFactory.fromBitmap(getBitmap(R.drawable.posticon_default, context));
        }
    }

    static Bitmap getPostIconBitmap(int code, Context context) {
        switch (code) {
            case 127867:
                return getBitmap(R.drawable.posticon_127867, context);
            case 127881:
                return getBitmap(R.drawable.posticon_127881, context);
            case 128064:
                return getBitmap(R.drawable.posticon_128064, context);
            case 128076:
                return getBitmap(R.drawable.posticon_128076, context);
            case 128077:
                return getBitmap(R.drawable.posticon_128077, context);
            case 128078:
                return getBitmap(R.drawable.posticon_128078, context);
            case 128293:
                return getBitmap(R.drawable.posticon_128293, context);
            case 128405:
                return getBitmap(R.drawable.posticon_128405, context);
            case 128514:
                return getBitmap(R.drawable.posticon_128514, context);
            case 128517:
                return getBitmap(R.drawable.posticon_128517, context);
            case 128521:
                return getBitmap(R.drawable.posticon_128521, context);
            case 128522:
                return getBitmap(R.drawable.posticon_128522, context);
            case 128525:
                return getBitmap(R.drawable.posticon_128525, context);
            case 128526:
                return getBitmap(R.drawable.posticon_128526, context);
            case 128528:
                return getBitmap(R.drawable.posticon_128528, context);
            case 128557:
                return getBitmap(R.drawable.posticon_128557, context);
            case 128580:
                return getBitmap(R.drawable.posticon_128580, context);
            case 128591:
                return getBitmap(R.drawable.posticon_128591, context);
            case 129300:
                return getBitmap(R.drawable.posticon_129300, context);
            case 129314:
                return getBitmap(R.drawable.posticon_129314, context);
            case 129315:
                return getBitmap(R.drawable.posticon_129315, context);
            case 9996:
                return getBitmap(R.drawable.posticon_9996, context);
            case 0:
            default:
                return getBitmap(R.drawable.posticon_default, context);
        }
    }

}

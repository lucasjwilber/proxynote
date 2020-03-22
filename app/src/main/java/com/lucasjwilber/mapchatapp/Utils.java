package com.lucasjwilber.mapchatapp;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Utils {
    static String getZoneOfCoordinates(double lat, double lng) {
        return getZoneOfLatOrLng(lat) + "/" + getZoneOfLatOrLng(lng);
    }
    private static String getZoneOfLatOrLng(double latOrLng) {
        String[] latSplit = Double.toString(latOrLng).split("\\.");
        double zone = Math.abs(latOrLng*10 % 1)*100;
        if (zone >= 75)
            return latSplit[0] + "." + latSplit[1].charAt(0) + "75";
        else if (zone >= 50 && zone <= 74)
            return latSplit[0] + "." + latSplit[1].charAt(0) + "50";
        else if (zone >= 25 && zone <= 49)
            return latSplit[0] + "." + latSplit[1].charAt(0) + "25";
        else
            return latSplit[0] + "." + latSplit[1].charAt(0) + "00";
    }
    // turn the double into a String for easier manipulation, then back to a double.
    // there's probably a better way to do this.

    //needs to round each .X
    static double getZone(double coordinate) {
        String[] latSplit = Double.toString(coordinate).split("\\.");
        String result;
        double zone = Math.abs(coordinate*10 % 1)*100;
        if (zone >= 75)
            result = latSplit[0] + "." + latSplit[1].charAt(0) + "75";
        else if (zone >= 50 && zone <= 74)
            result = latSplit[0] + "." + latSplit[1].charAt(0) + "50";
        else if (zone >= 25 && zone <= 49)
            result = latSplit[0] + "." + latSplit[1].charAt(0) + "25";
        else
            result = latSplit[0] + "." + latSplit[1].charAt(0) + "00";
        return Double.parseDouble(result);
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
            // if lat/long were manually changed in firestore they become Longs
            double distance;
            if (map.get("distanceFromPost").getClass() == Long.class) {
                Log.i("ljw", "issa long");
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
        if (type.equals("imperial")) {
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
        } else { // if (type.equals("metric")
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

}

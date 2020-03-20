package com.lucasjwilber.mapchatapp;

import java.util.ArrayList;
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
            long score = (long) map.get("score");
            double lat = (double) map.get("lat");
            double lng = (double) map.get("lng");
            String text = (String) map.get("text");
            String id = (String) map.get("id");
            String userId = (String) map.get("userId");
            String username = (String) map.get("username");
            long timestamp = (long) map.get("timestamp");

            comments.add(new Comment(id, userId, username, text, timestamp, lat, lng, score));
        }
        return comments;
    }

    static double getDistance(double lat1, double lon1, double lat2, double lon2, String unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit.equals("K")) {
                dist = dist * 1.609344;
            } else if (unit.equals("N")) {
                dist = dist * 0.8684;
            }
            return (dist);
        }
    }
}

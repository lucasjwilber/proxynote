package com.lucasjwilber.mapchatapp;

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
            double distance = (double) map.get("distanceFromPost");
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

    static String getHowFarAway(double d, String type) {
        double distance;
        String unit;

        if (type.equals("imperial")) {
            distance = d * 0.8684;
            unit = "miles";
            if (distance < 0.18939393939) { //distance < 1000 feet
                unit = "feet";
                distance = (int) Math.round(distance * 5280);
            } else {
                distance *= 10;
                distance = Math.round(distance);
                distance = (int) distance / 10;
            }
        } else { //metric
            distance = d * 1.609344;
            unit = "km";
            if (distance < 0.1) {
                unit = "meters";
                distance = (int) Math.round(distance * 1000);
            } else {
                distance *= 10;
                distance = Math.round(distance);
                distance = (int) distance / 10;
            }
        }

        //TODO: cut off the 3rd+ decimals
        return distance + " " + unit + " away";
    }

    static String getHowLongAgo(long timestamp) {
        long seconds = (new Date().getTime() - timestamp) / 1000;
        long number;
        String unit;
        if (seconds < 60) {
            number = seconds;
            unit = "second";
        } else if (seconds >= 60 && seconds < 3600) {
            number = seconds/60;
            unit = "minute";
        } else if (seconds >= 3600 && seconds < 86400) {
            number = seconds/3600;
            unit = "hour";
        } else { //if (seconds <= 86400) {
            number = seconds/86400;
            unit = "day";
        }
        if (number == 1) {
            return number + " " + unit + " ago";
        } else {
            return number + " " + unit + "s ago";
        }
    }

}

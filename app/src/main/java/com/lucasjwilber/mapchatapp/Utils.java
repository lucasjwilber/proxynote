package com.lucasjwilber.mapchatapp;

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
}

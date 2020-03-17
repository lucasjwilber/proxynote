package com.lucasjwilber.mapchatapp;

// thanks to:
// https://inducesmile.com/android-programming/how-to-create-custom-infowindow-with-google-map-marker-in-android/

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PostInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private Context context;

    public PostInfoWindowAdapter(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public View getInfoWindow(Marker marker) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        View view =  inflater.inflate(R.layout.post_infowindow, null);
        TextView commentTV = view.findViewById(R.id.commentWindowTV);

        StringBuilder commentHTML = new StringBuilder();

        //if it's the user's location marker it wont be tagged
        if (marker.getTag() == null) {
            commentHTML.append("<h4>" + marker.getTitle() + "</h4>");
            commentHTML.append("<p>" + marker.getSnippet() + "</p>");
        } else {
            Post post = (Post) marker.getTag();
            commentHTML.append("<h4>" + post.getTitle() + "</h4>");
            commentHTML.append("<p>" + post.getText() + "</p>");

            // add comments
//        Post c = (Post) marker.getTag();
//        if (c != null && c.comments != null) {
//            Date date = new java.util.Date((long) c.getTimestamp());
//            SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-M-yyyy hh:mm:ss", Locale.US);
//            String formattedDate = sdf.format(date);
//            for (Comment comment : c.comments) {
//                //  TODO: use time for when comment was made, not comment
////                commentHTML.append("<br><p><i><b>" + comment.getUsername() + "</b>, " + formattedDate + "</i></p>");
//                commentHTML.append("<br><p><i><b>" + comment.getUsername() + "</b></i></p>");
//                commentHTML.append("<p>" + comment.getBody() + "</p>");
//            }
//        }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            commentTV.setText(Html.fromHtml(commentHTML.toString(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            commentTV.setText(Html.fromHtml(commentHTML.toString()));
        }
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

}

package com.lucasjwilber.mapchatapp;


import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.bumptech.glide.annotation.GlideOption;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.BaseRequestOptions;

@com.bumptech.glide.annotation.GlideExtension
public class MyGlideExtension {

    private MyGlideExtension() {}

    @NonNull
    @GlideOption
    static BaseRequestOptions<?> roundedCorners(BaseRequestOptions<?> options, @NonNull Context context, int cornerRadius) {
        int px = Math.round(cornerRadius * (context.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return options.transform(new RoundedCorners(px));
    }
}

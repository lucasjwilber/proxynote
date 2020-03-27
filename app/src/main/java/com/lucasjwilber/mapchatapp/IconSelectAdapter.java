package com.lucasjwilber.mapchatapp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

public class IconSelectAdapter extends RecyclerView.Adapter<IconSelectAdapter.IconViewholder> {

    private Drawable[] icons;
    int selectedIcon;

    public IconSelectAdapter(Context context) {

        //when adding new icons be sure to update onBindViewholder() below and getPostIcon() in Utils
        icons = new Drawable[]{
                context.getDrawable(R.drawable.posticon_127867),
                context.getDrawable(R.drawable.posticon_127881),
                context.getDrawable(R.drawable.posticon_128064),
                context.getDrawable(R.drawable.posticon_128076),
                context.getDrawable(R.drawable.posticon_128077),
                context.getDrawable(R.drawable.posticon_128078),
                context.getDrawable(R.drawable.posticon_128293),
                context.getDrawable(R.drawable.posticon_128405),
                context.getDrawable(R.drawable.posticon_128514),
                context.getDrawable(R.drawable.posticon_128517),
                context.getDrawable(R.drawable.posticon_128521),
                context.getDrawable(R.drawable.posticon_128522),
                context.getDrawable(R.drawable.posticon_128525),
                context.getDrawable(R.drawable.posticon_128526),
                context.getDrawable(R.drawable.posticon_128528),
                context.getDrawable(R.drawable.posticon_128557),
                context.getDrawable(R.drawable.posticon_128580),
                context.getDrawable(R.drawable.posticon_128591),
                context.getDrawable(R.drawable.posticon_129300),
                context.getDrawable(R.drawable.posticon_129314),
                context.getDrawable(R.drawable.posticon_129315),
                context.getDrawable(R.drawable.posticon_9996),
        };
    }

    static class IconViewholder extends RecyclerView.ViewHolder {
        ImageView imageView;

        IconViewholder(ImageView view) {
            super(view);
            imageView = view;
        }

    }

    //TODO: do i need the viewType parameter here?
    @Override
    public IconViewholder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView iconView = (ImageView) LayoutInflater.from(parent.getContext()).inflate(R.layout.posticon_imageview, parent, false);
        return new IconViewholder(iconView);
    }

    @Override
    public void onBindViewHolder(IconViewholder holder, int position) {
        holder.imageView.setImageDrawable(icons[position]);
        switch (position) {
            case 0:
                holder.imageView.setTag(127867);
                break;
            case 1:
                holder.imageView.setTag(127881);
                break;
            case 2:
                holder.imageView.setTag(128064);
                break;
            case 3:
                holder.imageView.setTag(128076);
                break;
            case 4:
                holder.imageView.setTag(128077);
                break;
            case 5:
                holder.imageView.setTag(128078);
                break;
            case 6:
                holder.imageView.setTag(128293);
                break;
            case 7:
                holder.imageView.setTag(128405);
                break;
            case 8:
                holder.imageView.setTag(128514);
                break;
            case 9:
                holder.imageView.setTag(128517);
                break;
            case 10:
                holder.imageView.setTag(128521);
                break;
            case 11:
                holder.imageView.setTag(128522);
                break;
            case 12:
                holder.imageView.setTag(128525);
                break;
            case 13:
                holder.imageView.setTag(128526);
                break;
            case 14:
                holder.imageView.setTag(128528);
                break;
            case 15:
                holder.imageView.setTag(128557);
                break;
            case 16:
                holder.imageView.setTag(128580);
                break;
            case 17:
                holder.imageView.setTag(128591);
                break;
            case 18:
                holder.imageView.setTag(129300);
                break;
            case 19:
                holder.imageView.setTag(129314);
                break;
            case 20:
                holder.imageView.setTag(129315);
                break;
            case 21:
                holder.imageView.setTag(9996);
                break;
            default:
                holder.imageView.setTag(0);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return icons.length;
    }

    public int getSelectedIcon() {
        return selectedIcon;
    }
    public void setSelectedIcon(int icon) {
        selectedIcon = icon;
    }

}
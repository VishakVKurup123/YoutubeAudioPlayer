package com.vvk.youtubeaudioplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by developer on 11/19/17.
 */
// https://www.journaldev.com/10416/android-listview-with-custom-adapter-example-tutorial

public class PlaylistAdapter extends ArrayAdapter<VideoData> {
    private ArrayList<VideoData> dataSet;
    private Context mContext;
    private int mLastPosition = -1;
    private static final int IO_BUFFER_SIZE = 4 * 1024;
    private PlaylistAdapterEventListener mPlaylistAdapterEventListener;

    // View lookup cache
    private static class ViewHolder {
        TextView txtTitle;
        ImageView imgAlbum;
        ImageButton deleteButton;
    }

    public PlaylistAdapter(ArrayList<VideoData> data, Context context) {
        super(context, R.layout.listitem, data);
        this.dataSet = data;
        this.mContext=context;
    }

    public void addPlaylistAdapterEventListener(PlaylistAdapterEventListener playlistAdapterEventListener) {
        mPlaylistAdapterEventListener = playlistAdapterEventListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final VideoData dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        final ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.listitem, parent, false);
            viewHolder.txtTitle = (TextView) convertView.findViewById(R.id.title);
            viewHolder.imgAlbum = (ImageView) convertView.findViewById(R.id.albumImage);
            viewHolder.deleteButton = (ImageButton) convertView.findViewById(R.id.deleteListBtn);
            result=convertView;
            viewHolder.deleteButton.setTag(position);
            viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                   if(mPlaylistAdapterEventListener != null) {
                       mPlaylistAdapterEventListener.onDeleteItemRequested((int)view.getTag());
                   }
                }
            });


            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > mLastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        result.startAnimation(animation);
        mLastPosition = position;

        viewHolder.txtTitle.setText(dataModel.getTitle());
        Bitmap albumImage = dataModel.getAlbumImage();

        viewHolder.imgAlbum.setImageBitmap(albumImage);

        //viewHolder.info.setOnClickListener(this);
        //viewHolder.info.setTag(position);
        // Return the completed view to render on screen
        return convertView;
    }



}

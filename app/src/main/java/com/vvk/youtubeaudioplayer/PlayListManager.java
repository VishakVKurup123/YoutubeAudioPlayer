package com.vvk.youtubeaudioplayer;

import android.content.Context;
import android.graphics.Bitmap;

import com.squareup.okhttp.OkHttpClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by gwilliams on 10/8/17.
 */

public class PlayListManager implements AudioConfigurationEventListener, Serializable, ExtractionEventListener {

    private OkHttpClient client = new OkHttpClient();
    private ArrayList<VideoData> mSongList = new ArrayList<>();
    private PlayListManagerEventsListener mPlayListManagerEventsListener;
    private static PlayListManager mPlayListManager;
    private static Context mContext;
   // private BackUpManager mBackupManager;
    private SQLiteManager mSqliteManager;

    public int getPlayIndex() {
        return mPlayIndex;
    }

    public void setPlayIndex(int mPlayIndex) {
        this.mPlayIndex = mPlayIndex;
    }

    private int mPlayIndex = -1;

    private PlayListManager() {
        mSqliteManager = SQLiteManager.getInstance(mContext);
       // mBackupManager = BackUpManager.getInstance();
        // mSongList = mBackupManager.readPlayList();
        mSongList = mSqliteManager.getAllRecords();
    }

    public static PlayListManager getInstance(Context context) {
        if(null == mPlayListManager) {
            mContext = context;
            mPlayListManager = new PlayListManager();
        }
        return  mPlayListManager;
    }


    public void addUrltoList(String url){
        Util.extractVideoInfo(url,mContext, this);
     }

     public void removeSong(int index) {
        VideoData videoData = mSongList.get(index);
        mSqliteManager.removeData(videoData.getVideoId());
        mSongList.remove(index);

        // mBackupManager.writePlayList(mSongList);
     }

    public ArrayList<VideoData> getSonglList(){
        return mSongList;
    }

    public void addPlayListManagerEventsListener(PlayListManagerEventsListener playListManagerEventsListener) {
        mPlayListManagerEventsListener = playListManagerEventsListener;
    }


    @Override
    public void onUrlParsed(VideoData videoData) {
        mSongList.add(videoData);
//        mBackupManager.writePlayList(mSongList);
        if(mPlayListManagerEventsListener != null) {
            mPlayListManagerEventsListener.onSongAdded();
            mSqliteManager.insertNewMusicData(videoData);
        }
    }

    @Override
    public void extractionCompleted(HashMap<String, String> results) {
        final VideoData videoData = new VideoData(results.get("BASE_URL"));
        videoData.setVideoId(results.get("VIDEO_ID"));
        videoData.setTitle(results.get("TITLE"));
        videoData.setThumbNailUrl(results.get("THUMP_URL"));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap albumImage = Util.getBitmapfromUrl(videoData.getImageUrl());
                videoData.setAlbumImage(albumImage);
                Util.writeThumbNailImage(videoData.getAlbumImage(), videoData.getVideoId(),mContext);
                onUrlParsed(videoData);
            }
        }).start();
    }



}

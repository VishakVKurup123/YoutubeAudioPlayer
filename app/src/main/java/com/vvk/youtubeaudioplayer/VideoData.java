package com.vvk.youtubeaudioplayer;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by gwilliams on 10/7/17.
 */

public class VideoData implements Serializable {

    private String mBaseURL;
    private long mContentLength;
    private String mTitle;
    private String mAudioUrl;
    private String mVideoId;

    public String getThumbNailUrl() {
        return mThumbNailUrl;
    }

    public void setThumbNailUrl(String mThumbNailUrl) {
        this.mThumbNailUrl = mThumbNailUrl;
    }

    private String mThumbNailUrl;


    private BitmapDataObject mAlbumImage;
    private AudioConfigurationEventListener mAudioConfigurationEventListener;


    public void setAlbumImage(Bitmap mAlbumImage) {
        this.mAlbumImage = new BitmapDataObject(mAlbumImage);
    }

    public Bitmap getAlbumImage() {
        return mAlbumImage.getCurrentImage();
    }

    public String getVideoId() {
        return mVideoId;
    }

    public void setVideoId(String mVideoId) {
        this.mVideoId = mVideoId;
    }



    public String getAudioUrl() {
        return mAudioUrl;
    }
    public void setAudioURL(String mAudioUrl) {
        this.mAudioUrl = mAudioUrl;
    }

    public String getImageUrl() {
        String imageUrl = "https://img.youtube.com/vi/"+mVideoId+"/0.jpg";
        return imageUrl;
    }

    public void addAudioConfigurationEventLIstener(AudioConfigurationEventListener audioConfigurationEventLIstener) {
        mAudioConfigurationEventListener = audioConfigurationEventLIstener;
    }



    public String getBaseURL() {
        return mBaseURL;
    }

    public void setBaseURL(String mBaseURL) {
        this.mBaseURL = mBaseURL;
    }

    public long getContentLength() {
        return mContentLength;
    }

    public void setContentLength(long mContentLength) {
        this.mContentLength = mContentLength;
    }

    public String getTitle() {
        return mTitle;
    }

    public void  setTitle(String title) {
        mTitle = title;
    }


    public VideoData(String baseUrl){
        mBaseURL = baseUrl;
    }


    public String toString() {
        String s = "";
        s+="BaseURL "+mBaseURL+"\n";
        s+="Title "+mTitle+"\n";
        s+="ContentLength "+mContentLength;
        return s;
    }

}

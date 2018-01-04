package com.vvk.youtubeaudioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Vishak V Kurup on 10/14/17.
 */

public class MediaPlayerUtil implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, Runnable, ExtractionEventListener {

    private MediaPlayer mMediaPlayer;
    private int mResumePosition = 0;
    private MediaPlayerEventListener mMediaPlayerEventListener;
    private int mCurrentPlayStatus = PlayStatus.STOP;
    private Thread mDurationCheckThread;
    private static  MediaPlayerUtil mMediaPlayerUtil;
    private Context mContext;

    public static MediaPlayerUtil getInstance() {
        if(null == mMediaPlayerUtil) {
            mMediaPlayerUtil = new MediaPlayerUtil();
        }
        return mMediaPlayerUtil;
    }


    public void initMediaPlayer(String url, Context context) {
        mContext = context;
        Util.extractVideoInfo(url, mContext, this);
        notifyPlayStatus(PlayStatus.BUFFERING);
  }

    public void addMediaPlayerEventListener(MediaPlayerEventListener mediaPlayerEventListener) {
        mMediaPlayerEventListener = mediaPlayerEventListener;
    }

    private void  notifyPlayStatus(int playStatus) {
        mCurrentPlayStatus = playStatus;
        if(mMediaPlayerEventListener != null) {
            mMediaPlayerEventListener.onPlayStatusChanged(playStatus);
        }
    }

    private void  notifySongCompleted() {
        if(mMediaPlayerEventListener != null) {
            mMediaPlayerEventListener.onSongCompleted();
            notifyPlayStatus(PlayStatus.STOP);
        }
    }

    public  int getCurrentPlaystatus(){
        return mCurrentPlayStatus;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.v("MP","Song complete");
        notifySongCompleted();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        //Log.v("MP","onSeekComplete "+ mediaPlayer.getDuration());

    }

    private void playMedia() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            notifyPlayStatus(PlayStatus.PLAY);
            mDurationCheckThread = new Thread(this);
            mDurationCheckThread.start();
        }

    }

    public void stopMedia() {
        if (mMediaPlayer == null) return;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    public void pauseMedia() {
        if (mMediaPlayer !=null && mMediaPlayer.isPlaying()) {
            mResumePosition = mMediaPlayer.getCurrentPosition();
            mMediaPlayer.pause();
            notifyPlayStatus(PlayStatus.PAUSE);
        }
    }

    public void resumeMedia() {
        if (mMediaPlayer!=null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(mResumePosition);
            mMediaPlayer.start();
            notifyPlayStatus(PlayStatus.PLAY);
        }
    }

    public void seek(int progress) {
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(progress);
        }
    }

    public void setVolume(float leftVolume, float rightVolume) {
        if(mMediaPlayer!=null) {
            mMediaPlayer.setVolume(leftVolume, rightVolume);
        }
    }

    @Override
    public void run() {
      //  Log.v("MP", "Thread start "+mCurrentPlayStatus);
        while(mCurrentPlayStatus != PlayStatus.STOP) {
       //     Log.v("MP", "Thread started");
            if(mCurrentPlayStatus == PlayStatus.PLAY) {
                int duration = mMediaPlayer.getDuration();
                int currentPosition = mMediaPlayer.getCurrentPosition();
                mMediaPlayerEventListener.onSongProgress(currentPosition, duration);
         //       Log.v("MP", "Duration "+duration+":"+currentPosition);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e("MP", "Thread ",e);
            }
        }
    }


    @Override
    public void extractionCompleted(HashMap<String, String> results) {
        String audioUrl = results.get("AUDIO_URL");
        if(mMediaPlayer != null) {
            //Reset so that the MediaPlayer is not pointing to another data source
            mMediaPlayer.reset();
        }

        mMediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mMediaPlayer.reset();

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mMediaPlayer.setDataSource(audioUrl);
        } catch (IOException e) {
            e.printStackTrace();
            //stopSelf();
        }
        mMediaPlayer.prepareAsync();
    }
}

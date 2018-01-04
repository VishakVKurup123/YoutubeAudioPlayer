package com.vvk.youtubeaudioplayer;

/**
 * Created by developer on 11/23/17.
 */

public interface MediaPlayerEventListener {

    void onPlayStatusChanged(int status);
    void onSongCompleted();
    void onSongProgress(int current, int total);
}

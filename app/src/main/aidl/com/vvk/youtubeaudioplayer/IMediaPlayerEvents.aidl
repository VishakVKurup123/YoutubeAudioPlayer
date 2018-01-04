// IMediaPlayerEvents.aidl
package com.vvk.youtubeaudioplayer;

// Declare any non-default types here with import statements

interface IMediaPlayerEvents {
        void onPlayStatusChanged(int status);
        void onSongCompleted();
        void onSongProgress(int current, int total);
}

// IPlaybackControls.aidl
package com.vvk.youtubeaudioplayer;
import  com.vvk.youtubeaudioplayer.IMediaPlayerEvents;
// Declare any non-default types here with import statements

interface IPlaybackControls {
        void skipNext();
        void pause();
        void skipPrev();
        void resume();
        void seek(int time);
        void playFromList(int index);
        void addMediaPlayerEventsListener(IMediaPlayerEvents iMediaPlayerEvents);
        int getCurrentPlayIndex();
        int getCurrentPlayStatus();
}

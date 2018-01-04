package com.vvk.youtubeaudioplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

public class AudioPlayerService extends Service implements  MediaPlayerEventListener,AudioManager.OnAudioFocusChangeListener {


    public static final String ACTION_PLAY = "com.vvk.youtubeaudioservice.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.vvk.youtubeaudioservice.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.vvk.youtubeaudioservice.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.vvk.youtubeaudioservice.ACTION_NEXT";
    public static final String ACTION_STOP = "com.vvk.youtubeaudioservice.ACTION_STOP";

    private MediaPlayerUtil mMediaPlayerUtil;
    private PlayListManager mPlayListManager;
    private IMediaPlayerEvents mIMediaPlayerEvents;
    private int mCurrentIndex = -1;
    private StorageUtil mStorageUtil;
    private int mPlayStatus = PlayStatus.STOP;

    //MediaSession
    private MediaSessionManager mMediaSessionManager;
    private MediaSession mMediaSession;
    private MediaController.TransportControls transportControls;
    private MediaSession.Token mToken;

    private AudioManager mAudioManager;

    private int mRepeatMode;

    private IPlaybackControls.Stub mIPlaybackControls = new IPlaybackControls.Stub() {
        @Override
        public void skipNext() throws RemoteException {
           nextSong();
        }

        @Override
        public void pause() throws RemoteException {
            mMediaPlayerUtil.pauseMedia();
        }

        @Override
        public void skipPrev() throws RemoteException {
            prevSong();
        }

        @Override
        public void resume() throws RemoteException {
            int index = mCurrentIndex;
            if(mCurrentIndex == -1) {
                index = 0;
            }

            if(mPlayStatus == PlayStatus.STOP ) {
                playFromList(index);
            } else {
                mMediaPlayerUtil.resumeMedia();
            }
        }

        @Override
        public void seek(int time) throws RemoteException {
            mMediaPlayerUtil.seek(time);
        }

        @Override
        public void playFromList(int index) throws RemoteException {
            playItem(index);
        }

        @Override
        public void addMediaPlayerEventsListener(IMediaPlayerEvents iMediaPlayerEvents) throws RemoteException {
            mIMediaPlayerEvents = iMediaPlayerEvents;
        }

        @Override
        public int getCurrentPlayIndex() throws RemoteException {
            return mCurrentIndex;
        }

        @Override
        public int getCurrentPlayStatus() throws RemoteException {
            return mPlayStatus;
        }
    };

    private void nextSong() {
        mCurrentIndex++;
        if (mCurrentIndex >= mPlayListManager.getSonglList().size()) {
            mCurrentIndex = 0;
        }
        playItem(mCurrentIndex);
    }

    private void prevSong() {
        mCurrentIndex--;
        if (mCurrentIndex < 0) {
            mCurrentIndex = mPlayListManager.getSonglList().size() - 1;
        }
        playItem(mCurrentIndex);
    }


    private void playItem(int index) {
        if (index < mPlayListManager.getSonglList().size()) {
            mCurrentIndex = index;
            mStorageUtil.storeAudioIndex(mCurrentIndex);
            VideoData videoData = mPlayListManager.getSonglList().get(index);
            mMediaPlayerUtil.initMediaPlayer(videoData.getBaseURL(), getApplicationContext());
        }
    }

    public AudioPlayerService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStorageUtil = new StorageUtil(getApplicationContext());
        mCurrentIndex = mStorageUtil.loadAudioIndex();
        mMediaPlayerUtil = MediaPlayerUtil.getInstance();
        mMediaPlayerUtil.addMediaPlayerEventListener(this);
        mPlayListManager = PlayListManager.getInstance(getApplicationContext());

        try {
            initMediaSession();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIPlaybackControls;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPlayStatusChanged(int status) {
        if(mIMediaPlayerEvents != null) {
            try {
                mPlayStatus = status;
                mIMediaPlayerEvents.onPlayStatusChanged(status);
                final String title = mPlayListManager.getSonglList().get(mCurrentIndex).getTitle();
                buildNotification(status, title);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSongCompleted() {
        if(mIMediaPlayerEvents != null) {
            try {
                mIMediaPlayerEvents.onSongCompleted();
                nextSong();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSongProgress(int current, int total) {
        if(mIMediaPlayerEvents != null) {
            try {
                mIMediaPlayerEvents.onSongProgress(current,total);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.v(Constants.TAG, "onAudioFocusChange "+focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mMediaPlayerUtil.resumeMedia();
                mMediaPlayerUtil.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                mMediaPlayerUtil.pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mMediaPlayerUtil.pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                mMediaPlayerUtil.setVolume(0.1f, 0.1f);
                break;
            default:
                break;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void buildNotification(int playStatus, String title) {


        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            Notification.Action actionPause = null;
            if(playStatus == PlayStatus.PAUSE) {
                actionPause = new Notification.Action.Builder(android.R.drawable.ic_media_play,
                        "Play", playbackAction(0)).build();
            } else {
                actionPause = new Notification.Action.Builder(android.R.drawable.ic_media_pause,
                        "Pause", playbackAction(1)).build();
            }

            Notification.Action actionSkipNext = new Notification.Action.Builder(android.R.drawable.ic_media_next,
                    "Next", playbackAction(2)).build();
            Notification.Action actionSkipPrev = new Notification.Action.Builder(android.R.drawable.ic_media_previous,
                    "Previous", playbackAction(3)).build();


            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // The id of the channel.
            String id = "my_channel_01";
            // The user-visible name of the channel.
            CharSequence name = getString(R.string.channel_name);
            // The user-visible description of the channel.
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(id, name,importance);
            // Configure the notification channel.
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mNotificationManager.createNotificationChannel(mChannel);




            Notification noti = new Notification.Builder(getApplicationContext(),"ID")
                    .setSmallIcon(R.drawable.music_player)
                    .setContentTitle(title)
                    // .setLargeIcon(albumArtBitmap))
                    .setStyle(new Notification.MediaStyle()
                            .setMediaSession(mToken)
                            .setShowActionsInCompactView(0))
                    .setChannelId(id)
                    .setActions(actionSkipPrev)
                    .setActions(actionPause)
                    .setActions(actionSkipPrev)
                    .build();
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(101, noti);
        } else {

            NotificationCompat.Action action = null;
            if(playStatus == PlayStatus.PAUSE) {
                action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_play,
                        "Play", playbackAction(0)).build();
            } else {
                action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause,
                        "Pause", playbackAction(1)).build();
            }
            NotificationCompat.Action skipNextAction  = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_next,
                    "Next", playbackAction(2)).build();
            NotificationCompat.Action skipPrevAction  = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_previous,
                    "Previous", playbackAction(3)).build();

            Notification noti = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.music_player)
                    .setContentTitle(title)
                    .addAction(skipPrevAction)
                    .addAction(action)
                    .addAction(skipNextAction)
                    .build();
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(101, noti);
        }
    }


    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, AudioPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void initMediaSession() throws RemoteException {
        if (mMediaSessionManager != null) return; //mMediaSessionManager exists

        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mMediaSession = new MediaSession(getApplicationContext(),"Youtube Audio player");

        mToken = mMediaSession.getSessionToken();



        //Get MediaSessions transport controls
        transportControls = mMediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mMediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mMediaSession's MetaData
        //updateMetaData();

        // Attach Callback to receive MediaSession updates
        mMediaSession.setCallback(new MediaSession.Callback() {
            // Implement callbacks
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPlay() {
                super.onPlay();
                mMediaPlayerUtil.resumeMedia();
                //buildNotification(PlayStatus.PLAY);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPause() {
                super.onPause();
                mMediaPlayerUtil.pauseMedia();
                //buildNotification(PlayStatus.PAUSE);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                nextSong();
                //updateMetaData();
               // buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
               prevSong();
               // updateMetaData();
               // buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                //  removeNotification();
                //Stop the service
                //stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private boolean requestAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }
}

package com.vvk.youtubeaudioplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements PlaylistAdapterEventListener {

    private IPlaybackControls mIPlaybackControls;
    private PlayListManager mPlayListManager;
    private ArrayList<VideoData> mSongsList;
    private ListView mListView;
    private static PlaylistAdapter mAdapter;

    private ImageView mNowPlayImage;
    private ImageButton mPlayButton;
    private int mPlayStatus;
    private int mCurrentPlayIndex = -1;
    private SeekBar mSeekBar;
    private TextView mTitleTextView;
    private TextView mTimeDisplay;
    private String mTitle;
    private String mCurrentUrl  = "https://m.youtube.com";
    private AdView mAdView;

    private IMediaPlayerEvents.Stub mIMediaPlayerEvents = new IMediaPlayerEvents.Stub() {
        @Override
        public void onPlayStatusChanged(int status) throws RemoteException {
            mPlayStatus = status;
            switch (status) {
                case PlayStatus.PAUSE:
                    mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                    break;

                case PlayStatus.PLAY:
                    mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                    break;

                case PlayStatus.BUFFERING:
                    String title = getString(R.string.strBuffer);
                    mTitleTextView.setText(title);
                    mTitle = title;
                    mTimeDisplay.setText("");
                    mSeekBar.setProgress(1);
                    mNowPlayImage.setImageResource(R.drawable.default_album);
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onSongCompleted() throws RemoteException {
            //skipNext();
        }

        @Override
        public void onSongProgress(final int current, final int total) throws RemoteException {
            int index = mIPlaybackControls.getCurrentPlayIndex();
            if(index != mCurrentPlayIndex) {
                changePlayIndex(index);
            }


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSeekBar.setMax(total);
                    mSeekBar.setProgress(current);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    String currentTime = simpleDateFormat.format(new Date(current));
                    String totalTime = simpleDateFormat.format(new Date(total));
                    mTimeDisplay.setText(currentTime + " / " + totalTime);
                    if (mCurrentPlayIndex > -1 && mCurrentPlayIndex < mSongsList.size()) {
                        final String title = mSongsList.get(mCurrentPlayIndex).getTitle();
                        if (!title.equals(mTitle)) {
                            mTitleTextView.setText(title);
                            mCurrentUrl = mSongsList.get(mCurrentPlayIndex).getBaseURL();
                            Bitmap albumImage = mSongsList.get(mCurrentPlayIndex).getAlbumImage();
                            mNowPlayImage.setImageBitmap(albumImage);
                        }
                        mTitle = title;
                    }
                }
            });
        }
    };

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mIPlaybackControls = IPlaybackControls.Stub.asInterface(service);
            try {
                mIPlaybackControls.addMediaPlayerEventsListener(mIMediaPlayerEvents);
                int index = mIPlaybackControls.getCurrentPlayIndex();
                changePlayIndex(index);
                mPlayStatus = mIPlaybackControls.getCurrentPlayStatus();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {


        }
    };

    private void changePlayIndex(final int index){
        mCurrentPlayIndex = index;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListView.setItemChecked(index, true);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mNowPlayImage = (ImageView) findViewById(R.id.nowPlayAlbum);
        mPlayButton = (ImageButton) findViewById(R.id.playButton);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setMax(100);
        mSeekBar.setProgress(1);
        mSeekBar.setPadding(0, 0, 0, 0);

        mTitleTextView = (TextView) findViewById(R.id.now_play_title);
        mTimeDisplay = (TextView) findViewById(R.id.timeDisplay);

        mListView = (ListView) findViewById(R.id.list);
        mPlayListManager = PlayListManager.getInstance(getApplicationContext());
        mSongsList = mPlayListManager.getSonglList();

        mAdapter = new PlaylistAdapter(mSongsList, getApplicationContext());
        mAdapter.addPlaylistAdapterEventListener(this);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.v("MP", "onItemClick" );
                if(position != mCurrentPlayIndex || mPlayStatus == PlayStatus.STOP) {
                    playItemFromList(position);
                    changePlayIndex(position);
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(mIPlaybackControls!=null) {
                    try {
                        mIPlaybackControls.seek(seekBar.getProgress());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        startPlayback();
    }

    private void startPlayback() {
        Intent playerIntent = new Intent(this, AudioPlayerService.class);
        startService(playerIntent);
        bindService(playerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }


    public void onYoutubeBtnClicked(View view) {
        pause();
        Intent intent = new Intent(this, YoutubeViewActivity.class);
        intent.putExtra("URL", mCurrentUrl);
        startActivity(intent);
    }

    public void onPlayBtnClicked(View view) {
        if(mCurrentPlayIndex == -1){
           // playItemFromList(0);
        } else if(mPlayStatus == PlayStatus.PAUSE || mPlayStatus == PlayStatus.STOP) {
            resume();
        } else if(mPlayStatus == PlayStatus.PLAY) {
           pause();
        }

    }

    public void onPrevBtnClicked(View view) {
        skipPrev();
    }

    public void onNextBtnClicked(View view) {
        skipNext();
    }


    private void pause() {
        if(mIPlaybackControls!=null) {
            try {
                mIPlaybackControls.pause();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void resume() {
        if(mIPlaybackControls!=null) {
            try {
                mIPlaybackControls.resume();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void skipPrev() {
        if(mIPlaybackControls!=null) {
            try {
                mIPlaybackControls.skipPrev();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void skipNext() {
        if(mIPlaybackControls!=null) {
            try {
                mIPlaybackControls.skipNext();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void playItemFromList(int index) {
        if(mIPlaybackControls!=null) {
            try {
                mIPlaybackControls.playFromList(index);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDeleteItemRequested(int index) {
        if(index == mCurrentPlayIndex) {
            pause();
        }
        mPlayListManager.removeSong(index);
        mAdapter.notifyDataSetChanged();
    }
}

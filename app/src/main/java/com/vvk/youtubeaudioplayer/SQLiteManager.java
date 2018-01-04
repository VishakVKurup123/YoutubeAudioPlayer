package com.vvk.youtubeaudioplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by developer on 12/3/17.
 */

public class SQLiteManager extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "playlist.db";

    private static SQLiteManager mSqliteManager;

    private Context mContext;

    private SQLiteDatabase mDatabase = null;
    /**
     * Table details
    */
    private final String TABLE_MUSIC_LIST = "tblMusicList";
    private final String COLUMN_SONG_ID = "iSongId";
    private final String COLUMN_BASE_URL = "sBaseUrl";
    private final String COLUMN_VIDEO_ID = "sVideoId";
    private final String COLUMN_TITLE = "sTitle";


    public static SQLiteManager getInstance(Context context) {
        if(null == mSqliteManager) {
            mSqliteManager = new SQLiteManager(context);
        }
        return mSqliteManager;
    }

    public SQLiteManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        mDatabase = this.getWritableDatabase();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v("MP-SQLITE","onCreate");
        db.execSQL("create table " + TABLE_MUSIC_LIST + " ( " + COLUMN_SONG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_BASE_URL + " VARCHAR, " +
                COLUMN_TITLE + " VARCHAR, " +
                COLUMN_VIDEO_ID + " VARCHAR);");
     }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertNewMusicData(VideoData videoData) {

        mDatabase = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_BASE_URL, videoData.getBaseURL());
        contentValues.put(COLUMN_VIDEO_ID, videoData.getVideoId());
        contentValues.put(COLUMN_TITLE, videoData.getTitle());
        mDatabase.insert(TABLE_MUSIC_LIST, null, contentValues);
        mDatabase.close();
    }

    public ArrayList<VideoData> getAllRecords() {
        ArrayList<VideoData> videoList = new ArrayList<VideoData>();
        try {
            SQLiteDatabase database = this.getReadableDatabase();

            if (null != database) {

                Cursor cursor = database.query(TABLE_MUSIC_LIST, null, null, null, null, null, null);

                VideoData videoData;
                if (cursor.getCount() > 0) {
                    for (int i = 0; i < cursor.getCount(); i++) {
                        cursor.moveToNext();
                        videoData = new VideoData("");
                        videoData.setBaseURL(cursor.getString(1));
                        videoData.setTitle(cursor.getString(2));
                        videoData.setVideoId(cursor.getString(3));
                        Bitmap albumImage = Util.readThumbNailImage(videoData.getVideoId(),mContext);
                        videoData.setAlbumImage(albumImage);
                        videoList.add(videoData);
                    }
                }
                cursor.close();
                database.close();
            }
        } catch(Exception e){
            Log.e("MP", "Exception ", e);
        }
        return videoList;
    }

    public void removeData(String videoId) {
        try {
            SQLiteDatabase database = mDatabase;
            database = this.getReadableDatabase();
            database.delete(TABLE_MUSIC_LIST, COLUMN_VIDEO_ID + " = ?", new String[]{videoId});
            database.close();
        } catch(Exception e){
            Log.e("MP", "Exception ", e);
        }
    }

}

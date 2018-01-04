package com.vvk.youtubeaudioplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Created by developer on 11/21/17.
 */

public class Util {
    public static Bitmap getBitmapfromUrl(String imageUrl)
    {
        try
        {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;

        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;

        }
    }


    public static void extractVideoInfo(final String youtubeLink, Context context, final ExtractionEventListener extractionEventListener) {
        new YouTubeExtractor(context) {

            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if (ytFiles == null) {
                    return;
                }
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);

                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        Log.v("VVK", vMeta.getTitle() + " : " + ytFile.getFormat().getExt());
                        if (ytFile.getFormat().getExt().equals("m4a")) {
                            //final VideoData audioConfiguration = new VideoData(youtubeLink);
                            final HashMap<String, String> result = new HashMap<>();
                            result.put("BASE_URL",youtubeLink);
                            result.put("AUDIO_URL", ytFile.getUrl());
                            result.put("TITLE", vMeta.getTitle());
                            result.put("THUMP_URL", vMeta.getThumbUrl());
                            result.put("VIDEO_ID", vMeta.getVideoId());
                            extractionEventListener.extractionCompleted(result);
                        }
                    }
                }
            }
        }.extract(youtubeLink, true, false);
    }

    public static void writeThumbNailImage(Bitmap bmp, String videoId, Context context) {
        try {
    //        File dir = new File(Environment.getExternalStorageDirectory()+"/youtubemedia/");
//            // if the directory does not exist, create it
//            if (!dir.exists()) {
//                System.out.println("creating directory: " + dir.getName());
//                boolean result = false;
//                try{
//                    result = dir.mkdirs();
//                    result = true;
//                }
//                catch(SecurityException se){
//                    //handle it
//                }
//                if(result) {
//                    System.out.println("DIR created");
//                }
//            }

            //File file = new File(videoId+".bmp");
            FileOutputStream fOut = context.openFileOutput(videoId+".bmp",
                    Context.MODE_PRIVATE);
            BitmapDataObject bitmapDataObject = new BitmapDataObject(bmp);
            ObjectOutputStream out = new ObjectOutputStream(fOut);
            out.writeObject(bitmapDataObject);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap readThumbNailImage(String videoId, Context context) {
        Bitmap bmp = null;
        try {
            FileInputStream fIn = context.openFileInput(videoId+".bmp");
            //File file = new File(videoId+".bmp");
            ObjectInputStream in = new ObjectInputStream(fIn);
            BitmapDataObject bitmapDataObject = (BitmapDataObject) in.readObject();
            bmp = bitmapDataObject.getCurrentImage();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  bmp;
    }

}

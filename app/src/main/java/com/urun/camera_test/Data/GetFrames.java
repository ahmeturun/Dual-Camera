package com.urun.camera_test.Data;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;

import java.util.ArrayList;

/**
 * Created by urun on 28.11.2016.
 */

public class GetFrames {
    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    public ArrayList<Bitmap> RetrieveFramesAsBitmapArray(String pathOfFile){
        /* Setting the file on the given path as the Data Source of MediaMetadataRetriever*/
        mediaMetadataRetriever.setDataSource(pathOfFile);
        /* Retrieving video length with MediaMetadataRetriever class extractMetadata function, and this function returns the time
        * as millisecods. So divide that value by 1000 and get the time as seconds.*/
        long videoLength = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))/1000;
        /* Setting first bitmap to the first frame before going into the loop. */
        Bitmap frame = mediaMetadataRetriever.getFrameAtTime(0);
        ArrayList<Bitmap> allFrames = new ArrayList<Bitmap>();
        int frameCounter = 0;/* This variable will keep increasing for saving bitmaps one-by-one to the Bitmap[] array*/
        for (int i = 0; i<=25*videoLength;i++) {//Multiply videoLength by 25 so we can get 25 frame from each second on the video.
            if(frame==null) break;
            /* Retrieving the next frame each time loop starts*/
            frame = mediaMetadataRetriever.getFrameAtTime(100000*i,MediaMetadataRetriever.OPTION_CLOSEST);
            allFrames.add(frame);
        }
        return allFrames;
    }

    public void CreateVideoFromFrames(ArrayList<Bitmap> fistFrameList, ArrayList<Bitmap> secondFrameList){
        /* Merging two Frame List from two video files in here. Then creating a video file with those merged Bitmaps.*/
    }

}

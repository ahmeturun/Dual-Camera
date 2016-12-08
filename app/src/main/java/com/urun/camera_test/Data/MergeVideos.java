package com.urun.camera_test.Data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.IOException;

import wseemann.media.FFmpegMediaMetadataRetriever;


/**
 * Created by urun on 28.11.2016.
 */

public class MergeVideos {
    public MergeVideos(Context context) {
        this.context = context;
    }
    Context context;
    //**IMPORTANT**//
    /*ANDROID BUG on ANDROID API 21 MARSHMALLOW  => link: 'https://code.google.com/p/android/issues/detail?id=193194'
     * THE MediaMetadataRetriever.OPTION_CLOSEST_SYNC VALUE DOESN'T WORK CORRECTLY WITH
     * 'getFrameAtTime()' FUNCTION. THIS VALUE SUPPOSE TO RETURN THE FRAME AT THE GIVEN TIME,
     * BUT ON API LEVEL 21, IT RETURNS ONLY THE KEYFRAME FOR THE GIVEN TIME WHICH RESULTS ONLY 4-5 DIFFERENT
     * FRAMES STORED IN ONE SECOND. */
    //**SOLUTION**//
    /* IN ORDER TO AVOID THIS BUG, WE'LL USE FFMPEG LIBRARY WHICH IS A NATIVE LIBRARY SO
     * HOPEFULLY WON'T GET THE SAME ERROR.
     * Building FFMpeg solution: http://stackoverflow.com/a/40125403
     * Library that used to communucate with FFMpeg: https://github.com/wseemann/FFmpegMediaMetadataRetriever(the aar file on the project has taken fromt there.)
     * */

    /* The conversion methods below taken from: http://stackoverflow.com/q/34672157*/
    // convert from Bitmap to Picture (jcodec native structure)
    public Picture fromBitmap(Bitmap src) {
        Picture dst = Picture.create(src.getWidth(), src.getHeight(), ColorSpace.RGB);
        fromBitmap(src, dst);
        return dst;
    }

    public void fromBitmap(Bitmap src, Picture dst) {
        int[] dstData = dst.getPlaneData(0);
        int[] packed = new int[src.getWidth() * src.getHeight()];

        src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

        for (int i = 0, srcOff = 0, dstOff = 0; i < src.getHeight(); i++) {
            for (int j = 0; j < src.getWidth(); j++, srcOff++, dstOff += 3) {
                int rgb = packed[srcOff];
                dstData[dstOff] = (rgb >> 16) & 0xff;
                dstData[dstOff + 1] = (rgb >> 8) & 0xff;
                dstData[dstOff + 2] = rgb & 0xff;
            }
        }
    }

    public void AllStepsAtOnce(String pathOfFileBack,String pathOfFileFront) throws IOException {
        FFmpegMediaMetadataRetriever fFmpegMediaMetadataRetrieverBack = new FFmpegMediaMetadataRetriever();
        FFmpegMediaMetadataRetriever fFmpegMediaMetadataRetrieverFront = new FFmpegMediaMetadataRetriever();
        /* Setting the data source of FFmpegMediaMetadataRetriever variable to the video file.*/
        /* Duration(videoLength) is returning as milliseconds so multiply by 1000 to get microsecond value.*/
        fFmpegMediaMetadataRetrieverBack.setDataSource(pathOfFileBack);
        fFmpegMediaMetadataRetrieverFront.setDataSource(pathOfFileFront);
        /* Setting the data source of FFmpegMediaMetadataRetriever variable to the video file.*/
        /* Duration(videoLength) is returning as milliseconds so multiply by 1000 to get microsecond value.*/
        long videoLengthBack = Long.parseLong(fFmpegMediaMetadataRetrieverBack.extractMetadata(fFmpegMediaMetadataRetrieverBack.METADATA_KEY_DURATION))*1000;
        long videoLengthFront = Long.parseLong(fFmpegMediaMetadataRetrieverFront.extractMetadata(fFmpegMediaMetadataRetrieverFront.METADATA_KEY_DURATION))*1000;
        /*The videos taken from front and back cameras can be 1 or 2 frame more/less than each other
        (because the stopping of records executed sequently and the video which stopped recording last, might have 1 or 2 frames extra.)
        So to merge videos without facing the problem of getting a null frame from the longer video, choose the video which
        has less frame than the other video as merged video's length.*/
        long videoLength = (videoLengthBack<videoLengthFront) ? videoLengthBack:videoLengthFront;
        /* Setting first bitmap to the first frame before going into the loop. */
        Bitmap frameBack = fFmpegMediaMetadataRetrieverFront.getFrameAtTime(0);
        Bitmap frameFront = fFmpegMediaMetadataRetrieverBack.getFrameAtTime(0);

        Bitmap bitmapResult;
        Canvas canvas;
        Paint paint;
        /* Merging two Frame List from two video files in here. Then creating a video file with those merged Bitmaps.(Using Jcodec library here.)*/
        SequenceEncoder sequenceEncoder = new SequenceEncoder(new File(Environment.getExternalStorageDirectory(),"/combinedvideo.mp4"));

        //create runnables in here. all runnables will return a bitmap and inside the runnable use the below for loop's content
        // except the part where sequenceEncoder.encodenativeframe() starts.
        //there is going to be a bigger loop over where we create runnables and this for loop will count until: framesize/runnableobjects.
        //int the bigger loop run sequenceencoder.encodeNativeFrame() as much as the bitmaps created by runnables(the number of runnable created).

        for (int i = 0; i <videoLength/5 ; i = i + 40000) {
            //Bitmap1 = runnable.run();
            //Bitmap2 = runnable.run();
            //Bitmap3 = runnable.run();
            //Bitmap4 = runnable.run();
            //Bitmap5 = runnable.run();
            //sequenceEncoder.encodeNativeFrame(Bitmap1);
            //sequenceEncoder.encodeNativeFrame(Bitmap2);
            //sequenceEncoder.encodeNativeFrame(Bitmap3);
            //sequenceEncoder.encodeNativeFrame(Bitmap4);
            //sequenceEncoder.encodeNativeFrame(Bitmap5);
        }

        for (int i = 0; i < videoLength; i= i + 40000) {
            frameBack = fFmpegMediaMetadataRetrieverBack.getFrameAtTime(i,FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            frameFront = fFmpegMediaMetadataRetrieverFront.getFrameAtTime(i,FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            if(frameBack == null || frameFront == null) break;

            bitmapResult = Bitmap.createBitmap(frameBack.getWidth() * 2, frameBack.getHeight(),Bitmap.Config.RGB_565);
            canvas = new Canvas(bitmapResult);
            paint = new Paint();
            canvas.drawBitmap(frameBack, 0,0,paint);
            canvas.drawBitmap(frameFront,frameBack.getWidth(),0,paint);

            /* Merging two Frame List from two video files in here. Then creating a video file with those merged Bitmaps.(Using Jcodec library here.)*/
            Bitmap currentMergedFrame = bitmapResult;
            Picture bitmapToPicture = fromBitmap(currentMergedFrame);


            sequenceEncoder.encodeNativeFrame(bitmapToPicture);
            frameBack.recycle();
            frameFront.recycle();
            frameBack=null;
            frameFront=null;
            System.gc();
        }
        sequenceEncoder.finish();
    }

}

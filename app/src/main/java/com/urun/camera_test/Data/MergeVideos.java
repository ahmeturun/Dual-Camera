package com.urun.camera_test.Data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;

import org.jcodec.api.JCodecException;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
     * Library that used to communucate with FFMpeg: https://github.com/wseemann/FFmpegMediaMetadataRetriever(the aar file on the project has taken fromt there.)*/
    public ArrayList<Bitmap> RetrieveFramesAsBitmapArray(String pathOfFile) throws IOException, JCodecException {/*including aar for ffmpeg solution: http://stackoverflow.com/a/39770874*/
        FFmpegMediaMetadataRetriever fFmpegMediaMetadataRetriever = new FFmpegMediaMetadataRetriever();
        fFmpegMediaMetadataRetriever.setDataSource(pathOfFile);/* Setting the data source of FFmpegMediaMetadataRetriever variable to the video file.*/
        /* Duration(videoLength) is returning as milliseconds so multiply by 1000 to get microsecond value.*/
        long videoLength = Long.parseLong(fFmpegMediaMetadataRetriever.extractMetadata(fFmpegMediaMetadataRetriever.METADATA_KEY_DURATION))*1000;
        /* Setting first bitmap to the first frame before going into the loop. */
        Bitmap frame = fFmpegMediaMetadataRetriever.getFrameAtTime(0);
        ArrayList<Bitmap> FfmpegFrames = new ArrayList<Bitmap>();
        for (int i = 0; i < videoLength; i= i + 40000) {/* Retrieving the next frame each time loop starts.
            * We want to retrieve 25 frames from each second,
            * ergo the interval between frames should be 40000 microsecond.
            * The Math : 0, 40000, 80000,...,1000000 => 40000*25 = 1000000 Us = 1 sec*/
            if(frame==null) break;
            frame = fFmpegMediaMetadataRetriever.getFrameAtTime(i,FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
            FfmpegFrames.add(frame);
        }
        return FfmpegFrames;
    }
    public ArrayList<Bitmap> MergeFrames(ArrayList<Bitmap> firstFrameList, ArrayList<Bitmap> secondFrameList){
        ArrayList<Bitmap> mergedFrames = new ArrayList<Bitmap>();
        Bitmap bitmapFirstFrame, bitmapSecondFrame;
        Bitmap bitmapResult;
        Canvas canvas;
        Paint paint;
        /*The videos taken from front and back cameras can be 1 or 2 frame more/less than each other
        (because the stopping of records executed sequently and the video which stopped recording last, might have 1 or 2 frames extra.)
        So to merge videos without facing the problem of getting a null frame from the lesser video, choose the video which
        has less frame than the other video as merged video's length.*/
        int smallerFrameRate = firstFrameList.size()<secondFrameList.size() ? firstFrameList.size() : secondFrameList.size();
        smallerFrameRate--;
        for (int i = 0; i < smallerFrameRate; i++) {
            bitmapFirstFrame = firstFrameList.get(i);
            bitmapSecondFrame = secondFrameList.get(i);
            bitmapResult = Bitmap.createBitmap(bitmapFirstFrame.getWidth(), bitmapFirstFrame.getHeight() * 2,Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmapResult);
            paint = new Paint();
            canvas.drawBitmap(bitmapFirstFrame, 0,0,paint);
            canvas.drawBitmap(bitmapSecondFrame,0,bitmapFirstFrame.getHeight(),paint);
            mergedFrames.add(bitmapResult);
        }
        return mergedFrames;
    }

    public void CreateVideoFromFrames(ArrayList<Bitmap> mergedFrames) throws IOException {
        /* Merging two Frame List from two video files in here. Then creating a video file with those merged Bitmaps.(Using Jcodec library here.)*/
        SequenceEncoder sequenceEncoder = new SequenceEncoder(new File(Environment.getExternalStorageDirectory(),"/combinedvideo.mp4"));
        Bitmap currentMergedFrame;
        Picture bitmapToPicture;
        for (int i = 0; i < mergedFrames.size(); i++) {
            currentMergedFrame = mergedFrames.get(i);
            bitmapToPicture = fromBitmap(currentMergedFrame);
            sequenceEncoder.encodeNativeFrame(bitmapToPicture);
        }
        sequenceEncoder.finish();
    }
    /* The converesion methods below taken from: http://stackoverflow.com/q/34672157*/
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

}

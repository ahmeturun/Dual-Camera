package com.urun.camera_test.Data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import com.urun.camera_test.CameraAccess.CameraPreview;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by urun on 28.11.2016.
 */

public class MergeVideos {
    public MergeVideos(Context context) throws IOException {
        this.context = context;
    }
    private Context context;
    private long pictureNumber = 0;/*pictureNumber is keeping the frame number and we're using this value while we're saving the frame to the device.*/
    private SequenceEncoder sequenceEncoder = new SequenceEncoder(new File(Environment.getExternalStorageDirectory(),"/combinedvideo.mp4"));
    private final Object key = new Object();
    private final Object key2 = new Object();
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

    public void getFrameFromPreview(CameraPreview cameraPreview, final String savingName) {
        synchronized(key) {
            cameraPreview.camera.setPreviewCallback(new Camera.PreviewCallback() {
                long timeMillisForReference = System.nanoTime() / 100;
                /*timeDifferenceCoefficient will increase the difference factor between first frame time value and
                *current frame time value(starting with 40000) as multiply of 40000 e.g. for first frame and second frame time difference= 40000*1,
                *for first frame and third frame time difference = 40000*2 ...*/
                int timeDifferenceCoefficient = 1;

                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                        long timeMillisForCapture = System.nanoTime() / 100;
                        if (timeMillisForReference + 40000 * timeDifferenceCoefficient <= timeMillisForCapture) {
                            int[] argb8888 = new int[320 * 240];/*the reason for setting this arrays size to 320*240 is that we have to set the array according to preview width and height.*/
                            decodeYUV(argb8888, data, 320, 240);
                            Bitmap bitmap = Bitmap.createBitmap(argb8888, 320, 240, Bitmap.Config.ARGB_8888);
                            File currFrame = new File(Environment.getExternalStorageDirectory() + "/" + Long.toString(pictureNumber) + savingName + ".jpg");
                            FileOutputStream fileOutputStream = null;
                            try {
                                fileOutputStream = new FileOutputStream(currFrame);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                                fileOutputStream.flush();
                                fileOutputStream.close();
                                timeMillisForReference = timeMillisForCapture;
                                timeDifferenceCoefficient++;/*Increasing the coefficient to set the time difference between  next frame and first frame correctly.*/
                            } catch (IOException e) {
                                Log.e("from_onPreview: ",e.getMessage());
                            }
                        }

                }
            });
        }
    }

    public void MergeFrames() throws IOException {
        synchronized(key) {
            File root = Environment.getExternalStorageDirectory();
            Bitmap bitmapBack = BitmapFactory.decodeFile(root + "/" + pictureNumber + "back.jpg");
            Bitmap bitmapFront = BitmapFactory.decodeFile(root + "/" + pictureNumber + "front.jpg");
            Bitmap bitmapResult = Bitmap.createBitmap(bitmapBack.getWidth(), bitmapBack.getHeight() * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapResult);
            Paint paint = new Paint();
            canvas.drawBitmap(bitmapBack, 0, 0, paint);
            canvas.drawBitmap(bitmapFront, 0, bitmapBack.getHeight(), paint);
            FileOutputStream outputStream = new FileOutputStream(String.format(root + "/%dcombined.jpg", pictureNumber));
            bitmapResult.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            Log.e("picture_saved", "Picture has been saved succesfully: " + System.currentTimeMillis());
            pictureNumber++;
        }
    }

    public void EncodeTheFrame(){
        synchronized(key) {
            File root = Environment.getExternalStorageDirectory();
            Bitmap combinedFrame = BitmapFactory.decodeFile(root + "/" + pictureNumber + "combined.jpg");
            Picture combinedPicture = fromBitmap(combinedFrame);
            try {
                sequenceEncoder.encodeNativeFrame(combinedPicture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void finishDecoding() throws IOException {
        synchronized(key) {
                sequenceEncoder.finish();
        }
    }


    /*Below function has taken from: http://stackoverflow.com/questions/9325861/converting-yuv-rgbimage-processing-yuv-during-onpreviewframe-in-android
    * It's been used for converting the format of picture data type returned from onPreviewFrame.*/
    // decode Y, U, and V values on the YUV 420 buffer described as YCbCr_422_SP by Android
    // David Manpearl 081201
    public void decodeYUV(int[] out, byte[] fg, int width, int height)
            throws NullPointerException, IllegalArgumentException {
        int sz = width * height;
        if (out == null)
            throw new NullPointerException("buffer out is null");
        if (out.length < sz)
            throw new IllegalArgumentException("buffer out size " + out.length
                    + " < minimum " + sz);
        if (fg == null)
            throw new NullPointerException("buffer 'fg' is null");
        if (fg.length < sz)
            throw new IllegalArgumentException("buffer fg size " + fg.length
                    + " < minimum " + sz * 3 / 2);
        int i, j;
        int Y, Cr = 0, Cb = 0;
        for (j = 0; j < height; j++) {
            int pixPtr = j * width;
            final int jDiv2 = j >> 1;
            for (i = 0; i < width; i++) {
                Y = fg[pixPtr];
                if (Y < 0)
                    Y += 255;
                if ((i & 0x1) != 1) {
                    final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                    Cb = fg[cOff];
                    if (Cb < 0)
                        Cb += 127;
                    else
                        Cb -= 128;
                    Cr = fg[cOff + 1];
                    if (Cr < 0)
                        Cr += 127;
                    else
                        Cr -= 128;
                }
                int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                if (R < 0)
                    R = 0;
                else if (R > 255)
                    R = 255;
                int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1)
                        + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                if (G < 0)
                    G = 0;
                else if (G > 255)
                    G = 255;
                int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                if (B < 0)
                    B = 0;
                else if (B > 255)
                    B = 255;
                out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
            }
        }

    }

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


}

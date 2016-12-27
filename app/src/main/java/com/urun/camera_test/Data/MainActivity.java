package com.urun.camera_test.Data;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.H264TrackImpl;
import com.urun.camera_test.CameraAccess.CameraPreview;
import com.urun.camera_test.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class MainActivity extends Activity{

    private Camera camera_front, camera_back;
    SurfaceView surfaceView_back,surfaceView_front;
    SurfaceHolder surfaceHolder_back,surfaceHolder_front;
    CameraPreview cameraPreview_back,cameraPreview_front;
    Button capture_video;
    int pictureNumber=0;
    boolean captureFlag;
    boolean finishedEncodingFlag;
    AvcEncoder mAvEncoder = new AvcEncoder();
    Queue backFrames = new Queue();
    Queue frontFrames = new Queue();
    final Object key = new Object();
    int combinedFrameNumber=0;

    public MainActivity() throws IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*textview for recording message on the UI*/
        final TextView recordBack = (TextView)findViewById(R.id.recording_text_back);
        final TextView recordFront = (TextView)findViewById(R.id.recording_text_front);

        /*setting captureFlag to false at the beginning of the activity.(as not recording)*/
        captureFlag = true;

        /* Initializing camera preview from FRONT*/
        surfaceView_front = (SurfaceView) findViewById(R.id.camera_preview_front);
        surfaceHolder_front = surfaceView_front.getHolder();
        cameraPreview_front = new CameraPreview(getApplicationContext(),camera_front,surfaceHolder_front,0);
        surfaceHolder_front.addCallback(cameraPreview_front);
        surfaceHolder_front.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        /* Initializing camera preview from BACK*/
        surfaceView_back = (SurfaceView) findViewById(R.id.camera_preview_back);
        surfaceHolder_back = surfaceView_back.getHolder();
        cameraPreview_back = new CameraPreview(getApplicationContext(),camera_back,surfaceHolder_back,1);
        surfaceHolder_back.addCallback(cameraPreview_back);
        surfaceHolder_back.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);



        /* Setting onClick function for capture_video button*/
        capture_video = (Button)findViewById(R.id.capture_video);
        capture_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFlag = !captureFlag;
                if (!captureFlag) {
                    // Setting UI recording messages to visible
                    recordBack.setVisibility(View.VISIBLE);
                    recordFront.setVisibility(View.VISIBLE);

                    try {
                        /*Taking frames from front camera while the preview is available*/
                        getFrameFromPreview(cameraPreview_back, "front");
                        /*Taking frames from front camera while the preview is available*/
                        getFrameFromPreview(cameraPreview_front, "back");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    recordBack.setVisibility(View.INVISIBLE);
                    recordFront.setVisibility(View.INVISIBLE);
                    /* Recording session has ended, initializing mediaRecorders for later usage*/
                    cameraPreview_back.camera.stopPreview();
                    cameraPreview_front.camera.stopPreview();
                    try {
                        finishedEncodingFlag = true;
                        mAvEncoder.close();
                        H264TrackImpl h264Track = new H264TrackImpl( new BufferedInputStream( new FileInputStream( Environment.getExternalStorageDirectory()+ "/video_encoded.264") ));
                        Movie m = new Movie();
                        m.addTrack(h264Track);

                        IsoFile out = new DefaultMp4Builder().build(m);
                        FileOutputStream fos = new FileOutputStream(new File("h264_output.mp4"));
                        out.getBox(fos.getChannel());
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }

    private void getFrameFromPreview(CameraPreview cameraPreview, final String savingName) throws IOException {
        cameraPreview.camera.setPreviewCallback(new Camera.PreviewCallback() {
            Bitmap bitmapBack,bitmapFront;
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                int[] argb8888 = new int[640 * 480];/*the reason for setting this arrays size to 320*240 is that we have to set the array according to preview width and height.*/
                decodeYUV(argb8888, data, 640, 480);
                if (savingName == "back") {
                    backFrames.add(Bitmap.createBitmap(argb8888, 640, 480, Bitmap.Config.ARGB_8888));
                } else {
                    frontFrames.add(Bitmap.createBitmap(argb8888, 640, 480, Bitmap.Config.ARGB_8888));
                }
                Log.e("created picture: ", "" + pictureNumber);
                pictureNumber++;
                if (!frontFrames.isEmpty() && !backFrames.isEmpty() && !finishedEncodingFlag) {
                    synchronized (key) {
                        bitmapBack = (Bitmap) frontFrames.poll();
                        bitmapFront = (Bitmap) backFrames.poll();
                        if (bitmapBack != null && bitmapFront != null) {
                            Bitmap bitmapResult = Bitmap.createBitmap(640, 960, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmapResult);
                            Paint paint = new Paint();
                            canvas.drawBitmap(bitmapBack, 0, 0, paint);
                            canvas.drawBitmap(bitmapFront, 0, bitmapBack.getHeight(), paint);
                            byte[] byteArray = getYV12(bitmapResult.getWidth(), bitmapResult.getHeight(), bitmapResult);//the byte array version of merged pictures

                            mAvEncoder.offerEncoder(byteArray);
                            combinedFrameNumber++;
                            Log.e("EncodeSuccesful_Frame: ", "" + combinedFrameNumber);
                        } else {
                            Log.e("pictures_check", "Pictures not ready.");
                        }
                    }
                }

            }
        });
    }

    //Below two functions also used in taking yuv colors from bitmap but this time getting the color yv12 format
    // https://gist.github.com/wobbals/5725412

    private byte [] getYV12(int inputWidth, int inputHeight, Bitmap scaled) {

        int [] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte [] yuv = new byte[inputWidth*inputHeight*3/2];
        encodeYV12(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }
    //I managed to solve the color issue by changing the u and v chrominance values in the function
    //the format  that is sent from camera is different from yuv480sp so swapping this value worked.
    private void encodeYV12(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = (width * height);

        int yIndex = 0;
        int uIndex = frameSize  ;
        int vIndex = frameSize + (frameSize / 4);

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                V = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                U = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // YV12 has a plane of Y and two chroma plans (U, V) planes each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[vIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
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


    @Override
    protected void onPause() {
        super.onPause();
        if(camera_back != null){
            camera_back.stopPreview();
            camera_back.release();
            camera_back = null;
        }
        if(camera_front != null){
            camera_front.stopPreview();
            camera_front.release();
            camera_front = null;
        }
    }
}

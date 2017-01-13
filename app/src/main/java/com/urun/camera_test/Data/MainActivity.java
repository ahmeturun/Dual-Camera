package com.urun.camera_test.Data;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
    RenderScript rs;
    ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

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

        final RenderScript rs_back = RenderScript.create(this);
        final RenderScript rs_front = RenderScript.create(this);

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
                        getFrameFromPreview(cameraPreview_back, "front",rs_back);
                        /*Taking frames from front camera while the preview is available*/
                        getFrameFromPreview(cameraPreview_front, "back",rs_front);

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

    private void getFrameFromPreview(CameraPreview cameraPreview, final String savingName, final RenderScript rs) throws IOException {
        cameraPreview.camera.setPreviewCallback(new Camera.PreviewCallback() {
            byte[] bitmapBack,bitmapFront;
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (savingName == "back") {
                    backFrames.add(data);
                } else {
                    frontFrames.add(data);
                }
                Log.e("created picture: ", "" + pictureNumber);
                pictureNumber++;
                if (!frontFrames.isEmpty() && !backFrames.isEmpty() && !finishedEncodingFlag) {
                    synchronized (key) {
                        bitmapBack = (byte[]) frontFrames.poll();
                        bitmapFront = (byte[]) backFrames.poll();
                        if (bitmapBack != null && bitmapFront != null) {
//                            Bitmap bitmapResult = Bitmap.createBitmap(640, 960, Bitmap.Config.ARGB_8888);
//                            Canvas canvas = new Canvas(bitmapResult);
//                            Paint paint = new Paint();
//                            canvas.drawBitmap(bitmapBack, 0, 0, paint);
//                            canvas.drawBitmap(bitmapFront, 0, bitmapBack.getHeight(), paint);

                            mAvEncoder.offerEncoder(mergeYUV420PData(bitmapBack,bitmapFront));
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
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V  = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // YV12 has a plane of Y and two chroma plans (U, V) planes each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) Y;
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uIndex++] = (byte)U;
                    yuv420sp[vIndex++] = (byte)V;
                }

                index ++;
            }
        }
    }

    /*Below function has been used for converting the format of picture data type returned from onPreviewFrame.*/

    public void convertYuvToRGB(int W, int H,byte[]yuvByteArray,byte[]outBytes, RenderScript rs){

        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(rs));



        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
                .setX(W).setY(H)
                .setYuvFormat(android.graphics.ImageFormat.NV21);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);


        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(W).setY(H);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(yuvByteArray);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        out.copyTo(outBytes);

//        Bitmap bmpout = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
//        out.copyTo(bmpout);
//        return bmpout;
    }

    public byte[] mergeYUV420PData(byte[] first, byte[] second){
        //combined Y(luma) component => (Y1+Y2)
        byte[] YCombined = new byte[614400];
        System.arraycopy(first,0,YCombined,0,307200);
        System.arraycopy(second,0,YCombined,307200,307200);
        //combined U(Cb) component => (U1+U2)
        byte[] UCombined = new byte[153600];
        System.arraycopy(first,307200,UCombined,0,76800);
        System.arraycopy(second,307200,UCombined,76800,76800);
        //combined Y(Cr) component => (V1+V2)
        byte[] VCombined = new byte[153600];
        System.arraycopy(first,384000,VCombined,0,76800);
        System.arraycopy(second,384000,VCombined,76800,76800);

        //combining seperate combined components together=> (Y1+Y2)+(U1+U2)+(V1+V2)
        byte[] YUVCombined = new byte[921600];
        System.arraycopy(YCombined,0, YUVCombined,0,614400);
        System.arraycopy(UCombined,0, YUVCombined,614400,153600);
        System.arraycopy(VCombined,0, YUVCombined,768000,153600);

        return YUVCombined;

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

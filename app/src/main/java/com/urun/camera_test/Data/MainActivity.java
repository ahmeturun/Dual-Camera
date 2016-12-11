package com.urun.camera_test.Data;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.urun.camera_test.CameraAccess.CameraPreview;
import com.urun.camera_test.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends Activity{

    private Camera camera_front, camera_back;
    SurfaceView surfaceView_back,surfaceView_front;
    SurfaceHolder surfaceHolder_back,surfaceHolder_front;
    CameraPreview cameraPreview_back,cameraPreview_front;
    Button capture_back, capture_front;
    Button capture_video;
    int pictureNumber=0;
    boolean captureFlag;

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


        /* Setting onClick function for "capture" button*/
        capture_front = (Button) findViewById(R.id.button_capture_back);
        capture_front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Taking picture from front camera(name references are switched so front takes back camera parameters. Don't get confused.)
                takePic(cameraPreview_front);
                // Taking picture from back camera
                takePic(cameraPreview_back);
            }
        });


        /* Setting onClick function for capture_video button*/
        capture_video = (Button)findViewById(R.id.capture_video);
        capture_video.setOnClickListener(v -> {
            try {
                final MergeVideos mergingInstance = new MergeVideos(getApplicationContext());
                captureFlag = !captureFlag;
                if (!captureFlag) {
                    // Setting UI recording messages to visible
                    recordBack.setVisibility(View.VISIBLE);
                    recordFront.setVisibility(View.VISIBLE);
                /*Setting the Threads.*/
                    Runnable RFrameBack = () -> mergingInstance.getFrameFromPreview(cameraPreview_front, "back");
                    Thread threadFrontFrame = new Thread(RFrameBack);
                    threadFrontFrame.start();
                    threadFrontFrame.join();
                    Runnable RFrameFront = () -> mergingInstance.getFrameFromPreview(cameraPreview_back, "front");
                    Thread threadBackFrame = new Thread(RFrameFront);
                    threadBackFrame.start();
                    threadBackFrame.join();
                    Runnable RMergeFrame = () -> {
                        try {
                            mergingInstance.MergeFrames();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                    Thread threadMerge = new Thread(RMergeFrame);
                    threadMerge.start();
                    threadMerge.join();
                    Runnable REncodeFrame = () -> mergingInstance.EncodeTheFrame();
                    Thread threadEncodeFrame = new Thread(REncodeFrame);
                    threadEncodeFrame.start();
                    threadEncodeFrame.join();
                } else {
                    recordBack.setVisibility(View.INVISIBLE);
                    recordFront.setVisibility(View.INVISIBLE);
                    Runnable RFinishEncode = () -> {
                        try {
                            mergingInstance.finishDecoding();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };
                    Thread threadEndEncoding = new Thread(RFinishEncode);
                    threadEndEncoding.start();
                }
            }catch (IOException e){
                Log.v("button_click: ",e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        });
    }


    private void takePic(CameraPreview cameraPreview_front) {
        if(cameraPreview_front!=null) {
            cameraPreview_front.camera.takePicture(null,null,new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    FileOutputStream outStream = null;
                    ++pictureNumber;
                    try {
                        outStream = new FileOutputStream(String.format("/sdcard/%dfront.jpg", pictureNumber));
                        outStream.write(data);
                        outStream.close();
                        Log.e("picture_saved", "Picture has been saved succesfully: " + pictureNumber);
                        camera.release();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e("file_not_found: ","couldn't save the file "+e.getMessage());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("IOexception: ","couldn't save the file "+e.getMessage());
                    } finally {
                    }
                    Log.e("Log", "onPictureTaken - jpeg");
                }
            });
            Toast.makeText(MainActivity.this, "Picture Has Been Taken", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(MainActivity.this, "Failed!!!", Toast.LENGTH_SHORT).show();
        }
    }

    public void mergePicture() throws IOException {
        File root = Environment.getExternalStorageDirectory();
        Bitmap bitmapBack = BitmapFactory.decodeFile(root+"/"+pictureNumber+"back.jpg");
        Bitmap bitmapFront = BitmapFactory.decodeFile(root+"/"+pictureNumber+"front.jpg");
        Bitmap bitmapResult = Bitmap.createBitmap(bitmapBack.getWidth(), bitmapBack.getHeight() * 2,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapResult);
        Paint paint = new Paint();
        canvas.drawBitmap(bitmapBack, 0,0,paint);
        canvas.drawBitmap(bitmapFront,0,bitmapBack.getHeight(),paint);
        FileOutputStream outputStream = new FileOutputStream(String.format(root+"/%dcombined.jpg",pictureNumber));
        bitmapResult.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        outputStream.close();
        Log.e("picture_saved", "Picture has been saved succesfully: " + System.currentTimeMillis());
        Toast.makeText(this, "jpeg taken as bitmap.", Toast.LENGTH_SHORT).show();
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

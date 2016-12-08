package com.urun.camera_test.Data;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
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

import com.urun.camera_test.CameraAccess.CameraPreview;
import com.urun.camera_test.R;

import org.jcodec.api.JCodecException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Exchanger;


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
        //getting the number of camera exists on the device
        int numberOfCameras= Camera.getNumberOfCameras();
        Toast.makeText(this, "Number Of Cameras: "+numberOfCameras, Toast.LENGTH_SHORT).show();
        //textview for recording message on the UI
        final TextView recordBack = (TextView)findViewById(R.id.recording_text_back);
        final TextView recordFront = (TextView)findViewById(R.id.recording_text_front);

        //setting captureFlag to false at the beginning of the activity
        captureFlag = true;

        // Camera preview from FRONT
        surfaceView_front = (SurfaceView) findViewById(R.id.camera_preview_front);
        surfaceHolder_front = surfaceView_front.getHolder();
        cameraPreview_front = new CameraPreview(getApplicationContext(),camera_front,surfaceHolder_front,0);
        surfaceHolder_front.addCallback(cameraPreview_front);
        surfaceHolder_front.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // Camera preview from BACK
        surfaceView_back = (SurfaceView) findViewById(R.id.camera_preview_back);
        surfaceHolder_back = surfaceView_back.getHolder();
        cameraPreview_back = new CameraPreview(getApplicationContext(),camera_back,surfaceHolder_back,1);
        Toast.makeText(MainActivity.this, "CameraPreview Object has been created", Toast.LENGTH_SHORT).show();
        surfaceHolder_back.addCallback(cameraPreview_back);
        surfaceHolder_back.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Setting onClick function for "capture" button
        capture_front = (Button) findViewById(R.id.button_capture_back);
        capture_front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Taking picture from front camera(name references are switched so front takes back camera parameters. Don't get confused.)
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
                                surfaceHolder_back.addCallback(cameraPreview_back);
                                surfaceHolder_back.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
                // Taking picture from back camera
                if(cameraPreview_back!=null) {
                    cameraPreview_back.camera.takePicture(null,null,new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            FileOutputStream outStream = null;
                            try {
                                outStream = new FileOutputStream(String.format("/sdcard/%dback.jpg", pictureNumber));
                                outStream.write(data);
                                outStream.close();
                                Log.e("picture_saved", "Picture has been saved succesfully: " + System.currentTimeMillis());
                                camera.release();
                                mergePicture();
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
        });
        // Setting onClick function for capture_video button
        capture_video = (Button)findViewById(R.id.capture_video);
        capture_video.setOnClickListener(new View.OnClickListener() {
            MediaRecorder mediaRecorder_front = new MediaRecorder();
            MediaRecorder mediaRecorder_back = new MediaRecorder();
            @Override
            public void onClick(View v) {

                cameraPreview_back.camera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        long pictureNumber = 0;
                        int [] argb8888 = new int[320 * 240];
                        decodeYUV(argb8888, data, 320, 240);
                        Bitmap bitmap = Bitmap.createBitmap(argb8888, 320, 240, Bitmap.Config.ARGB_8888);
                        pictureNumber++;
                    }
                });

                captureFlag = !captureFlag;
                // Starting record for back camera
                startRecording(cameraPreview_front,mediaRecorder_front,"back",0);
                // Starting record for front camera
                startRecording(cameraPreview_back,mediaRecorder_back,"front",1);
                if(!captureFlag) {
                    // Setting UI recording messages to visible
                    recordBack.setVisibility(View.VISIBLE);
                    recordFront.setVisibility(View.VISIBLE);
                }else{
                    recordBack.setVisibility(View.INVISIBLE);
                    recordFront.setVisibility(View.INVISIBLE);
                    // Recording session has ended, initializing mediaRecorders for later usage
                    mediaRecorder_front = new MediaRecorder();
                    mediaRecorder_back = new MediaRecorder();
                    Toast.makeText(MainActivity.this, "Media Recorded!", Toast.LENGTH_SHORT).show();
                    MergeVideos workOnMerge = new MergeVideos(getApplicationContext());
//                    try {
//                        workOnMerge.AllStepsAtOnce(Environment.getExternalStorageDirectory()+"/back.mp4",Environment.getExternalStorageDirectory()+"/front.mp4");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }

            }
        });
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

    public void startRecording(CameraPreview cameraPreview,MediaRecorder mediaRecorder, String videoName,int cameraId){
        if(!captureFlag) {
            try {
                cameraPreview.camera.unlock();
                try {
                    mediaRecorder.setCamera(cameraPreview.camera);
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                    mediaRecorder.setOrientationHint(90);
//                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    CamcorderProfile camcorderProfile = CamcorderProfile.get(cameraId,CamcorderProfile.QUALITY_480P);
//                    camcorderProfile.videoFrameHeight = 1920;
//                    camcorderProfile.videoFrameWidth = 1080;
                    mediaRecorder.setProfile(camcorderProfile);
                    mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/"+videoName+".mp4");
//                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
//                    mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
                    mediaRecorder.setMaxFileSize(50000000);
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IOException e) {
                        Log.e("media_recorder_start:",e.getMessage());
                    }
                } catch (Exception e){
                    Log.e("media_recorder_prblm: ",e.getMessage());
                }
            } catch (Exception ex) {
                Log.e("unlock_fail: ", ex.getMessage());
            }
        }else{
            if(mediaRecorder!=null) {
                mediaRecorder.reset(); // clear recorder configuration
                mediaRecorder.release(); // release the recorder object
                cameraPreview.camera.lock(); // lock camera for later use
                Toast.makeText(this, "stopped recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

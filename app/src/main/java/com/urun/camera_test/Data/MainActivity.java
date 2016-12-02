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

import org.jcodec.api.JCodecException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


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
                    // Recording session has ended, initialing mediaRecorders for later usage
                    mediaRecorder_front = new MediaRecorder();
                    mediaRecorder_back = new MediaRecorder();
                    Toast.makeText(MainActivity.this, "Media Recorded!", Toast.LENGTH_SHORT).show();

                    MergeVideos workOnMerge = new MergeVideos(getApplicationContext());
                    /* Getting frames from video taken from back camera. */
                    ArrayList<Bitmap> backFrames = null;
                    try {
                        backFrames = workOnMerge.RetrieveFramesAsBitmapArray(Environment.getExternalStorageDirectory()+"/back.mp4");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JCodecException e) {
                        e.printStackTrace();
                    }
                    /* Getting frames from video taken from front camera. */
                    ArrayList<Bitmap> frontFrames = null;
                    try {
                        frontFrames = workOnMerge.RetrieveFramesAsBitmapArray(Environment.getExternalStorageDirectory()+"/front.mp4");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JCodecException e) {
                        e.printStackTrace();
                    }
                    /* Merging all the frames that are belong to the same moment as up-down frame. */
                    ArrayList<Bitmap> mergedFrames = workOnMerge.MergeFrames(backFrames,frontFrames);

                    try {
                        workOnMerge.CreateVideoFromFrames(mergedFrames);
                        Log.e("merged_video: ","videos merged succesfully.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
                    CamcorderProfile camcorderProfile = CamcorderProfile.get(cameraId,CamcorderProfile.QUALITY_LOW);
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

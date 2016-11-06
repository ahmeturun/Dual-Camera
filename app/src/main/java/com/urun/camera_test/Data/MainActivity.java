package com.urun.camera_test.Data;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.Toast;

import com.urun.camera_test.CameraAccess.CameraPreview;
import com.urun.camera_test.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity{

    private Camera camera_front, camera_back;
    SurfaceView surfaceView_back,surfaceView_front;
    SurfaceHolder surfaceHolder_back,surfaceHolder_front;
    CameraPreview cameraPreview_back,cameraPreview_front;
    Button capture_back, capture_front;
    Camera.PictureCallback jpegcallback;
    InputStream inputStream1;
    int pictureNumber=0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int numberOfCameras= Camera.getNumberOfCameras();
        final DatabaseHelper databaseHelper = new DatabaseHelper(this);
        Toast.makeText(this, "Number Of Cameras: "+numberOfCameras, Toast.LENGTH_SHORT).show();

        // Camera preview from FRONT
        surfaceView_front = (SurfaceView) findViewById(R.id.camera_preview_front);
        surfaceHolder_front = surfaceView_front.getHolder();
        cameraPreview_front = new CameraPreview(getApplicationContext(),camera_front,surfaceHolder_front,0);
        surfaceHolder_front.addCallback(cameraPreview_front);
        surfaceHolder_front.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        surfaceView_back = (SurfaceView) findViewById(R.id.camera_preview_back);
        surfaceHolder_back = surfaceView_back.getHolder();
        cameraPreview_back = new CameraPreview(getApplicationContext(),camera_back,surfaceHolder_back,1);
        Toast.makeText(MainActivity.this, "CameraPreview Object has been created", Toast.LENGTH_SHORT).show();
        surfaceHolder_back.addCallback(cameraPreview_back);
        surfaceHolder_back.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        capture_front = (Button) findViewById(R.id.button_capture_back);
        capture_front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
//                                databaseHelper.insertPictureData(0,data);
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
                    Toast.makeText(MainActivity.this, "Failed!!!!!", Toast.LENGTH_SHORT).show();
                }
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
                    Toast.makeText(MainActivity.this, "Failed!!!!!", Toast.LENGTH_SHORT).show();
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

package com.urun.camera_test.CameraAccess;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ahmet on 10/5/2016.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    int camera_id;
    public Context context;
    private static String TAG = "camera_error";

    public CameraPreview(Context context, Camera camera, SurfaceHolder surfaceHolder,int camera_id) {
        super(context);
        this.context = context;
        this.camera = camera;
        this.surfaceHolder = surfaceHolder;
        this.camera_id = camera_id;

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            camera = Camera.open(camera_id);
        } catch (RuntimeException e) {
            // check for exceptions
            Log.v("camera_open: ",e.getMessage());
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();

        // modify parameter
        param.setPreviewSize(349, 349);
        camera.setParameters(param);
        camera.setDisplayOrientation(90);
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    public static Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d("Log", "onPictureTaken - raw");
        }
    };

    /** Handles data for jpeg picture */
    public static Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.i("Log", "onShutter'd");
        }
    };
    public static Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(String.format("/sdcard/Pictures/%d.jpg", System.currentTimeMillis()));
                outStream.write(data);
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.v("filenotfound: ",e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.v("Ioexception: ",e.getMessage());
            } finally {
                Log.v("finally: ","doesn't know");
            }
            Log.d("Log", "onPictureTaken - jpeg");
        }
    };

    public void takePic(){
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

}

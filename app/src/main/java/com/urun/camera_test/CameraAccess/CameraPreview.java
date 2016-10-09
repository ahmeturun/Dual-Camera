package com.urun.camera_test.CameraAccess;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ahmet on 10/5/2016.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder surfaceHolder;
    public Camera camera;
    int camera_id;
    private static String TAG = "camera_error";

    public CameraPreview(Context context, Camera camera, SurfaceHolder surfaceHolder,int camera_id) {
        super(context);
        this.camera = camera;
        this.surfaceHolder = surfaceHolder;
        this.camera_id = camera_id;

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Toast.makeText(getContext(), "Surface Created", Toast.LENGTH_SHORT).show();
        try {
            // open the camera
            camera = Camera.open(camera_id);
            Toast.makeText(getContext(), "camera "+camera_id +" has been taken", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            // check for exceptions
            Toast.makeText(getContext(), "failed to connect to camera: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            System.err.println(e);
            Log.d("camera_open_exception: ",e.getMessage());
            return;
        } catch (Exception e){
            Toast.makeText(getContext(), "Can't open camera: "+camera_id+"\n"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        Camera.Parameters param;
        param = camera.getParameters();

        // modify parameter
        param.setPreviewSize(400, 349);
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
        Toast.makeText(getContext(), "Surface Changed.", Toast.LENGTH_SHORT).show();
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
        if(camera != null) {
            Toast.makeText(getContext(), "Surface Destroyed Successfuly", Toast.LENGTH_SHORT).show();
        }
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
                outStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
                outStream.write(data);
                outStream.close();
                Log.d("picture_saved", "Picture has been saved succesfully: " + data.length);
                camera.release();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d("file_not_found: ","couldn't save the file "+e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("IOexception: ","couldn't save the file "+e.getMessage());
            } finally {
            }
            Log.d("Log", "onPictureTaken - jpeg");
        }
    };

    public void takePic(){
        if(camera!=null) {
            camera.takePicture(null, null, jpegCallback);
        }
    }

}

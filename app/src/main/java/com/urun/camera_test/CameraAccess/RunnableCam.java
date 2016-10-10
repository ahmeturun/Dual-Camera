package com.urun.camera_test.CameraAccess;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.urun.camera_test.R;
import com.urun.camera_test.model.CameraComponent;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ahmet on 10/10/2016.
 */

public class RunnableCam extends Activity implements Runnable {

    CameraComponent cameraComponentFirst;

    public RunnableCam(CameraComponent cameraComponentFirst) {
        this.cameraComponentFirst = cameraComponentFirst;
    }

    @Override
    public void run() {
        cameraComponentFirst.cameraPreview.camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(String.format("/sdcard/back%d.jpg", System.currentTimeMillis()));
                    outStream.write(data);
                    outStream.close();
                    Log.e("picture_saved", "Picture has been saved succesfully: " + data.length);
                    camera.release();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e("file_not_found: ", "couldn't save the file " + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("IOexception: ", "couldn't save the file " + e.getMessage());
                }
                Log.e("Log", "onPictureTaken - jpeg");
            }
        });
    }
}

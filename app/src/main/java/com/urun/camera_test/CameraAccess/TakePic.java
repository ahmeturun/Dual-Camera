package com.urun.camera_test.CameraAccess;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;

import com.urun.camera_test.Data.MainActivity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ahmet on 10/9/2016.
 */

public class TakePic extends AsyncTask<CameraPreview, String, CameraPreview> {

    @Override
    protected CameraPreview doInBackground(final CameraPreview... params) {
        Log.e("doinback_compt:" ,"done");
        return params[0];
    }


    @Override
    protected void onPostExecute(CameraPreview cameraPreview) {
        super.onPostExecute(cameraPreview);
        cameraPreview.camera.takePicture(null,null,new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(String.format("/sdcard/front%d.jpg", System.currentTimeMillis()));
                    outStream.write(data);
                    outStream.close();
                    Log.e("picture_saved", "Picture has been saved succesfully: " + data.length);
                    camera.release();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e("file_not_found: ","couldn't save the file "+e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("IOexception: ","couldn't save the file "+e.getMessage());
                } catch (Exception e){
                    Log.e("general_exception: ","couldn't save the file "+e.getMessage());
                }
            }
        });
        Log.e("post_execute: ","execution finished.");

    }
}

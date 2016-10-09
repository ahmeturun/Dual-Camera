package com.urun.camera_test.CameraAccess;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.urun.camera_test.Data.MainActivity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ahmet on 10/9/2016.
 */

public class TakePic extends AsyncTask<CameraPreview, String, Integer> {
    @Override
    protected Integer doInBackground(CameraPreview... params) {
            params[0].camera.takePicture(null,null,new Camera.PictureCallback() {
                @Override
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
            });
        return 0;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);

    }

    /*http://stackoverflow.com/questions/18948341/cancel-an-asynctask-inside-itself*/

}

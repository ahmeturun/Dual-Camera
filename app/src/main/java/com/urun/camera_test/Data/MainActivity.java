package com.urun.camera_test.Data;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.urun.camera_test.CameraAccess.CameraPreview;
import com.urun.camera_test.R;


public class MainActivity extends Activity {

//    private Camera camera_front, camera_back;
//    SurfaceView surfaceView_back,surfaceView_front;
//    SurfaceHolder surfaceHolder;
//    CameraPreview cameraPreview_back,cameraPreview_front;
//    Button capture_back, capture_front;

    class FrontCamThread extends Thread {
        private Camera camera_front;
        SurfaceView surfaceView_front;
        SurfaceHolder surfaceHolder;
        CameraPreview cameraPreview_front;
        Button  capture_front;
        public void run() {
            // Camera preview from FRONT
            surfaceView_front = (SurfaceView) findViewById(R.id.camera_preview_front);
            surfaceHolder = surfaceView_front.getHolder();
            cameraPreview_front = new CameraPreview(getApplicationContext(), camera_front, surfaceHolder, 0);
            surfaceHolder.addCallback(cameraPreview_front);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }
    class BackCamThread extends Thread {
        private Camera camera_back;
        SurfaceView surfaceView_back;
        SurfaceHolder surfaceHolder;
        CameraPreview cameraPreview_back;
        Button capture_back;
        public void run() {
            // Camera preview from BACK
            surfaceView_back = (SurfaceView) findViewById(R.id.camera_preview_back);
            surfaceHolder = surfaceView_back.getHolder();
            cameraPreview_back = new CameraPreview(getApplicationContext(),camera_back,surfaceHolder,1);
            surfaceHolder.addCallback(cameraPreview_back);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrontCamThread frontCamThread = new FrontCamThread();
        BackCamThread backCamThread = new BackCamThread();
        frontCamThread.start();
        backCamThread.start();

//        capture_front = (Button) findViewById(R.id.button_capture_back);
//        capture_front.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                cameraPreview_back.takePic();
//                }
//        });
//        capture_back = (Button) findViewById(R.id.button_capture_front);
//        capture_back.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                cameraPreview_front.takePic();
//            }
//        });





    }

}

package com.urun.camera_test.model;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.urun.camera_test.CameraAccess.CameraPreview;

/**
 * Created by ahmet on 10/10/2016.
 */

public class CameraComponent {

    public SurfaceView surfaceView;
    public SurfaceHolder surfaceHolder;
    public CameraPreview cameraPreview;

    public CameraComponent(SurfaceView surfaceView, SurfaceHolder surfaceHolder, CameraPreview cameraPreview) {
        this.surfaceView = surfaceView;
        this.surfaceHolder = surfaceHolder;
        this.cameraPreview = cameraPreview;
    }
}

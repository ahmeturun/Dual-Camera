//package com.urun.camera_test.Data;
//
///**
// * Created by ahmet on 1/13/2017.
// */
//import android.app.Activity;
//import android.content.res.AssetManager;
//import android.graphics.Bitmap;
//import android.os.Bundle;
//import android.renderscript.Allocation;
//import android.renderscript.Element;
//import android.renderscript.RenderScript;
//import android.renderscript.ScriptIntrinsicYuvToRGB;
//import android.renderscript.Type;
//import android.widget.ImageView;
//import com.urun.camera_test.R;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//public class HelloRenderActivity extends Activity {
//
//    public static final int W = 540;
//    public static final int H = 360;
//    private RenderScript rs;
//    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);
//
//        AssetManager assets = getAssets();
//        byte[] yuvByteArray = new byte[291600];
//        byte[] outBytes = new byte[W * H * 4];
//
//        InputStream is = null;
//        try {
//            is = assets.open("yuv.bin");
//            System.out.println("read: " + is.read(yuvByteArray));
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                is.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        ImageView iv = (ImageView) findViewById(R.id.image);
//        rs = RenderScript.create(this);
//        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(rs));
//
//
//        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
//                .setX(W).setY(H)
//                .setYuvFormat(android.graphics.ImageFormat.NV21);
//        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
//
//
//        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
//                .setX(W).setY(H);
//        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
//
//        in.copyFrom(yuvByteArray);
//
//        yuvToRgbIntrinsic.setInput(in);
//        yuvToRgbIntrinsic.forEach(out);
//
//        out.copyTo(outBytes);
//
//        Bitmap bmpout = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
//        out.copyTo(bmpout);
//
//        iv.setImageBitmap(bmpout);
//    }
//
//}
//
//
//

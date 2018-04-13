package com.example.a100541476.visimphelper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    Camera mCamera;

    private Handler handler = new Handler();

    // Define the code block to be executed
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Insert custom code here
            Log.d("in handler", "handler");
            mCamera = Camera.open();
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(640,480);
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            params.setPictureFormat(ImageFormat.JPEG);
            SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
            try {
                mCamera.setPreviewTexture(st);
            }catch(IOException e){
                Log.d("IOEXCEPTION", e.getMessage());
            }
            mCamera.setParameters(params);
            mCamera.startPreview();
            Log.d("COUNTDOWN", "COUNTING DOWN...");
            mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Toast.makeText(getApplicationContext(), "Image snapshot Done",Toast.LENGTH_LONG).show();
                    mCamera.stopPreview();
                    mCamera.release();

                }
            });
            // Repeat every 2 seconds
            handler.postDelayed(runnable, 5000);
        }
    };

// Start the Runnable immediately


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        handler.post(runnable);
//        mCamera = Camera.open();
//
//
//        new CountDownTimer(3000, 1000){
//            public void onFinish(){
//                Camera.Parameters params = mCamera.getParameters();
//                params.setPreviewSize(640,480);
//                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                params.setPictureFormat(ImageFormat.JPEG);
//                SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
//                try {
//                    mCamera.setPreviewTexture(st);
//                }catch(IOException e){
//                    Log.d("IOEXCEPTION", e.getMessage());
//                }
//                mCamera.setParameters(params);
//                mCamera.startPreview();
//                Log.d("COUNTDOWN", "COUNTING DOWN...");
//                mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
//                    @Override
//                    public void onPictureTaken(byte[] data, Camera camera) {
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                        Toast.makeText(getApplicationContext(), "Image snapshot Done",Toast.LENGTH_LONG).show();
//                        mCamera.stopPreview();
//                        mCamera.release();
//
//                    }
//                });
//            }
//            public void onTick(long millisUntilFinished){
//                Log.d("Time left", Long.toString(millisUntilFinished));
//            }
//        }.start();
//



//
//        Camera myCamera = Camera.open();
//        if(myCamera!=null){
//            try{
//                SurfaceView dummy = (SurfaceView) findViewById(R.id.surfaceView);
//                myCamera.setPreviewDisplay(dummy.getHolder());
//                myCamera.startPreview();
//                myCamera.takePicture(null, null, getJpegCallback());
//            }catch(IOException e){
//                Log.d("take snapshots", e.getMessage());
//            }
//            finally{
//                myCamera.release();
//            }
//        } else{
//            Log.d("take snapshots", "failed");
//        }
//    }
//
//    private Camera.PictureCallback getJpegCallback(){
//        Camera.PictureCallback jpeg = new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] bytes, Camera camera) {
//                Log.d("getJpegCallback", "Success");
//            }
//        };
//        return null;
    }
}

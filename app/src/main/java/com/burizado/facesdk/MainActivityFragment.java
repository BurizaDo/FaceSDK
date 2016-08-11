package com.burizado.facesdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements Camera.PreviewCallback {

    BroadcastReceiver receiver;
    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Handler h = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        if(msg.what == 1){
                            ((ImageView)getView().findViewById(R.id.image)).setImageResource(R.drawable.smile);
                        }else if(msg.what == 2){
                            ((ImageView)getView().findViewById(R.id.image)).setImageResource(R.drawable.sleep);
                        }else if(msg.what == 0){
                            ((ImageView)getView().findViewById(R.id.image)).setImageResource(R.drawable.nothing);
                        }
                        return false;
                    }
                });
                if(intent.getAction().equals("SMILING")){
                    h.sendEmptyMessage(1);
                }else if(intent.getAction().equals("BLINK")){
                    h.sendEmptyMessage(2);
                }else if(intent.getAction().equals("NOTHING")){
                    h.sendEmptyMessage(0);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SMILING");
        intentFilter.addAction("BLINK");
        intentFilter.addAction("NOTHING");
        this.getContext().registerReceiver(receiver, intentFilter);
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getContext().unregisterReceiver(receiver);
    }

    Camera mCamera;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    SurfaceHolder.Callback my_callback() {
        SurfaceHolder.Callback ob1 = new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                mCamera.setDisplayOrientation(90);
                try {
//                    mCamera.setPreviewDisplay(holder);
                    mCamera.setPreviewDisplay(new SurfaceView(getContext()).getHolder());
                    mCamera.setPreviewCallback(MainActivityFragment.this);
                } catch (IOException exception) {
                    mCamera.release();
                    mCamera = null;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {
                mCamera.startPreview();
            }
        };
        return ob1;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;
        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, width, height);
        YuvImage yuvimage=new YuvImage(data, ImageFormat.NV21,width,height,null);
        yuvimage.compressToJpeg(rect, 100, outstr);
        Bitmap bmp = BitmapFactory.decodeByteArray(outstr.toByteArray(), 0, outstr.size());
        Matrix matrix = new Matrix();
        matrix.preRotate(270);
        Bitmap bmp2 = Bitmap.createBitmap(bmp ,0,0, bmp .getWidth(), bmp.getHeight(),matrix,true);
        parse(bmp2);
        ((ImageView)getView().findViewById(R.id.image)).setImageBitmap(bmp2);

    }

    private void parse(Bitmap bitmap){
        FaceDetector detector = new FaceDetector.Builder(getContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        if (!detector.isOperational()) {
            Log.d("detector", "~~~~~~~~shit~~~~~~~~~");
        }
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = detector.detect(frame);
        detector.release();
        for(int i = 0; i < faces.size(); ++ i){
            Face f = faces.valueAt(i);
            Log.d("face", " smile:  " + f.getIsSmilingProbability());
            Log.d("face", " left eye:  " + f.getIsLeftEyeOpenProbability());
            Log.d("face", " right eye:  " + f.getIsRightEyeOpenProbability());
            if(f.getIsSmilingProbability() > 0.5){
                getContext().sendBroadcast(new Intent("SMILING"));
            }else if(f.getIsLeftEyeOpenProbability() > 0.5 || f.getIsRightEyeOpenProbability() > 0.5){
                getContext().sendBroadcast(new Intent("BLINK"));
            }
        }
    }
}

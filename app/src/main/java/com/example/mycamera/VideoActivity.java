package com.example.mycamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class VideoActivity extends AppCompatActivity implements SurfaceHolder.Callback, Handler.Callback { // IS-A Handler.Callback


    private final static int CAMERA_PERMISSION_CODE = 0;
    private  static String CAMERA_ID = "0"; // 0 --> back camera and 1 --> front camera
    private final static int MSG_SURFACE_CREATED = 0;
    private final static int MSG_CAMERA_OPENED = 1;


    private Handler mHandler = new Handler(this); // this is MainActivity which IS-A Handler.Callback
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Surface mCameraSurface;
    private boolean mIsCameraSurfaceCreated;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraStateCallBack;
    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback;
    private CameraCaptureSession mCameraCaptureSession;

    private Button mButton;
    private boolean mIsRecordingVideo;

    private MediaRecorder mMediaRecorder;

    private Button switcMode;

    private Button switchCamera;

    private static final String cameraFront ="1";
    private static final String cameraBack = "0";

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    CameraCharacteristics characteristics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mSurfaceView = findViewById(R.id.surface_view1);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this); // this is MainActivity which IS-A SurfaceHolder.Callback


        mButton = findViewById(R.id.button1);

        mButton.setOnClickListener(new View.OnClickListener() { // step 1: define and display recording button and manage UI
            @Override
            public void onClick(View v) {
                if (!mIsRecordingVideo) {
                    startVideoRecording();
                } else {
                    stopVideoRecording();
                }
            }
        });
        switcMode = findViewById(R.id.switchMode1);
        switcMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               closeCameraSession();

                Intent i = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(i);
            }
        });

        switchCamera = findViewById(R.id.switchCamera1);
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switchCamera1();

            }
        });
    }

    //to switch camera from front to back and vice-versa.
    public void switchCamera1(){
        if(CAMERA_ID.equals(cameraFront)){

           closeCameraSession();
            CAMERA_ID = cameraBack;
           mIsCameraSurfaceCreated = true;
            try {
                mCameraManager.openCamera(CAMERA_ID, mCameraStateCallBack, new Handler());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }
        else if(CAMERA_ID.equals(cameraBack)){
            closeCameraSession();
            CAMERA_ID = cameraFront;
            mIsCameraSurfaceCreated=true;
            try {
                mCameraManager.openCamera(CAMERA_ID, mCameraStateCallBack, new Handler());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        onStart();
    }

    private void startVideoRecording() {
        Log.d("*****************************","handleVideoRecording");

        if (mCameraDevice != null) {

            closeCameraSessionForStartingRecording(); // step 3: close the previous camera session

            List<Surface> surfaceList = new ArrayList<Surface>();
            try {
                setupMediaRecorder(); // step 4: setting up the media recorder
            } catch (IOException e) {
                e.printStackTrace();
            }

            final CaptureRequest.Builder recordingBuilder;
            try {
                if(mCameraSurface==null){

                    mSurfaceView = findViewById(R.id.surface_view1);
                    mSurfaceHolder = mSurfaceView.getHolder();
                    mSurfaceHolder.addCallback(this);
                }


                recordingBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD); // step 11: building recording capture request

                surfaceList.add(mCameraSurface);
                recordingBuilder.addTarget(mCameraSurface); // step 12: add surface view as target

                surfaceList.add(mMediaRecorder.getSurface());
                recordingBuilder.addTarget(mMediaRecorder.getSurface()); // step 13: Add media recorder surface as target

                Log.d("*****************************","surfaces added");
                mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() { // step 14: call create capture session
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        Log.d("*****************************","recording configured");
                        mCameraCaptureSession = session;

                        try {
                            mCameraCaptureSession.setRepeatingRequest(recordingBuilder.build(), null, null); // step 15: call set repeating request with the recording capture request built
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("*****************************","user entered recording UI");
                                mMediaRecorder.start(); // step 16: call the MediaRecorder#start
                                mButton.setText("Recording..."); // step 17: change UI to recording mode
                                mIsRecordingVideo = true;
                            }
                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d("*****************************","Recording onConfigureFailed");
                    }
                }, mHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        else{
            if(mSurfaceView==null) {
                mSurfaceView = findViewById(R.id.surface_view1);
                mSurfaceHolder = mSurfaceView.getHolder();
                mSurfaceHolder.addCallback(this);
            }
            if(mCameraDevice==null)
                handleCamera();
        }
    }

    private void stopVideoRecording() {
        Log.d("*****************************","stopVideoRecording");
        closeCameraSessionForStartingRecording(); // step 18: close the recording capture session
         configureCamera(); // step 19: call code to restart preview only capture session
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediaRecorder.stop(); // step 20: stop media recorder
                mMediaRecorder.reset();
                mButton.setText("Record Video"); // step 21: Update UI back to preview mode
                mIsRecordingVideo = false;
            }
        });
    }



    private void setupMediaRecorder() throws IOException {
        // look at media recorder architecture https://developer.android.com/reference/android/media/MediaRecorder

        Log.d("*****************************","setupMediaRecorder");
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder(); // step 5: create media recorder instance
        }

        mMediaRecorder.setOutputFile(getOutputFile().getAbsolutePath()); // step 6: setting up output file

        // step 7: setting up audio source and video source
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


        // step 8: setting up recording parameters

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        // step 9: adjust orientation according to sensor

        mMediaRecorder.setOrientationHint(getRotation());



        mMediaRecorder.prepare(); // step 10: call prepare on media recorder instance
    }

    //to get degree of rotation for proper orientaion of recorded video.
    private int getRotation(){
        int device_rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int jpegOrientation =0;
        try{
            characteristics = mCameraManager.getCameraCharacteristics(CAMERA_ID);
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int surfaceRotaion = ORIENTATIONS.get(device_rotation);
            jpegOrientation = (surfaceRotaion+ sensorOrientation + 270)%360;
        }
        catch(CameraAccessException e){
            e.printStackTrace();
        }
        return jpegOrientation;

    }

    private File getOutputFile() {
        File dir = new File(Environment.getExternalStorageDirectory().toString(), "MyVideos");
        if (!dir.exists()) {
            dir.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File imageFile = new File (dir.getPath() + File.separator + "VID_"+timeStamp+".mp4");

        Log.d("*********************************","imagefilename="+imageFile.getAbsolutePath());

        return imageFile;
    }




    @Override
    public void surfaceCreated(SurfaceHolder holdconfigureCameraer) {
        Log.d("******************","1 surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("************************","2 surfaceChanged");
        mCameraSurface = holder.getSurface();
        mHandler.sendEmptyMessage(MSG_SURFACE_CREATED);
        mIsCameraSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("************************","surfaceDestroyed");
        mIsCameraSurfaceCreated = false;
    }


    @Override
    protected void onStart() {
        super.onStart();

        if(mSurfaceView==null) {
            mSurfaceView = findViewById(R.id.surface_view1);
            mSurfaceHolder = mSurfaceView.getHolder();       // this is MainActivity which IS-A SurfaceHolder.Callback
            mSurfaceHolder.addCallback(this);
        }


        if(mCameraDevice==null)
            handleCamera();
    }

    @SuppressLint("MissingPermission")
    private void handleCamera() {

        Log.d ("****************************","3 handle camera");

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraIds[] = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                Log.e ("******************************","cameraId="+cameraId);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mCameraStateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d("********************************","4 onOpened -"+camera.getId());
                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d("********************************","onDisconnected -"+camera.getId());

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d("********************************","onDisconnected -"+camera.getId());
            }
        };

        try {
            mCameraManager.openCamera(CAMERA_ID, mCameraStateCallBack, new Handler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        android.util.Log.e ("****************************","msg.what="+msg.what);
        android.util.Log.e ("****************************","mIsCameraSurfaceCreated="+mIsCameraSurfaceCreated);
        android.util.Log.e ("****************************","mCameraDevice="+mCameraDevice);
        switch (msg.what) {
            case MSG_SURFACE_CREATED:
            case MSG_CAMERA_OPENED:
                if (mIsCameraSurfaceCreated && (mCameraDevice != null)) {
                    mIsCameraSurfaceCreated = false;
                    configureCamera();
                }
        }
        return true;
    }

    //configure camera for preview
    private void configureCamera() {
        Log.d ("****************************","4 configureCamera");

        if(mCameraDevice==null){
            handleCamera();
        }

        List<Surface> surfaceList = new ArrayList<Surface>();
        surfaceList.add(mCameraSurface); // surface to be viewed

        mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.d ("***************************","onConfigured");
                mCameraCaptureSession = session;

                try {
                    CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(mCameraSurface);

                    mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                    Log.d ("****************************","5 setRepeatingRequest");
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.d ("***************************","onConfigureFailed");
            }
        };

        try {
            mCameraDevice.createCaptureSession(surfaceList, mCameraCaptureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    //to close session after and before recording
    private void closeCameraSessionForStartingRecording(){
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mCameraCaptureSession.close();
            mCameraCaptureSession = null;

        }
    }

    //to close camera before going to other mode
    private void closeCameraSession() {
        Log.d("*****************************","closeCameraSession");
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mCameraCaptureSession.close();
            mCameraCaptureSession = null;

        }
        if(mMediaRecorder!=null){
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if(mCameraDevice!=null){
            mCameraDevice.close();
            mCameraDevice =null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mMediaRecorder!=null)
            mMediaRecorder.release();
        mMediaRecorder = null;
        closeCameraSession();
        if(mCameraDevice!=null)
            mCameraDevice.close();
        mCameraDevice = null;
    }
}




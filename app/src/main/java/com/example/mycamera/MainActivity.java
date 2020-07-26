package com.example.mycamera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
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
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback , android.os.Handler.Callback {
    private final static int CAMERA_PERMISSION_CODE =0;

    private final static int MSG_SURFACE_CREATED =0;
    private final static int MSG_CAMERA_OPENED =1;
    private static final String cameraFront ="1";
    private static final String cameraBack = "0";
    private  static String CAMERA_ID ="0";

   private Button switchCamera;
    private Handler mHandler = new Handler(this);


    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Surface mCameraSurface;
    private boolean mIsCameraSurfaceCreated;

    private Button mButton;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraStateCallBack;

    private CameraCaptureSession mCameraCaptureSession;

    private ImageReader mCaptureImageReader = null;
    private ImageView imageView;

    CameraCharacteristics characteristics;

     private Button switchMode;

   private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);
   static{
       ORIENTATIONS.append(Surface.ROTATION_0,90);
       ORIENTATIONS.append(Surface.ROTATION_90,0);
       ORIENTATIONS.append(Surface.ROTATION_180,270);
       ORIENTATIONS.append(Surface.ROTATION_270,180);
   }

   //called when a request triggers capture to start and when the capture is completed.
    //tracks progress of capturing request.
   CameraCaptureSession.CaptureCallback mCaptureCallback =  new CameraCaptureSession.CaptureCallback() {


       @Override
       public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
           super.onCaptureStarted(session, request, timestamp, frameNumber);
           Log.i("in what","Cameracapturesession.Capturecallback.on capture started");
       }

       @Override
       public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
           super.onCaptureProgressed(session, request, partialResult);
       }

       @Override
       public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
           super.onCaptureCompleted(session, request, result);
           Log.i("in what","Cameracapturesession.Capturecallback.on capture completed");
       }
   };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Log.i("inside what ",": onCreate activity 1");

        switchCamera = findViewById(R.id.switchCamera);
        mButton = findViewById(R.id.button);
        mButton.setText("take image");
        imageView= findViewById(R.id.image_view);
        imageView.setVisibility(View.INVISIBLE);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCaptureImage();
            }
        });
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera1();
            }
        });

        switchMode = findViewById(R.id.switchMode);
        switchMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                closeCameraSession();
                Intent i = new Intent(getApplicationContext(),VideoActivity.class);
                startActivity(i);

            }
        });


    }

    public void setImageReader(){
        if(mCaptureImageReader==null) {
            mCaptureImageReader = ImageReader.newInstance(600, 600, ImageFormat.JPEG, 2);
            mCaptureImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    saveImage(reader);
                }
            }, mHandler);
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("inside what ",": Surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
             mCameraSurface =holder.getSurface();
             if(mCameraSurface ==null)
             Log.i("inside what","surface changed sufrace is null");
             else{
                 Log.i("inside what","surface changed sufrace is not null");
             }
             mIsCameraSurfaceCreated = true;
             mHandler.sendEmptyMessage(MSG_SURFACE_CREATED);

        Log.i("inside what ",": Surface Changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("inside what ",": surface Destroyed");
        mIsCameraSurfaceCreated = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("inside what ",": onStart");
        if(mSurfaceView==null) {
            mSurfaceView = findViewById(R.id.surface_view);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(this);
        }

        if(mCaptureImageReader==null)
        setImageReader();

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
      handleCamera();
        else{
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, CAMERA_PERMISSION_CODE);
        }


    }

    @SuppressLint("MissingPermission")
    public void handleCamera(){
        Log.i("inside what ",": HandleCamera");
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraStateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
                Log.i("inside what ",": onOpened in handle camera");
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.i("inside what ",": ondisconnected in handle camera");
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.i("inside what ",": onError in handle camera");
            }
        };
        try{
            mCameraManager.openCamera(CAMERA_ID,mCameraStateCallBack,new Handler());
            Log.i("inside what ",": calling openCamera");
        }
        catch(CameraAccessException e){
            e.printStackTrace();
        }

    }
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        Log.i("inside what ",": handleMessage");

        switch (msg.what){
            case MSG_SURFACE_CREATED:
            case MSG_CAMERA_OPENED:
                if(mIsCameraSurfaceCreated && (mCameraDevice!=null)){
                    mIsCameraSurfaceCreated =false;
                    configureCamera();
                }

        }
        return true;
    }

    private void handleCaptureImage(){

        if(mCameraDevice!=null){
            if(mCameraCaptureSession!=null){
                try{
                    Log.i("in what","handle capture image");
                    CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_SINGLE);

               captureRequestBuilder.addTarget(mCaptureImageReader.getSurface());
               captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY,(byte)100);
               mCameraCaptureSession.capture(captureRequestBuilder.build(),mCaptureCallback,mHandler);
                }
                catch(CameraAccessException e){
                    e.printStackTrace();
                }

            }

        }
    }

    private void saveImage(ImageReader reader){
        Log.i("in what","save image");
        Image image = reader.acquireLatestImage();
        byte[] bytes = getJpegData(image);
       rotateAndSaveImage(bytes);

       mSurfaceView.setVisibility(View.INVISIBLE);
       switchCamera.setVisibility(View.INVISIBLE);
       imageView.setVisibility(View.VISIBLE);
       switchMode.setVisibility(View.INVISIBLE);
       mButton.setText("click more");

       //to go to camera again for more images.
       mButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               goTosurfaceView();
           }
       });
    }

    //to click more images.
    private void goTosurfaceView(){
        imageView.setVisibility(View.INVISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);
        switchCamera.setVisibility(View.VISIBLE);
        switchMode.setVisibility(View.VISIBLE);
        mButton.setText("take image");
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCaptureImage();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("in what","on pause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("in what","on destroy");

    }

    private File getOutputFile(){
        File dir = new File(Environment.getExternalStorageDirectory().toString(),"MyPictures");
        if(!dir.exists()){
            dir.mkdir();
        }
        Log.i("in what","getoutput file");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File imageFile = new File(dir.getPath() + File.separator+ "PIC_" + timeStamp + ".jpg");
        return imageFile;
    }



    private void rotateAndSaveImage(byte[] input){
        Log.i("in what","rotate and save image");
        Bitmap sourceBitmap = BitmapFactory.decodeByteArray(input,0,input.length);
        Matrix m =new Matrix();
        File f = getOutputFile();
        m.setRotate(getRotation(),sourceBitmap.getWidth(),sourceBitmap.getHeight());
        Bitmap rotatedBitmap = Bitmap.createBitmap(sourceBitmap,0,0,sourceBitmap.getWidth(),sourceBitmap.getHeight(),m,true);
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(f);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
            imageView.setImageBitmap(rotatedBitmap);
            //to add the image to gallery
            addImageTOGallery(f.getPath(),this);
            fos.flush();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            try{
                fos.close();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }


    }

    private void writeBytesToFile(byte[] input){
        Log.i("in what","writeBytesToFile");
        File file = new File(Environment.getExternalStorageDirectory(),"myImage.jpg");
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(file);
            fos.write(input);

        }
        catch(FileNotFoundException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            try{
                if(fos!=null){
                    fos.close();
                }
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private byte[] getJpegData(Image image){

        Log.i("in what","getJpegdata");
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        return byteArray;
    }


    //calculate rotation
    private float getRotation(){
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

    //callback object for receiving updates about the state of a camera capture session

   private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {

        //called when camera device has finished configuring itself and session can start processing capture request.
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCameraCaptureSession = session;
            try{
                //makes a buider instance which initialises the request feild to one of templates defined in camera device
                Log.i("in what","statecallback.on configured");
                CaptureRequest.Builder previewRequestBuilder  = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                //output suface for builder instance.
                previewRequestBuilder.addTarget(mCameraSurface);
                mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),null,null);
            }
            catch(CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }


    };


    private void configureCamera() {
        Log.i("inside what ", ":configureCamera ");
        List<OutputConfiguration> outputConfigurationList = new ArrayList<>();
        //stream for preview
        OutputConfiguration previewStream = new OutputConfiguration(mCameraSurface);
        //stream for captured image
        OutputConfiguration captureStream = new OutputConfiguration(mCaptureImageReader.getSurface());
        outputConfigurationList.add(previewStream);
        outputConfigurationList.add(captureStream);
        try {
            //creates capture session by output configration
            mCameraDevice.createCaptureSessionByOutputConfigurations(outputConfigurationList, mCameraCaptureSessionStateCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("inside what ",": onRequestPermission ");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode== CAMERA_PERMISSION_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                handleCamera();
            }
        }
    }

    //to add saved image to gallery
  private static void addImageTOGallery(final String filePath,final Context context){
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.DATE_TAKEN,System.currentTimeMillis());
      values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
      values.put(MediaStore.MediaColumns.DATA,filePath);
      context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
  }

  //to switch camera from front to back and vice-versa.
  public void switchCamera1(){
        if(CAMERA_ID.equals(cameraFront)){
            CAMERA_ID = cameraBack;
            closeCamera();
            mIsCameraSurfaceCreated=true;
            reopencamera();
        }
        else if(CAMERA_ID.equals(cameraBack)){
            CAMERA_ID = cameraFront;
            closeCamera();
            mIsCameraSurfaceCreated=true;
            reopencamera();
        }
  }

  //to close camera before switching from front to back and vice-versa.
  public void closeCamera(){
        if(mCameraDevice!=null)
    mCameraDevice.close();
        if(mCaptureImageReader!=null)
    mCaptureImageReader.close();
        if(mCameraDevice!=null)
      mCameraDevice=null;
        if(mCaptureImageReader!=null)
      mCaptureImageReader = null;
  }

  //to reopen camera with changed camera id on switching.
  void reopencamera(){
     mSurfaceView = findViewById(R.id.surface_view);
     mSurfaceHolder =mSurfaceView.getHolder();
      mSurfaceHolder.addCallback(this);
      mHandler.sendEmptyMessage(MSG_SURFACE_CREATED);
      setImageReader();
      handleCamera();
  }

    private void closeCameraSession() {
        Log.d("inside what","closeCameraSession");
        if (mCameraCaptureSession != null && mCameraDevice!=null) {
            try {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if(mCameraDevice!=null)
            mCameraDevice.close();
        if(mCaptureImageReader!=null)
            mCaptureImageReader.close();
        mCaptureImageReader=null;
        mCameraDevice = null;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("inside what ",": onRestart");
    }

    @Override
    protected void onStop() {
        Log.i("in what","Onstop");
        super.onStop();
        if(mCameraCaptureSession!=null && mCameraDevice!=null){
            try{
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
            }
            catch(CameraAccessException e){
                e.printStackTrace();
            }
            mCameraCaptureSession.close();
            mCameraDevice.close();
        }
        if(mCaptureImageReader!=null)
            mCaptureImageReader.close();
        mCaptureImageReader = null;
        mCameraCaptureSession = null;
        if(mCameraDevice!=null)
            mCameraDevice.close();
        mCameraDevice = null;
    }

}















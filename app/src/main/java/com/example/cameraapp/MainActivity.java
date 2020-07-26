package com.example.cameraapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
private ImageView imageView;
private  static final int  IMAGE_REQUEST=1;
String currentImagePath = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void captureImage(View view){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(cameraIntent.resolveActivity(getPackageManager())!=null){
            File imageFile = null;

            try{
                imageFile = getImageFile();
            }
            catch(IOException e){
                e.printStackTrace();
            }
            if(imageFile!=null) {
             Uri imageUri = FileProvider.getUriForFile(this,"com.example.cameraapp.FileProvider",imageFile);
             cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
             startActivityForResult(cameraIntent,IMAGE_REQUEST);
            }
        }
    }
    public void displayImage(View view){
        Intent intent = new Intent(this,DisplayImage.class);
       intent.putExtra("image_path",currentImagePath);
       startActivity(intent);
    }


    private File getImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageName = "jpg_"+timestamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageName,".jpg",storageDir);
        currentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }
}

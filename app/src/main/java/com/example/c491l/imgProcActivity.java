package com.example.c491l;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.c490.Util;
import com.c490.quantcomp;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.io.OptionalDataException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

//import com.jaredrummler.android.colorpicker.ColorPickerDialog;

import yuku.ambilwarna.AmbilWarnaDialog;

public class imgProcActivity extends AppCompatActivity {

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private Bitmap img;
    private ImageView myImage;
    private static int mDefaultColor;
    private ImageButton Button;
    private ImageButton gallery;
    private ImageButton editBtn;
    private static final int PICK_IMAGE = 100;
    Uri imageURI;

    public static final float map(float value, float istart, float istop, float ostart, float ostop) {
        return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
    }

    private int getNavigationBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight)
                return realHeight - usableHeight;
            else
                return 0;
        }
        return 0;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgproc);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        myImage = findViewById(R.id.myImage);
        myImage.setAlpha(1.0f);
        Button = findViewById(R.id.button);
        gallery = findViewById(R.id.camera_roll);
        editBtn = findViewById(R.id.editColor);
        ViewGroup.LayoutParams params = myImage.getLayoutParams();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        params.width = dm.widthPixels * 5 / 6;
        params.height = (dm.heightPixels + getNavigationBarHeight()) * 5 / 6;

        myImage.setLayoutParams(params);
        myImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (img == null) {
                    return false;
                }
                int ox = (int) event.getX();
                int oy = (int) event.getY();
                System.out.println("TAP { o " + ox + ", " + oy + " }");
                int x = (int) map(ox, 0, 900, 0, img.getWidth());
                int y = (int) map(oy, 230, 1600, 0, img.getHeight());
                if (x < 0) {
                    return false;
                }
                if (y < 0) {
                    return false;
                }
                if (x > img.getWidth()) {
                    return false;
                }
                if (y > img.getHeight()) {
                    return false;
                }
                System.out.println("TAP { " + x + ", " + y + " }");
                //Pass color here
                Bitmap out = quantcomp.run_iproc(img, x, y, mDefaultColor);
                myImage.setImageBitmap(out);
                return false;
            }
        });

        Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
        editBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openColorPicker();
            }
        });
    }

    public void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, mDefaultColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mDefaultColor = color;

                System.out.println("dis is the color value: " + mDefaultColor);

            }
        });
        colorPicker.show();
    }

    private static final int REQUEST_TAKE_PHOTO = 1;

    private void openGallery() {
        System.out.println("Open gallery request");
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = uriFromFile(photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private Uri uriFromFile(File f) {
        return FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", f);
    }

    private String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        System.out.println(requestCode + " xxxxxxx " + resultCode);
        try {
            switch (requestCode) {
                case REQUEST_TAKE_PHOTO: {
                    if (resultCode == RESULT_OK) {
                        File file = new File(mCurrentPhotoPath);
                        Bitmap o = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(file));
                        if (o != null) {
                            System.out.println("took photo at [" + mCurrentPhotoPath + "]");
                            Bitmap b = o.copy(o.getConfig(), true);
                            img = Util.scaleBI(b, 512, 768);
                            myImage.setImageBitmap(img);
                        }
                    }
                    break;
                }
                case PICK_IMAGE:
                    if (resultCode == RESULT_OK) {
                        imageURI = intent.getData();
                        Bitmap o = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageURI);
                        if (o != null) {
                            System.out.println("picked photo at [" + imageURI.getPath() + "]");
                            Bitmap b = o.copy(o.getConfig(), true);
                            img = Util.scaleBI(b, 512, 768);
                            myImage.setImageBitmap(img);
                        }
                    }
                    break;
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }
}

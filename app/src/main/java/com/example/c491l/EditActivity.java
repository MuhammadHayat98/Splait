package com.example.c491l;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;

import yuku.ambilwarna.AmbilWarnaDialog;

public class EditActivity extends AppCompatActivity {
    private ImageButton editBtn;
    private static int mDefaultColor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        mDefaultColor = ContextCompat.getColor(EditActivity.this, R.color.colorPrimary);
        editBtn = findViewById(R.id.editColor);
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

    public static int getSelectedColor() {
        return mDefaultColor;
    }
}
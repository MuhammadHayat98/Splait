package com.example.c491l;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ImageButton mLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogin = findViewById(R.id.buttonLogin);
        mLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openCameraView();
            }
        });
    }

    public void openCameraView() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

}
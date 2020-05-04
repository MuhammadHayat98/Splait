package com.example.c491l;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ImageButton mLogin;
    private ImageButton register;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogin = findViewById(R.id.buttonLogin);
        register = findViewById(R.id.signup);
        mLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openCameraView();
            }
        });

        register.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                registerPage();
            }
        });
    }

    public void openCameraView() {
        Intent intent = new Intent(this, splash.class);
        startActivity(intent);
    }
    public void registerPage(){
        Intent intent = new Intent(this, registerPage.class);
        startActivity(intent);
    }

} 
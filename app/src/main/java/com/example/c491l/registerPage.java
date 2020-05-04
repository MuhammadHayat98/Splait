package com.example.c491l;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

public class registerPage extends AppCompatActivity {
    private ImageButton Login;
    private EditText email;
    private EditText user;
    private EditText pass;
    private EditText re_pass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);
        Login = findViewById(R.id.buttonLogin2);

        email = findViewById(R.id.emailR);
        user = findViewById(R.id.UserR);
        pass = findViewById(R.id.PassR);
        re_pass = findViewById(R.id.reEnter);

//        handle validation and registration here

        Login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                backToLogin();
            }
        });
    }
    public void backToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}

package com.example.c491l;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private ImageButton mLogin;
    private ImageButton register;
    private EditText email;
    private EditText pass;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLogin = findViewById(R.id.buttonLogin);
        email = findViewById(R.id.email);
        pass = findViewById(R.id.password);
        register = findViewById(R.id.signup);
        auth = FirebaseAuth.getInstance();
        mLogin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String txtEmail = email.getText().toString();
                String txtPass = pass.getText().toString();
                loginUser(txtEmail, txtPass);
            }
        });

        register.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                registerPage();
            }
        });
    }

    private void loginUser(String email, String pass) {
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                openCameraView();
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
        finish();
    }

} 
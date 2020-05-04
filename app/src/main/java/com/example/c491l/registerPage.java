package com.example.c491l;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class registerPage extends AppCompatActivity {
    private ImageButton register;
    private EditText email;
    private EditText user;
    private EditText pass;
    private EditText re_pass;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);
        register = findViewById(R.id.register);

        email = findViewById(R.id.emailR);
        user = findViewById(R.id.UserR);
        pass = findViewById(R.id.PassR);
        re_pass = findViewById(R.id.reEnter);
        auth = FirebaseAuth.getInstance();
//        handle validation and registration here

        register.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String txtEmail = email.getText().toString();
                String txtPass = pass.getText().toString();

                if(TextUtils.isEmpty(txtEmail) || TextUtils.isEmpty(txtPass)) {
                    Toast.makeText(registerPage.this, "Empty Credentials", Toast.LENGTH_SHORT).show();
                } else if(txtPass.length() < 6) {
                    Toast.makeText(registerPage.this, "Password too short", Toast.LENGTH_SHORT).show();
                } else  {
                    registerUser(txtEmail, txtPass);
                }
            }
        });
    }

    private void registerUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(registerPage.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    Toast.makeText(registerPage.this, "Registration Success!", Toast.LENGTH_SHORT).show();
                    backToLogin();
                } else {
                    Toast.makeText(registerPage.this, "Uh oh Registration failed :(", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void backToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

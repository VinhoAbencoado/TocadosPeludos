package com.example.tocadospeludos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (UserStorage.isLoggedIn(this)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText emailInput = findViewById(R.id.etEmail);
        EditText passwordInput = findViewById(R.id.etPassword);
        Button loginButton = findViewById(R.id.btnLogin);

        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            Intent intent = new Intent(this, RecoverActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show();
                return;
            }
            if (UserStorage.login(this, email, password)) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, "E-mail ou senha incorretos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
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

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText nameInput = findViewById(R.id.etName);
        EditText emailInput = findViewById(R.id.etEmail);
        EditText passwordInput = findViewById(R.id.etPassword);
        EditText confirmInput = findViewById(R.id.etConfirmPassword);
        Button registerButton = findViewById(R.id.btnRegister);

        findViewById(R.id.tvLogin).setOnClickListener(v -> finish());

        registerButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmInput.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show();
                return;
            }
            if (UserStorage.userExists(this, email)) {
                Toast.makeText(this, "E-mail já cadastrado", Toast.LENGTH_SHORT).show();
                return;
            }
            if (UserStorage.registerUser(this, name, email, password)) {
                UserStorage.login(this, email, password);
                Toast.makeText(this, "Conta criada com sucesso", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Falha ao criar conta", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

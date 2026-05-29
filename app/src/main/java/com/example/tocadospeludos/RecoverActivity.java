package com.example.tocadospeludos;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecoverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recover);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText emailInput = findViewById(R.id.etEmail);
        Button recoverButton = findViewById(R.id.btnRecover);

        recoverButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Digite seu e-mail", Toast.LENGTH_SHORT).show();
                return;
            }
            if (UserStorage.userExists(this, email)) {
                Toast.makeText(this, "Se existe uma conta com esse e-mail, um link foi enviado.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "E-mail não encontrado", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
    }
}

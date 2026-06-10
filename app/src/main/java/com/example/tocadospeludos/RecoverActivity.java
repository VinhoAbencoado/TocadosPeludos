package com.example.tocadospeludos;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
            emailInput.setError(null);
            if (email.isEmpty()) {
                emailInput.setError("Informe o e-mail");
                emailInput.requestFocus();
                return;
            }
            if (!PasswordUtils.isValidEmail(email)) {
                emailInput.setError("E-mail inválido");
                emailInput.requestFocus();
                return;
            }
            // Recuperação por e-mail exige backend (adiado). Deixamos claro que é uma simulação.
            new AlertDialog.Builder(this)
                    .setTitle("Recuperação de senha (simulação)")
                    .setMessage("Este aplicativo funciona apenas localmente, sem servidor de e-mail. "
                            + "Em uma versão com backend, enviaríamos as instruções para "
                            + email + ".\n\nPor enquanto, esta etapa é apenas demonstrativa.")
                    .setPositiveButton("Entendi", (d, w) -> finish())
                    .show();
        });

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
    }
}

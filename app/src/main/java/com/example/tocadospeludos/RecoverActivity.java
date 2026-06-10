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
                emailInput.setError(getString(R.string.err_email_required));
                emailInput.requestFocus();
                return;
            }
            if (!PasswordUtils.isValidEmail(email)) {
                emailInput.setError(getString(R.string.err_email_invalid));
                emailInput.requestFocus();
                return;
            }
            // Recuperação por e-mail exige backend (adiado). Deixamos claro que é uma simulação.
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dlg_recover_sim))
                    .setMessage(getString(R.string.msg_recover_sim, email))
                    .setPositiveButton(getString(R.string.dialog_understood), (d, w) -> finish())
                    .show();
        });

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
    }
}

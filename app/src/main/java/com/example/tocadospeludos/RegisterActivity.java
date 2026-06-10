package com.example.tocadospeludos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
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
        EditText cnpjInput = findViewById(R.id.etCnpj);
        EditText emailInput = findViewById(R.id.etEmail);
        EditText passwordInput = findViewById(R.id.etPassword);
        EditText confirmInput = findViewById(R.id.etConfirmPassword);
        RadioGroup accountType = findViewById(R.id.rgAccountType);
        Button registerButton = findViewById(R.id.btnRegister);

        // Alterna os campos conforme o tipo de conta selecionado.
        accountType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isOng = checkedId == R.id.rbOng;
            cnpjInput.setVisibility(isOng ? View.VISIBLE : View.GONE);
            nameInput.setHint(isOng ? "Nome da ONG" : "Nome");
        });

        findViewById(R.id.tvLogin).setOnClickListener(v -> finish());

        registerButton.setOnClickListener(v -> {
            boolean isOng = accountType.getCheckedRadioButtonId() == R.id.rbOng;
            String name = nameInput.getText().toString().trim();
            String cnpj = cnpjInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmInput.getText().toString();

            // Limpa erros anteriores.
            nameInput.setError(null);
            cnpjInput.setError(null);
            emailInput.setError(null);
            passwordInput.setError(null);
            confirmInput.setError(null);

            boolean valid = true;
            View firstError = null;

            if (name.isEmpty()) {
                nameInput.setError(isOng ? "Informe o nome da ONG" : "Informe o nome");
                firstError = nameInput;
                valid = false;
            }

            if (isOng) {
                if (cnpj.isEmpty()) {
                    cnpjInput.setError("Informe o CNPJ da ONG");
                    if (firstError == null) firstError = cnpjInput;
                    valid = false;
                } else if (!PasswordUtils.isValidCnpj(cnpj)) {
                    cnpjInput.setError("CNPJ inválido (14 dígitos)");
                    if (firstError == null) firstError = cnpjInput;
                    valid = false;
                }
            }

            if (email.isEmpty()) {
                emailInput.setError("Informe o e-mail");
                if (firstError == null) firstError = emailInput;
                valid = false;
            } else if (!PasswordUtils.isValidEmail(email)) {
                emailInput.setError("E-mail inválido");
                if (firstError == null) firstError = emailInput;
                valid = false;
            } else if (UserStorage.userExists(this, email)) {
                emailInput.setError("E-mail já cadastrado");
                if (firstError == null) firstError = emailInput;
                valid = false;
            }

            if (password.isEmpty()) {
                passwordInput.setError("Informe a senha");
                if (firstError == null) firstError = passwordInput;
                valid = false;
            } else if (!PasswordUtils.isStrongPassword(password)) {
                passwordInput.setError("Mínimo 8 caracteres, com letras e números");
                if (firstError == null) firstError = passwordInput;
                valid = false;
            }

            if (!password.equals(confirmPassword)) {
                confirmInput.setError("As senhas não coincidem");
                if (firstError == null) firstError = confirmInput;
                valid = false;
            }

            if (!valid) {
                if (firstError != null) firstError.requestFocus();
                return;
            }

            String type = isOng ? UserStorage.TYPE_ONG : UserStorage.TYPE_ADOPTER;
            if (UserStorage.registerUser(this, name, email, password, type, isOng ? cnpj : null)) {
                UserStorage.login(this, email, password);
                Toast.makeText(this, "Conta criada com sucesso", Toast.LENGTH_SHORT).show();
                Class<?> home = isOng ? OngHomeActivity.class : HomeActivity.class;
                startActivity(new Intent(this, home));
                finish();
            } else {
                Toast.makeText(this, "Falha ao criar conta", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

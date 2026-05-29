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

public class DocumentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_documents);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String email = UserStorage.getCurrentUserEmail(this);
        EditText idInput = findViewById(R.id.etIdDocument);
        EditText proofInput = findViewById(R.id.etProofOfResidence);
        EditText declarationInput = findViewById(R.id.etDeclaration);
        EditText authorizationInput = findViewById(R.id.etAuthorization);
        Button backButton = findViewById(R.id.btnBackDocuments);
        Button submitButton = findViewById(R.id.btnSubmitDocuments);

        backButton.setOnClickListener(v -> finish());

        submitButton.setOnClickListener(v -> {
            String idDocument = idInput.getText().toString().trim();
            String proof = proofInput.getText().toString().trim();
            String declaration = declarationInput.getText().toString().trim();
            String authorization = authorizationInput.getText().toString().trim();

            if (idDocument.isEmpty() || proof.isEmpty() || declaration.isEmpty() || authorization.isEmpty()) {
                Toast.makeText(this, "Preencha todos os documentos", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean saved = UserStorage.saveUserDocuments(this, email, idDocument, proof, declaration, authorization);
            if (saved) {
                Toast.makeText(this, "Documentos salvos com sucesso", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Falha ao salvar documentos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

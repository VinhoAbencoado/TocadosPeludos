package com.example.tocadospeludos;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CreateAnimalActivity extends AppCompatActivity {

    private static final String[] ACCEPTED_TYPES = {"image/*"};

    private Uri photoUri;
    private TextView photoName;
    private ActivityResultLauncher<String[]> photoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isCurrentUserOng(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_animal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText nameInput = findViewById(R.id.etAnimalName);
        EditText speciesInput = findViewById(R.id.etAnimalSpecies);
        EditText descriptionInput = findViewById(R.id.etAnimalDescription);
        photoName = findViewById(R.id.tvPhotoName);

        photoPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            photoUri = persist(uri);
            photoName.setText(photoUri == null ? "Nenhuma foto anexada" : "Anexada: " + getDisplayName(photoUri));
        });

        findViewById(R.id.btnAttachPhoto).setOnClickListener(v -> photoPicker.launch(ACCEPTED_TYPES));
        findViewById(R.id.btnBackAnimal).setOnClickListener(v -> finish());

        findViewById(R.id.btnSaveAnimal).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String species = speciesInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (name.isEmpty() || species.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Preencha nome, espécie e descrição", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = UserStorage.getCurrentUserEmail(this);
            String org = UserStorage.getCurrentUserName(this);
            String photo = photoUri != null ? photoUri.toString() : "";
            if (AppData.addAnimal(this, name, species, description, photo, email, org)) {
                Toast.makeText(this, "Animal cadastrado", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Falha ao cadastrar animal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Uri persist(Uri uri) {
        if (uri == null) {
            return null;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // segue sem permissão persistente
        }
        return uri;
    }

    private String getDisplayName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            // usa fallback
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        return name != null ? name : "foto";
    }
}

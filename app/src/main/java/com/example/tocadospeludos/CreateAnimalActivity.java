package com.example.tocadospeludos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
    private static final String STATE_PHOTO_URI = "state_photo_uri";

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

        // Restaura a foto selecionada após rotação.
        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString(STATE_PHOTO_URI, null);
            if (saved != null) {
                photoUri = Uri.parse(saved);
            }
        }
        updatePhotoLabel();

        photoPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            photoUri = persist(uri);
            updatePhotoLabel();
        });

        findViewById(R.id.btnAttachPhoto).setOnClickListener(v -> photoPicker.launch(ACCEPTED_TYPES));
        findViewById(R.id.btnBackAnimal).setOnClickListener(v -> finish());

        findViewById(R.id.btnSaveAnimal).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String species = speciesInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (name.isEmpty() || species.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_animal_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            String email = UserStorage.getCurrentUserEmail(this);
            String org = UserStorage.getCurrentUserName(this);
            String photo = photoUri != null ? photoUri.toString() : "";
            if (AppData.addAnimal(this, name, species, description, photo, email, org)) {
                Toast.makeText(this, getString(R.string.toast_animal_created), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, getString(R.string.toast_animal_create_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // EditTexts com id são salvos automaticamente; aqui guardamos só a foto escolhida.
        if (photoUri != null) {
            outState.putString(STATE_PHOTO_URI, photoUri.toString());
        }
    }

    private void updatePhotoLabel() {
        if (photoName != null) {
            photoName.setText(photoUri == null
                    ? getString(R.string.no_photo_attached)
                    : getString(R.string.photo_attached));
        }
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
}

package com.example.tocadospeludos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Tela de detalhe do animal (visão do adotante): foto, descrição completa e
 * botão para se candidatar à adoção.
 */
public class AnimalDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ANIMAL_ID = "extra_animal_id";

    private static final int PHOTO_MAX_PX = 1024;

    private Animal animal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_animal_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.animalDetailRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String animalId = getIntent().getStringExtra(EXTRA_ANIMAL_ID);
        animal = AppData.getAnimalById(this, animalId);
        if (animal == null) {
            Toast.makeText(this, getString(R.string.toast_animal_unavailable), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btnBackAnimalDetail).setOnClickListener(v -> finish());

        bind();
    }

    private void bind() {
        ImageView photo = findViewById(R.id.ivAnimalPhoto);
        TextView name = findViewById(R.id.tvAnimalName);
        TextView species = findViewById(R.id.tvAnimalSpecies);
        TextView org = findViewById(R.id.tvAnimalOrg);
        TextView status = findViewById(R.id.tvAnimalStatus);
        TextView description = findViewById(R.id.tvAnimalDescription);
        Button apply = findViewById(R.id.btnApplyAdoption);

        name.setText(animal.getName());
        species.setText(animal.getSpecies());
        if (animal.getOwnerOrg().isEmpty() && animal.getOwnerEmail().isEmpty()) {
            org.setVisibility(View.GONE);
        } else {
            String orgName = animal.getOwnerOrg().isEmpty() ? "ONG" : animal.getOwnerOrg();
            org.setText(getString(R.string.card_by_org, orgName));
            org.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
            org.setOnClickListener(v -> {
                Intent intent = new Intent(this, OngProfileActivity.class);
                intent.putExtra(OngProfileActivity.EXTRA_ONG_EMAIL, animal.getOwnerEmail());
                intent.putExtra(OngProfileActivity.EXTRA_ONG_NAME, animal.getOwnerOrg());
                startActivity(intent);
            });
        }
        description.setText(animal.getDescription());

        Bitmap bitmap = ImageUtils.loadBitmap(this, animal.getPhotoUri(), PHOTO_MAX_PX);
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
        } else {
            photo.setImageResource(R.drawable.ico_pata);
            photo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        boolean available = animal.isAvailable();
        status.setText(available ? "Disponível" : "Adotado");
        status.setBackgroundResource(available ? R.drawable.bg_chip_done : R.drawable.bg_chip_pending);
        status.setTextColor(ContextCompat.getColor(this,
                available ? R.color.status_done : R.color.text_secondary));

        String email = UserStorage.getCurrentUserEmail(this);
        boolean alreadyApplied = AppData.hasApplied(this, animal.getId(), email);

        if (!available) {
            apply.setText(getString(R.string.card_animal_already_adopted));
            apply.setEnabled(false);
        } else if (alreadyApplied) {
            apply.setText(getString(R.string.card_application_sent));
            apply.setEnabled(false);
        } else {
            apply.setText(getString(R.string.card_apply));
            apply.setEnabled(true);
            apply.setOnClickListener(v -> applyForAdoption(apply));
        }
    }

    private void applyForAdoption(Button apply) {
        String email = UserStorage.getCurrentUserEmail(this);
        String adopterName = UserStorage.getCurrentUserName(this);

        // Estimula o adotante a preencher os documentos antes (a ONG os avaliará).
        if (!UserStorage.hasUserDocuments(this, email)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dlg_docs_incomplete))
                    .setMessage(getString(R.string.msg_docs_incomplete))
                    .setPositiveButton(getString(R.string.btn_apply_anyway), (d, w) -> doApply(email, adopterName, apply))
                    .setNegativeButton(getString(R.string.dialog_cancel), null)
                    .show();
            return;
        }
        doApply(email, adopterName, apply);
    }

    private void doApply(String email, String adopterName, Button apply) {
        if (AppData.addApplication(this, animal, email, adopterName)) {
            Toast.makeText(this, getString(R.string.toast_application_sent_to, animal.getOwnerOrg()), Toast.LENGTH_SHORT).show();
            apply.setText(getString(R.string.card_application_sent));
            apply.setEnabled(false);
        } else {
            Toast.makeText(this, getString(R.string.toast_already_applied), Toast.LENGTH_SHORT).show();
        }
    }
}

package com.example.tocadospeludos;

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
            Toast.makeText(this, "Animal indisponível", Toast.LENGTH_SHORT).show();
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
        if (animal.getOwnerOrg().isEmpty()) {
            org.setVisibility(View.GONE);
        } else {
            org.setText("Por " + animal.getOwnerOrg());
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
            apply.setText("Animal já adotado");
            apply.setEnabled(false);
        } else if (alreadyApplied) {
            apply.setText("Candidatura enviada");
            apply.setEnabled(false);
        } else {
            apply.setText("Candidatar-se à adoção");
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
                    .setTitle("Documentos incompletos")
                    .setMessage("Seus documentos de adoção ainda não estão completos. "
                            + "Você pode se candidatar mesmo assim, mas a ONG poderá pedir os documentos antes de aprovar.")
                    .setPositiveButton("Candidatar mesmo assim", (d, w) -> doApply(email, adopterName, apply))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        doApply(email, adopterName, apply);
    }

    private void doApply(String email, String adopterName, Button apply) {
        if (AppData.addApplication(this, animal, email, adopterName)) {
            Toast.makeText(this, "Candidatura enviada para " + animal.getOwnerOrg(), Toast.LENGTH_SHORT).show();
            apply.setText("Candidatura enviada");
            apply.setEnabled(false);
        } else {
            Toast.makeText(this, "Você já se candidatou a este animal", Toast.LENGTH_SHORT).show();
        }
    }
}

package com.example.tocadospeludos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

/**
 * Perfil público da ONG, visível ao adotante a partir do detalhe do animal.
 * Mostra contato e os animais disponíveis daquela ONG.
 */
public class OngProfileActivity extends AppCompatActivity {

    public static final String EXTRA_ONG_EMAIL = "extra_ong_email";
    public static final String EXTRA_ONG_NAME = "extra_ong_name";

    private String ongEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ong_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ongProfileRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ongEmail = getIntent().getStringExtra(EXTRA_ONG_EMAIL);
        String ongName = getIntent().getStringExtra(EXTRA_ONG_NAME);

        // Prefere o nome atual do cadastro; cai para o nome vindo do intent.
        String storedName = UserStorage.getName(this, ongEmail);
        if (storedName != null && !storedName.isEmpty()) {
            ongName = storedName;
        }

        TextView name = findViewById(R.id.tvProfileOngName);
        TextView emailView = findViewById(R.id.tvProfileOngEmail);
        TextView phoneView = findViewById(R.id.tvProfileOngPhone);

        name.setText(ongName != null && !ongName.isEmpty() ? ongName : "ONG");
        emailView.setText(ongEmail != null && !ongEmail.isEmpty() ? ongEmail : "Contato não informado");
        String phone = UserStorage.getPhone(this, ongEmail);
        phoneView.setText(getString(R.string.card_phone_label, phone != null && !phone.isEmpty() ? phone : getString(R.string.value_not_informed)));

        findViewById(R.id.btnBackOngProfile).setOnClickListener(v -> finish());

        renderAnimals();
    }

    private void renderAnimals() {
        LinearLayout container = findViewById(R.id.ongAnimalsContainer);
        TextView empty = findViewById(R.id.tvNoOngAnimals);
        container.removeAllViews();

        List<Animal> animals = AppData.getAnimalsByOwner(this, ongEmail);
        int shown = 0;
        for (Animal animal : animals) {
            if (animal.isAvailable()) {
                container.addView(buildAnimalCard(animal));
                shown++;
            }
        }
        empty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
    }

    private View buildAnimalCard(Animal animal) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalDetailActivity.class);
            intent.putExtra(AnimalDetailActivity.EXTRA_ANIMAL_ID, animal.getId());
            startActivity(intent);
        });

        Bitmap photo = ImageUtils.loadBitmap(this, animal.getPhotoUri(), 600);
        if (photo != null) {
            ImageView image = new ImageView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(150));
            ip.setMargins(0, 0, 0, dp(10));
            image.setLayoutParams(ip);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setImageBitmap(photo);
            card.addView(image);
        }

        TextView title = new TextView(this);
        title.setText(animal.getName() + "  (" + animal.getSpecies() + ")");
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        card.addView(title);

        TextView desc = new TextView(this);
        desc.setText(animal.getDescription());
        desc.setTextSize(14);
        desc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams dpr = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dpr.setMargins(0, dp(6), 0, 0);
        desc.setLayoutParams(dpr);
        card.addView(desc);

        return card;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

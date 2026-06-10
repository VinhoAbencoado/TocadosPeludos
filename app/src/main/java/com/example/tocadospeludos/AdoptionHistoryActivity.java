package com.example.tocadospeludos;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Histórico de adoções concluídas (candidaturas aprovadas) da ONG logada.
 */
public class AdoptionHistoryActivity extends AppCompatActivity {

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isCurrentUserOng(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adoption_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adoptionHistoryRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = UserStorage.getCurrentUserEmail(this);
        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        LinearLayout container = findViewById(R.id.historyContainer);
        TextView empty = findViewById(R.id.tvNoHistory);
        container.removeAllViews();

        JSONArray approved = AppData.getApprovedApplicationsForOwner(this, email);
        empty.setVisibility(approved.length() == 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < approved.length(); i++) {
            JSONObject app = approved.optJSONObject(i);
            if (app != null) {
                container.addView(buildCard(app));
            }
        }
    }

    private View buildCard(JSONObject app) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(app.optString("animalName", "Animal")
                + "  (" + app.optString("animalSpecies", "") + ")");
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        card.addView(title);

        TextView adopter = new TextView(this);
        adopter.setText(getString(R.string.card_adopted_by, app.optString("adopterName", "-")));
        adopter.setTextSize(14);
        adopter.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.setMargins(0, dp(6), 0, 0);
        adopter.setLayoutParams(ap);
        card.addView(adopter);

        TextView contact = new TextView(this);
        contact.setText(app.optString("adopterEmail", ""));
        contact.setTextSize(13);
        contact.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        card.addView(contact);

        TextView chip = new TextView(this);
        chip.setText(getString(R.string.card_adoption_done));
        chip.setTextSize(12);
        chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
        chip.setTextColor(ContextCompat.getColor(this, R.color.status_done));
        chip.setBackgroundResource(R.drawable.bg_chip_done);
        chip.setPadding(dp(12), dp(5), dp(12), dp(5));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, dp(10), 0, 0);
        chip.setLayoutParams(cp);
        card.addView(chip);

        return card;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

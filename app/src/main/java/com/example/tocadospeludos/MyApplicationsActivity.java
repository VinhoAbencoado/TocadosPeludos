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
 * Lista as candidaturas de adoção do adotante logado, com o status atual
 * (pendente/aprovada/recusada) atribuído pela ONG.
 */
public class MyApplicationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_applications);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myApplicationsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnBackApplications).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        LinearLayout container = findViewById(R.id.applicationsAdopterContainer);
        TextView empty = findViewById(R.id.tvNoApplicationsAdopter);
        container.removeAllViews();

        JSONArray apps = AppData.getApplicationsForAdopter(this, UserStorage.getCurrentUserEmail(this));
        empty.setVisibility(apps.length() == 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.optJSONObject(i);
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

        TextView org = new TextView(this);
        org.setText(getString(R.string.card_ong_label, app.optString("ownerOrg", "-")));
        org.setTextSize(13);
        org.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.setMargins(0, dp(4), 0, dp(10));
        org.setLayoutParams(op);
        card.addView(org);

        String status = app.optString("status", AppData.STATUS_PENDING);
        TextView statusChip = new TextView(this);
        statusChip.setText(statusLabel(status));
        statusChip.setTextSize(13);
        statusChip.setTypeface(statusChip.getTypeface(), android.graphics.Typeface.BOLD);
        statusChip.setTextColor(ContextCompat.getColor(this, statusColor(status)));
        statusChip.setBackgroundResource(statusChipBg(status));
        statusChip.setPadding(dp(12), dp(5), dp(12), dp(5));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusChip.setLayoutParams(sp);
        card.addView(statusChip);

        TextView hint = new TextView(this);
        hint.setText(statusHint(status));
        hint.setTextSize(13);
        hint.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hp.setMargins(0, dp(10), 0, 0);
        hint.setLayoutParams(hp);
        card.addView(hint);

        return card;
    }

    private String statusLabel(String status) {
        switch (status) {
            case AppData.STATUS_APPROVED:
                return "Aprovada";
            case AppData.STATUS_REJECTED:
                return "Recusada";
            default:
                return "Pendente";
        }
    }

    private String statusHint(String status) {
        switch (status) {
            case AppData.STATUS_APPROVED:
                return "Parabéns! A ONG aprovou sua candidatura. Entre em contato para combinar a adoção.";
            case AppData.STATUS_REJECTED:
                return "A ONG não pôde seguir com esta candidatura desta vez.";
            default:
                return "Sua candidatura está em análise pela ONG.";
        }
    }

    private int statusColor(String status) {
        switch (status) {
            case AppData.STATUS_APPROVED:
                return R.color.status_done;
            case AppData.STATUS_REJECTED:
                return R.color.danger;
            default:
                return R.color.status_pending;
        }
    }

    private int statusChipBg(String status) {
        return AppData.STATUS_APPROVED.equals(status)
                ? R.drawable.bg_chip_done
                : R.drawable.bg_chip_pending;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

package com.example.tocadospeludos;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tela da ONG para validar a presença (check-in) dos adotantes em um evento.
 * Lê o QR Code a partir de uma imagem (ZXing core, sem câmera/sem nova dependência)
 * ou valida o código digitado, e lista os participantes inscritos.
 */
public class CheckInActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private static final String[] ACCEPTED_TYPES = {"image/*"};

    private String eventId;
    private ActivityResultLauncher<String[]> qrPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isCurrentUserOng(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_check_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.checkInRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        Event event = AppData.getEventById(this, eventId);
        if (event == null) {
            Toast.makeText(this, getString(R.string.toast_event_unavailable), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // A ONG só faz check-in dos próprios eventos.
        if (!UserStorage.getCurrentUserEmail(this).equalsIgnoreCase(event.getOwnerEmail())) {
            Toast.makeText(this, getString(R.string.toast_event_not_yours), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView title = findViewById(R.id.tvCheckInEventTitle);
        title.setText(event.getTitle());

        qrPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                handleQrImage(uri);
            }
        });

        findViewById(R.id.btnBackCheckIn).setOnClickListener(v -> finish());
        findViewById(R.id.btnScanQr).setOnClickListener(v -> qrPicker.launch(ACCEPTED_TYPES));

        EditText manualCode = findViewById(R.id.etManualCode);
        findViewById(R.id.btnValidateCode).setOnClickListener(v -> {
            String code = manualCode.getText().toString().trim();
            if (code.isEmpty()) {
                manualCode.setError(getString(R.string.err_code_required));
                return;
            }
            validateCode(code);
            manualCode.setText("");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderParticipants();
    }

    private void handleQrImage(Uri uri) {
        String qrText = QRCodeReader.decodeFromUri(this, uri);
        if (qrText == null) {
            Toast.makeText(this, getString(R.string.toast_qr_read_failed), Toast.LENGTH_LONG).show();
            return;
        }
        String code = QRCodeReader.extractTicketCode(qrText);
        validateCode(code);
    }

    private void validateCode(String code) {
        JSONObject participant = UserStorage.checkInByCode(this, eventId, code);
        if (participant != null) {
            Toast.makeText(this, getString(R.string.toast_checkin_confirmed,
                    participant.optString("adopterName", "adotante")), Toast.LENGTH_LONG).show();
            renderParticipants();
        } else {
            Toast.makeText(this, getString(R.string.toast_code_no_match), Toast.LENGTH_LONG).show();
        }
    }

    private void renderParticipants() {
        LinearLayout container = findViewById(R.id.participantsContainer);
        TextView empty = findViewById(R.id.tvNoParticipants);
        container.removeAllViews();

        JSONArray participants = UserStorage.getEventParticipants(this, eventId);
        empty.setVisibility(participants.length() == 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < participants.length(); i++) {
            JSONObject p = participants.optJSONObject(i);
            if (p != null) {
                container.addView(buildParticipantCard(p));
            }
        }
    }

    private View buildParticipantCard(JSONObject participant) {
        final String adopterEmail = participant.optString("adopterEmail", "");
        final boolean checkedIn = participant.optBoolean("checkedIn", false);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setText(participant.optString("adopterName", "Adotante"));
        name.setTextSize(17);
        name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
        name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        card.addView(name);

        TextView email = new TextView(this);
        email.setText(adopterEmail);
        email.setTextSize(13);
        email.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        card.addView(email);

        TextView code = new TextView(this);
        code.setText("Código: " + participant.optString("code", "-"));
        code.setTextSize(13);
        code.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, dp(2), 0, dp(10));
        code.setLayoutParams(cp);
        card.addView(code);

        TextView status = new TextView(this);
        status.setText(checkedIn ? "Presente" : "Aguardando check-in");
        status.setTextSize(12);
        status.setTypeface(status.getTypeface(), android.graphics.Typeface.BOLD);
        status.setTextColor(ContextCompat.getColor(this, checkedIn ? R.color.status_done : R.color.status_pending));
        status.setBackgroundResource(checkedIn ? R.drawable.bg_chip_done : R.drawable.bg_chip_pending);
        status.setPadding(dp(12), dp(5), dp(12), dp(5));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        status.setLayoutParams(sp);
        card.addView(status);

        Button toggle = new Button(this);
        toggle.setAllCaps(false);
        toggle.setTypeface(toggle.getTypeface(), android.graphics.Typeface.BOLD);
        toggle.setText(checkedIn ? "Desfazer check-in" : "Marcar presença");
        toggle.setBackgroundResource(R.drawable.bg_button_outline);
        toggle.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        toggle.setMinHeight(dp(44));
        toggle.setStateListAnimator(null);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(0, dp(10), 0, 0);
        toggle.setLayoutParams(tp);
        toggle.setOnClickListener(v -> {
            UserStorage.setEventCheckIn(this, adopterEmail, eventId, !checkedIn);
            renderParticipants();
        });
        card.addView(toggle);

        return card;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

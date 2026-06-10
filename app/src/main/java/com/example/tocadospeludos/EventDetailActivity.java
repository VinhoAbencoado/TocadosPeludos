package com.example.tocadospeludos;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.util.UUID;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT = "extra_event";

    private static final int QR_SIZE_PX = 600;

    private Event event;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.eventDetailRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        event = (Event) getIntent().getSerializableExtra(EXTRA_EVENT);
        if (event == null) {
            Toast.makeText(this, "Evento indisponível", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView title = findViewById(R.id.tvEventTitle);
        TextView date = findViewById(R.id.tvEventDate);
        TextView location = findViewById(R.id.tvEventLocation);
        TextView description = findViewById(R.id.tvEventDescription);

        title.setText(event.getTitle());
        date.setText(event.getDate());
        location.setText(event.getLocation());
        description.setText(event.getDescription());

        Button backButton = findViewById(R.id.btnBackEvent);
        backButton.setOnClickListener(v -> finish());

        Button registerButton = findViewById(R.id.btnRegisterEvent);
        registerButton.setOnClickListener(v -> register());

        String email = UserStorage.getCurrentUserEmail(this);
        if (UserStorage.isRegisteredForEvent(this, email, event.getId())) {
            JSONObject reg = findRegistration(email);
            if (reg != null) {
                showTicket(reg.optString("code", ""), reg.optString("qrContent", ""));
                registerButton.setText("Você já está inscrito");
                registerButton.setEnabled(false);
            }
        }
    }

    private void register() {
        String email = UserStorage.getCurrentUserEmail(this);
        String code = "TP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrContent = buildQrContent(email, code);

        JSONObject reg = UserStorage.registerForEvent(this, email, event, code, qrContent);
        if (reg == null) {
            Toast.makeText(this, "Não foi possível concluir a inscrição", Toast.LENGTH_SHORT).show();
            return;
        }

        // Usa os dados retornados (cobre o caso de já existir uma inscrição anterior).
        String savedCode = reg.optString("code", code);
        String savedQr = reg.optString("qrContent", qrContent);
        showTicket(savedCode, savedQr);

        Button registerButton = findViewById(R.id.btnRegisterEvent);
        registerButton.setText("Você já está inscrito");
        registerButton.setEnabled(false);

        Toast.makeText(this, "Inscrição confirmada! QR Code salvo no seu perfil.", Toast.LENGTH_LONG).show();
    }

    private String buildQrContent(String email, String code) {
        // Conteúdo legível por leitores de QR; identifica evento, participante e código do ingresso.
        return "TOCADOSPELUDOS\n"
                + "Evento: " + event.getTitle() + "\n"
                + "Data: " + event.getDate() + "\n"
                + "Participante: " + email + "\n"
                + "Codigo: " + code;
    }

    private void showTicket(String code, String qrContent) {
        LinearLayout ticketContainer = findViewById(R.id.ticketContainer);
        ImageView qrImage = findViewById(R.id.ivQrCode);
        TextView ticketCode = findViewById(R.id.tvTicketCode);

        Bitmap qr = QRCodeGenerator.generate(qrContent, QR_SIZE_PX);
        if (qr != null) {
            qrImage.setImageBitmap(qr);
        }
        ticketCode.setText("Código: " + code);
        ticketContainer.setVisibility(View.VISIBLE);
    }

    private JSONObject findRegistration(String email) {
        org.json.JSONArray registrations = UserStorage.getEventRegistrations(this, email);
        for (int i = 0; i < registrations.length(); i++) {
            JSONObject reg = registrations.optJSONObject(i);
            if (reg != null && event.getId().equals(reg.optString("eventId", ""))) {
                return reg;
            }
        }
        return null;
    }
}

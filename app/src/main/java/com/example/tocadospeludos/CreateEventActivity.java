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

public class CreateEventActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isCurrentUserOng(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText titleInput = findViewById(R.id.etEventTitle);
        EditText dateInput = findViewById(R.id.etEventDate);
        EditText locationInput = findViewById(R.id.etEventLocation);
        EditText descriptionInput = findViewById(R.id.etEventDescription);

        findViewById(R.id.btnBackEvent).setOnClickListener(v -> finish());

        findViewById(R.id.btnSaveEvent).setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String date = dateInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (title.isEmpty() || date.isEmpty() || location.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = UserStorage.getCurrentUserEmail(this);
            String org = UserStorage.getCurrentUserName(this);
            if (AppData.addEvent(this, title, description, date, location, email, org)) {
                Toast.makeText(this, "Evento publicado", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Falha ao publicar evento", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

package com.example.tocadospeludos;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class CreateEventActivity extends AppCompatActivity {

    /** Quando presente, a tela entra em modo de edição do evento informado. */
    public static final String EXTRA_EVENT_ID = "extra_event_id";
    private static final String STATE_DATE_MILLIS = "state_date_millis";

    private long selectedDateMillis = 0L;
    private String editingEventId;

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
        TextView screenTitle = findViewById(R.id.tvCreateEventTitle);
        Button saveButton = findViewById(R.id.btnSaveEvent);

        // Campo de data: escolhido por DatePicker, não digitado.
        dateInput.setFocusable(false);
        dateInput.setClickable(true);
        dateInput.setOnClickListener(v -> showDatePicker(dateInput));

        findViewById(R.id.btnBackEvent).setOnClickListener(v -> finish());

        // Modo edição: pré-preenche os campos.
        editingEventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (editingEventId != null) {
            Event event = AppData.getEventById(this, editingEventId);
            if (event == null) {
                Toast.makeText(this, getString(R.string.toast_event_unavailable), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (screenTitle != null) screenTitle.setText(getString(R.string.card_edit_event));
            saveButton.setText(getString(R.string.card_save_changes));
            titleInput.setText(event.getTitle());
            descriptionInput.setText(event.getDescription());
            locationInput.setText(event.getLocation());
            dateInput.setText(event.getDate());
            selectedDateMillis = event.getDateMillis();
        }

        // Restaura a data escolhida após rotação (o texto do campo é restaurado automaticamente).
        if (savedInstanceState != null) {
            selectedDateMillis = savedInstanceState.getLong(STATE_DATE_MILLIS, selectedDateMillis);
        }

        saveButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String date = dateInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (title.isEmpty() || date.isEmpty() || location.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_event_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok;
            if (editingEventId != null) {
                ok = AppData.updateEvent(this, editingEventId, title, description, date, location, selectedDateMillis);
                if (ok) Toast.makeText(this, getString(R.string.toast_event_updated), Toast.LENGTH_SHORT).show();
            } else {
                String email = UserStorage.getCurrentUserEmail(this);
                String org = UserStorage.getCurrentUserName(this);
                ok = AppData.addEvent(this, title, description, date, location, email, org, selectedDateMillis);
                if (ok) Toast.makeText(this, getString(R.string.toast_event_published), Toast.LENGTH_SHORT).show();
            }
            if (ok) {
                finish();
            } else {
                Toast.makeText(this, getString(R.string.toast_event_save_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_DATE_MILLIS, selectedDateMillis);
    }

    private void showDatePicker(EditText dateInput) {
        Calendar c = Calendar.getInstance();
        if (selectedDateMillis > 0L) {
            c.setTimeInMillis(selectedDateMillis);
        }
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateMillis = DateUtils.atStartOfDay(year, month, dayOfMonth);
            dateInput.setText(DateUtils.formatDate(selectedDateMillis));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        // Não permite escolher datas passadas.
        dialog.getDatePicker().setMinDate(DateUtils.startOfToday());
        dialog.show();
    }
}

package com.example.tocadospeludos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class OngHomeActivity extends AppCompatActivity {

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }
        // Conta de adotante não acessa o painel da ONG.
        if (!UserStorage.isCurrentUserOng(this)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ong_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = UserStorage.getCurrentUserEmail(this);

        String orgName = UserStorage.getCurrentUserName(this);
        TextView welcome = findViewById(R.id.tvOngWelcome);
        if (orgName != null) {
            welcome.setText(orgName);
        }

        View eventsContent = findViewById(R.id.eventsContent);
        View animalsContent = findViewById(R.id.animalsContent);
        View applicationsContent = findViewById(R.id.applicationsContent);
        View accountContent = findViewById(R.id.accountContent);

        BottomNavigationView nav = findViewById(R.id.bottomNavigationOng);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            eventsContent.setVisibility(id == R.id.nav_events ? View.VISIBLE : View.GONE);
            animalsContent.setVisibility(id == R.id.nav_animals ? View.VISIBLE : View.GONE);
            applicationsContent.setVisibility(id == R.id.nav_applications ? View.VISIBLE : View.GONE);
            accountContent.setVisibility(id == R.id.nav_account ? View.VISIBLE : View.GONE);
            return true;
        });
        nav.setSelectedItemId(R.id.nav_events);

        findViewById(R.id.btnNewEvent).setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));
        findViewById(R.id.btnNewAnimal).setOnClickListener(v ->
                startActivity(new Intent(this, CreateAnimalActivity.class)));

        findViewById(R.id.btnEditOng).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.btnChangePasswordOng).setOnClickListener(v -> showChangePasswordDialog());
        findViewById(R.id.btnAdoptionHistory).setOnClickListener(v ->
                startActivity(new Intent(this, AdoptionHistoryActivity.class)));

        findViewById(R.id.btnLogoutOng).setOnClickListener(v -> confirmLogout());
        findViewById(R.id.btnDeleteAccountOng).setOnClickListener(v -> confirmDeleteAccount());

        setupAccountInfo();
    }

    private void showEditProfileDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), 0);

        EditText nameField = new EditText(this);
        nameField.setHint(getString(R.string.hint_ong_name));
        nameField.setText(UserStorage.getCurrentUserName(this));
        layout.addView(nameField);

        EditText phoneField = new EditText(this);
        phoneField.setHint(getString(R.string.hint_phone));
        phoneField.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        String currentPhone = UserStorage.getCurrentUserPhone(this);
        if (currentPhone != null) phoneField.setText(currentPhone);
        layout.addView(phoneField);

        EditText cnpjField = new EditText(this);
        cnpjField.setHint(getString(R.string.hint_cnpj));
        cnpjField.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        String currentCnpj = UserStorage.getCnpj(this, email);
        if (currentCnpj != null) cnpjField.setText(currentCnpj);
        layout.addView(cnpjField);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_edit_ong))
                .setView(layout)
                .setPositiveButton(getString(R.string.dialog_save), null)
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameField.getText().toString().trim();
            String phone = phoneField.getText().toString().trim();
            String cnpj = cnpjField.getText().toString().trim();
            if (name.isEmpty()) {
                nameField.setError(getString(R.string.err_ong_name_required));
                return;
            }
            if (!phone.isEmpty() && !PasswordUtils.isValidPhone(phone)) {
                phoneField.setError(getString(R.string.err_phone_invalid));
                return;
            }
            if (cnpj.isEmpty() || !PasswordUtils.isValidCnpj(cnpj)) {
                cnpjField.setError(getString(R.string.err_cnpj_invalid));
                return;
            }
            if (UserStorage.updateProfile(this, email, name, phone, cnpj)) {
                Toast.makeText(this, getString(R.string.toast_data_updated), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                TextView welcome = findViewById(R.id.tvOngWelcome);
                if (welcome != null) welcome.setText(name);
                setupAccountInfo();
            } else {
                Toast.makeText(this, getString(R.string.toast_update_failed), Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), 0);

        EditText currentField = passwordField(getString(R.string.hint_current_password));
        EditText newField = passwordField(getString(R.string.hint_new_password));
        EditText confirmField = passwordField(getString(R.string.hint_confirm_new_password));
        layout.addView(currentField);
        layout.addView(newField);
        layout.addView(confirmField);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_change_password))
                .setView(layout)
                .setPositiveButton(getString(R.string.dialog_save), null)
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = currentField.getText().toString();
            String newPass = newField.getText().toString();
            String confirm = confirmField.getText().toString();
            if (!PasswordUtils.isStrongPassword(newPass)) {
                newField.setError(getString(R.string.err_password_weak));
                return;
            }
            if (!newPass.equals(confirm)) {
                confirmField.setError(getString(R.string.err_passwords_mismatch));
                return;
            }
            int result = UserStorage.changePassword(this, email, current, newPass);
            if (result == UserStorage.PASSWORD_OK) {
                Toast.makeText(this, getString(R.string.toast_password_changed), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else if (result == UserStorage.PASSWORD_WRONG_CURRENT) {
                currentField.setError(getString(R.string.err_current_password_wrong));
            } else {
                Toast.makeText(this, getString(R.string.toast_password_change_failed), Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private EditText passwordField(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return field;
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_logout))
                .setMessage(getString(R.string.msg_logout_confirm))
                .setPositiveButton(getString(R.string.dlg_logout), (d, w) -> {
                    UserStorage.logout(this);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_delete_account))
                .setMessage(getString(R.string.msg_delete_account_ong))
                .setPositiveButton(getString(R.string.dialog_delete), (d, w) -> {
                    if (UserStorage.deleteAccount(this, email)) {
                        Toast.makeText(this, getString(R.string.toast_account_deleted), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, getString(R.string.toast_account_delete_failed), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderDashboard();
        renderEvents();
        renderAnimals();
        renderApplications();
    }

    private void renderDashboard() {
        TextView events = findViewById(R.id.tvCountEvents);
        TextView animals = findViewById(R.id.tvCountAnimals);
        TextView pending = findViewById(R.id.tvCountPending);
        if (events == null) {
            return;
        }
        long now = DateUtils.startOfToday();
        events.setText(String.valueOf(AppData.countUpcomingEventsForOwner(this, email, now)));
        animals.setText(String.valueOf(AppData.countAvailableAnimals(this, email)));
        pending.setText(String.valueOf(
                AppData.countApplicationsForOwner(this, email, AppData.STATUS_PENDING)));
    }

    private void setupAccountInfo() {
        TextView name = findViewById(R.id.tvOngName);
        TextView cnpj = findViewById(R.id.tvOngCnpj);
        TextView emailView = findViewById(R.id.tvOngEmail);
        TextView phoneView = findViewById(R.id.tvOngPhone);
        String orgName = UserStorage.getCurrentUserName(this);
        String orgCnpj = UserStorage.getCnpj(this, email);
        String orgPhone = UserStorage.getPhone(this, email);
        name.setText(orgName != null ? orgName : "ONG");
        cnpj.setText("CNPJ: " + (orgCnpj != null && !orgCnpj.isEmpty() ? orgCnpj : "não informado"));
        emailView.setText(email);
        if (phoneView != null) {
            phoneView.setText(getString(R.string.card_phone_label, orgPhone != null && !orgPhone.isEmpty() ? orgPhone : getString(R.string.value_not_informed)));
        }
    }

    // ===================== EVENTOS =====================

    private void renderEvents() {
        LinearLayout container = findViewById(R.id.eventsContainer);
        TextView empty = findViewById(R.id.tvNoEvents);
        container.removeAllViews();

        List<Event> events = AppData.getEventsByOwner(this, email);
        empty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);

        for (Event event : events) {
            LinearLayout card = newCard();
            card.addView(cardTitle(event.getTitle()));
            card.addView(cardSubtitle(event.getDate() + "  •  " + event.getLocation()));
            card.addView(cardBody(event.getDescription()));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(rowParams());

            Button edit = actionButton("Editar", R.drawable.bg_button_outline, R.color.dark_green);
            edit.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateEventActivity.class);
                intent.putExtra(CreateEventActivity.EXTRA_EVENT_ID, event.getId());
                startActivity(intent);
            });
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            ep.setMargins(0, 0, dp(6), 0);
            edit.setLayoutParams(ep);

            Button checkIn = actionButton("Check-in", R.drawable.bg_button_outline, R.color.dark_green);
            checkIn.setOnClickListener(v -> {
                Intent intent = new Intent(this, CheckInActivity.class);
                intent.putExtra(CheckInActivity.EXTRA_EVENT_ID, event.getId());
                startActivity(intent);
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cp.setMargins(dp(6), 0, 0, 0);
            checkIn.setLayoutParams(cp);

            row.addView(edit);
            row.addView(checkIn);
            card.addView(row);

            Button delete = actionButton(getString(R.string.dialog_delete), R.drawable.bg_button_outline, R.color.danger);
            LinearLayout.LayoutParams dlp = rowParams();
            dlp.setMargins(0, dp(8), 0, 0);
            delete.setLayoutParams(dlp);
            delete.setOnClickListener(v -> confirmDeleteEvent(event));
            card.addView(delete);

            container.addView(card);
        }
    }

    private void confirmDeleteEvent(Event event) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_delete_event))
                .setMessage(getString(R.string.msg_delete_event, event.getTitle()))
                .setPositiveButton(getString(R.string.dialog_delete), (d, w) -> {
                    AppData.deleteEvent(this, event.getId());
                    Toast.makeText(this, getString(R.string.toast_event_deleted), Toast.LENGTH_SHORT).show();
                    renderEvents();
                })
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    // ===================== ANIMAIS =====================

    private void renderAnimals() {
        LinearLayout container = findViewById(R.id.animalsContainer);
        TextView empty = findViewById(R.id.tvNoAnimals);
        container.removeAllViews();

        List<Animal> animals = AppData.getAnimalsByOwner(this, email);
        empty.setVisibility(animals.isEmpty() ? View.VISIBLE : View.GONE);

        for (Animal animal : animals) {
            LinearLayout card = newCard();

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

            card.addView(cardTitle(animal.getName() + "  (" + animal.getSpecies() + ")"));

            boolean available = animal.isAvailable();
            TextView status = new TextView(this);
            status.setText(available ? "Disponível" : "Adotado");
            status.setTextSize(12);
            status.setTypeface(status.getTypeface(), Typeface.BOLD);
            status.setTextColor(ContextCompat.getColor(this, available ? R.color.status_done : R.color.text_secondary));
            status.setBackgroundResource(available ? R.drawable.bg_chip_done : R.drawable.bg_chip_pending);
            status.setPadding(dp(12), dp(5), dp(12), dp(5));
            LinearLayout.LayoutParams sp = wrapParams();
            sp.setMargins(0, dp(2), 0, dp(8));
            status.setLayoutParams(sp);
            card.addView(status);

            card.addView(cardBody(animal.getDescription()));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(rowParams());

            Button edit = actionButton("Editar", R.drawable.bg_button_outline, R.color.dark_green);
            edit.setOnClickListener(v -> showEditAnimalDialog(animal));
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            ep.setMargins(0, 0, dp(6), 0);
            edit.setLayoutParams(ep);

            Button toggle = actionButton(available ? "Marcar adotado" : "Reabrir",
                    R.drawable.bg_button_outline, R.color.dark_green);
            toggle.setOnClickListener(v -> {
                AppData.setAnimalStatus(this, animal.getId(),
                        available ? Animal.STATUS_ADOPTED : Animal.STATUS_AVAILABLE);
                Toast.makeText(this, available ? "Marcado como adotado" : "Animal reaberto para adoção",
                        Toast.LENGTH_SHORT).show();
                renderAnimals();
            });
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tp.setMargins(dp(6), 0, 0, 0);
            toggle.setLayoutParams(tp);

            row.addView(edit);
            row.addView(toggle);
            card.addView(row);

            Button delete = actionButton(getString(R.string.dialog_delete), R.drawable.bg_button_outline, R.color.danger);
            LinearLayout.LayoutParams dp2 = rowParams();
            dp2.setMargins(0, dp(8), 0, 0);
            delete.setLayoutParams(dp2);
            delete.setOnClickListener(v -> confirmDeleteAnimal(animal));
            card.addView(delete);

            container.addView(card);
        }
    }

    private void confirmDeleteAnimal(Animal animal) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_delete_animal))
                .setMessage(getString(R.string.msg_delete_animal, animal.getName()))
                .setPositiveButton(getString(R.string.dialog_delete), (d, w) -> {
                    AppData.deleteAnimal(this, animal.getId());
                    Toast.makeText(this, getString(R.string.toast_animal_deleted), Toast.LENGTH_SHORT).show();
                    renderAnimals();
                })
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    private void showEditAnimalDialog(Animal animal) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), 0);

        EditText nameField = new EditText(this);
        nameField.setHint(getString(R.string.hint_name));
        nameField.setText(animal.getName());
        layout.addView(nameField);

        EditText speciesField = new EditText(this);
        speciesField.setHint(getString(R.string.hint_species));
        speciesField.setText(animal.getSpecies());
        layout.addView(speciesField);

        EditText descField = new EditText(this);
        descField.setHint(getString(R.string.hint_description));
        descField.setText(animal.getDescription());
        descField.setMinLines(3);
        descField.setGravity(Gravity.TOP | Gravity.START);
        layout.addView(descField);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_edit_animal))
                .setView(layout)
                .setPositiveButton(getString(R.string.dialog_save), null)
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameField.getText().toString().trim();
            String species = speciesField.getText().toString().trim();
            String desc = descField.getText().toString().trim();
            if (name.isEmpty() || species.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_fill_animal_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            // photoUri null preserva a foto atual.
            if (AppData.updateAnimal(this, animal.getId(), name, species, desc, null)) {
                Toast.makeText(this, getString(R.string.toast_animal_updated), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                renderAnimals();
            } else {
                Toast.makeText(this, getString(R.string.toast_update_failed), Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    // ===================== CANDIDATURAS =====================

    private void renderApplications() {
        LinearLayout container = findViewById(R.id.applicationsContainer);
        TextView empty = findViewById(R.id.tvNoApplications);
        container.removeAllViews();

        JSONArray apps = AppData.getApplicationsForOwner(this, email);
        empty.setVisibility(apps.length() == 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.optJSONObject(i);
            if (app == null) {
                continue;
            }
            final String appId = app.optString("id");
            String status = app.optString("status", AppData.STATUS_PENDING);

            LinearLayout card = newCard();
            card.addView(cardTitle(app.optString("adopterName", "Adotante")));
            card.addView(cardSubtitle(app.optString("adopterEmail", "")));
            card.addView(cardBody("Animal: " + app.optString("animalName", "")
                    + " (" + app.optString("animalSpecies", "") + ")"));

            TextView statusView = new TextView(this);
            statusView.setText("Status: " + statusLabel(status));
            statusView.setTextSize(13);
            statusView.setTypeface(statusView.getTypeface(), Typeface.BOLD);
            statusView.setTextColor(ContextCompat.getColor(this, statusColor(status)));
            LinearLayout.LayoutParams stp = wrapParams();
            stp.setMargins(0, dp(4), 0, dp(8));
            statusView.setLayoutParams(stp);
            card.addView(statusView);

            final String adopterEmail = app.optString("adopterEmail", "");
            final String adopterName = app.optString("adopterName", "Adotante");
            Button viewDocs = actionButton("Ver dados do adotante", R.drawable.bg_button_outline, R.color.dark_green);
            LinearLayout.LayoutParams vp = rowParams();
            vp.setMargins(0, 0, 0, dp(8));
            viewDocs.setLayoutParams(vp);
            viewDocs.setOnClickListener(v -> showAdopterDocuments(adopterEmail, adopterName));
            card.addView(viewDocs);

            if (AppData.STATUS_PENDING.equals(status)) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(rowParams());

                Button approve = actionButton("Aprovar", R.drawable.bg_button_primary, R.color.white);
                approve.setOnClickListener(v -> {
                    AppData.setApplicationStatus(this, appId, AppData.STATUS_APPROVED);
                    Toast.makeText(this, getString(R.string.toast_application_approved), Toast.LENGTH_SHORT).show();
                    renderApplications();
                    renderAnimals();
                });
                LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                ap.setMargins(0, 0, dp(6), 0);
                approve.setLayoutParams(ap);

                Button reject = actionButton(getString(R.string.btn_reject), R.drawable.bg_button_outline, R.color.danger);
                reject.setOnClickListener(v -> new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dlg_reject_application))
                        .setMessage(getString(R.string.msg_reject_application, app.optString("adopterName", "este adotante")))
                        .setPositiveButton(getString(R.string.btn_reject), (d, w) -> {
                            AppData.setApplicationStatus(this, appId, AppData.STATUS_REJECTED);
                            Toast.makeText(this, getString(R.string.toast_application_rejected), Toast.LENGTH_SHORT).show();
                            renderApplications();
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), null)
                        .show());
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                rp.setMargins(dp(6), 0, 0, 0);
                reject.setLayoutParams(rp);

                row.addView(approve);
                row.addView(reject);
                card.addView(row);
            }

            container.addView(card);
        }
    }

    /** Mostra os dados/documentos cadastrados pelo adotante (mesmo dispositivo, armazenamento local). */
    private void showAdopterDocuments(String adopterEmail, String adopterName) {
        org.json.JSONObject docs = UserStorage.getUserDocuments(this, adopterEmail);
        StringBuilder sb = new StringBuilder();
        if (docs == null) {
            sb.append("Este adotante ainda não preencheu os documentos de adoção.");
        } else {
            appendField(sb, docs, "Nome completo", "fullName");
            appendField(sb, docs, "Nascimento", "birthDate");
            appendField(sb, docs, "CPF", "cpf");
            appendField(sb, docs, "RG", "rg");
            appendField(sb, docs, getString(R.string.hint_phone), "phone");
            appendField(sb, docs, "Endereço", "address");

            sb.append("\nAnexos:\n");
            appendAttachment(sb, docs, "Documento de identidade", "idDocument");
            appendAttachment(sb, docs, "Comprovante de residência", "proofOfResidence");
            appendAttachment(sb, docs, "Termo de responsabilidade", "declaration");
            appendAttachment(sb, docs, "Autorização do responsável", "authorization");
        }

        new AlertDialog.Builder(this)
                .setTitle(adopterName)
                .setMessage(sb.toString().trim())
                .setPositiveButton(getString(R.string.dialog_close), null)
                .show();
    }

    private void appendField(StringBuilder sb, org.json.JSONObject docs, String label, String key) {
        String value = docs.optString(key, "").trim();
        sb.append(label).append(": ").append(value.isEmpty() ? "—" : value).append("\n");
    }

    private void appendAttachment(StringBuilder sb, org.json.JSONObject docs, String label, String key) {
        String value = docs.optString(key, "").trim();
        sb.append("• ").append(label).append(": ").append(value.isEmpty() ? "não enviado" : "enviado").append("\n");
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

    // ===================== UI helpers =====================

    private LinearLayout newCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = rowParams();
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);
        return card;
    }

    private TextView cardTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams p = rowParams();
        p.setMargins(0, 0, 0, dp(4));
        tv.setLayoutParams(p);
        return tv;
    }

    private TextView cardSubtitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setLayoutParams(rowParams());
        return tv;
    }

    private TextView cardBody(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams p = rowParams();
        p.setMargins(0, dp(8), 0, dp(12));
        tv.setLayoutParams(p);
        return tv;
    }

    private Button actionButton(String text, int bgRes, int textColorRes) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setBackgroundResource(bgRes);
        b.setTextColor(ContextCompat.getColor(this, textColorRes));
        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));
        b.setPadding(dp(16), 0, dp(16), 0);
        b.setStateListAnimator(null);
        return b;
    }

    private LinearLayout.LayoutParams rowParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

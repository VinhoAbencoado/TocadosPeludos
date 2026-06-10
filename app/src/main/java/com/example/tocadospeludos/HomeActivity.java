package com.example.tocadospeludos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private String animalQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }
        // Conta de ONG usa o painel próprio.
        if (UserStorage.isCurrentUserOng(this)) {
            startActivity(new Intent(this, OngHomeActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView welcomeText = findViewById(R.id.welcomeText);
        String userName = UserStorage.getCurrentUserName(this);
        if (userName != null) {
            welcomeText.setText("Olá, " + userName + "! Bem-vindo ao seu painel");
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        View homeContent = findViewById(R.id.homeContent);
        View profileContent = findViewById(R.id.profileContent);
        View settingsContent = findViewById(R.id.settingsContent);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            homeContent.setVisibility(itemId == R.id.nav_home ? View.VISIBLE : View.GONE);
            profileContent.setVisibility(itemId == R.id.nav_profile ? View.VISIBLE : View.GONE);
            settingsContent.setVisibility(itemId == R.id.nav_settings ? View.VISIBLE : View.GONE);
            return true;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        refreshDocumentStatus();

        Button completeButton = findViewById(R.id.btnCompleteAccount);
        completeButton.setOnClickListener(v -> bottomNavigation.setSelectedItemId(R.id.nav_profile));

        Button fillDocsButton = findViewById(R.id.btnFillDocuments);
        fillDocsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, DocumentActivity.class));
        });

        Button contactButton = findViewById(R.id.btnContactSupport);
        contactButton.setOnClickListener(v -> {
            android.content.Intent emailIntent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
            emailIntent.setData(android.net.Uri.parse("mailto:suporte@tocadospeludos.com.br"));
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Contato e suporte - Toca dos Peludos");
            try {
                startActivity(android.content.Intent.createChooser(emailIntent, "Enviar e-mail para o suporte"));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "Nenhum app de e-mail encontrado. Escreva para suporte@tocadospeludos.com.br", Toast.LENGTH_LONG).show();
            }
        });

        Button editProfileButton = findViewById(R.id.btnEditProfile);
        editProfileButton.setOnClickListener(v -> showEditProfileDialog());

        Button changePasswordButton = findViewById(R.id.btnChangePassword);
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        Button myApplicationsButton = findViewById(R.id.btnMyApplications);
        myApplicationsButton.setOnClickListener(v ->
                startActivity(new Intent(this, MyApplicationsActivity.class)));

        EditText searchAnimals = findViewById(R.id.etSearchAnimals);
        searchAnimals.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                animalQuery = s.toString().trim().toLowerCase();
                renderAnimals();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        Button logoutButton = findViewById(R.id.btnLogout);
        logoutButton.setOnClickListener(v -> confirmLogout());

        Button deleteButton = findViewById(R.id.btnDeleteAccount);
        deleteButton.setOnClickListener(v -> confirmDeleteAccount());

        refreshProfileInfo();
    }

    private void refreshProfileInfo() {
        TextView nameView = findViewById(R.id.tvProfileName);
        TextView emailView = findViewById(R.id.tvProfileEmail);
        TextView phoneView = findViewById(R.id.tvProfilePhone);
        if (nameView == null) {
            return;
        }
        String name = UserStorage.getCurrentUserName(this);
        String email = UserStorage.getCurrentUserEmail(this);
        String phone = UserStorage.getCurrentUserPhone(this);
        nameView.setText(name != null ? name : "-");
        emailView.setText(email != null ? email : "-");
        phoneView.setText("Telefone: " + (phone != null && !phone.isEmpty() ? phone : "-"));
    }

    private void showEditProfileDialog() {
        String email = UserStorage.getCurrentUserEmail(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), 0);

        EditText nameField = new EditText(this);
        nameField.setHint("Nome");
        nameField.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        nameField.setText(UserStorage.getCurrentUserName(this));
        layout.addView(nameField);

        EditText phoneField = new EditText(this);
        phoneField.setHint("Telefone");
        phoneField.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        String currentPhone = UserStorage.getCurrentUserPhone(this);
        if (currentPhone != null) phoneField.setText(currentPhone);
        layout.addView(phoneField);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setView(layout)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameField.getText().toString().trim();
            String phone = phoneField.getText().toString().trim();
            if (name.isEmpty()) {
                nameField.setError("Informe o nome");
                return;
            }
            if (!phone.isEmpty() && !PasswordUtils.isValidPhone(phone)) {
                phoneField.setError("Telefone inválido (use DDD + número)");
                return;
            }
            if (UserStorage.updateProfile(this, email, name, phone, null)) {
                Toast.makeText(this, "Perfil atualizado", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshProfileInfo();
                TextView welcome = findViewById(R.id.welcomeText);
                if (welcome != null) {
                    welcome.setText("Olá, " + name + "! Bem-vindo ao seu painel");
                }
            } else {
                Toast.makeText(this, "Não foi possível atualizar o perfil", Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private void showChangePasswordDialog() {
        String email = UserStorage.getCurrentUserEmail(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), 0);

        EditText currentField = passwordField("Senha atual");
        EditText newField = passwordField("Nova senha");
        EditText confirmField = passwordField("Confirmar nova senha");
        layout.addView(currentField);
        layout.addView(newField);
        layout.addView(confirmField);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Alterar senha")
                .setView(layout)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = currentField.getText().toString();
            String newPass = newField.getText().toString();
            String confirm = confirmField.getText().toString();
            if (!PasswordUtils.isStrongPassword(newPass)) {
                newField.setError("Mínimo 8 caracteres, com letras e números");
                return;
            }
            if (!newPass.equals(confirm)) {
                confirmField.setError("As senhas não coincidem");
                return;
            }
            int result = UserStorage.changePassword(this, email, current, newPass);
            if (result == UserStorage.PASSWORD_OK) {
                Toast.makeText(this, "Senha alterada com sucesso", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else if (result == UserStorage.PASSWORD_WRONG_CURRENT) {
                currentField.setError("Senha atual incorreta");
            } else {
                Toast.makeText(this, "Não foi possível alterar a senha", Toast.LENGTH_SHORT).show();
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
                .setTitle("Sair")
                .setMessage("Deseja realmente sair da sua conta?")
                .setPositiveButton("Sair", (d, w) -> {
                    UserStorage.logout(this);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir conta")
                .setMessage("Esta ação é permanente e apagará seus dados locais (perfil, documentos e inscrições). Deseja continuar?")
                .setPositiveButton("Excluir", (d, w) -> {
                    String currentEmail = UserStorage.getCurrentUserEmail(this);
                    if (UserStorage.deleteAccount(this, currentEmail)) {
                        Toast.makeText(this, "Conta excluída", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Falha ao excluir conta", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDocumentStatus();
        refreshProfileInfo();
        renderEvents();
        renderAnimals();
        renderRegistrations();
    }

    private void renderEvents() {
        LinearLayout container = findViewById(R.id.eventsContainer);
        TextView empty = findViewById(R.id.tvNoEventsHome);
        if (container == null) {
            return;
        }
        container.removeAllViews();

        java.util.List<Event> events = AppData.getAllEvents(this);
        if (empty != null) {
            empty.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
        }
        for (Event event : events) {
            container.addView(buildEventCard(event));
        }
    }

    private View buildEventCard(Event event) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);
        card.setOnClickListener(v -> openEvent(event));

        TextView title = new TextView(this);
        title.setText(event.getTitle());
        title.setTextSize(18);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        card.addView(title);

        TextView desc = new TextView(this);
        desc.setText(event.getDescription());
        desc.setTextSize(14);
        desc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, dp(6), 0, dp(12));
        desc.setLayoutParams(descParams);
        card.addView(desc);

        TextView dateChip = new TextView(this);
        dateChip.setText(event.getDate());
        dateChip.setTextSize(13);
        dateChip.setTypeface(dateChip.getTypeface(), android.graphics.Typeface.BOLD);
        dateChip.setTextColor(ContextCompat.getColor(this, R.color.on_green_container));
        dateChip.setBackgroundResource(R.drawable.bg_chip_done);
        dateChip.setPadding(dp(12), dp(5), dp(12), dp(5));
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateChip.setLayoutParams(chipParams);
        card.addView(dateChip);

        return card;
    }

    private void renderAnimals() {
        LinearLayout container = findViewById(R.id.animalsContainer);
        TextView empty = findViewById(R.id.tvNoAnimalsHome);
        if (container == null) {
            return;
        }
        container.removeAllViews();

        java.util.List<Animal> animals = AppData.getAvailableAnimals(this);
        java.util.List<Animal> filtered = new java.util.ArrayList<>();
        for (Animal animal : animals) {
            if (matchesQuery(animal)) {
                filtered.add(animal);
            }
        }
        if (empty != null) {
            empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            empty.setText(animalQuery.isEmpty()
                    ? "Ainda não há animais cadastrados para adoção."
                    : "Nenhum animal encontrado para a busca.");
        }
        String email = UserStorage.getCurrentUserEmail(this);
        for (Animal animal : filtered) {
            container.addView(buildAnimalCard(animal, email));
        }
    }

    private boolean matchesQuery(Animal animal) {
        if (animalQuery.isEmpty()) {
            return true;
        }
        return animal.getName().toLowerCase().contains(animalQuery)
                || animal.getSpecies().toLowerCase().contains(animalQuery)
                || animal.getOwnerOrg().toLowerCase().contains(animalQuery);
    }

    private View buildAnimalCard(Animal animal, String email) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);
        card.setOnClickListener(v -> openAnimal(animal));

        Bitmap photo = ImageUtils.loadBitmap(this, animal.getPhotoUri(), 600);
        if (photo != null) {
            ImageView image = new ImageView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(160));
            ip.setMargins(0, 0, 0, dp(12));
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

        if (!animal.getOwnerOrg().isEmpty()) {
            TextView org = new TextView(this);
            org.setText("Por " + animal.getOwnerOrg());
            org.setTextSize(12);
            org.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            card.addView(org);
        }

        TextView desc = new TextView(this);
        desc.setText(animal.getDescription());
        desc.setTextSize(14);
        desc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, dp(8), 0, dp(12));
        desc.setLayoutParams(descParams);
        card.addView(desc);

        Button apply = new Button(this);
        apply.setAllCaps(false);
        apply.setTypeface(apply.getTypeface(), android.graphics.Typeface.BOLD);
        apply.setMinHeight(dp(44));
        apply.setStateListAnimator(null);
        apply.setPadding(dp(16), 0, dp(16), 0);

        boolean alreadyApplied = AppData.hasApplied(this, animal.getId(), email);
        if (alreadyApplied) {
            apply.setText("Candidatura enviada");
            apply.setEnabled(false);
            apply.setBackgroundResource(R.drawable.bg_button_outline);
            apply.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            apply.setText("Candidatar-se à adoção");
            apply.setBackgroundResource(R.drawable.bg_button_primary);
            apply.setTextColor(ContextCompat.getColor(this, R.color.white));
            apply.setOnClickListener(v -> {
                String name = UserStorage.getCurrentUserName(this);
                if (AppData.addApplication(this, animal, email, name)) {
                    Toast.makeText(this, "Candidatura enviada para " + animal.getOwnerOrg(), Toast.LENGTH_SHORT).show();
                    renderAnimals();
                } else {
                    Toast.makeText(this, "Você já se candidatou a este animal", Toast.LENGTH_SHORT).show();
                }
            });
        }
        card.addView(apply);

        return card;
    }

    private void openEvent(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT, event);
        startActivity(intent);
    }

    private void openAnimal(Animal animal) {
        Intent intent = new Intent(this, AnimalDetailActivity.class);
        intent.putExtra(AnimalDetailActivity.EXTRA_ANIMAL_ID, animal.getId());
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void renderRegistrations() {
        LinearLayout container = findViewById(R.id.registrationsContainer);
        TextView emptyState = findViewById(R.id.tvNoRegistrations);
        if (container == null) {
            return;
        }
        container.removeAllViews();

        JSONArray registrations = UserStorage.getEventRegistrations(this, UserStorage.getCurrentUserEmail(this));
        if (emptyState != null) {
            emptyState.setVisibility(registrations.length() == 0 ? View.VISIBLE : View.GONE);
        }

        int qrSize = (int) (200 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < registrations.length(); i++) {
            JSONObject reg = registrations.optJSONObject(i);
            if (reg != null) {
                container.addView(buildRegistrationCard(reg, qrSize));
            }
        }
    }

    private View buildRegistrationCard(JSONObject reg, int qrSize) {
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);
        int marginBottom = (int) (16 * density);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.rounded_background);
        card.setPadding(pad, pad, pad, pad);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, marginBottom);
        card.setLayoutParams(cardParams);

        TextView title = new TextView(this);
        title.setText(reg.optString("title", "Evento"));
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView info = new TextView(this);
        info.setText(reg.optString("date", "") + "  •  " + reg.optString("location", ""));
        info.setTextSize(13);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.setMargins(0, (int) (4 * density), 0, (int) (12 * density));
        info.setLayoutParams(infoParams);
        card.addView(info);

        ImageView qrImage = new ImageView(this);
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(qrSize, qrSize);
        qrImage.setLayoutParams(qrParams);
        Bitmap qr = QRCodeGenerator.generate(reg.optString("qrContent", ""), qrSize);
        if (qr != null) {
            qrImage.setImageBitmap(qr);
        }
        card.addView(qrImage);

        TextView code = new TextView(this);
        code.setText("Código: " + reg.optString("code", "-"));
        code.setTextSize(13);
        LinearLayout.LayoutParams codeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        codeParams.setMargins(0, (int) (8 * density), 0, 0);
        code.setLayoutParams(codeParams);
        card.addView(code);

        return card;
    }

    private void refreshDocumentStatus() {
        boolean docsComplete = UserStorage.hasUserDocuments(this, UserStorage.getCurrentUserEmail(this));

        TextView documentStatus = findViewById(R.id.tvDocumentStatus);
        if (documentStatus != null) {
            documentStatus.setText(docsComplete ? "Documentos: concluído" : "Documentos: pendente");
            documentStatus.setBackgroundResource(docsComplete ? R.drawable.bg_chip_done : R.drawable.bg_chip_pending);
            documentStatus.setTextColor(ContextCompat.getColor(this,
                    docsComplete ? R.color.status_done : R.color.status_pending));
            int padH = (int) (12 * getResources().getDisplayMetrics().density);
            int padV = (int) (6 * getResources().getDisplayMetrics().density);
            documentStatus.setPadding(padH, padV, padH, padV);
        }

        View accountPrompt = findViewById(R.id.accountPrompt);
        if (accountPrompt != null) {
            accountPrompt.setVisibility(docsComplete ? View.GONE : View.VISIBLE);
        }
    }
}

package com.example.tocadospeludos;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DocumentActivity extends AppCompatActivity {

    private static final String[] ACCEPTED_TYPES = {"image/*", "application/pdf"};

    /**
     * Definição de um campo do formulário. Campos de texto guardam o valor digitado;
     * campos de anexo guardam a URI do arquivo (foto ou PDF) escolhido.
     */
    private static class Field {
        final String key;
        final String label;
        final String hint;
        final boolean attachment;
        final boolean required;
        final int inputType;
        final int minLines;
        String mask; // null = sem máscara

        EditText input;
        TextView fileLabel;
        Button viewButton;
        // Valor salvo do anexo: "app-file://..." (interno) ou "content://..." (externo, ainda não copiado).
        String attachmentValue;
        ActivityResultLauncher<String[]> picker;

        Field(String key, String label, String hint, boolean attachment, boolean required, int inputType, int minLines) {
            this.key = key;
            this.label = label;
            this.hint = hint;
            this.attachment = attachment;
            this.required = required;
            this.inputType = inputType;
            this.minLines = minLines;
        }

        static Field text(String key, String label, String hint, boolean required, int inputType, int minLines) {
            return new Field(key, label, hint, false, required, inputType, minLines);
        }

        static Field masked(String key, String label, String hint, boolean required, int inputType, String mask) {
            Field f = new Field(key, label, hint, false, required, inputType, 1);
            f.mask = mask;
            return f;
        }

        static Field attachment(String key, String label, boolean required) {
            return new Field(key, label, null, true, required, 0, 0);
        }
    }

    private final List<Field> fields = new ArrayList<>();
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_documents);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = UserStorage.getCurrentUserEmail(this);

        buildFieldDefinitions();
        registerPickers();
        buildForm();
        prefill();
        // Restaura edições em andamento após rotação (sobrepõe o prefill do armazenamento).
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
        updateProgress();

        Button backButton = findViewById(R.id.btnBackDocuments);
        backButton.setOnClickListener(v -> finish());

        Button submitButton = findViewById(R.id.btnSubmitDocuments);
        submitButton.setOnClickListener(v -> submit());
    }

    private void buildFieldDefinitions() {
        // Dados pessoais (texto)
        fields.add(Field.text("fullName", "Nome completo", "Ex.: Maria da Silva Souza",
                true, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 1));
        fields.add(Field.masked("birthDate", "Data de nascimento", "DD/MM/AAAA",
                true, InputType.TYPE_CLASS_NUMBER, MaskTextWatcher.DATE));
        fields.add(Field.masked("cpf", "CPF", "000.000.000-00",
                true, InputType.TYPE_CLASS_NUMBER, MaskTextWatcher.CPF));
        fields.add(Field.text("rg", "RG", "Número do documento de identidade",
                true, InputType.TYPE_CLASS_TEXT, 1));
        fields.add(Field.masked("phone", "Telefone para contato", "(00) 00000-0000",
                true, InputType.TYPE_CLASS_NUMBER, MaskTextWatcher.PHONE_11));
        fields.add(Field.text("address", "Endereço completo", "Rua, número, bairro, cidade - UF, CEP",
                true, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS, 2));

        // Documentos (anexo de foto ou PDF)
        fields.add(Field.attachment("idDocument", "Documento de identidade (RG ou CPF)", true));
        fields.add(Field.attachment("proofOfResidence", "Comprovante de residência", true));
        fields.add(Field.attachment("declaration", "Termo de responsabilidade assinado", true));
        fields.add(Field.attachment("authorization", "Autorização do responsável (se menor de idade)", false));
    }

    private void registerPickers() {
        for (Field field : fields) {
            if (field.attachment) {
                final Field f = field;
                f.picker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri == null) {
                        return;
                    }
                    // Copia o anexo para o armazenamento interno do app (não depende da URI externa).
                    String stored = AttachmentStore.copyToInternal(this, uri, f.key);
                    if (stored == null) {
                        // Fallback: mantém a URI externa com permissão persistente.
                        persist(uri);
                        stored = uri.toString();
                        Toast.makeText(this, getString(R.string.toast_copy_fallback), Toast.LENGTH_SHORT).show();
                    }
                    f.attachmentValue = stored;
                    updateFileLabel(f);
                    updateProgress();
                });
            }
        }
    }

    private void buildForm() {
        LinearLayout container = findViewById(R.id.formContainer);

        boolean documentsHeaderAdded = false;
        addSectionHeader(container, "Dados pessoais", false);
        for (Field field : fields) {
            if (field.attachment && !documentsHeaderAdded) {
                addSectionHeader(container, "Documentos", true);
                documentsHeaderAdded = true;
            }
            if (field.attachment) {
                addAttachmentField(container, field);
            } else {
                addTextField(container, field);
            }
        }
    }

    private void addSectionHeader(LinearLayout container, String text, boolean withTopMargin) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(18);
        header.setTypeface(header.getTypeface(), Typeface.BOLD);
        header.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        LinearLayout.LayoutParams params = rowParams();
        params.setMargins(0, withTopMargin ? dp(8) : 0, 0, dp(12));
        header.setLayoutParams(params);
        container.addView(header);
    }

    private void addTextField(LinearLayout container, Field field) {
        container.addView(buildLabel(field));

        EditText input = new EditText(this);
        input.setHint(field.hint);
        input.setInputType(field.inputType);
        input.setBackgroundResource(R.drawable.bg_field);
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setMinHeight(dp(52));
        input.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        if (field.minLines > 1) {
            input.setMinLines(field.minLines);
            input.setGravity(Gravity.TOP | Gravity.START);
        }
        LinearLayout.LayoutParams params = rowParams();
        params.setMargins(0, 0, 0, dp(16));
        input.setLayoutParams(params);
        field.input = input;

        if (field.mask != null) {
            input.addTextChangedListener(new MaskTextWatcher(input, field.mask));
        }
        // Atualiza o indicador de progresso conforme o usuário preenche.
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateProgress();
            }
        });
        container.addView(input);
    }

    private void addAttachmentField(LinearLayout container, Field field) {
        container.addView(buildLabel(field));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(rowParams());

        Button attachButton = new Button(this);
        attachButton.setText(getString(R.string.card_attach_photo_pdf));
        attachButton.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        attachButton.setBackgroundColor(0x00000000);
        attachButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_camera, 0, 0, 0);
        attachButton.setCompoundDrawablePadding(dp(8));
        attachButton.setPadding(0, dp(4), 0, dp(4));
        attachButton.setAllCaps(false);
        attachButton.setLayoutParams(
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        attachButton.setOnClickListener(v -> field.picker.launch(ACCEPTED_TYPES));
        row.addView(attachButton);

        Button viewButton = new Button(this);
        viewButton.setText(getString(R.string.card_view));
        viewButton.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        viewButton.setBackgroundColor(0x00000000);
        viewButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_view, 0, 0, 0);
        viewButton.setCompoundDrawablePadding(dp(8));
        viewButton.setPadding(dp(16), dp(4), 0, dp(4));
        viewButton.setAllCaps(false);
        viewButton.setLayoutParams(
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        viewButton.setOnClickListener(v -> openAttachment(field));
        field.viewButton = viewButton;
        row.addView(viewButton);

        container.addView(row);

        TextView fileLabel = new TextView(this);
        fileLabel.setTextSize(12);
        fileLabel.setTypeface(fileLabel.getTypeface(), Typeface.ITALIC);
        LinearLayout.LayoutParams params = rowParams();
        params.setMargins(0, 0, 0, dp(16));
        fileLabel.setLayoutParams(params);
        field.fileLabel = fileLabel;
        container.addView(fileLabel);

        updateFileLabel(field);
    }

    private void openAttachment(Field field) {
        if (field.attachmentValue == null || field.attachmentValue.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_no_attachment_field), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!AttachmentStore.openAttachment(this, field.attachmentValue)) {
            Toast.makeText(this, getString(R.string.toast_attachment_open_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private TextView buildLabel(Field field) {
        TextView label = new TextView(this);
        label.setText(field.required ? field.label + " *" : field.label + " (opcional)");
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams params = rowParams();
        params.setMargins(0, 0, 0, dp(8));
        label.setLayoutParams(params);
        return label;
    }

    private void prefill() {
        JSONObject documents = UserStorage.getUserDocuments(this, email);
        if (documents == null) {
            return;
        }
        for (Field field : fields) {
            String value = documents.optString(field.key, "");
            if (value.isEmpty()) {
                continue;
            }
            if (field.attachment) {
                // Aceita anexos internos (app-file://) e URIs externas legadas (content://).
                if (AttachmentStore.isInternal(value) || value.startsWith("content://")) {
                    field.attachmentValue = value;
                    updateFileLabel(field);
                }
            } else if (field.input != null) {
                field.input.setText(value);
            }
        }
    }

    private void submit() {
        JSONObject documents = new JSONObject();
        List<String> missing = new ArrayList<>();
        try {
            for (Field field : fields) {
                String value = field.attachment
                        ? (field.attachmentValue != null ? field.attachmentValue : "")
                        : field.input.getText().toString().trim();
                documents.put(field.key, value);
                if (field.required && value.isEmpty()) {
                    missing.add(field.label);
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, getString(R.string.toast_docs_prepare_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!missing.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_fill_attach_missing, String.join(", ", missing)), Toast.LENGTH_LONG).show();
            return;
        }

        // Validações de formato com erro no próprio campo.
        Field cpfField = findField("cpf");
        if (cpfField != null && !Validators.isValidCpf(cpfField.input.getText().toString())) {
            cpfField.input.setError(getString(R.string.err_cpf_invalid));
            cpfField.input.requestFocus();
            return;
        }
        Field birthField = findField("birthDate");
        if (birthField != null && !Validators.isValidBirthDate(birthField.input.getText().toString())) {
            birthField.input.setError(getString(R.string.err_date_invalid));
            birthField.input.requestFocus();
            return;
        }
        Field phoneField = findField("phone");
        if (phoneField != null && !PasswordUtils.isValidPhone(phoneField.input.getText().toString())) {
            phoneField.input.setError(getString(R.string.err_phone_invalid));
            phoneField.input.requestFocus();
            return;
        }

        if (UserStorage.saveUserDocuments(this, email, documents)) {
            Toast.makeText(this, getString(R.string.toast_documents_saved), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, getString(R.string.toast_docs_save_failed), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Campos são montados dinamicamente (sem id), então salvamos manualmente.
        for (Field field : fields) {
            if (field.attachment) {
                if (field.attachmentValue != null) {
                    outState.putString("doc_" + field.key, field.attachmentValue);
                }
            } else if (field.input != null) {
                outState.putString("doc_" + field.key, field.input.getText().toString());
            }
        }
    }

    private void restoreState(Bundle state) {
        for (Field field : fields) {
            String value = state.getString("doc_" + field.key, null);
            if (value == null) {
                continue;
            }
            if (field.attachment) {
                field.attachmentValue = value.isEmpty() ? null : value;
                updateFileLabel(field);
            } else if (field.input != null) {
                field.input.setText(value);
            }
        }
    }

    private Field findField(String key) {
        for (Field field : fields) {
            if (field.key.equals(key)) {
                return field;
            }
        }
        return null;
    }

    /** Atualiza o indicador "X de N concluídos" (apenas campos obrigatórios). */
    private void updateProgress() {
        TextView progress = findViewById(R.id.tvProgress);
        if (progress == null) {
            return;
        }
        int total = 0;
        int done = 0;
        for (Field field : fields) {
            if (!field.required) {
                continue;
            }
            total++;
            boolean filled = field.attachment
                    ? (field.attachmentValue != null && !field.attachmentValue.isEmpty())
                    : (field.input != null && !field.input.getText().toString().trim().isEmpty());
            if (filled) {
                done++;
            }
        }
        progress.setText(getString(R.string.progress_count, done, total));
        boolean complete = done == total;
        progress.setBackgroundResource(complete ? R.drawable.bg_chip_done : R.drawable.bg_chip_pending);
        progress.setTextColor(ContextCompat.getColor(this,
                complete ? R.color.status_done : R.color.status_pending));
    }

    // Mantém a permissão de leitura do arquivo entre sessões.
    private Uri persist(Uri uri) {
        if (uri == null) {
            return null;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // Permissão persistente não concedida; o anexo ainda funciona nesta sessão.
        }
        return uri;
    }

    private void updateFileLabel(Field field) {
        if (field.fileLabel == null) {
            return;
        }
        boolean has = field.attachmentValue != null && !field.attachmentValue.isEmpty();
        field.fileLabel.setText(has ? getString(R.string.card_attached) : getString(R.string.card_no_file));
        if (field.viewButton != null) {
            field.viewButton.setEnabled(has);
            field.viewButton.setVisibility(has ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private LinearLayout.LayoutParams rowParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

package com.example.tocadospeludos;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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

        EditText input;
        TextView fileLabel;
        Uri uri;
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

        Button backButton = findViewById(R.id.btnBackDocuments);
        backButton.setOnClickListener(v -> finish());

        Button submitButton = findViewById(R.id.btnSubmitDocuments);
        submitButton.setOnClickListener(v -> submit());
    }

    private void buildFieldDefinitions() {
        // Dados pessoais (texto)
        fields.add(Field.text("fullName", "Nome completo", "Ex.: Maria da Silva Souza",
                true, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 1));
        fields.add(Field.text("birthDate", "Data de nascimento", "DD/MM/AAAA",
                true, InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE, 1));
        fields.add(Field.text("cpf", "CPF", "000.000.000-00",
                true, InputType.TYPE_CLASS_PHONE, 1));
        fields.add(Field.text("rg", "RG", "Número do documento de identidade",
                true, InputType.TYPE_CLASS_TEXT, 1));
        fields.add(Field.text("phone", "Telefone para contato", "(00) 00000-0000",
                true, InputType.TYPE_CLASS_PHONE, 1));
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
                    f.uri = persist(uri);
                    updateFileLabel(f);
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
        container.addView(input);
    }

    private void addAttachmentField(LinearLayout container, Field field) {
        container.addView(buildLabel(field));

        Button attachButton = new Button(this);
        attachButton.setText("Anexar foto ou PDF");
        attachButton.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        attachButton.setBackgroundColor(0x00000000);
        attachButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_camera, 0, 0, 0);
        attachButton.setCompoundDrawablePadding(dp(8));
        attachButton.setPadding(0, dp(4), 0, dp(4));
        attachButton.setAllCaps(false);
        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        attachButton.setLayoutParams(btnParams);
        attachButton.setOnClickListener(v -> field.picker.launch(ACCEPTED_TYPES));
        container.addView(attachButton);

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
                if (value.startsWith("content://")) {
                    field.uri = Uri.parse(value);
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
                        ? (field.uri != null ? field.uri.toString() : "")
                        : field.input.getText().toString().trim();
                documents.put(field.key, value);
                if (field.required && value.isEmpty()) {
                    missing.add(field.label);
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Falha ao preparar os documentos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!missing.isEmpty()) {
            Toast.makeText(this, "Preencha/anexe: " + String.join(", ", missing), Toast.LENGTH_LONG).show();
            return;
        }

        if (UserStorage.saveUserDocuments(this, email, documents)) {
            Toast.makeText(this, "Documentos salvos com sucesso", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Falha ao salvar documentos", Toast.LENGTH_SHORT).show();
        }
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
        field.fileLabel.setText(field.uri == null
                ? "Nenhum arquivo anexado"
                : "Anexado: " + getDisplayName(field.uri));
    }

    private String getDisplayName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            // ignora e usa fallback
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        return name != null ? name : "arquivo";
    }

    private LinearLayout.LayoutParams rowParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

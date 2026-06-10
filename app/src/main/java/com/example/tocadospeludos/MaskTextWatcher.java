package com.example.tocadospeludos;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * TextWatcher que aplica máscaras a partir de um padrão, onde '#' representa um dígito.
 * Ex.: CPF "###.###.###-##", telefone "(##) #####-####", data "##/##/####".
 *
 * <p>Reformata em tempo real conforme o usuário digita, ignorando caracteres não numéricos.
 */
public class MaskTextWatcher implements TextWatcher {

    public static final String CPF = "###.###.###-##";
    public static final String DATE = "##/##/####";
    public static final String PHONE_11 = "(##) #####-####";
    public static final String PHONE_10 = "(##) ####-####";

    private final EditText field;
    private final String mask;
    private boolean editing;

    public MaskTextWatcher(EditText field, String mask) {
        this.field = field;
        this.mask = mask;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (editing) {
            return;
        }
        editing = true;

        String digits = s.toString().replaceAll("\\D", "");
        // Telefone alterna entre 10 e 11 dígitos.
        String activeMask = mask;
        if (mask.equals(PHONE_11) || mask.equals(PHONE_10)) {
            activeMask = digits.length() > 10 ? PHONE_11 : PHONE_10;
        }

        String formatted = applyMask(digits, activeMask);
        field.setText(formatted);
        field.setSelection(formatted.length());

        editing = false;
    }

    private String applyMask(String digits, String activeMask) {
        StringBuilder out = new StringBuilder();
        int di = 0;
        for (int i = 0; i < activeMask.length() && di < digits.length(); i++) {
            char m = activeMask.charAt(i);
            if (m == '#') {
                out.append(digits.charAt(di));
                di++;
            } else {
                out.append(m);
            }
        }
        return out.toString();
    }
}

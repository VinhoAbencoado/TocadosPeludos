package com.example.tocadospeludos;

import java.util.Calendar;
import java.util.Locale;

/**
 * Validações de campos de documentos: CPF (com dígitos verificadores) e data.
 */
public final class Validators {

    private Validators() {
    }

    /** Valida CPF brasileiro pelos dígitos verificadores, ignorando máscara. */
    public static boolean isValidCpf(String cpf) {
        if (cpf == null) {
            return false;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return false;
        }
        // Rejeita sequências repetidas (000..., 111..., etc.), que passam no cálculo.
        boolean allEqual = true;
        for (int i = 1; i < 11; i++) {
            if (digits.charAt(i) != digits.charAt(0)) {
                allEqual = false;
                break;
            }
        }
        if (allEqual) {
            return false;
        }

        int check1 = computeCheckDigit(digits, 9, 10);
        int check2 = computeCheckDigit(digits, 10, 11);
        return check1 == (digits.charAt(9) - '0') && check2 == (digits.charAt(10) - '0');
    }

    private static int computeCheckDigit(String digits, int length, int startWeight) {
        int sum = 0;
        int weight = startWeight;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * weight;
            weight--;
        }
        int mod = sum % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }

    /**
     * Valida uma data no formato DD/MM/AAAA: existente no calendário, não futura
     * e com ano plausível (>= 1900).
     */
    public static boolean isValidBirthDate(String date) {
        if (date == null) {
            return false;
        }
        String[] parts = date.split("/");
        if (parts.length != 3 || parts[0].length() != 2 || parts[1].length() != 2 || parts[2].length() != 4) {
            return false;
        }
        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            if (year < 1900 || month < 1 || month > 12 || day < 1) {
                return false;
            }
            Calendar c = Calendar.getInstance(Locale.getDefault());
            c.setLenient(false);
            c.set(year, month - 1, day, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            c.getTime(); // dispara exceção se a data for inválida (ex.: 31/02)
            // Não pode ser no futuro.
            return !c.after(Calendar.getInstance());
        } catch (Exception e) {
            return false;
        }
    }
}

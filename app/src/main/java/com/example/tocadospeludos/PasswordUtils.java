package com.example.tocadospeludos;

import android.util.Base64;
import android.util.Patterns;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utilitários de segurança leve (sem backend): hash de senha com salt e
 * validações de campos do cadastro. Local-first, sem dependências externas.
 */
public class PasswordUtils {

    /** Marca de senhas com hash, para diferenciar de senhas legadas em texto puro. */
    private static final String HASH_PREFIX = "sha256$";
    private static final int SALT_BYTES = 16;

    // ===== Hash de senha =====

    /** Gera "sha256$<saltBase64>$<hashBase64>" para a senha informada. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] digest = digest(password, salt);
        return HASH_PREFIX
                + Base64.encodeToString(salt, Base64.NO_WRAP) + "$"
                + Base64.encodeToString(digest, Base64.NO_WRAP);
    }

    /**
     * Verifica a senha contra o valor armazenado. Aceita tanto o formato com
     * hash quanto senhas legadas gravadas em texto puro (migração suave).
     */
    public static boolean verify(String password, String stored) {
        if (stored == null) return false;
        if (!isHashed(stored)) {
            // Legado: comparação em texto puro.
            return stored.equals(password);
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 3) return false;
        byte[] salt = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] expected = Base64.decode(parts[2], Base64.NO_WRAP);
        byte[] actual = digest(password, salt);
        return MessageDigest.isEqual(expected, actual);
    }

    /** Indica se o valor armazenado já está no formato com hash. */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(HASH_PREFIX);
    }

    private static byte[] digest(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é garantido em Android; não deve ocorrer.
            throw new RuntimeException(e);
        }
    }

    // ===== Validações =====

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /** Telefone brasileiro: 10 (fixo) ou 11 (celular) dígitos, ignorando máscara. */
    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10 || digits.length() == 11;
    }

    /** CNPJ: exatamente 14 dígitos, ignorando máscara. */
    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null) return false;
        return cnpj.replaceAll("\\D", "").length() == 14;
    }

    /** Senha forte: mínimo 8 caracteres, com ao menos uma letra e um número. */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }
}

package com.example.tocadospeludos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserStorage {

    private static final String PREFS_NAME = "user_db";
    private static final String KEY_USERS = "users";
    private static final String KEY_CURRENT_USER = "current_user";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static JSONObject loadUsers(Context context) {
        String json = getPrefs(context).getString(KEY_USERS, "{}");
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static void saveUsers(Context context, JSONObject users) {
        getPrefs(context).edit().putString(KEY_USERS, users.toString()).apply();
    }

    public static final String TYPE_ADOPTER = "adopter";
    public static final String TYPE_ONG = "ong";

    public static boolean registerUser(Context context, String name, String email, String password) {
        return registerUser(context, name, email, password, TYPE_ADOPTER, null);
    }

    public static boolean registerUser(Context context, String name, String email, String password,
                                       String accountType, String cnpj) {
        if (name == null || email == null || password == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        if (email.isEmpty() || password.isEmpty() || name.trim().isEmpty()) {
            return false;
        }

        JSONObject users = loadUsers(context);
        if (users.has(email)) {
            return false;
        }

        try {
            JSONObject user = new JSONObject();
            user.put("name", name.trim());
            user.put("password", PasswordUtils.hash(password));
            user.put("accountType", TYPE_ONG.equals(accountType) ? TYPE_ONG : TYPE_ADOPTER);
            if (cnpj != null && !cnpj.trim().isEmpty()) {
                user.put("cnpj", cnpj.trim());
            }
            users.put(email, user);
            saveUsers(context, users);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static String getAccountType(Context context, String email) {
        if (email == null) {
            return TYPE_ADOPTER;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return TYPE_ADOPTER;
        }
        try {
            return users.getJSONObject(email).optString("accountType", TYPE_ADOPTER);
        } catch (JSONException e) {
            return TYPE_ADOPTER;
        }
    }

    public static boolean isOng(Context context, String email) {
        return TYPE_ONG.equals(getAccountType(context, email));
    }

    public static boolean isCurrentUserOng(Context context) {
        return isOng(context, getCurrentUserEmail(context));
    }

    public static String getCnpj(Context context, String email) {
        if (email == null) {
            return null;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return null;
        }
        try {
            return users.getJSONObject(email).optString("cnpj", null);
        } catch (JSONException e) {
            return null;
        }
    }

    /** Nome de qualquer usuário pelo e-mail (ex.: nome público da ONG). */
    public static String getName(Context context, String email) {
        if (email == null) {
            return null;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return null;
        }
        try {
            return users.getJSONObject(email).optString("name", null);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getPhone(Context context, String email) {
        if (email == null) {
            return null;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return null;
        }
        try {
            return users.getJSONObject(email).optString("phone", null);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String getCurrentUserPhone(Context context) {
        return getPhone(context, getCurrentUserEmail(context));
    }

    /**
     * Atualiza nome, telefone e (para ONG) CNPJ do usuário. Campos nulos são ignorados.
     * O e-mail é a chave do usuário e não é alterável aqui.
     */
    public static boolean updateProfile(Context context, String email, String name, String phone, String cnpj) {
        if (email == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return false;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            if (name != null && !name.trim().isEmpty()) {
                user.put("name", name.trim());
            }
            if (phone != null) {
                user.put("phone", phone.trim());
            }
            if (cnpj != null && TYPE_ONG.equals(user.optString("accountType", TYPE_ADOPTER))) {
                user.put("cnpj", cnpj.trim());
            }
            users.put(email, user);
            saveUsers(context, users);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /** Resultado de troca de senha, para dar feedback específico ao usuário. */
    public static final int PASSWORD_OK = 0;
    public static final int PASSWORD_WRONG_CURRENT = 1;
    public static final int PASSWORD_ERROR = 2;

    public static int changePassword(Context context, String email, String currentPassword, String newPassword) {
        if (email == null || currentPassword == null || newPassword == null) {
            return PASSWORD_ERROR;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return PASSWORD_ERROR;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            String storedPass = user.optString("password", "");
            if (!PasswordUtils.verify(currentPassword, storedPass)) {
                return PASSWORD_WRONG_CURRENT;
            }
            user.put("password", PasswordUtils.hash(newPassword));
            users.put(email, user);
            saveUsers(context, users);
            return PASSWORD_OK;
        } catch (JSONException e) {
            return PASSWORD_ERROR;
        }
    }

    public static boolean login(Context context, String email, String password) {
        if (email == null || password == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return false;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            String storedPass = user.optString("password", "");
            if (PasswordUtils.verify(password, storedPass)) {
                // Migração suave: re-grava em hash se ainda estiver em texto puro.
                if (!PasswordUtils.isHashed(storedPass)) {
                    user.put("password", PasswordUtils.hash(password));
                    users.put(email, user);
                    saveUsers(context, users);
                }
                getPrefs(context).edit().putString(KEY_CURRENT_USER, email).apply();
                return true;
            }
        } catch (JSONException e) {
            // ignore
        }
        return false;
    }

    public static boolean isLoggedIn(Context context) {
        return getPrefs(context).contains(KEY_CURRENT_USER);
    }

    public static String getCurrentUserEmail(Context context) {
        return getPrefs(context).getString(KEY_CURRENT_USER, null);
    }

    public static String getCurrentUserName(Context context) {
        String email = getCurrentUserEmail(context);
        if (email == null) {
            return null;
        }
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return null;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            return user.optString("name", null);
        } catch (JSONException e) {
            return null;
        }
    }

    public static void logout(Context context) {
        getPrefs(context).edit().remove(KEY_CURRENT_USER).apply();
    }

    /**
     * Chaves obrigatórias para considerar o cadastro de adoção completo.
     * "authorization" (autorização do responsável) é opcional, exigida só para menores de idade.
     */
    public static final String[] REQUIRED_DOCUMENT_KEYS = {
            "fullName", "birthDate", "cpf", "rg", "phone", "address",
            "idDocument", "proofOfResidence", "declaration"
    };

    public static boolean saveUserDocuments(Context context, String email, JSONObject documents) {
        if (email == null || documents == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return false;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            user.put("documents", documents);
            users.put(email, user);
            saveUsers(context, users);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static JSONObject getUserDocuments(Context context, String email) {
        if (email == null) {
            return null;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return null;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            return user.optJSONObject("documents");
        } catch (JSONException e) {
            return null;
        }
    }

    public static boolean hasUserDocuments(Context context, String email) {
        if (email == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return false;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            JSONObject documents = user.optJSONObject("documents");
            if (documents == null) {
                return false;
            }
            for (String key : REQUIRED_DOCUMENT_KEYS) {
                if (documents.optString(key, "").trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static JSONArray getEventRegistrations(Context context, String email) {
        if (email == null) {
            return new JSONArray();
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return new JSONArray();
        }
        try {
            JSONObject user = users.getJSONObject(email);
            JSONArray registrations = user.optJSONArray("registrations");
            return registrations != null ? registrations : new JSONArray();
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static boolean isRegisteredForEvent(Context context, String email, String eventId) {
        if (eventId == null) {
            return false;
        }
        JSONArray registrations = getEventRegistrations(context, email);
        for (int i = 0; i < registrations.length(); i++) {
            JSONObject reg = registrations.optJSONObject(i);
            if (reg != null && eventId.equals(reg.optString("eventId", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registra o usuário em um evento, guardando o conteúdo do QR Code para uso posterior.
     * Se já houver inscrição para o mesmo eventId, retorna o registro existente sem duplicar.
     */
    public static JSONObject registerForEvent(Context context, String email, Event event, String code, String qrContent) {
        if (email == null || event == null) {
            return null;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return null;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            JSONArray registrations = user.optJSONArray("registrations");
            if (registrations == null) {
                registrations = new JSONArray();
            }

            for (int i = 0; i < registrations.length(); i++) {
                JSONObject reg = registrations.optJSONObject(i);
                if (reg != null && event.getId().equals(reg.optString("eventId", ""))) {
                    return reg;
                }
            }

            JSONObject reg = new JSONObject();
            reg.put("eventId", event.getId());
            reg.put("title", event.getTitle());
            reg.put("date", event.getDate());
            reg.put("location", event.getLocation());
            reg.put("code", code);
            reg.put("qrContent", qrContent);
            reg.put("adopterEmail", email);
            reg.put("adopterName", user.optString("name", email));
            reg.put("checkedIn", false);
            registrations.put(reg);

            user.put("registrations", registrations);
            users.put(email, user);
            saveUsers(context, users);
            return reg;
        } catch (JSONException e) {
            return null;
        }
    }

    /** Remove a inscrição do usuário em um evento (cancelamento pelo adotante). */
    public static boolean cancelEventRegistration(Context context, String email, String eventId) {
        if (email == null || eventId == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return false;
        }
        try {
            JSONObject user = users.getJSONObject(email);
            JSONArray registrations = user.optJSONArray("registrations");
            if (registrations == null) {
                return false;
            }
            JSONArray updated = new JSONArray();
            boolean removed = false;
            for (int i = 0; i < registrations.length(); i++) {
                JSONObject reg = registrations.optJSONObject(i);
                if (reg != null && eventId.equals(reg.optString("eventId", ""))) {
                    removed = true;
                    continue;
                }
                updated.put(reg);
            }
            if (removed) {
                user.put("registrations", updated);
                users.put(email, user);
                saveUsers(context, users);
            }
            return removed;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Retorna os participantes inscritos em um evento, varrendo todos os usuários.
     * Cada item traz adopterEmail, adopterName, code e checkedIn.
     */
    public static JSONArray getEventParticipants(Context context, String eventId) {
        JSONArray result = new JSONArray();
        if (eventId == null) {
            return result;
        }
        JSONObject users = loadUsers(context);
        java.util.Iterator<String> keys = users.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject user = users.optJSONObject(key);
            if (user == null) {
                continue;
            }
            JSONArray registrations = user.optJSONArray("registrations");
            if (registrations == null) {
                continue;
            }
            for (int i = 0; i < registrations.length(); i++) {
                JSONObject reg = registrations.optJSONObject(i);
                if (reg != null && eventId.equals(reg.optString("eventId", ""))) {
                    try {
                        JSONObject participant = new JSONObject();
                        participant.put("adopterEmail", reg.optString("adopterEmail", key));
                        participant.put("adopterName", reg.optString("adopterName", key));
                        participant.put("code", reg.optString("code", ""));
                        participant.put("checkedIn", reg.optBoolean("checkedIn", false));
                        result.put(participant);
                    } catch (JSONException e) {
                        // ignora item inválido
                    }
                }
            }
        }
        return result;
    }

    /** Marca/atualiza o check-in de um participante em um evento. */
    public static boolean setEventCheckIn(Context context, String adopterEmail, String eventId, boolean checkedIn) {
        if (adopterEmail == null || eventId == null) {
            return false;
        }
        adopterEmail = adopterEmail.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(adopterEmail)) {
            return false;
        }
        try {
            JSONObject user = users.getJSONObject(adopterEmail);
            JSONArray registrations = user.optJSONArray("registrations");
            if (registrations == null) {
                return false;
            }
            for (int i = 0; i < registrations.length(); i++) {
                JSONObject reg = registrations.optJSONObject(i);
                if (reg != null && eventId.equals(reg.optString("eventId", ""))) {
                    reg.put("checkedIn", checkedIn);
                    user.put("registrations", registrations);
                    users.put(adopterEmail, user);
                    saveUsers(context, users);
                    return true;
                }
            }
            return false;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Valida um código de ingresso para um evento e, se válido, faz o check-in.
     * Retorna o objeto do participante (com adopterName) ou null se o código não bater.
     */
    public static JSONObject checkInByCode(Context context, String eventId, String code) {
        if (eventId == null || code == null || code.trim().isEmpty()) {
            return null;
        }
        String target = code.trim();
        JSONArray participants = getEventParticipants(context, eventId);
        for (int i = 0; i < participants.length(); i++) {
            JSONObject p = participants.optJSONObject(i);
            if (p != null && target.equalsIgnoreCase(p.optString("code", ""))) {
                setEventCheckIn(context, p.optString("adopterEmail", ""), eventId, true);
                try {
                    p.put("checkedIn", true);
                } catch (JSONException ignored) {
                }
                return p;
            }
        }
        return null;
    }

    public static boolean deleteAccount(Context context, String email) {
        if (email == null) {
            email = getCurrentUserEmail(context);
        }
        if (email == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        if (!users.has(email)) {
            return false;
        }
        users.remove(email);
        saveUsers(context, users);
        if (email.equals(getCurrentUserEmail(context))) {
            logout(context);
        }
        return true;
    }

    public static boolean userExists(Context context, String email) {
        if (email == null) {
            return false;
        }
        email = email.trim().toLowerCase();
        JSONObject users = loadUsers(context);
        return users.has(email);
    }
}

package com.example.tocadospeludos;

import android.content.Context;
import android.content.SharedPreferences;

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

    public static boolean registerUser(Context context, String name, String email, String password) {
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
            user.put("password", password);
            users.put(email, user);
            saveUsers(context, users);
            return true;
        } catch (JSONException e) {
            return false;
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
            if (storedPass.equals(password)) {
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

    public static boolean saveUserDocuments(Context context, String email, String idDocument, String proofOfResidence, String declaration, String authorization) {
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
            JSONObject documents = new JSONObject();
            documents.put("idDocument", idDocument != null ? idDocument.trim() : "");
            documents.put("proofOfResidence", proofOfResidence != null ? proofOfResidence.trim() : "");
            documents.put("declaration", declaration != null ? declaration.trim() : "");
            documents.put("authorization", authorization != null ? authorization.trim() : "");
            user.put("documents", documents);
            users.put(email, user);
            saveUsers(context, users);
            return true;
        } catch (JSONException e) {
            return false;
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
            return !documents.optString("idDocument", "").isEmpty()
                && !documents.optString("proofOfResidence", "").isEmpty()
                && !documents.optString("declaration", "").isEmpty()
                && !documents.optString("authorization", "").isEmpty();
        } catch (JSONException e) {
            return false;
        }
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

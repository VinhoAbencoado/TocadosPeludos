package com.example.tocadospeludos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Armazenamento global (compartilhado entre todas as contas) de eventos, animais
 * e candidaturas de adoção. Persiste em SharedPreferences como arrays JSON.
 */
public class AppData {

    private static final String PREFS_NAME = "app_data";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_ANIMALS = "animals";
    private static final String KEY_APPLICATIONS = "applications";
    private static final String KEY_SEEDED = "seeded";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static JSONArray loadArray(Context context, String key) {
        String json = prefs(context).getString(key, "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static void saveArray(Context context, String key, JSONArray array) {
        prefs(context).edit().putString(key, array.toString()).apply();
    }

    private static String newId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ===================== EVENTOS =====================

    public static List<Event> getAllEvents(Context context) {
        seedIfNeeded(context);
        List<Event> events = new ArrayList<>();
        JSONArray array = loadArray(context, KEY_EVENTS);
        for (int i = 0; i < array.length(); i++) {
            Event event = Event.fromJson(array.optJSONObject(i));
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    public static List<Event> getEventsByOwner(Context context, String ownerEmail) {
        List<Event> result = new ArrayList<>();
        if (ownerEmail == null) {
            return result;
        }
        for (Event event : getAllEvents(context)) {
            if (ownerEmail.equalsIgnoreCase(event.getOwnerEmail())) {
                result.add(event);
            }
        }
        return result;
    }

    public static boolean addEvent(Context context, String title, String description, String date,
                                   String location, String ownerEmail, String ownerOrg) {
        try {
            Event event = new Event(newId("evt"), title, description, date, location, ownerEmail, ownerOrg);
            JSONArray array = loadArray(context, KEY_EVENTS);
            array.put(event.toJson());
            saveArray(context, KEY_EVENTS, array);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static boolean deleteEvent(Context context, String eventId) {
        return removeById(context, KEY_EVENTS, eventId);
    }

    // ===================== ANIMAIS =====================

    public static List<Animal> getAllAnimals(Context context) {
        List<Animal> animals = new ArrayList<>();
        JSONArray array = loadArray(context, KEY_ANIMALS);
        for (int i = 0; i < array.length(); i++) {
            Animal animal = Animal.fromJson(array.optJSONObject(i));
            if (animal != null) {
                animals.add(animal);
            }
        }
        return animals;
    }

    public static List<Animal> getAvailableAnimals(Context context) {
        List<Animal> result = new ArrayList<>();
        for (Animal animal : getAllAnimals(context)) {
            if (animal.isAvailable()) {
                result.add(animal);
            }
        }
        return result;
    }

    public static Animal getAnimalById(Context context, String animalId) {
        if (animalId == null) {
            return null;
        }
        for (Animal animal : getAllAnimals(context)) {
            if (animalId.equals(animal.getId())) {
                return animal;
            }
        }
        return null;
    }

    public static List<Animal> getAnimalsByOwner(Context context, String ownerEmail) {
        List<Animal> result = new ArrayList<>();
        if (ownerEmail == null) {
            return result;
        }
        for (Animal animal : getAllAnimals(context)) {
            if (ownerEmail.equalsIgnoreCase(animal.getOwnerEmail())) {
                result.add(animal);
            }
        }
        return result;
    }

    public static boolean addAnimal(Context context, String name, String species, String description,
                                    String photoUri, String ownerEmail, String ownerOrg) {
        try {
            Animal animal = new Animal(newId("ani"), name, species, description,
                    Animal.STATUS_AVAILABLE, photoUri, ownerEmail, ownerOrg);
            JSONArray array = loadArray(context, KEY_ANIMALS);
            array.put(animal.toJson());
            saveArray(context, KEY_ANIMALS, array);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static boolean deleteAnimal(Context context, String animalId) {
        return removeById(context, KEY_ANIMALS, animalId);
    }

    /** Atualiza os campos editáveis do animal, preservando id, dono e status. */
    public static boolean updateAnimal(Context context, String animalId, String name, String species,
                                       String description, String photoUri) {
        if (animalId == null) {
            return false;
        }
        JSONArray array = loadArray(context, KEY_ANIMALS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && animalId.equals(obj.optString("id"))) {
                try {
                    obj.put("name", name);
                    obj.put("species", species);
                    obj.put("description", description);
                    if (photoUri != null) {
                        obj.put("photoUri", photoUri);
                    }
                    saveArray(context, KEY_ANIMALS, array);
                    return true;
                } catch (JSONException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public static boolean setAnimalStatus(Context context, String animalId, String status) {
        JSONArray array = loadArray(context, KEY_ANIMALS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && animalId.equals(obj.optString("id"))) {
                try {
                    obj.put("status", status);
                    saveArray(context, KEY_ANIMALS, array);
                    return true;
                } catch (JSONException e) {
                    return false;
                }
            }
        }
        return false;
    }

    // ===================== CANDIDATURAS =====================

    public static boolean hasApplied(Context context, String animalId, String adopterEmail) {
        if (animalId == null || adopterEmail == null) {
            return false;
        }
        JSONArray array = loadArray(context, KEY_APPLICATIONS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && animalId.equals(obj.optString("animalId"))
                    && adopterEmail.equalsIgnoreCase(obj.optString("adopterEmail"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean addApplication(Context context, Animal animal, String adopterEmail, String adopterName) {
        if (animal == null || adopterEmail == null) {
            return false;
        }
        if (hasApplied(context, animal.getId(), adopterEmail)) {
            return false;
        }
        try {
            JSONObject application = new JSONObject();
            application.put("id", newId("app"));
            application.put("animalId", animal.getId());
            application.put("animalName", animal.getName());
            application.put("animalSpecies", animal.getSpecies());
            application.put("adopterEmail", adopterEmail);
            application.put("adopterName", adopterName != null ? adopterName : adopterEmail);
            application.put("ownerEmail", animal.getOwnerEmail());
            application.put("ownerOrg", animal.getOwnerOrg());
            application.put("status", STATUS_PENDING);

            JSONArray array = loadArray(context, KEY_APPLICATIONS);
            array.put(application);
            saveArray(context, KEY_APPLICATIONS, array);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public static JSONArray getApplicationsForAdopter(Context context, String adopterEmail) {
        JSONArray result = new JSONArray();
        if (adopterEmail == null) {
            return result;
        }
        JSONArray array = loadArray(context, KEY_APPLICATIONS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && adopterEmail.equalsIgnoreCase(obj.optString("adopterEmail"))) {
                result.put(obj);
            }
        }
        return result;
    }

    public static JSONArray getApplicationsForOwner(Context context, String ownerEmail) {
        JSONArray result = new JSONArray();
        if (ownerEmail == null) {
            return result;
        }
        JSONArray array = loadArray(context, KEY_APPLICATIONS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && ownerEmail.equalsIgnoreCase(obj.optString("ownerEmail"))) {
                result.put(obj);
            }
        }
        return result;
    }

    public static boolean setApplicationStatus(Context context, String applicationId, String status) {
        JSONArray array = loadArray(context, KEY_APPLICATIONS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && applicationId.equals(obj.optString("id"))) {
                try {
                    obj.put("status", status);
                    saveArray(context, KEY_APPLICATIONS, array);
                    // Ao aprovar, marca o animal como adotado.
                    if (STATUS_APPROVED.equals(status)) {
                        setAnimalStatus(context, obj.optString("animalId"), Animal.STATUS_ADOPTED);
                    }
                    return true;
                } catch (JSONException e) {
                    return false;
                }
            }
        }
        return false;
    }

    // ===================== UTILITÁRIOS =====================

    private static boolean removeById(Context context, String key, String id) {
        if (id == null) {
            return false;
        }
        JSONArray array = loadArray(context, key);
        JSONArray updated = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && id.equals(obj.optString("id"))) {
                removed = true;
                continue;
            }
            updated.put(obj);
        }
        if (removed) {
            saveArray(context, key, updated);
        }
        return removed;
    }

    /** Popula eventos de exemplo na primeira execução, para o feed não ficar vazio. */
    private static void seedIfNeeded(Context context) {
        if (prefs(context).getBoolean(KEY_SEEDED, false)) {
            return;
        }
        addEvent(context, "Feira de Adoção",
                "Conheça novos amigos peludos neste sábado. Diversos pets resgatados estarão disponíveis para adoção responsável.",
                "20 mai", "Praça Central", "", "Toca dos Peludos");
        addEvent(context, "Workshop de Cuidados",
                "Dicas práticas para você cuidar melhor do seu pet, com veterinários e adestradores convidados.",
                "28 mai", "Centro Comunitário", "", "Toca dos Peludos");
        addEvent(context, "Passeio Pet Friendly",
                "Encontro ao ar livre para passeios e diversão. Traga seu pet e faça novos amigos.",
                "05 jun", "Parque da Cidade", "", "Toca dos Peludos");
        prefs(context).edit().putBoolean(KEY_SEEDED, true).apply();
    }
}

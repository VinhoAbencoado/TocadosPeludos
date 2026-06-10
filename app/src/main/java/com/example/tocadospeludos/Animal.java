package com.example.tocadospeludos;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Animal disponibilizado por uma ONG para adoção.
 */
public class Animal implements Serializable {

    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_ADOPTED = "adopted";

    private final String id;
    private final String name;
    private final String species;
    private final String description;
    private final String status;
    private final String photoUri;
    private final String ownerEmail;
    private final String ownerOrg;

    public Animal(String id, String name, String species, String description, String status,
                  String photoUri, String ownerEmail, String ownerOrg) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.description = description;
        this.status = status != null ? status : STATUS_AVAILABLE;
        this.photoUri = photoUri != null ? photoUri : "";
        this.ownerEmail = ownerEmail != null ? ownerEmail : "";
        this.ownerOrg = ownerOrg != null ? ownerOrg : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerOrg() {
        return ownerOrg;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("species", species);
        json.put("description", description);
        json.put("status", status);
        json.put("photoUri", photoUri);
        json.put("ownerEmail", ownerEmail);
        json.put("ownerOrg", ownerOrg);
        return json;
    }

    public static Animal fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        return new Animal(
                json.optString("id", ""),
                json.optString("name", ""),
                json.optString("species", ""),
                json.optString("description", ""),
                json.optString("status", STATUS_AVAILABLE),
                json.optString("photoUri", ""),
                json.optString("ownerEmail", ""),
                json.optString("ownerOrg", ""));
    }
}

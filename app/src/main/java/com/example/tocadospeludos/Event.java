package com.example.tocadospeludos;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Modelo de evento. É Serializable para trafegar entre activities via Intent
 * e tem conversão de/para JSON para ser persistido em {@link AppData}.
 */
public class Event implements Serializable {

    private final String id;
    private final String title;
    private final String description;
    private final String date;
    private final String location;
    private final String ownerEmail;
    private final String ownerOrg;
    /** Data em epoch millis (00:00 do dia) para ordenar e ocultar eventos passados. 0 = legado/sem data. */
    private final long dateMillis;

    public Event(String id, String title, String description, String date, String location) {
        this(id, title, description, date, location, "", "", 0L);
    }

    public Event(String id, String title, String description, String date, String location,
                 String ownerEmail, String ownerOrg) {
        this(id, title, description, date, location, ownerEmail, ownerOrg, 0L);
    }

    public Event(String id, String title, String description, String date, String location,
                 String ownerEmail, String ownerOrg, long dateMillis) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
        this.ownerEmail = ownerEmail != null ? ownerEmail : "";
        this.ownerOrg = ownerOrg != null ? ownerOrg : "";
        this.dateMillis = dateMillis;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerOrg() {
        return ownerOrg;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("description", description);
        json.put("date", date);
        json.put("location", location);
        json.put("ownerEmail", ownerEmail);
        json.put("ownerOrg", ownerOrg);
        json.put("dateMillis", dateMillis);
        return json;
    }

    public static Event fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        return new Event(
                json.optString("id", ""),
                json.optString("title", ""),
                json.optString("description", ""),
                json.optString("date", ""),
                json.optString("location", ""),
                json.optString("ownerEmail", ""),
                json.optString("ownerOrg", ""),
                json.optLong("dateMillis", 0L));
    }
}

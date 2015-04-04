package com.dismu.api;

import com.dismu.logging.Loggers;
import org.json.simple.JSONObject;

public class DismuSession {
    private String id;
    private String secret;

    public DismuSession(String id, String secret) {
        this.id = id;
        this.secret = secret;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String toString() {
        return "DismuSession[id=" + id + "]";
    }

    public static DismuSession fromJSON(JSONObject json) {
        return new DismuSession((String) json.get("sessionId"), (String) json.get("sessionSecret"));
    }
}

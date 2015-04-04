package com.dismu.api;

import com.dismu.logging.Loggers;
import org.json.simple.JSONObject;

public class APIResult {
    private String error;
    private JSONObject response;

    public APIResult() {

    }

    public APIResult(JSONObject response) {
        this.response = response;
    }

    public APIResult(String error) {
        this.error = error;
    }

    public boolean isSuccessful() {
        return response.get("status").equals(0l);
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public JSONObject getResponse() {
        return response;
    }

    public void setResponse(JSONObject response) {
        this.response = response;
    }
}

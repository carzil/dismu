package com.dismu.api;

import com.dismu.logging.Loggers;
import org.json.simple.JSONObject;

public class APIResult {
    public static final long OK = 0;
    public static final long INVALID_SIGNATURE = 1;
    public static final long INVALID_SESSION_ID = 2;
    public static final long WRONG_NAME_OR_PASSWORD = 3;
    public static final long INTERNAL_ERROR = 4;

    private JSONObject response;

    public APIResult() {

    }

    public APIResult(JSONObject response) {
        this.response = response;
    }

    public boolean isSuccessful() {
        return response != null && response.get("status").equals(OK);
    }

    public String getError() {
        if (response == null) {
            return "response is null";
        }
        return response.get("error").toString();
    }

    public long getStatus() {
        return (long) response.get("status");
    }

    public JSONObject getResponse() {
        return response;
    }

    public void setResponse(JSONObject response) {
        this.response = response;
    }
}

package com.dismu.api;

import com.dismu.utils.Utils;
import org.json.simple.JSONObject;


public class AuthAPI {
    private static DismuSession session;

    public static DismuSession getSession() {
        return session;
    }

    public static APIResult auth(String username, String password) {
        APIResult result = APIUtils.sendRequest("auth", String.format("username=%s&password=%s&deviceInfo=%s", username, password, Utils.getInfo()));
        if (result.isSuccessful()) {
            JSONObject json = result.getResponse();
            session = DismuSession.fromJSON(json);
        }
        return result;
    }

    public static APIResult deauth() {
        // Here we don't need to send anything because sendSignedRequest will send sessionId
        APIResult result = APIUtils.sendSignedRequest("deauth", "");
        session = null;
        return result;
    }

}

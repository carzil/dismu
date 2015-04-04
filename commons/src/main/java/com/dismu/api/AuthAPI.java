package com.dismu.api;

import org.json.simple.JSONObject;


public class AuthAPI {
    private static DismuSession session;

    public static DismuSession getSession() {
        return session;
    }

    public static boolean auth(String username, String password) {
        APIResult response = APIUtils.sendRequest("auth", String.format("username=%s&password=%s", username, password));
        if (response.isSuccessful()) {
            JSONObject json = response.getResponse();
            session = DismuSession.fromJSON(json);
            return true;
        } else {
            return false;
        }
    }

    public static boolean deauth() {
        APIResult result = APIUtils.sendSignedRequest("deauth", String.format("sessionId=%s", session.getId()));
        session = null;
        return result.isSuccessful();
    }

}

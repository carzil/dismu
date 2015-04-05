package com.dismu.api;

import com.dismu.logging.Loggers;
import com.dismu.utils.Utils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class APIUtils {
    public static String getMasterServerAPIUrl() {
        return Utils.getMasterServerUrl() + "api/";
    }

    public static String generateSignature(String method, DismuSession session) {
        return Utils.getMD5(method + session.getId() + session.getSecret());
    }

    public static APIResult sendSignedRequest(String method, String s, DismuSession session) {
        String signature = generateSignature(method, session);
        return sendRequest(method, s + String.format("&sessionId=%s&signature=%s", session.getId(), signature));
    }

    public static APIResult sendSignedRequest(String method, String s) {
        return sendSignedRequest(method, s, AuthAPI.getSession());
    }

    public static APIResult sendRequest(String method, String s) {
        try {
            String type = "application/x-www-form-urlencoded";
            URL u = new URL(getMasterServerAPIUrl() + method);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", type);
            conn.setRequestProperty("Content-Length", String.valueOf(s.length()));
            Loggers.apiLogger.debug("send request, method={}", method, s);
            OutputStream os = conn.getOutputStream();
            os.write(s.getBytes());
            os.flush();
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Utils.copyStream(is, bos);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(new String(bos.toByteArray()));
            Loggers.apiLogger.debug("got response '{}'", new String(bos.toByteArray()));
            return new APIResult(json);
        } catch (IOException | ParseException e) {
            Loggers.apiLogger.error("cannot send API request", e);
            return new APIResult();
        }
    }
}

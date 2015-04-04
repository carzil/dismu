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
//        return "http://dismu.herokuapp.com/api/";
        return "http://localhost:3000/api/";
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
            Loggers.apiLogger.debug("send request, method={}, body='{}'", method, s);
            OutputStream os = conn.getOutputStream();
            os.write(s.getBytes());
            os.flush();
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[4096];
            while (-1 != (len = is.read(buffer))) {
                bos.write(buffer, 0, len);
            }
            bos.flush();
            JSONParser parser = new JSONParser();
            String str = new String(bos.toByteArray());
            JSONObject json = (JSONObject)parser.parse(str);
            APIResult result;
            if (json.get("error") != null) {
                result = new APIResult((String) json.get("error"));
            } else {
                result = new APIResult(json);
            }
            Loggers.apiLogger.debug("got response {}", new String(bos.toByteArray()));
            return result;
        } catch (IOException | ParseException e) {
            Loggers.apiLogger.error("cannot send API request", e);
        }
        return null;
    }
}

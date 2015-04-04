package com.dismu.api;

import com.dismu.logging.Loggers;
import com.dismu.utils.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ConnectionAPI {
    public Seed[] getNeighbours(String userId) {
        try {
            APIResult response = APIUtils.sendSignedRequest("seedlist", String.format("userId=%s", userId));
            ArrayList<Seed> result = new ArrayList<Seed>();
            if (response.isSuccessful()) {
                JSONObject json = response.getResponse();
                JSONArray seeds = (JSONArray) json.get("seeds");
                for (JSONObject seed1 : (Iterable<JSONObject>) seeds) {
                    Seed seed = new Seed();
                    JSONObject current = seed1;
                    seed.userId = (String) current.get("userId");
                    seed.groupId = (String) current.get("groupId");
                    try {
                        seed.localIP = InetAddress.getByName((String) current.get("localIP"));
                        seed.remoteIP = InetAddress.getByName((String) current.get("remoteIP"));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    seed.port = ((Long) current.get("port")).intValue();
                    result.add(seed);
                }
                Seed[] res = new Seed[result.size()];
                for (int i = 0; i < result.size(); ++i) {
                    res[i] = result.get(i);
                }
                return res;
            } else {
                Loggers.apiLogger.error("API call failed, error '{}'", response.getError());
            }
        } catch (NullPointerException e) {

        }
        return new Seed[0];
    }

    public void register(String userId, String groupId, String localIP, int port) {
        String remoteIP = "";
        try {
            remoteIP = Utils.getRemoteIP();
        } catch (IOException e) {
            e.printStackTrace();
        }
        APIResult result = APIUtils.sendSignedRequest(
                "register",
                String.format("userId=%s&groupId=%s&localIP=%s&remoteIP=%s&port=%d", userId, groupId, localIP, remoteIP, port)
        );
        if (!result.isSuccessful()) {
            Loggers.apiLogger.error("API call failed, error '{}'", result.getError());
        }
    }

    public void unregister(String userId) {
        APIResult result = APIUtils.sendSignedRequest("unregister", String.format("userId=%s", userId));
        if (!result.isSuccessful()) {
            Loggers.apiLogger.error("API call failed, error '{}'", result.getError());
        }
    }
}

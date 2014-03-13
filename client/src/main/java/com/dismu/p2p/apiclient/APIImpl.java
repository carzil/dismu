package com.dismu.p2p.apiclient;

import com.dismu.utils.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;

public class APIImpl implements API {
    @Override
    public Seed[] getNeighbours(String userId) {
        try {
            JSONObject response = Utils.sendJSONRequest(
                    Utils.getMasterServerAPIUrl()+"seedlist",
                    String.format("{\"userId\":\"%s\"}", userId)
            );
            ArrayList<Seed> result = new ArrayList<Seed>();
            JSONArray seeds = (JSONArray)response.get("seeds");
            Iterator<JSONObject> iterator = seeds.iterator();
            while (iterator.hasNext()) {
                Seed seed = new Seed();
                JSONObject current = iterator.next();
                seed.userId = (String)current.get("userId");
                seed.groupId = (String)current.get("groupId");
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
        } catch(NullPointerException e) {
        }
        return new Seed[0];
    }

    @Override
    public void register(String userId, String groupId, int port) {
        String localIP = "", remoteIP = "";
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
            remoteIP = Utils.getRemoteIP();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.sendJSONRequest(
                Utils.getMasterServerAPIUrl()+"register",
                String.format(
                        "{\"userId\":\"%s\", " +
                        "\"groupId\":\"%s\", " +
                        "\"localIP\":\"%s\", " +
                        "\"remoteIP\":\"%s\", " +
                        "\"port\":%d}",
                        userId, groupId, localIP, remoteIP, port
                )
        );
    }

    @Override
    public void unregister(String userId) {
        Utils.sendJSONRequest(
                Utils.getMasterServerAPIUrl()+"unregister",
                String.format("{\"userId\":\"%s\"}", userId)
        );
    }
}

package com.dismu.utils;

import com.dismu.logging.Loggers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;

public class Utils {
    public static JSONObject sendJSONRequest(String address, String s) {
        try {
            String type = "application/json";
            URL u = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", type );
            conn.setRequestProperty("Content-Length", String.valueOf(s.length()));
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
            JSONObject obj = (JSONObject)parser.parse(str);
            return obj;
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    /**
     * Returns folder for application data, if it hasn't created yet, creates it.
     * @return File application data folder's path
     */
    public static File getAppFolderPath() {
        File appFolder;
        if (Utils.isWindows()) {
            appFolder = new File(System.getenv("APPDATA"), ".dismu");
        } else {
            appFolder = new File(System.getProperty("user.home"), ".dismu");
        }
        if (!appFolder.exists()) {
            appFolder.mkdirs();
        }
        return appFolder;
    }

    /**
     * Copies file, if destination file doesn't exists creates it.
     * @param sourceFile source file
     * @param destinationFile file to copy
     */
    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        if (!destinationFile.exists()) {
            Loggers.playerLogger.info("{}", destinationFile.getAbsolutePath());
            destinationFile.createNewFile();
        }
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destinationFile).getChannel();
        destination.transferFrom(source, 0, source.size());
    }

    public static String getMasterServerAPIUrl() {
        return "http://dismu-head-nodejs-85224.euw1.nitrousbox.com/api/";
    }

    public static String getRemoteIP() throws IOException {
        URL whatismyip = new URL("http://checkip.amazonaws.com/");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                whatismyip.openStream()));

        String ip = in.readLine(); //you get the IP as a String
        return ip;
    }
}

package com.dismu.utils;

import com.dismu.logging.Loggers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import java.lang.reflect.InvocationTargetException;

public class Utils {
    static PlatformUtils platformUtils = null;

    public static synchronized void setPlatformUtils(PlatformUtils cl) {
        platformUtils = cl;
    }

    public static JSONObject sendJSONRequest(String address, String s) {
        try {
            String type = "application/json";
            URL u = new URL(address);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", type);
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
            return (JSONObject)parser.parse(str);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks if current OS is Windows
     * @return true, if Windows, otherwise false
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    public static boolean isPC() {
        return Utils.isLinux() || Utils.isWindows() || Utils.isMac();
    }

    /**
     * Returns folder for application data, if it hasn't created yet, creates it.
     * @return File application data folder's path.
     */
    public synchronized static File getAppFolderPath() {
        try {
            return platformUtils.getAppFolderPath();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
        /*File appFolder;
        
        if (Utils.isWindows()) {
            appFolder = new File(System.getenv("APPDATA"), ".dismu");
        } else {
            appFolder = new File(System.getProperty("user.home"), ".dismu");
        }
        if (!appFolder.exists()) {
            appFolder.mkdirs();
        }
        return appFolder;*/
    }

    /**
     * Copies file, if destination file doesn't exists creates it.
     * @param sourceFile source file
     * @param destinationFile file to copy
     */
    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        if (sourceFile.equals(destinationFile)) {
            return;
        }
        if (!destinationFile.exists()) {
            destinationFile.createNewFile();
        }
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destinationFile).getChannel();
        destination.transferFrom(source, 0, source.size());
    }

    public static String getMasterServerAPIUrl() {
        return "http://dismu.herokuapp.com/api/";
    }

    /**
     * Returns remote IP of current host.
     * @return String, containing IP address
     */
    public static String getRemoteIP() throws IOException {
        URL whatismyip = new URL("http://checkip.amazonaws.com/");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                whatismyip.openStream()));

        return in.readLine(); //you get the IP as a String
    }

    /**
     * Get Adler32 hash of input stream.
     * @param stream stream to get hash
     * @return hash of stream
     * @throws IOException
     */
    public static long getAdler32StreamHash(InputStream stream) throws IOException {
        int readCount;
        byte[] chunk = new byte[1024];
        Checksum checksum = new Adler32();
        while ((readCount = stream.read(chunk)) != -1) {
            checksum.update(chunk, 0, readCount);
        }
        return checksum.getValue();
    }

    /**
     * Get Adler32 hash of file.
     * @param file file to get hash
     * @return hash of file
     * @throws IOException
     */
    public static long getAdler32FileHash(File file) throws IOException {
        return getAdler32StreamHash(new BufferedInputStream(new FileInputStream(file)));
    }

    public static byte[] readStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    public static Thread runThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }

    private static String[] splitFilename(String filename) {
        return filename.split("\\.(?=[^\\.]+$)");
    }

    public static String fileExtension(String filename) {
        String[] tmp = splitFilename(filename);
        if (tmp.length == 1) {
            return null;
        }
        return tmp[1];
    }

    public static String fileBasename(String filename) {
        return splitFilename(filename)[0];
    }

    public static String titleCase(String string) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : string.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    public static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight) {
        return createResizedCopy(originalImage, scaledWidth, scaledHeight, false);
    }

    public static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();
        if (preserveAlpha) {
            g.setComposite(AlphaComposite.Src);
        }
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }

    public static boolean openInBrowser(String uri) {
        if (isLinux()) {
            Runtime runtime = Runtime.getRuntime();
            try {
                // TODO: escape symbols in uri
                runtime.exec(new String[] {"sh", "-c", String.format("xdg-open %s", uri)});
            } catch (IOException e) {
                Loggers.miscLogger.error("cannot open uri '{}'");
                return false;
            }
        } else {
            try {
                Desktop.getDesktop().browse(new URI(uri));
            } catch (IOException | URISyntaxException ex) {
                Loggers.miscLogger.error("open uri '{}'", uri, ex);
                return false;
            }
        }
        return true;
    }
}

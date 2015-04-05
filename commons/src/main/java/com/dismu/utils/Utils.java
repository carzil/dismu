package com.dismu.utils;

import com.dismu.logging.Loggers;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    static PlatformUtils platformUtils = null;
    private static final int HASH_SEED = 0x9747b28c;
    private static final XXHashFactory hashFactory = XXHashFactory.fastestInstance();
    public static final int MEGABYTE = 1 << 20;

    public static synchronized void setPlatformUtils(PlatformUtils cl) {
        platformUtils = cl;
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
    public static long getStreamHash64(InputStream stream) throws IOException {
        int readCount;
        byte[] chunk = new byte[8192];
        StreamingXXHash64 hasher = hashFactory.newStreamingHash64(HASH_SEED);
        hasher.reset();
        while ((readCount = stream.read(chunk)) != -1) {
            hasher.update(chunk, 0, readCount);
        }
        return hasher.getValue();
    }

    /**
     * Get Adler32 hash of file.
     * @param file file to get hash
     * @return hash of file
     * @throws IOException
     */
    public static long getFileHash64(File file) throws IOException {
        return getStreamHash64(new BufferedInputStream(new FileInputStream(file)));
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

    public static String getSalt() {
        return "$deedbeaf12341";
    }

    public static String getMD5(String string) {
        try {
            MessageDigest md5hasher = MessageDigest.getInstance("MD5");
            md5hasher.update(string.getBytes());
            StringBuilder stringBuffer = new StringBuilder();
            for (byte b : md5hasher.digest()) {
                stringBuffer.append(String.format("%02x", b & 0xff));
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            Loggers.miscLogger.error("cannot get md5 hash", e);
            return null;
        }
    }

    public static Thread runThread(Runnable runnable) {
        // TODO: rewrite this with cached pool
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }

    public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        int readSize;
        byte[] buffer = new byte[4096];
        while ((readSize = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, readSize);
        }
        outputStream.flush();
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
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

    public static String getOsInfo() {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        return String.format("OS: %s (%s), OS Version: %s", osName, osArch, osVersion);
    }

    public static String getDismuVersion() {
        return "v0.1";
    }

    public static String getMasterServerUrl() {
//        return "http://dismu.herokuapp.com/api/";
        return "http://localhost:3000/";
    }

    public static String getInfo() {
        return String.format("Dismu %s, %s", getDismuVersion(), getOsInfo());
    }

    public static String getSignUpUrl() {
        return getMasterServerUrl() + "signup/";
    }
}

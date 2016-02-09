package com.beth.smartthings_msband;

import android.content.Context;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by beth on 1/13/16.
 */
public class Helpers {
    // hardcoded for now because lol
    private static final String authCode = "Bearer b0fe3e21-d0b0-4a9d-a64c-c15715878203";
    private static final String baseUrl = "https://graph.api.smartthings.com/api/smartapps/installations/f51d420e-c732-45a2-abf9-414763075a64/";

    private static Map<UUID, String> uuidsToNames = null;

    public static BandClient connectBandClient(Context context) {
        BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
        if (devices.length == 0) {
            return null; // TODO: Probably exception handling here
        }
        return BandClientManager.getInstance().create(context, devices[0]);
    }

    public static boolean hasConnectedBandClient(BandClient client, Context context) throws InterruptedException, BandException {
        if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        return ConnectionState.CONNECTED == client.connect().await();
    }


    public static String apiCall(String urlString) {
        try {
            URL url = new URL(baseUrl + urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", authCode);
            urlConnection.setRequestMethod("GET");
            try {
                int responseCode = urlConnection.getResponseCode();
                String responseMessage = urlConnection.getResponseMessage();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseMessage = readStream(urlConnection.getInputStream());
                }

                return responseMessage;
            } catch (Exception e) {
                Log.d("tile event", e.toString());
            } finally {
                urlConnection.disconnect();
            }
        } catch (Exception ex) {

        }
        return null;
    }

    private static String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

    private static Map<UUID, String> readPageNameHashesFromFile() {

        Log.e("read hashes from file", "printing before");
        printMap();
        Map<UUID, String> map = new HashMap<UUID, String>();

        try {
            FileInputStream fis = new FileInputStream("/data/user/0/com.beth.smartthings_msband/files/pageUuidMap/smartthings_msband_pagenamehashes");
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (Map<UUID, String>) ois.readObject();
            ois.close();
            Log.e("read hashes from file", "printing after");
            printMap();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("read hashes from file - exception", e.toString());
        }

        return map;
    }

    private static void writePageNameHashesToFile(Map<UUID, String> map) {
        Log.e("Helpers", "writePageNameHashesToFile");
        if (uuidsToNames != null) {
            String mapString = "Map: ";
            for (Map.Entry<UUID, String> entry : uuidsToNames.entrySet()) {
                mapString += entry.toString();
                mapString += "\n";
            }
            Log.e("Helpers", mapString);
        } else {
            Log.e("Helpers", "Map is null :(");
        }

        if (map == null) { // TODO: Do I need this?
            map = new HashMap<UUID, String>();
        }

        try {
            FileOutputStream fos = new FileOutputStream("/data/user/0/com.beth.smartthings_msband/files/pageUuidMap/smartthings_msband_pagenamehashes");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
            oos.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private static void ensureMapIsUpToDate() {
        // TODO: fix this
        //if (uuidsToNames == null) {
            uuidsToNames = readPageNameHashesFromFile();
        //}
    }

    private static void updateMapFile() {
        Log.e("Update map file", "updating");
        printMap();
        writePageNameHashesToFile(uuidsToNames);
    }

    public static void putValueMap (UUID key, String value) {
        Log.e("putValueMap", value);
        ensureMapIsUpToDate();
        uuidsToNames.put(key, value);
        updateMapFile();
    }

    public static UUID getKeyByValueMap (String value) {
        ensureMapIsUpToDate();
        Log.e("Get key by value map", value);
        printMap();
        UUID key = null;
        for (Map.Entry<UUID, String> entry : uuidsToNames.entrySet()) {
            if (entry.getValue().equals(value)) {
                key = entry.getKey();
                break;
            }
        }
        return key;
    }

    public static String getValueByKeyMap (UUID key) {
        ensureMapIsUpToDate();
        return uuidsToNames.get(key);
    }

    public static boolean containsValueMap (String value) {
        ensureMapIsUpToDate();
        return uuidsToNames.containsValue(value);
    }

    public static boolean containsKeyMap(UUID key) {
        ensureMapIsUpToDate();
        Log.e("Contains key map", key.toString());
        printMap();
        return uuidsToNames.containsKey(key);
    }

    public static void clearMap() {
        if (uuidsToNames != null) {
            uuidsToNames.clear();
        }

        uuidsToNames = new HashMap<UUID, String>();
        writePageNameHashesToFile(uuidsToNames);
    }

    public static void printMap() {
        if (uuidsToNames != null) {
            String mapString = "Map: ";
            for (Map.Entry<UUID, String> entry : uuidsToNames.entrySet()) {
                mapString += entry.toString();
                mapString += "\n";
            }
            Log.e("Helpers - printMap", mapString);
        } else {
            Log.e("Helpers", "Map is null :(");
        }

    }
}

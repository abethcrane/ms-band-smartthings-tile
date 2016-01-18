package com.beth.smartthings_msband;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by beth on 1/13/16.
 */
public class Helpers {
    // hardcoded for now because lol
    private static final String authCode = "Bearer b0fe3e21-d0b0-4a9d-a64c-c15715878203";
    private static final String baseUrl = "https://graph.api.smartthings.com/api/smartapps/installations/f51d420e-c732-45a2-abf9-414763075a64/";


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
}

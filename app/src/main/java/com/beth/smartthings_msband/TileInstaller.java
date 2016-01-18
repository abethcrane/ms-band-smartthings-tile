package com.beth.smartthings_msband;
import com.beth.smartthings_msband.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.BandTileManager;
import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.FilledButtonData;
import com.microsoft.band.tiles.pages.TextButtonData;
import com.microsoft.band.tiles.pages.FilledButton;
import com.microsoft.band.tiles.pages.TextButton;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by beth on 12/24/15.
 */
public class TileInstaller extends Activity {

    private BandClient client = null;
    private Button btnStart;
    private Button btnSmartThings;
    private TextView txtStatus;

    private static final String clientID = "a9013b57-e3e2-42e5-a8df-00455dbff18e";
    private static final String clientSecret = "aa11e637-bcc7-4f14-a98e-7eef30e1bfe1";

    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static final UUID pageId1 = UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");
   // private UUID[] pageUUIDs = new UUID[10]; // what is max num of pages?
    //private int numPages = 0;

    // TODO on first startup i already have pages, but don't know their uuids
    // And don't know what names they correspond to. Maybe store this data to disk.
    private JSONArray switchesArray = new JSONArray();
    public static Map<String, UUID> namesToUuids = new HashMap<String, UUID>();
    public static Map<UUID, String> uuidToNames  = new HashMap<UUID, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.txtStatus);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("");
                new GetSwitches().execute("switches");
            }
        });

        btnSmartThings = (Button)   findViewById(R.id.btnSmartThings);

/*
        AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                new NetHttpTransport(), new JacksonFactory(),
                new GenericUrl("https://graph.api.smartthings.com/oauth/token"),
                new BasicAuthentication(clientID, clientSecret),
                clientID,
                "https://graph.api.smartthings.com/oauth/authorize").setCredentialDataStore(
                StoredCredential.getDefaultDataStore(
                        new FileDataStoreFactory(
                                new File("datastoredir")
                        )
                )
        ).build();*/
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private class appTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    if (addTile()) {
                        updatePages();
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case DEVICE_ERROR:
                        exceptionMessage = "Please make sure bluetooth is on and the band is in range.\n";
                        break;
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    case BAND_FULL_ERROR:
                        exceptionMessage = "Band is full. Please use Microsoft Health to remove a tile.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.append(string);
            }
        });
    }

    private boolean doesTileExist(List<BandTile> tiles, UUID tileId) {
        for (BandTile tile:tiles) {
            if (tile.getTileId().equals(tileId)) {
                return true;
            }
        }
        return false;
    }

    private boolean addTile() throws Exception {
        if (doesTileExist(client.getTileManager().getTiles().await(), tileId)) {
            return true;
        }

		/* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap tileIcon = BitmapFactory.decodeResource(getBaseContext().getResources(), R.raw.b_icon, options);

        BandTile tile = new BandTile.Builder(tileId, "Button Tile", tileIcon)
                .setPageLayouts(createButtonLayout())
                .build();
        appendToUI("Button Tile is adding ...\n");
        if (client.getTileManager().addTile(this, tile).await()) {
            appendToUI("Button Tile is added.\n");
            return true;
        } else {
            appendToUI("Unable to add button tile to the band.\n");
            return false;
        }
    }

    private PageLayout createButtonLayout() {
        return new PageLayout(
                new FlowPanel(15, 0, 260, 105, FlowPanelOrientation.VERTICAL)
                        //.addElements(new FilledButton(0, 0, 210, 45).setMargins(0, 5, 0, 0).setId(13).setBackgroundColor(Color.WHITE))
                        .addElements(new TextButton(0, 0, 210, 45).setMargins(0, 5, 0, 0).setId(12).setPressedColor(Color.WHITE))
                        .addElements(new FilledButton(0, 5, 210, 45).setMargins(0, 5, 0, 0).setId(21).setBackgroundColor(Color.GREEN))
        );
    }

    private void updatePages() throws BandIOException {
        // TODO: Sort array alphabetically by name
        // Check if my existing page names are in there
        // If so, do nothing, if not, add them in to the pages list
        client.getTileManager().removePages(tileId);
        appendToUI("Just removed old pages");

        int numSwitches = switchesArray.length();
        for (int i = 0; i < numSwitches; i++) {
            try {JSONObject switchElement = switchesArray.getJSONObject(i);
                String name = (String) switchElement.get("name");
                UUID pageUuid;
                // If we've seen this switch before just update its page, no need to recreate it
                if (namesToUuids.containsKey(name)) {
                    pageUuid = namesToUuids.get(name);
                } else { // If this is our first time seeing this switch, let's add it in
                    pageUuid = UUID.randomUUID();
                    namesToUuids.put(name, pageUuid);
                    uuidToNames.put(pageUuid, name);
                }
                String value = (String) switchElement.get("value");

                int layoutNum = 12;
                int colorValue = Color.WHITE;
                if (value == "on") {
                    layoutNum = 12;
                    colorValue = Color.GREEN;
                }

                client.getTileManager().setPages(
                        tileId,
                        new PageData(pageUuid, 0)
                                .update(new FilledButtonData(layoutNum, colorValue))
                                .update(new TextButtonData(21, name))
                );
            } catch (Exception ex) {
            }
        }
        appendToUI("I just fetched the switch data!\n\n");
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private void updateSwitches(String switches) {
        try {
            switchesArray = new JSONArray(switches);
            new appTask().execute();
        } catch (Exception ex) {
            Log.d("get switches", ex.toString()); // probably if it was an error code? idek handle that.
        }
    }

    private class GetSwitches extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return Helpers.apiCall(params[0]);
        }


        protected void onPostExecute(String result) {
            updateSwitches(result);
        }

    }

}


package com.beth.smartthings_msband;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.TextBlock;
import com.microsoft.band.tiles.pages.TextBlockFont;
import com.microsoft.band.tiles.pages.TextButton;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Created by beth on 12/24/15.
 */
public class TileInstaller extends Activity {

    private BandClient client = null;
    private TextView txtStatus;

    private static final String clientID = "a9013b57-e3e2-42e5-a8df-00455dbff18e";
    private static final String clientSecret = "aa11e637-bcc7-4f14-a98e-7eef30e1bfe1";

    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private static int maxPages = 8;

    private TileUpdater tileUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.txtStatus);

        Button btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("Establishing connection / first time info pull down\n\n");
                new InitializeApp().execute();
                /*
                // oauth. unicorn tears.
                try {
                    final AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
                        BearerToken.authorizationHeaderAccessMethod(),
                        new NetHttpTransport(), new JacksonFactory(),
                        new GenericUrl("https://graph.api.smartthings.com/oauth/token"),
                        new BasicAuthentication(clientID, clientSecret),
                        clientID,
                        "https://graph.api.smartthings.com/oauth/token") //authorize to get the authcode, token to use the code
                        /*.setCredentialDataStore(
                            StoredCredential.getDefaultDataStore(
                                new FileDataStoreFactory(
                                    new File("datastoredir")
                                )
                            )
                        .build();
                    /*
                    String authorizationUrl = flow.newAuthorizationUrl()
                            .setRedirectUri("https://bethcrane.com/foo")
                            .setScopes(new ArrayList<>(Arrays.asList("app")))
                            .build();

                    final String authorizationCode = "AKoplE"; // we cheated
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                TokenResponse tokenResponse = flow.newTokenRequest(authorizationCode)
                                        .setScopes(new ArrayList<>(Arrays.asList("app")))
                                        .setRedirectUri("https://bethcrane.com/bar")
                                        .execute();

                                return;
                            } catch (Exception e) {
                                return;
                            }
                        }
                    });
                    t.start();

                    return;

                } catch (Exception e) {
                 // throw e;
                    Log.e("error creating authURL",e.getMessage());
                    appendToUI("error creating authURL"+e.getMessage());
                    return;
                }*/
            }
        });

        Button btnReinstall = (Button) findViewById(R.id.btnReinstall);

        btnReinstall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            txtStatus.setText("Deleting / reinstalling tile\n\n");

            new InitializeAppDeleteTile().execute();
            }
        });
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
            } catch (Exception e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private class InitializeApp extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                client = Helpers.connectBandClient(getBaseContext());
                if (Helpers.hasConnectedBandClient(client, getBaseContext())) {
                    tileUpdater = new TileUpdater(client, getBaseContext());
                    appendToUI("Band is connected.\n");
                    // Create the file in case it doesn't already exist
                    File myDir = getFilesDir();
                    File f = new File(myDir+"/pageUuidMap/", "smartthings_msband_pagenamehashes");
                    if (f.getParentFile().mkdirs()) {
                        f.createNewFile();
                    }
                    // Add if it doesn't exist
                    addTile();
                    tileUpdater.resetPages();
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                HandleBandException(e);
            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private class InitializeAppDeleteTile extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                client = Helpers.connectBandClient(getBaseContext());
                if (Helpers.hasConnectedBandClient(client, getBaseContext())) {
                    tileUpdater = new TileUpdater(client, getBaseContext());
                    appendToUI("Band is connected.\n");
                    // Create the file in case it doesn't already exist
                    FileOutputStream fos = openFileOutput("smartthings_msband_pagenamehashes", Context.MODE_APPEND);
                    fos.close();
                    List<BandTile> tiles = client.getTileManager().getTiles().await();
                    for(BandTile t : tiles) {
                        if(client.getTileManager().removeTile(t).await()){
                            appendToUI("Just removed an old tile\n");
                        }
                    }
                    addTile();
                    tileUpdater.resetPages();
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                HandleBandException(e);
            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

    private void HandleBandException(BandException e) {
        String exceptionMessage;
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
                .addElements(new TextBlock(0, 0, 210, 45, TextBlockFont.SMALL).setMargins(0, 5, 0, 0).setId(1))
                .addElements(
                    new FlowPanel(0, 0, 210, 50, FlowPanelOrientation.HORIZONTAL)
                    .addElements(new TextBlock(0, 0, 45, 45, TextBlockFont.SMALL).setMargins(0, 5, 0, 0).setId(2))
                    .addElements(new TextButton(0, 0, 80, 45).setMargins(5, 5, 0, 0).setId(3))
                    .addElements(new TextButton(0, 0, 80, 45).setMargins(5, 5, 0, 0).setId(4))
                )
        );
    }



}


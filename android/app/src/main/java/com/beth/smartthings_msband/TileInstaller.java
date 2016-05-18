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

import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

/**
 * Created by beth on 12/24/15.
 */
public class TileInstaller extends Activity {

    private BandClient client = null;
    private TextView txtStatus;

    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");

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
                txtStatus.setText("Establishing connection / force sync with smartthings\n\n");
                new InitializeApp(false /*do not delete tile*/).execute();
            }
        });

        Button btnReinstall = (Button) findViewById(R.id.btnReinstall);

        btnReinstall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            txtStatus.setText("Deleting / reinstalling tile\n\n");

            new InitializeApp(true /*delete tile*/).execute();
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

        boolean deleteTile;

        public InitializeApp(boolean deleteTile) {
            super();
            this.deleteTile = deleteTile;
        }

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

                    if (deleteTile) {
                        for (BandTile t : tiles) {
                            if (client.getTileManager().removeTile(t).await()) {
                                appendToUI("Just removed an old tile\n");
                            }
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
        Bitmap tileIcon = BitmapFactory.decodeResource(getBaseContext().getResources(), R.raw.lightbulb_icon_white, options);

        BandTile tile = new BandTile.Builder(tileId, "SmartThings Tile", tileIcon)
                .setPageLayouts(createButtonLayout())
                .build();
        appendToUI("SmartThings Tile is adding ...\n");
        if (client.getTileManager().addTile(this, tile).await()) {
            appendToUI("SmartThings Tile is added.\n");
            return true;
        } else {
            appendToUI("Unable to add SmartThings Tile to the band.\n");
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


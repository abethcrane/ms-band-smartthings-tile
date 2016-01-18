package com.beth.smartthings_msband;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import com.microsoft.band.tiles.TileButtonEvent;
import com.microsoft.band.tiles.TileEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Created by beth on 12/24/15.
 */
public class TileEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == "com.microsoft.band.action.ACTION_TILE_OPENED") {
            // handle tile opened event
        }
        else if (intent.getAction() == "com.microsoft.band.action.ACTION_TILE_BUTTON_PRESSED") {
            Bundle b = intent.getExtras();
            TileButtonEvent data = b.getParcelable("TILE_EVENT_DATA"); // Tile Name, Tile ID, Page ID, Element ID
            UUID pageUuid = data.getPageID();
            String switchName = "3%20Lights%20Together";
            // TODO - convert spaces to %20!
            if (TileInstaller.uuidToNames.containsKey(pageUuid)) {
                switchName = TileInstaller.uuidToNames.get(pageUuid);
            }

            switchName = Uri.encode(switchName);
            Toast.makeText(context, switchName + " button pressed!", Toast.LENGTH_SHORT).show();
            final String finalSwitchName = switchName;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Helpers.apiCall("toggle/" + finalSwitchName);
                }
            } );
            t.start();
        }
        else if (intent.getAction() == "com.microsoft.band.action.ACTION_TILE_CLOSED") {
            // handle tile closed event
        }
    }



}

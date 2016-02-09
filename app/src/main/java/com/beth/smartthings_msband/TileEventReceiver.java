package com.beth.smartthings_msband;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.tiles.TileButtonEvent;

import java.util.UUID;

/**
 * Created by beth on 12/24/15.
 */
public class TileEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() == "com.microsoft.band.action.ACTION_TILE_BUTTON_PRESSED") {
            Bundle b = intent.getExtras();
            TileButtonEvent data = b.getParcelable("TILE_EVENT_DATA"); // Tile Name, Tile ID, Page ID, Element ID
            UUID pageUuid = data.getPageID();
            Toast.makeText(context, "Looking up " + pageUuid.toString(), Toast.LENGTH_SHORT).show();
            String switchName = "<switchname>";
            if (Helpers.containsKeyMap(pageUuid)) {
                switchName = Helpers.getValueByKeyMap(pageUuid);
            } else {
                Toast.makeText(context, "Trying to print the map", Toast.LENGTH_SHORT).show();
                Helpers.printMap();
            }

            int buttonId = data.getElementID();
            final String value;
            if (buttonId == 3) {
                value = "on";
            } else {
                value = "off";
            }

            Toast.makeText(context, switchName + " turned " + value + "!", Toast.LENGTH_SHORT).show();
            final String encodedSwitchName = Uri.encode(switchName);
            final String name = switchName;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO: check success of the call v and then update the value. It'll be faster
                    Helpers.apiCall("set/" + encodedSwitchName + "/" + value);
                    try {
                        BandClient client = Helpers.connectBandClient(context);
                        TileUpdater tileUpdater = new TileUpdater(client, context);
                        tileUpdater.updateSwitchesUI();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } );
            t.start();
        }
    }



}

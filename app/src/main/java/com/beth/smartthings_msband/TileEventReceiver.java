package com.beth.smartthings_msband;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.microsoft.band.tiles.TileButtonEvent;

import java.io.File;
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

            // There's no point doing anything if we don't have an accurate switchName
            File folder = context.getExternalFilesDir(null);
            if (Helpers.containsKeyMap(pageUuid, folder)) {
                String switchName = Helpers.getValueByKeyMap(pageUuid, folder);

                int buttonId = data.getElementID();
                String value;
                if (buttonId == 3) {
                    value = "on";
                } else {
                    value = "off";
                }

                // We can't access the band from a broadcast receiver, so we create an intent to do that
                Intent updateUiIntent = new Intent(context, UpdateUIIntent.class);
                updateUiIntent.putExtra("encodedSwitchName", Uri.encode(switchName));
                updateUiIntent.putExtra("value", value);
                context.startService(updateUiIntent);
            }
        }
    }
}

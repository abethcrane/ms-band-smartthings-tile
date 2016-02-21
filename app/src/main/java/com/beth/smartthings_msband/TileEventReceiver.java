package com.beth.smartthings_msband;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
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

            // There's no point doing anything if we don't have an accurate switchName
            if (Helpers.containsKeyMap(pageUuid)) {
                String switchName = Helpers.getValueByKeyMap(pageUuid);

                int buttonId = data.getElementID();
                String value;
                if (buttonId == 3) {
                    value = "on";
                } else {
                    value = "off";
                }

                Toast.makeText(context, switchName + " turned " + value + "!", Toast.LENGTH_SHORT).show();

                Intent updateUiIntent = new Intent(context, UpdateUIIntent.class);
                updateUiIntent.putExtra("encodedSwitchName", Uri.encode(switchName));
                updateUiIntent.putExtra("value", value);
                context.startService(updateUiIntent);
            }
        }
    }



}

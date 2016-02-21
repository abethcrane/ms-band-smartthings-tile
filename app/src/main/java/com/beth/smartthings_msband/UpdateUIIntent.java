package com.beth.smartthings_msband;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.tiles.TileButtonEvent;

import java.util.UUID;

/**
 * Created by beth on 12/24/15.
 */
public class UpdateUIIntent extends IntentService {

    public UpdateUIIntent() {
        super("UpdateUIIntent");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String encodedSwitchName = intent.getStringExtra("encodedSwitchName");
        String value = intent.getStringExtra("value");

        // TODO: check success of the call v and then update only that value. It'll be faster ?
        Helpers.apiCall("set/" + encodedSwitchName + "/" + value);
        try {
            BandClient client = Helpers.connectBandClient(this);
            if (Helpers.hasConnectedBandClient(client, this)) {
                TileUpdater tileUpdater = new TileUpdater(client, this);
                tileUpdater.updateSwitchesUI();
            } else {
                Log.e("event receiver", "No band client");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

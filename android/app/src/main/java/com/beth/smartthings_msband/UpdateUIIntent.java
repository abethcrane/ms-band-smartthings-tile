package com.beth.smartthings_msband;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.microsoft.band.BandClient;

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

        Helpers.apiCall("set/" + encodedSwitchName + "/" + value);
        try {
            BandClient client = Helpers.connectBandClient(this);
            if (Helpers.hasConnectedBandClient(client, this)) {
                TileUpdater tileUpdater = new TileUpdater(client, this);
                tileUpdater.updateSwitchesUI();
            } else {
                Log.e("Event receiver", "No band client");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

package com.beth.smartthings_msband;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.TextBlockData;
import com.microsoft.band.tiles.pages.TextButtonData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by beth on 2/8/16.
 */
public class TileUpdater {

    private static final UUID tileId = UUID.fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    private BandClient client;
    private Context context;

    public TileUpdater(BandClient client, Context context) {
        this.client = client;
        this.context = context;
    }

    public void resetPages() throws BandIOException {
        client.getTileManager().removePages(tileId);
        Helpers.clearMap();
        updateSwitchesUI();
    }

    private void updatePage(UUID pageUuid, String name, String value, int hoverColor) throws BandIOException {
        Log.e("update page", name + " to be " + value);

        // We always make a new page data because we can't update the existing elements
        PageData page = new PageData(pageUuid, 0);
        page
            .update(new TextBlockData(1, name))
            .update(new TextBlockData(2, value))
            .update(new TextButtonData(3, "On"))
            .update(new TextButtonData(4, "Off"));

        // Currently updating existing page uuids isn't working, I keep getting BandIOExceptions
        // Except it's not throwing them properly?? Anyway, break in and you see it
        client.getTileManager().setPages(tileId, page);
    }

    public void updatePages(JSONArray switchesArray) {
        int numSwitches = switchesArray.length();
        for (int i = 0; i < numSwitches; i++) {
            String name = null;
            String value = null;
            try {
                JSONObject switchElement = switchesArray.getJSONObject(i);
                name = (String) switchElement.get("name");
                value = (String) switchElement.get("value");
                value += ".";

                UUID pageUuid = null;

                // If we've seen this switch before just update its page, no need to recreate it
                if (Helpers.containsValueMap(name)) {
                    pageUuid = Helpers.getKeyByValueMap(name);
                }

                if (pageUuid == null) { // If this is our first time seeing this switch, let's add it in
                    pageUuid = UUID.randomUUID();
                    Helpers.putValueMap(pageUuid, name);
                }
                updatePage(pageUuid, name, value.toUpperCase(), Color.BLACK);
            } catch (Exception e) {
                Log.e("update page", e.toString());
            }
        }
    }

    public void updateSwitchesUI() {
        new UpdateSwitchesUI().execute("switches");
    }

    private class UpdateSwitchesUI extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return Helpers.apiCall(params[0]);
        }

        protected void onPostExecute(String result) {
            try {
                updatePages(new JSONArray(result));
            } catch (Exception ex) {
                Log.d("get switches", ex.toString()); // probably if it was an error code? idek handle that.
            }
        }

    }
}

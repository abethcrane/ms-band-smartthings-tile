using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Microsoft.Band;
using Microsoft.Band.Tiles.Pages;
using Newtonsoft.Json.Linq;
using Windows.Storage;

namespace TileEvents.Universal
{
    class TileUIUpdater
    {

        private Guid tileId;
        private IBandClient client;

        public TileUIUpdater(IBandClient client, Guid id)
        {
            this.client = client;
            this.tileId = id;
        }

        public void resetPages()
        {
            client.TileManager.RemovePagesAsync(tileId);
            StorageFolder folder = ApplicationData.Current.LocalFolder;
            Helpers.clearDictionary(folder);
            string apiCallResult = Helpers.apiCall("switches");
            updatePages(JArray.Parse(apiCallResult));
        }

        private async void updatePage(Guid pageUuid, string name, string value)
        {
            // We always make a new page data because we can't update the existing elements //TODO: is this true in windows??
            PageData page = new PageData(
                pageUuid,
                0,
                new TextBlockData(1, name),
                new TextBlockData(2, value),
                new TextButtonData(3, "On"),
                new TextButtonData(4, "Off")
            );

            try
            {
                await client.TileManager.SetPagesAsync(tileId, page);
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine(ex.ToString());
            }
           
        }

        public void updatePages(JArray switchesArray)
        {
            StorageFolder folder = ApplicationData.Current.LocalFolder;
            
            foreach (JObject switchElement in switchesArray)
            {
                string name = (string)switchElement["name"];
                string value = (string)switchElement["value"] + ".";

                Guid pageUuid;

                // If we've seen this switch before just update its page, no need to recreate it
                if (Helpers.containsValueDictionary(name, folder))
                {
                    pageUuid = Helpers.getKeyByValueDictionary(name, folder);
                }

                if (pageUuid == Guid.Empty)
                { // If this is our first time seeing this switch, let's add it in
                    pageUuid = Guid.NewGuid();
                    Helpers.putValueDictionary(pageUuid, name, folder);
                }
                updatePage(pageUuid, name, value.ToUpper());
            }
        }
    }
}

/*
    Copyright (c) Microsoft Corporation All rights reserved.  
 
    MIT License: 
 
    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
    and to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
 
    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. 
 
    THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
    TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
    THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
    TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

using Microsoft.Band;
using Microsoft.Band.Tiles;
using Microsoft.Band.Tiles.Pages;
using System;
using System.Threading.Tasks;
using Windows.Storage;
using Windows.Storage.Streams;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Media.Imaging;
using TileEvents.Universal;
using Newtonsoft.Json.Linq;

namespace TileEvents
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    partial class MainPage
    {
        private App viewModel;
        private bool handlingClick;
        private int buttonPressedCount = 0;
        private IBandClient bandClient;
        private Guid myTileId = new Guid("497B746E-4F5F-44D4-96E2-FC46D407B6E3"); // new Guid();

        private void Sync (object sender, RoutedEventArgs e)
        {
            Initialize(sender, e, false);
        }

        private void Reinstall(object sender, RoutedEventArgs e)
        {
            Initialize(sender, e, true);
        }

        private async void Initialize(object sender, RoutedEventArgs e, bool deleteTile)
        {
            if (handlingClick)
            {
                return;
            }

            this.viewModel.StatusMessage = "Running ...";

            handlingClick = true;
            try
            {
                IBandInfo[] pairedBands = await BandClientManager.Instance.GetBandsAsync();
                bandClient = await BandClientManager.Instance.ConnectAsync(pairedBands[0]);
                
                if (pairedBands.Length < 1)
                {
                    this.viewModel.StatusMessage = "This sample app requires a Microsoft Band paired to your device. Also make sure that you have the latest firmware installed on your Band, as provided by the latest Microsoft Health app.";
                    return;
                }
                
                BandTile myTile = new BandTile(myTileId)
                {
                    Name = "My Tile",
                    TileIcon = await LoadIcon("ms-appx:///Assets/SampleTileIconLarge.png"),
                    SmallIcon = await LoadIcon("ms-appx:///Assets/SampleTileIconSmall.png")
                };

                await BuildLayout(myTile);

                this.viewModel.StatusMessage = "Woo got the layout ...";

                // Remove the Tile from the Band, if present. An application won't need to do this everytime it runs. 
                // But in case you modify this sample code and run it again, let's make sure to start fresh.
                if (deleteTile)
                {
                    await bandClient.TileManager.RemoveTileAsync(myTileId);
                }
                if (!bandClient.TileManager.TileInstalledAndOwned(ref myTileId, new System.Threading.CancellationToken())) {
                    // Create the Tile on the Band.
                    await bandClient.TileManager.AddTileAsync(myTile);
                }
                     
                Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
                {
                    TileUIUpdater t = new TileUIUpdater(bandClient, myTileId);
                    t.resetPages();
                });

                this.viewModel.StatusMessage = "Woo reset the pages ...";

                // Subscribe to Tile events.
                TaskCompletionSource<bool> closePressed = new TaskCompletionSource<bool>();

                bandClient.TileManager.TileButtonPressed += EventHandler_TileButtonPressed;

                bandClient.TileManager.TileClosed += (s, args) => 
                {
                    closePressed.TrySetResult(true);
                };

                this.viewModel.StatusMessage = "Woo added the event handlers ...";
                try
                {
                    await bandClient.TileManager.StartReadingsAsync();
                } catch (Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine(ex.ToString());
                }
                /*
                // Receive events until the Tile is closed.
                this.viewModel.StatusMessage = "Check the Tile on your Band (it's the last Tile). Waiting for events ...";

                await closePressed.Task;
                    
                // Stop listening for Tile events.
                await bandClient.TileManager.StopReadingsAsync();

                this.viewModel.StatusMessage = "Done.";*/
            }
            catch (Exception ex)
            {
                this.viewModel.StatusMessage = ex.ToString();
            }
            finally
            {
                handlingClick = false;
            }
        }

        async void EventHandler_TileButtonPressed(object sender, BandTileEventArgs<IBandTileButtonPressedEvent> e)
        {
            // This method is called when the user presses the
            // button in our tile’s layout.
            //
            // e.TileEvent.TileId is the tile’s Guid.
            // e.TileEvent.Timestamp is the DateTimeOffset of the event.
            // e.TileEvent.PageId is the Guid of our page with the button.
            // e.TileEvent.ElementId is the value assigned to the button

            Guid pageId = e.TileEvent.PageId;
            StorageFolder folder = ApplicationData.Current.LocalFolder;

            if (Helpers.containsKeyDictionary(pageId, folder))
            {
                string switchName = Helpers.getValueByKeyDictionary(pageId, folder);

                string value;
                if (e.TileEvent.ElementId == 3)
                {
                    value = "on";
                }
                else
                {
                    value = "off";
                }

                Helpers.apiCall("set/" + Uri.EscapeUriString(switchName) + "/" + value);
                string apiCallResult = Helpers.apiCall("switches");

                //IBandInfo[] pairedBands = await BandClientManager.Instance.GetBandsAsync();
                //bandClient = await BandClientManager.Instance.ConnectAsync(pairedBands[0]);
                TileUIUpdater t = new TileUIUpdater(bandClient, myTileId);
                t.updatePages(JArray.Parse(apiCallResult));
            }
#pragma warning restore CS4014 // Because this call is not awaited, execution of the current method continues before the call is completed
        }



        private async Task BuildLayout(BandTile myTile)
        {
            FlowPanel panel = new FlowPanel() {
                Rect = new PageRect(15, 0, 260, 105),
                Orientation = FlowPanelOrientation.Vertical
            };

            panel.Elements.Add(new TextBlock
            {
                ElementId = 1,
                Rect = new PageRect(0, 0, 210, 45),
                Margins = new Margins(0, 5, 0, 0),
                //Color = new BandColor(0xFF, 0xFF, 0xFF),
                Font = TextBlockFont.Small
            });

            FlowPanel buttonsPanel = new FlowPanel()
            {
                Rect = new PageRect(0, 0, 210, 50),
                Orientation = FlowPanelOrientation.Horizontal
            };

            buttonsPanel.Elements.Add(new TextBlock
            {
                ElementId = 2,
                Rect = new PageRect(0, 0, 45, 45),
                Margins = new Margins(0, 5, 0, 0),
                Font = TextBlockFont.Small
            });

            buttonsPanel.Elements.Add(new TextButton
            {
                ElementId = 3,
                Rect = new PageRect(0, 0, 80, 45),
                Margins = new Margins(5, 5, 0, 0)
            });

            buttonsPanel.Elements.Add(new TextButton
            {
                ElementId = 4,
                Rect = new PageRect(0, 0, 80, 45),
                Margins = new Margins(5, 5, 0, 0)
            });

            panel.Elements.Add(buttonsPanel);
            
            myTile.PageLayouts.Add(new PageLayout(panel));
        }
        
        private PageData GetPageData()
        {
            PageData page = new PageData(
                new Guid("2211CB2A-70D3-4919-929B-ED397E3F6858"), 
                0,
                new TextBlockData(1, "test1"),
                new TextBlockData(2, "test2"),
                new TextButtonData(3, "test3"),
                new TextButtonData(4, "test4")
                );

            return page;
        }

        private async Task<BandIcon> LoadIcon(string uri)
        {
            StorageFile imageFile = await StorageFile.GetFileFromApplicationUriAsync(new Uri(uri));

            using (IRandomAccessStream fileStream = await imageFile.OpenAsync(FileAccessMode.Read))
            {
                WriteableBitmap bitmap = new WriteableBitmap(1, 1);
                await bitmap.SetSourceAsync(fileStream);
                return bitmap.ToBandIcon();
            }
        }

    }
}

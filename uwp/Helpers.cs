using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.IO;
using System.Text;
using Windows.Storage;

namespace WebHelpers
{
    using System.Threading;

    public static class ExtensionMethods
    {
        public static WebResponse GetResponse(this WebRequest request)
        {
            ManualResetEvent evt = new ManualResetEvent(false);
            WebResponse response = null;
            request.BeginGetResponse((IAsyncResult ar) => {
                response = request.EndGetResponse(ar);
                evt.Set();
            }, null);
            evt.WaitOne();
            return response as WebResponse;
        }

        public static Stream GetRequestStream(this WebRequest request)
        {
            ManualResetEvent evt = new ManualResetEvent(false);
            Stream requestStream = null;
            request.BeginGetRequestStream((IAsyncResult ar) => {
                requestStream = request.EndGetRequestStream(ar);
                evt.Set();
            }, null);
            evt.WaitOne();
            return requestStream;
        }
    }
}

namespace TileEvents
{
    using WebHelpers;

    class Helpers
    {
        // The "Bearer " is important. See http://docs.smartthings.com/en/latest/smartapp-web-services-developers-guide/authorization.html
        private static string authCode = "Bearer <>";
        // The final "/" is important. Make sure this is left there.
        private static string baseUrl = "https://graph.api.smartthings.com/api/smartapps/installations/<>/";

        private static Dictionary<Guid, string> GuidsToNames = null;
        private static string fileName = "smartthings_msband_pagenamehashes";

        public static string apiCall(string urlString)
        {
            Uri url = new Uri(baseUrl + urlString);
            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
            request.Headers["Authorization"] = authCode;
            request.Method = "GET";

            HttpWebResponse response = (HttpWebResponse)request.GetResponse();
            try
            {
                HttpStatusCode responseCode = response.StatusCode;
                string responseMessage = response.StatusDescription;

                if (responseCode == HttpStatusCode.OK)
                {
                    Stream receiveStream = response.GetResponseStream();

                    StreamReader readStream = new StreamReader(receiveStream, Encoding.UTF8);
                    responseMessage = readStream.ReadToEnd();
                }

                return responseMessage;
            }

            catch (Exception e)
            {
                System.Diagnostics.Debug.WriteLine(e.ToString());
            }

            return null;
        }

        private static async System.Threading.Tasks.Task<Dictionary<Guid, string>> readPageNameHashesFromFile(StorageFolder folder)
        {
            Dictionary<Guid, string> Dictionary = new Dictionary<Guid, string>();

            try
            {
                StorageFile file = await folder.GetFileAsync(fileName);
                string fileText = await FileIO.ReadTextAsync(file);
                Dictionary = JsonConvert.DeserializeObject<Dictionary<Guid, string>>(fileText);
                printDictionary();
            }
            catch (Exception e)
            {
                System.Diagnostics.Debug.WriteLine(e.ToString());
            }

            return Dictionary;
        }

        private static async void writePageNameHashesToFile(StorageFolder folder)
        {
            try
            {
                string dictionaryString = JsonConvert.SerializeObject(GuidsToNames);
                StorageFile file = await folder.CreateFileAsync(fileName, CreationCollisionOption.ReplaceExisting);
                await Windows.Storage.FileIO.WriteTextAsync(file, dictionaryString);
            }
            catch (Exception e)
            {
                System.Diagnostics.Debug.WriteLine(e.ToString());
            }
        }

        private static async void ensureDictionaryIsUpToDate(StorageFolder folder)
        {
            if (GuidsToNames == null)
            {
                GuidsToNames = await readPageNameHashesFromFile(folder);
            }
        }

        private static void updateDictionaryFile(StorageFolder folder)
        {
            writePageNameHashesToFile(folder);
        }

        public static void putValueDictionary(Guid key, string value, StorageFolder folder)
        {
            ensureDictionaryIsUpToDate(folder);
            GuidsToNames.Add(key, value);
            updateDictionaryFile(folder);
        }

        public static Guid getKeyByValueDictionary(string value, StorageFolder folder)
        {
            ensureDictionaryIsUpToDate(folder);
            Guid key = GuidsToNames.FirstOrDefault(x => x.Value == value).Key;
            return key;
        }

        public static string getValueByKeyDictionary(Guid key, StorageFolder folder)
        {
            ensureDictionaryIsUpToDate(folder);
            return GuidsToNames[key];
        }

        public static bool containsValueDictionary(string value, StorageFolder folder)
        {
            ensureDictionaryIsUpToDate(folder);
            return GuidsToNames.ContainsValue(value);
        }

        public static bool containsKeyDictionary(Guid key, StorageFolder folder)
        {
            ensureDictionaryIsUpToDate(folder);
            return GuidsToNames.ContainsKey(key);
        }

        public static void clearDictionary(StorageFolder folder)
        {
            if (GuidsToNames != null)
            {
                GuidsToNames.Clear();
            }

            GuidsToNames = new Dictionary<Guid, string>();
            writePageNameHashesToFile(folder);
        }

        public static void printDictionary()
        {
            if (GuidsToNames != null)
            {
                string DictionaryString = "Dictionary: ";
                foreach (KeyValuePair<Guid, string> kvp in GuidsToNames)
                {
                    DictionaryString += string.Format("Key = {0}, Value = {1}\n", kvp.Key, kvp.Value);
                }
                System.Diagnostics.Debug.WriteLine(DictionaryString);
            }
            else
            {
                System.Diagnostics.Debug.WriteLine("Dictionary is null :(");
            }

        }
    }
}

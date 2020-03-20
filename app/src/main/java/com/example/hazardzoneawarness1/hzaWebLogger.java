package com.example.hazardzoneawarness1;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class hzaWebLogger extends AsyncTask<HZALogData,Void,Void> {
    private static final String TAG = ".HZAWebLogger";
    //When a beacon is detected, log this event in the web server for OH&S staff to review
    //When the beacon is no longer detected, also log this event for OH&S staff to review
    //The two logged events could be used to know what kind of traffic is happening in a zone
    //and how much time staff spend in the zone. Maybe some hazardous materials could be stored elsewhere?

    //Need to send this data to the server: Title BeaconID, UserID, Entering or Exiting
    /*Beacon ID
    User ID
    person1
    Log Timestamp
    Date 2019-11-19
    Time 06:53:12 PM
    Lat
    Lon
    App Version
    Temperature °C
    Ambient Light Sensor
    NFC Data
    Humidity
    Pressure
    Air Quality
    Absolute Orientation
    Entering / Exiting
    */
    protected Void doInBackground(HZALogData... hzaLogDataIn){
        HZALogData hzaLogData = hzaLogDataIn[0];


        if(hzaLogData.getBeaconID().length() > 0) {
            //We have a beacon ID, so let's prepare fields to POST to the server
            Double lat = 0.0; //TODO Get last known GPS position
            Double lon = 0.0; //TODO Get last known GPS position

            //Get the current time in a usable format.
            //NOTE: The server is not auto populating the time using the current server time when left blank, so set it here.
            //SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            //Date rightNow = new Date();
            //The Drupal JSON API is expecting a UNIX timestamp (a number in LONG format) but Druapl is supposed to have changed away to support ISO RFC3339.
            Long unixTimestamp = System.currentTimeMillis() / 1000L;


            String _title = hzaLogData.getBeaconID().toUpperCase();
            String _field_log_beacon_id = hzaLogData.getBeaconID().toUpperCase();
            String _field_lat = lat.toString();
            String _field_lon =  lon.toString();
            String _field_log_user_id = hzaLogData.getUserID();
            String _field_entering_exiting = hzaLogData.getEntering().toString();
            String _field_app_version = Application.getProcessName();
            String _field_log_timestamp = unixTimestamp.toString(); //formatter.format(rightNow); //Server is not auto populating based on current server time so set it manually
            String _body = "";


            String payload = "{";
// build the data and data attributes part of the json request
            payload += "\"data\": {\"type\": \"node--beacon_log\",\"attributes\": {\"title\": \"" + _title + "\",";

            payload += "\"field_lat\": \"" + _field_lat + "\",";
            payload += "\"field_lon\": \"" + _field_lon + "\",";

            payload += "\"field_entering_exiting\": " + _field_entering_exiting + ",";
            payload += "\"field_app_version\": \"" + _field_app_version + "\",";
            payload += "\"field_log_timestamp\": \"" + _field_log_timestamp + "\","; //Server is not auto populating based on current server time so set it manually
            payload += "\"field_log_beacon_id\": \"" + _field_log_beacon_id + "\",";
            payload += "\"field_log_user_id\":  \"" + _field_log_user_id + "\",";
            payload += "\"body\": {\"value\": \"" + _body + "\",\"format\": \"plain_text\"}}"; //close body and attributes
            payload += "}}"; //match the first opening bracket and top data


            String usernameColonPassword = "hza_api:HZA_api_747";
            String basicAuthPayload = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
            String urlString = "http://myriad.myftp.org:747/jsonapi/node/beacon_log";

            OutputStream out = null;
            BufferedReader httpResponseReader = null;

            try {
                URL url = new URL(urlString);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.addRequestProperty("Authorization", basicAuthPayload);
                urlConnection.addRequestProperty("Accept", "application/vnd.api+json");
                urlConnection.addRequestProperty("Content-type", "application/vnd.api+json");
                urlConnection.setUseCaches( false );
                urlConnection.setDoOutput(true);


                //add the necessary details of the log data to POST to the server, aka payload
                out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(payload);
                writer.flush();
                writer.close();
                out.close();



                // Read response from web server, which will trigger HTTP Basic Authentication request to be sent.
                Log.d(TAG,"Posting beacon log to server: " + urlConnection.getURL().toString());
                Log.d(TAG,"Posting beacon log payload: " + payload);
                httpResponseReader =
                        new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                String lineRead;
                while((lineRead = httpResponseReader.readLine()) != null) {
                    Log.d(TAG,lineRead);
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {

                if (httpResponseReader != null) {
                    try {
                        httpResponseReader.close();
                    } catch (IOException ioe) {
                        // Close quietly
                    }
                }
            }


        }



        return null;
    }
}


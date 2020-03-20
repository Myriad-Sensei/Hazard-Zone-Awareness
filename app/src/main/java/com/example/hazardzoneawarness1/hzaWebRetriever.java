package com.example.hazardzoneawarness1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


public class hzaWebRetriever extends AsyncTask<Context,Void,Void> {
    String line;
    Context context;

    @Override
    protected Void doInBackground(Context... contextin) {
        context = contextin[0];

        LiveData<HazardZoneData> hazardZoneData = HazardZoneRepository.getHazardData();
        String beaconID = hazardZoneData.getValue().getBeaconID();
        HazardZoneData buildHZAData = new HazardZoneData();
        buildHZAData.setBeaconID(beaconID);

        if (beaconID.length() > 1) {

            try {
                String str = "http://myriad.myftp.org:747/jsonapi/node/Beacon?filter[title]=" + beaconID + "&include=field_facility.field_facility,field_hazard_zone.field_hazards";

                URL url = new URL(str);
                URLConnection urlc = url.openConnection();
                BufferedReader bfr = new BufferedReader(new InputStreamReader(urlc.getInputStream()));


                while ((line = bfr.readLine()) != null) {
                    JSONObject jso = new JSONObject(line);

                    JSONArray jsa = new JSONArray(jso.getString("included"));
                    //Drupal 8's JSON API allows to include other content types. Amazingly, everything we need is right here all in one web call.
                    // Now that we have the "included" content types in an array, we'll go through each one in a switch-case to do work.
                    // The switch-case allows the order of the array to be different and account for multiple matching results, such has multiple hazards in one zone

                    // Can't do a for-each loop with JSON array because they are actually objects.
                    for (int i = 0; i < jsa.length(); i++){
                        JSONObject node = jsa.getJSONObject(i);
                        JSONObject nodeAttributes = new JSONObject(node.getString("attributes"));

                        //By doing a JSON GET with Drupal, you can get the object structure to understand what case swithces and fields to look for
                        //Notepad++ has a nice plugin to format and view JSON data - I used it all the time with the GET to sort this out.

                        switch (node.getString("type")) {
                            case "node--facility":
                                buildHZAData.setFacilityID(node.getString("id"));
                                buildHZAData.setFacilityName(nodeAttributes.getString("title"));
                                //A Facility may have multiple phone numbers. Let's handle that by adding a newline escape character after the first array element
                                String phnum = "";
                                for (int s=0; s<nodeAttributes.getJSONArray("field_phone_number").length(); s++){
                                    if (s>0){ //this isn't the first array item, so let's add that carriage return.
                                                phnum += "\n";
                                    }
                                    phnum += nodeAttributes.getJSONArray("field_phone_number").getString(s);
                                }
                                buildHZAData.setFacilityPhoneNumber(phnum);

                                break;

                            case "node--hazard_zone":
                                buildHZAData.setHazardZoneID(node.getString("id"));
                                buildHZAData.setHazardZoneName(nodeAttributes.getString("title"));
                                break;

                            case "node--hazard":
                                JSONObject nodeHazard = new JSONObject(node.getString("attributes"));
                                buildHZAData.setHazardLink( nodeHazard.getString("field_attachment_link"));
                                buildHZAData.setHazardName(nodeHazard.getString("title"));
                                buildHZAData.setHazardLink(nodeHazard.getString("field_attachment_link"));
                                buildHZAData.setHazardSeverity(nodeHazard.getString("field_severity"));
                                buildHZAData.setGblHazardSeverity(nodeHazard.getString("field_severity"));
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //update the livedata object so the app updates the screen
            HazardZoneRepository.postHazardData(buildHZAData);

        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
//The webserver has finished giving all its data. Time to notify the user.

//Grab the shared data object
        LiveData<HazardZoneData> hazardZoneData = HazardZoneRepository.getHazardData();

//Configure the notification details
        String textTitle = "Hazard Zone Awareness - " + hazardZoneData.getValue().getFacilityName();
        String textContent = "";
        String Channel_ID = "";
        //When tapping the notification, the app should open
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,intent,0);

        //Set notification details and sound based on severity
        Uri sound;

        //The switch case below should have the same options in the webserver's Hazard Severity list.
        switch (hazardZoneData.getValue().getHazardSeverity()) {
            case "Lethal exposure":
                textContent = "Lethal exposure";
                sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.lethalexposure);  //Here is FILE_NAME is the name of file that you want to play
                Channel_ID = textContent;
                break;
            case "Health effects exposure":
                textContent = "Health effects exposure";
                sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.lethalexposure);  //Here is FILE_NAME is the name of file that you want to play
                Channel_ID = textContent;
            break;
            case "Avoid contact":
                textContent = "Avoid contact";
                sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.avoidcontact);  //Here is FILE_NAME is the name of file that you want to play
                Channel_ID = textContent;
                break;
            case "Low risk":
                textContent = "Low risk";
                sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.lowrisk);  //Here is FILE_NAME is the name of file that you want to play
                Channel_ID = textContent;

                break;
            case "Safe":
                textContent = "Safe";
                sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.safe);  //Here is FILE_NAME is the name of file that you want to play
                Channel_ID = textContent;
                break;
            default:
                textContent = "Unknown hazard";
                sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.unknownhazard);  //Here is FILE_NAME is the name of file that you want to play
                Channel_ID = textContent;

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Channel_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(textTitle)
                .setContentText(hazardZoneData.getValue().getHazardZoneName() + " - " + textContent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // notificationId is a unique int for each notification that you must define
        // NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            if(sound != null){
                // Changing Default mode of notification
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
                // Creating an Audio Attribute
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();

                // Creating Channel
                NotificationChannel notificationChannel = new NotificationChannel(Channel_ID,"Hazard Alert: " + Channel_ID, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.setSound(sound,audioAttributes);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        notificationManager.notify(0, builder.build());

        //Done notifying the user

        super.onPostExecute(aVoid);
    }
}

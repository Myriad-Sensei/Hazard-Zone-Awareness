package com.example.hazardzoneawarness1;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import java.util.Collection;

/*This is what monitors for beacons. With the newer version of Android, the scanning
will happen in the foreground as a service - a permanent notification will remain on the Android toolbar.
 */
public class HazardZoneAwareness extends Application implements BootstrapNotifier,  BeaconConsumer {
    private static final String TAG = ".HazardZoneAwareness";
    private RegionBootstrap regionBootstrap;
    private BeaconManager beaconManager;
    private String beaconID = "";
    private String lookupBeacon;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    //Called by the UI to stop scanning and disable foreground service
    public void disableMonitoring() {
        if (regionBootstrap != null) {
            regionBootstrap.disable();
            regionBootstrap = null;
        }

        beaconManager.unbind(this);
    }

    //Called by the UI to start everything up again
    public void enableMonitoring() {
        Region region = new Region("all-beacons-region",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        if (!beaconManager.isAnyConsumerBound()) {
            beaconManager.bind(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"Beacon scanning started up");

        beaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());

        //beaconManager.setDebug(true);
        Log.d(TAG,"BeaconManager Parsers getting set");
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // Detect the telemetry (TLM) frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        // Detect the URL frame:
       /* beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT));*/

        //start a foreground service
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Scanning for Beacons");
        Intent intent = new Intent(this, HazardZoneAwareness.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        builder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Hazard Zone Beacon Scanning",
                    "Hazard Zone Beacon Scanning", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Hazard Zone Beacon Scanning");
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }
        beaconManager.enableForegroundServiceScanning(builder.build(), 456);

        // For the above foreground scanning service to be useful, you need to disable
        // JobScheduler-based scans (used on Android 8+) and set a fast background scan
        // cycle that would otherwise be disallowed by the operating system.
        //
        beaconManager.setAndroidLScanningDisabled(false);
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(0);
        beaconManager.setBackgroundScanPeriod(1100);


//Loads Shared preferences - The user must enable the preference to start scanning.
        //The option is there to save power if the user actually doesn't want to scan
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("service_switch", false)){

            enableMonitoring();
        }

//Setup a shared preference listener for hpwAddress and restart transport
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("service_switch")) {
                    if (prefs.getBoolean(key, false)){
                        enableMonitoring();
                    }
                    else {
                        disableMonitoring();
                    }
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void onBeaconServiceConnect() {
        Log.d(TAG,"Got a onBeaconServiceConnect call");
        RangeNotifier rangeNotifier = new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  "+beacons.size());
                }

                Log.d(TAG,"Got a didRangeBeaconsInRegion call");
                for (Beacon beacon: beacons) {
                    if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                        // This is a Eddystone-UID frame
                        Identifier namespaceId = beacon.getId1();
                        Identifier instanceId = beacon.getId2();
                        Log.d(TAG, "I see a beacon transmitting namespace id: " + namespaceId +
                                " and instance id: " + instanceId +
                                " approximately " + beacon.getDistance() + " meters away.");

                        HazardZoneRepository.setBeaconDistance(beacon.getDistance());


                        beaconID = namespaceId.toString().replaceFirst("^[0,x]+(?!$)", "");


                        // Do we have telemetry data?
                        if (beacon.getExtraDataFields().size() > 0) {
                            long telemetryVersion = beacon.getExtraDataFields().get(0);
                            long batteryMilliVolts = beacon.getExtraDataFields().get(1);
                            long temperature1 = beacon.getExtraDataFields().get(2);
                            float temperature2 = -128.0f;
                            //the RSL10 stores the temperature sensor data in an unsigned 2 byte integer
                            if ((temperature1 & 0x0800) != 0) //0x0800 is the RSL10 saying the temperature sensor is not available.
                            {
                                Long t = ~((0xF000 | temperature1) - 1);
                                temperature2 =  -(t / 16.0f);
                            }
                            else
                            {
                                temperature2 =  temperature1 / 16.0f;
                            }
                            long pduCount = beacon.getExtraDataFields().get(3);
                            long uptime = beacon.getExtraDataFields().get(4);
                            Log.d(TAG, "The above beacon is sending telemetry version " + telemetryVersion +
                                    ", has been up for : " + uptime + " seconds" +
                                    ", has a temperature of " + temperature2 + " C" +
                                    ", has a battery level of " + batteryMilliVolts + " mV" +
                                    ", and has transmitted " + pduCount + " advertisements.");
                            HazardZoneRepository.setBeaconBattery(batteryMilliVolts);
                        }

                        //Beacon data is captured and ready to do other work with.
                        //Let's not kill the server and cell phone data plan by limiting the number of recalls to the server
                        LiveData<HazardZoneData> hazardZoneData = HazardZoneRepository.getHazardData();
                        if (hazardZoneData.getValue() == null) {
                            lookupBeacon = "";
                        } else {
                            lookupBeacon = hazardZoneData.getValue().getBeaconID();
                        }

                        if (!beaconID.equals(lookupBeacon)) {
                            //The most recently found beacon is not the same one we looked up last time
                            //This needs to be reworked because the assumption here is there could only ever be one
                            //beacon seen at a time.  Ideally, a new card_view is created for each seen beacon and updated
                            //with the correct data values respectively

                       //The web retriever needed the context of the application
                       //So since this a new beacon ID, clear the shared variable and set the new ID
                       //The web retriever will grab the shared variables beacon ID
                            HazardZoneData newHZAData = new HazardZoneData();
                            newHZAData.setBeaconID(beaconID);
                            HazardZoneRepository.setHazardData(newHZAData);
                            //now connect to the server to pull hazard data, this is done asynchronously
                            //so the code will continue immediately and not wait for the server
                            new hzaWebRetriever().execute(getApplicationContext());

                            //Prepare the data for Beacon_Log at the server
                            HZALogData hzaLogData = new HZALogData();
                            hzaLogData.setBeaconID(beaconID);
                            hzaLogData.setEntering(true);
                            //grab the username that is set in the shared preferences
                            hzaLogData.setUserID(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("user_id",""));
                            new hzaWebLogger().execute(hzaLogData);
                        }
                    }
                }
            }
        };
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("all-beacons-region", null, null, null));
            beaconManager.addRangeNotifier(rangeNotifier);
        } catch (RemoteException e) {   }
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.d(TAG,"Got a didEnterRegion call");
        // This call to disable will make it so the activity below only gets launched the first time a beacon is seen (until the next time the app is launched)
        // if you want the Activity to launch every single time beacons come into view, remove this call.
        //regionBootstrap.disable();
        Intent intent = new Intent(this, MainActivity.class);
        // IMPORTANT: in the AndroidManifest.xml definition of this activity, you must set android:launchMode="singleInstance" or you will get two instances
        // created when a user launches the activity manually and it gets launched from here.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        //Not used
        //Log.d(TAG,"Current region state is: " + (state == 1 ? "INSIDE" : "OUTSIDE ("+state+")"));
    }
    @Override
    public void didExitRegion(Region region) {
        Log.d(TAG,"Got a didExitRegion call");
        //Unlist the beacon from the app and log the event with server

        // display and play a notification indicating we are safe from the hazard now
        String textTitle = "Hazard Zone Awareness";
        String textContent = "Beacon is no longer in range";
        String Channel_ID = "No Beacon";

        //When tapping the notification, the app should open
        Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0,intent,0);

        //Set notification details and sound based on severity
        Uri sound;
        sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +this.getApplicationContext().getPackageName() + "/" + R.raw.nomorebeacon);  //Here is FILE_NAME is the name of file that you want to play

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getApplicationContext(), Channel_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this.getApplicationContext());

        // notificationId is a unique int for each notification that you must define
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
        //Clears notification
        //NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // notificationManager.cancel(NOTIFICATION_ID);
        HazardZoneRepository.setHazardData(new HazardZoneData(getApplicationContext()));

        //Log user has left the zone to the server
        HZALogData hzaLogData = new HZALogData();
        hzaLogData.setBeaconID(beaconID);
        hzaLogData.setEntering(false);
        hzaLogData.setUserID(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("user_id", ""));
        new hzaWebLogger().execute(hzaLogData);
    }
}


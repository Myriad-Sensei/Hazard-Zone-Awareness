package com.example.hazardzoneawarness1;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import android.text.Spanned;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import org.altbeacon.beacon.BeaconManager;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION  = 1;

    private static final String TAG = "HZA_Version_1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        createNotificationChannel();

        //This tells the user interface to update whenever the fields listed below are modified.
        //Basically just wiring the value to the screen element
        HazardZoneRepository.getHazardData().observe(this, new Observer<HazardZoneData>() {
            @Override
            public void onChanged(HazardZoneData hzadata) {
                ((TextView) MainActivity.this.findViewById(R.id.txtBeaconID)).setText(hzadata.getBeaconID());
                ((TextView) MainActivity.this.findViewById(R.id.txtFacilityName)).setText(hzadata.getFacilityName());
                ((TextView) MainActivity.this.findViewById(R.id.txtHazardName)).setText(hzadata.getHazardName());
                ((TextView) MainActivity.this.findViewById(R.id.txtHazardSeverity)).setText(hzadata.getHazardSeverity());
                ((TextView) MainActivity.this.findViewById(R.id.txtHazardZoneName)).setText(hzadata.getHazardZoneName());

                String phoneNumbers = hzadata.getFacilityPhoneNumber();
                TextView makeClickable = findViewById(R.id.txtFacilityPhone);
                makeClickable.setText(phoneNumbers);
                makeClickable.setAutoLinkMask(Linkify.PHONE_NUMBERS);
                makeClickable.setLinksClickable(true);
                makeClickable.setMovementMethod(LinkMovementMethod.getInstance());

                TextView sdsLink = (TextView) findViewById(R.id.txtHazardName);
                Spanned weblink = Html.fromHtml("<a href='"+hzadata.getHazardLink()+"'>"+hzadata.getHazardName()+"</a>");
                if (weblink.length()>5) {
                        sdsLink.setMovementMethod(LinkMovementMethod.getInstance());
                        sdsLink.setText(weblink);
                }
                else {
                sdsLink.setText("");
                }

                if (hzadata.getBeaconDistance() != 0.0) {
                    ((TextView) MainActivity.this.findViewById(R.id.txtBeaconDistance)).setText(String.format("%2f", hzadata.getBeaconDistance()));
                }
                else {
                    ((TextView) MainActivity.this.findViewById(R.id.txtBeaconDistance)).setText(getString(R.string.beacon_distance));
                }

                //You could add more fields to the user interface if there is value to the user
               /* if (hzadata.getBeaconBattery() != 0.0){
                    ((TextView) MainActivity.this.findViewById(R.id.txtBeaconBattery)).setText(hzadata.getBeaconBattery() + " mV");
                }
                else {
                    ((TextView) MainActivity.this.findViewById(R.id.txtBeaconBattery)).setText(getString(R.string.beacon_battery));
                }*/

            }
        });



        //Make sure the correct permissions are provided otherwise the app won't do anything
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        //Make sure a username is entered into the settings page. This is needed for the Beacon Log at the webserver.
        if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("user_id", "") == ""){
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Must set your User ID");
            builder.setMessage("Please enter your unique User ID as instructed by your OH&S advisers to enable beacon scanning.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    Intent intent = new Intent();
                    intent.setClassName(getApplicationContext(),"com.example.hazardzoneawarness1.SettingsActivity");
                    startActivity(intent);
                }

            });
            builder.show();

        }

        //Make sure the user's preference is respected regarding constantly scanning and using more battery.
        if(!PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("service_switch", false)){
            HazardZoneAwareness hazardZoneAwareness = ((HazardZoneAwareness) getApplicationContext());
            hazardZoneAwareness.disableMonitoring();
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Service Must Be Enabled");
            builder.setMessage("When the Hazard Zone Awareness service is not enabled, it will not be possible to detect beacons. Please enable the service in the settings menu.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    Intent intent = new Intent();
                    intent.setClassName(getApplicationContext(),"com.example.hazardzoneawarness1.SettingsActivity");
                    startActivity(intent);
                }

            });
            builder.show();

        }
        else {
            HazardZoneAwareness hazardZoneAwareness = ((HazardZoneAwareness) getApplicationContext());
            hazardZoneAwareness.enableMonitoring();
        }


        final FloatingActionButton fab = findViewById(R.id.fab);
        if (BeaconManager.getInstanceForApplication(this).getMonitoredRegions().size() > 0) {
            fab.setImageResource(R.mipmap.ic_beacon_on);
        }
        else {
            fab.setImageResource(R.mipmap.ic_beacon_off);
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HazardZoneAwareness hazardZoneAwareness = ((HazardZoneAwareness) getApplicationContext());
                Integer numRegions = BeaconManager.getInstanceForApplication(getApplicationContext()).getMonitoredRegions().size();

                if (BeaconManager.getInstanceForApplication(getApplicationContext()).getMonitoredRegions().size() > 0) {
                    hazardZoneAwareness.disableMonitoring();
                    fab.setImageResource(R.mipmap.ic_beacon_off);
                    Snackbar.make(view, "Hazard zone beacon scanning disabled", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                }
                else {
                    fab.setImageResource(R.mipmap.ic_beacon_on);
                    Snackbar.make(view, "Hazard zone beacon scanning enabled", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                        hazardZoneAwareness.enableMonitoring();

                }
            }
        });


    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "HazardBeacon";//getString(R.string.channel_name);
            String description = "Hazard Zone Awareness beacon scanner";//getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("HZAChannel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent();
            intent.setClassName(this,"com.example.hazardzoneawarness1.SettingsActivity");
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

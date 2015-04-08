/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.polytech.orientatewatch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.text.Normalizer;
import java.util.List;


public class MainActivity extends ActionBarActivity{

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    LocationManager mLocationManager;
    ForecastFragment forecastFragment = new ForecastFragment();

    private static boolean watchSettings = false;

    private static String rayon;
    private static String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intentsettings = this.getIntent();
        if (intentsettings != null && intentsettings.hasExtra("rayon") && intentsettings.hasExtra("type")) {
            rayon = intentsettings.getExtras().getString("rayon");
            type = intentsettings.getExtras().getString("type");
            watchSettings = true;
            //Log.e("ACCENT !!!!!!!!!!!!!!!!","\n\n" + type);
            try
            {
                double d = Double.parseDouble(rayon);
            }
            catch(NumberFormatException nfe)
            {
                Toast.makeText(getApplicationContext(), "Le rayon indiqué en paramètre n'est pas un nombre.\n le rayon 250 à donc était utilisé par défaut", Toast.LENGTH_LONG).show();
                rayon = "250";
            }
            int i = 0;
            String[] listFr = getResources().getStringArray(R.array.pref_type_options);
            String[] listUrl = getResources().getStringArray(R.array.pref_type_values);
            String tmp = Normalizer.normalize(type, Normalizer.Form.NFD);
            tmp = tmp.replaceAll("[^\\p{ASCII}]", "");
            for(String elem : listFr){
                elem = Normalizer.normalize(elem, Normalizer.Form.NFD);
                elem = elem.replaceAll("[^\\p{ASCII}]", "");
                //Log.e("EGAL !!!!!!!!!!!!!!!!",elem + " = " + elem + " valeur i : " + i );
                if (elem.equals(tmp)){
                    //Log.e("EGAL !!!!!!!!!!!!!!!!", elem + " = " + type + " valeur i : " + i);
                    break;
                }
                i++;
            }

            if (i < listFr.length){
                type = listUrl[i];
            }else{
                Toast.makeText(getApplicationContext(), "Le type indiqué en paramètre n'existe pas.\n le type Restaurant à donc était utilisé par défaut", Toast.LENGTH_LONG).show();
                type = "restaurant";
            }
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, forecastFragment)
                    .commit();
        }


        Location myLocation = getLastKnownLocation();

        if (myLocation != null){
            this.setLatitude(myLocation);
            this.setLongitude(myLocation);
        }


    }

    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List <String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_retourPremPage) {
            Utility.nextPageToken=null;
            forecastFragment.moreResults();
            return true;
        }
        if (id == R.id.more_res) {
            if (Utility.nextPageToken == null) {
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("Il n'y pas d'autres poi aux alentours !");
                dlgAlert.setTitle("Désolé :(");
                dlgAlert.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //dismiss the dialog
                            }
                        });
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();
                return true;
            }else{
                forecastFragment.moreResults();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreferredLocationInMap() {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);

        String range = sharedPrefs.getString(
                getString(R.string.pref_range_key),
                getString(R.string.pref_range_default));

    }

    public void setLatitude(Location location) {
        Utility.latitude = location.getLatitude();
    }

    public void setLongitude(Location location) {
        Utility.longitude = location.getLongitude();
    }

    public static boolean getWatchSettings() {
        return watchSettings;
    }

    public static String getRayon() {
        return rayon;
    }

    public static String getType() {
        return type;
    }

    public static void setWatchSettings(boolean watchSettings) {
        MainActivity.watchSettings = watchSettings;
    }
}

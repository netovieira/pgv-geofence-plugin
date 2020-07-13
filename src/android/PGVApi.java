package com.pgv.cordova.geofence;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.Manifest;


import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class PGVApi {
    public static final String TAG = "PGVGeofencePlugin";
    private static final String BASE_URL = "https://api.localtarget.com.br/api/";

    public static void iFoundOne(final Context context, final GeoNotification geoNotification, final Location location){
        if (geoNotification != null) {
            try {
                JSONObject obj = getUserInfo(context);
                obj.put("campaign_id", geoNotification.notification.id);
                obj.put("latitude", location.getLatitude());
                obj.put("longitude", location.getLongitude());

                Log.d(TAG, "******** BEFORE Request on i-found-one!");
                Log.d(TAG, "******** Notification DATA: " + obj.toString());
                PGVApi.iFoundOneRequest(obj.toString(), context);
            }catch (Exception e){
                Log.d(TAG, "******** GeofencePlugin JSONObject catch error: " + e.getMessage());
            }
        }else{
            Log.d(TAG, "******** GeofencePlugin geoNotification is null");
        }
    }


    public static void iFoundOneRequest(final String data, final Context context) {
        final String path = "i-found-one";
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(PGVApi.BASE_URL + path);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    Log.d(TAG, "******** PGVGEOFENCE WRITE JSON: " + data);
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(removerAcentos(data));

                    os.flush();
                    os.close();

                    Log.d(TAG, "******** PGVGEOFENCE STATUS: "  + String.valueOf(conn.getResponseCode()));
                    Log.d(TAG, "******** PGVGEOFENCE MSG: "     + conn.getResponseMessage());

                    try {
                        final JSONObject response = new JSONObject(conn.getResponseMessage());
                        conn.disconnect();
                        if(response.getBoolean("response")) {
                            if(response.getBoolean("update_fences")) {
                                JSONArray args = response.getJSONArray("campaigns");

                                GeoNotificationManager geoNotificationManager = new GeoNotificationManager(context);

                                List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();

                                GeoNotification n;

                                n = PGVApi.parseFromJSONObject( response.getJSONObject("location_fence") );
                                geoNotifications.add(n);

                                //FOREACH GEOFENCES
                                for (int i = 0; i < args.length(); i++) {
                                    n = PGVApi.parseFromJSONObject(args.optJSONObject(i));
                                    if (n != null) {
                                        geoNotifications.add(n);
                                    }
                                }

                                //REMOVE ACTIVE GEOFENCES
                                geoNotificationManager.removeAllGeoNotifications(null);
                                //RENEW DEVICE GEOFENCES
                                geoNotificationManager.addGeoNotifications(geoNotifications, null);
                            }
                        }

                    } catch (JSONException e) {
                        Log.d(TAG, "******** GeofencePlugin Response error on JSON parse: " + e.getMessage());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "******** PGVGEOFENCE POST EXCEPTION ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    public static String removerAcentos(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    public static void saveUserInfo(Context context, JSONObject userInfo){
        File file = new File(context.getFilesDir() + File.pathSeparator + "user_info.txt");
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(userInfo.toString());
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            Log.d(TAG, "******** PGVGEOFENCE saveUserInfo exception: " + e.getMessage());
        }
    }

    public static JSONObject getUserInfo(Context context){
        File file = new File(context.getFilesDir() + File.pathSeparator + "user_info.txt");
        if(file.exists()) {
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                return new JSONObject(bufferedReader.readLine());
            } catch (FileNotFoundException e) {
                Log.d(TAG, "******** PGVGEOFENCE getUserInfo FileNotFoundException: " + e.getMessage());
            } catch (JSONException e) {
                Log.d(TAG, "******** PGVGEOFENCE getUserInfo JSONException: " + e.getMessage());
            }
        }
        return new JSONObject();
    }

    static private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = GeoNotification.fromJson(object.toString());
        return geo;
    }
}

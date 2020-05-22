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

public class PGVApi {
    public static final String TAG = "PGVGeofencePlugin";
    private static final String BASE_URL = "https://api.localtarget.com.br/api/";

    public static void iFoundOne(final Context context, final GeoNotification geoNotification, final Location location){
        if (geoNotification != null) {
            try {
                JSONObject obj = new JSONObject(geoNotification.notification.getDataJson());
                obj.put("latitude", location.getLatitude());
                obj.put("longitude", location.getLongitude());

                Log.d(TAG, "******** BEFORE Request on i-found-one!");
                Log.d(TAG, "******** Notification DATA: " + obj.toString());
                PGVApi.sendPost("i-found-one", obj.toString());
            }catch (Exception e){
                Log.d(TAG, "******** GeofencePlugin JSONObject catch error: " + e.getMessage());
            }
        }else{
            Log.d(TAG, "******** GeofencePlugin geoNotification is null");
        }
    }


    public static void sendPost(final String path, final String data) {
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
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeUTF(data);

                    os.flush();
                    os.close();

                    Log.d(TAG, "******** PGVGEOFENCE STATUS: "  + String.valueOf(conn.getResponseCode()));
                    Log.d(TAG, "******** PGVGEOFENCE MSG: "     + conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    Log.d(TAG, "******** PGVGEOFENCE POST EXCEPTION ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}

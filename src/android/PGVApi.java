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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;

import org.apache.cordova.CallbackContext;

public class PGVApi {
    public static final String TAG = "PGVGeofencePlugin";
    private static final String BASE_URL = "https://api.localtarget.com.br/api/";

    public static void iFoundOne(final Context context, final GeoNotification geoNotification, final Number latitude, final Number longitude, final CallbackContext callbackContext)
    {
        try {
            JSONObject obj = getUserInfo(context);
            if( geoNotification != null ){
                obj.put("campaign_id", geoNotification.notification.id);
            }

            obj.put("latitude", latitude);
            obj.put("longitude", longitude);

            Log.d(TAG, "******** BEFORE Request on i-found-one!");
            Log.d(TAG, "******** Notification DATA: " + obj.toString());
            PGVApi.iFoundOneRequest(obj.toString(), context, callbackContext);
        }catch (Exception e){
            Log.d(TAG, "******** GeofencePlugin JSONObject catch error: " + e.getMessage());
        }
    }


    public static void iFoundOneRequest(final String data, final Context context, final CallbackContext callbackContext) {
        final String path = "i-found-one";
        Random r = new Random();
        final int interaction = r.nextInt(999);

        if(!PGVApi.activeRequest(context)) {
            PGVApi.inRequest(context);
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

                        Log.d(TAG, "******** iFoundOneRequest:"+ Integer.toString(interaction) +" PGVGEOFENCE WRITE JSON: " + data);
                        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                        os.writeBytes(removerAcentos(data));

                        os.flush();
                        os.close();

                        Log.d(TAG, "******** iFoundOneRequest:"+ Integer.toString(interaction) +" PGVGEOFENCE STATUS: "  + String.valueOf(conn.getResponseCode()));
                        Log.d(TAG, "******** iFoundOneRequest:"+ Integer.toString(interaction) +" PGVGEOFENCE MSG: "     + conn.getResponseMessage());

                        try {
                            InputStream inputStream = conn.getInputStream();
                            String stringJson = convertStreamToString(inputStream);
                            Log.d(TAG, "******** iFoundOneRequest:"+ Integer.toString(interaction) +" PGVGEOFENCE JSON STRING: " + stringJson);
                            final JSONObject response = new JSONObject(stringJson);
                            conn.disconnect();
                            if(response.getBoolean("response")) {
                                if(response.getBoolean("update_geofences")) {
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
                                    geoNotificationManager.removeAllGeoNotifications(callbackContext);
                                    //RENEW DEVICE GEOFENCES
                                    geoNotificationManager.addGeoNotifications(geoNotifications, callbackContext);
                                }
                            }

                        } catch (JSONException e) {
                            Log.d(TAG, "******** iFoundOneRequest:"+ Integer.toString(interaction) +" GeofencePlugin Response error on JSON parse: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "******** iFoundOneRequest:"+ Integer.toString(interaction) +" PGVGEOFENCE POST EXCEPTION ERROR: " + e.getMessage());
                    }
                    PGVApi.closeRequest(context);
                }
            });
            thread.start();
        }
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
        } catch (IOException e) {
            Log.d(TAG, "******** PGVGEOFENCE saveUserInfo IOException: " + e.getMessage());
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
            } catch (IOException e) {
                Log.d(TAG, "******** PGVGEOFENCE getUserInfo IOException: " + e.getMessage());
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

    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static void inRequest(Context context){
        File file = new File(context.getFilesDir() + File.pathSeparator + "in_request.txt");
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("1");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.d(TAG, "******** PGVGEOFENCE saveUserInfo IOException: " + e.getMessage());
        }
    }

    public static void closeRequest(Context context){
        File file = new File(context.getFilesDir() + File.pathSeparator + "in_request.txt");
        file.delete();
    }

    public static Boolean activeRequest(Context context){
        File file = new File(context.getFilesDir() + File.pathSeparator + "in_request.txt");
        return file.exists();
    }
}

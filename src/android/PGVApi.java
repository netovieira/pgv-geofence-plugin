package com.pgv.cordova.geofence;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.Manifest;


import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.example.pgvgeofence.R;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;

public class PGVApi {
    public static final String TAG = "PGVGeofencePlugin";
    private static final String BASE_URL = "https://api.localtarget.com.br/api/";

    private static class RequestReturn {
        public int status_code = 0;
        public JSONObject data;
        public String message;
    };


    public static void iFoundOne(final Context context, final GeoNotification geoNotification, final Location location){
        if (geoNotification != null) {
            try {
                JSONObject obj = new JSONObject(geoNotification.notification.getDataJson());
                obj.put("latitude", location.getLatitude());
                obj.put("longitude", location.getLongitude());

                Log.d(TAG, "******** BEFORE Request on i-found-one!");
                Log.d(TAG, "******** Notification DATA: " + obj.toString());
                RequestReturn requestReturn = sendPost("i-found-one", obj.toString());
                sendNotification(context, /*requestReturn.data.getString("title")*/ "Teste Notificacao", requestReturn.data.getString("message"));
            }catch (Exception e){
                Log.d(TAG, "******** GeofencePlugin JSONObject catch error: " + e.getMessage());
            }
        }else{
            Log.d(TAG, "******** GeofencePlugin geoNotification is null");
        }
    }

    private static void sendNotification(Context context, String title, String message) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(context, MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(context, "M_CH_ID");
//                .setSmallIcon(R.drawable.ic_action_location);

        // Define the notification settings.
        builder.setContentTitle(title)
                .setContentText(message)
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


    public static RequestReturn sendPost(final String path, final String data) {

        RequestReturn ret = new RequestReturn();

        StringBuilder stringBuilder = new StringBuilder();
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
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

            ret.status_code = conn.getResponseCode();

            if(ret.status_code == 200) {
                InputStream in = new BufferedInputStream(conn.getInputStream());
            }else{
                InputStream in = new BufferedInputStream(conn.getErrorStream());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            ret.message = stringBuilder.toString();
            if(ret.status_code == 200) {
                ret.data = new JSONObject(ret.message);
            }

            Log.e(TAG, "******** PGVGEOFENCE STATUS: "  + String.valueOf(ret.status_code));
            Log.d(TAG, "******** PGVGEOFENCE MSG: "     + stringBuilder.toString());

            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "******** PGVGEOFENCE POST EXCEPTION ERROR: " + e.getMessage());
            ret.message = e.getMessage();
        }

        return ret;
//            }
//        });
//
//        thread.start();
    }

    public static String removerAcentos(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }
}

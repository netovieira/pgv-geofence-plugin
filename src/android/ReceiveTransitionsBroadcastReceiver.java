package com.pgv.cordova.geofence;

import android.content.BroadcastReceiver;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

//CUSTOM
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

public class ReceiveTransitionsBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(GeofencePlugin.TAG, "ReceiveTransitionsBroadcastReceiver - onHandleIntent");
        try{
            GeoNotificationStore store = new GeoNotificationStore(context);
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                // Get the error code with a static method
                int errorCode = geofencingEvent.getErrorCode();
                String error = "Location Services error: " + Integer.toString(errorCode);
                Log.d(GeofencePlugin.TAG, error);
            } else {
                // Get the type of transition (entry or exit)
                int transitionType = geofencingEvent.getGeofenceTransition();
                if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                        || (transitionType == Geofence.GEOFENCE_TRANSITION_DWELL)
                        || (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)) {
                    Log.d(GeofencePlugin.TAG, ">>> GeofencePlugin: Geofence transition detected");
                    List<Geofence> triggerList = geofencingEvent.getTriggeringGeofences();
                    List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();

                    for (Geofence fence : triggerList) {
                        String fenceId = fence.getRequestId();
                        GeoNotification geoNotification = store.getGeoNotification(fenceId);
                        geoNotification.transitionType = transitionType;
                        PGVApi.iFoundOne(context, geoNotification, geofencingEvent.getTriggeringLocation());
                        geoNotifications.add(geoNotification);
                    }

                    if (geoNotifications.size() > 0) {
                        Log.d(GeofencePlugin.TAG, "******** GeofencePlugin onTransitionReceived called");
                        GeofencePlugin.onTransitionReceived(context, geoNotifications);
//                            GeofencePlugin.onTransitionReceived(this, geoNotifications, latitude, longitude);
                    }
                } else {
                    String error = "Geofence transition error: " + transitionType;
                    Log.d(GeofencePlugin.TAG, error);
                }
            }
        } catch (Exception e) {
            Log.d(GeofencePlugin.TAG, "******** Error general");
        }
    }
}

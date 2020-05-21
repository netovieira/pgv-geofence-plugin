package com.pgv.cordova.geofence;

import android.app.IntentService;
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

public class ReceiveTransitionsIntentService extends IntentService {
    protected static final String GeofenceTransitionIntent = "com.pgv.cordova.geofence.TRANSITION";
    protected BeepHelper beepHelper;
//    protected GeoNotificationNotifier notifier;
    protected GeoNotificationStore store;

    public static final String TAG = "PGVGeofencePlugin";
    public static final String SERVICE_MESSAGE = "PGVGeofencePluginService";
    /**
     * Sets an identifier for the service
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
        beepHelper = new BeepHelper();
        store = new GeoNotificationStore(this);
        Logger.setLogger(new Logger(GeofencePlugin.TAG, this, false));
//        notifier = new GeoNotificationNotifier(
//                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE),
//                this
//        );
    }

    /**
     * Handles incoming intents
     *
     * @param intent
     *            The Intent sent by Location Services. This Intent is provided
     *            to Location Services (inside a PendingIntent) when you call
     *            addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Logger logger = Logger.getLogger();
        logger.log(Log.DEBUG, "ReceiveTransitionsIntentService - onHandleIntent");
        Log.d(TAG, "ReceiveTransitionsIntentService - onHandleIntent");
        Intent broadcastIntent = new Intent(GeofenceTransitionIntent);
        try{
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()) {
                // Get the error code with a static method
                int errorCode = geofencingEvent.getErrorCode();
                String error = "Location Services error: " + Integer.toString(errorCode);
                // Log the error
                logger.log(Log.ERROR, error);
                broadcastIntent.putExtra("error", error);
                Log.e(TAG, error);
            } else {
                // Get the type of transition (entry or exit)
                int transitionType = geofencingEvent.getGeofenceTransition();
                if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                        || (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)) {
                    logger.log(Log.DEBUG, "Geofence transition detected");
                    List<Geofence> triggerList = geofencingEvent.getTriggeringGeofences();
                    List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();

                    for (Geofence fence : triggerList) {
                        String fenceId = fence.getRequestId();
                        GeoNotification geoNotification = store.getGeoNotification(fenceId);
                        geoNotification.transitionType = transitionType;
                        PGVApi.iFoundOne(getApplicationContext(), geoNotification, geofencingEvent.getTriggeringLocation());

                        geoNotifications.add(geoNotification);
                    }

                    if (geoNotifications.size() > 0) {
                        Log.d(TAG, "******** GeofencePlugin onTransitionReceived called");
                        broadcastIntent.putExtra("transitionData", Gson.get().toJson(geoNotifications));
                        GeofencePlugin.onTransitionReceived(getApplicationContext(), geoNotifications);
//                            GeofencePlugin.onTransitionReceived(this, geoNotifications, latitude, longitude);
                    }
                } else {
                    String error = "Geofence transition error: " + transitionType;
                    Log.e(TAG, error);
                    logger.log(Log.ERROR, error);
                    broadcastIntent.putExtra("error", error);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "******** Error general");
        }
        sendBroadcast(broadcastIntent);
    }
}

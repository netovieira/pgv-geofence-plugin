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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

public class ReceiveTransitionsIntentService extends IntentService {
    protected static final String GeofenceTransitionIntent = "com.pgv.cordova.geofence.TRANSITION";
    protected BeepHelper beepHelper;
//    protected GeoNotificationNotifier notifier;
    protected GeoNotificationStore store;

    public static final String TAG = "PGVGeofencePlugin";
    /**
     * Sets an identifier for the service
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
        beepHelper = new BeepHelper();
        store = new GeoNotificationStore(this);
        Logger.setLogger(new Logger(GeofencePlugin.TAG, this, false));
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
        Intent broadcastIntent = new Intent(GeofenceTransitionIntent);
//        notifier = new GeoNotificationNotifier(
//                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE),
//                this
//        );

        // TODO: refactor this, too long
        // First check for errors
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            // Get the error code with a static method
            int errorCode = geofencingEvent.getErrorCode();
            String error = "Location Services error: " + Integer.toString(errorCode);
            // Log the error
            logger.log(Log.ERROR, error);
            broadcastIntent.putExtra("error", error);
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
                    GeoNotification geoNotification = store
                            .getGeoNotification(fenceId);

                    if (geoNotification != null) {
                        if (geoNotification.notification != null) {
                            String url = "https://api.localtarget.com.br/api/i-found-one";

                            Location location = geofencingEvent.getTriggeringLocation();
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            RequestQueue requstQueue = Volley.newRequestQueue(getApplicationContext());
                            try {
                                JSONObject obj = new JSONObject(geoNotification.notification.getDataJson());
                                obj.put("latitude",  latitude);
                                obj.put("longitude", longitude);

                                Log.d(TAG, "Prepare request ("+url+") and send data: " + obj.toString());

                                JsonObjectRequest jsonObj = new JsonObjectRequest(Request.Method.POST, url, obj,
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                Log.d(TAG, "Request ("+url+") registered on success! (data: " + obj.toString() + ")");
                                            }
                                        }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d(TAG, "Request ("+url+") returned error! (data: " + obj.toString() + ")");
                                    }
                                });
                                requstQueue.add(jsonObj);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(TAG, "Error on json instance: " + e.printStackTrace());
                            }
                        }else{
                            Log.d(TAG, "GeofencePlugin geoNotification.notification is null");
                        }
                        geoNotification.transitionType = transitionType;
                        geoNotifications.add(geoNotification);
                    }else{
                        Log.d(TAG, "GeofencePlugin geoNotification is null");
                    }
                }

                if (geoNotifications.size() > 0) {
                    broadcastIntent.putExtra("transitionData", Gson.get().toJson(geoNotifications));
                    GeofencePlugin.onTransitionReceived(geoNotifications);
                }
            } else {
                String error = "Geofence transition error: " + transitionType;
                logger.log(Log.ERROR, error);
                broadcastIntent.putExtra("error", error);
            }
        }
        sendBroadcast(broadcastIntent);
    }
}

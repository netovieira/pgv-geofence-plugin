package com.pgv.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

public class GeoNotification {
    @Expose public String id;
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public int radius;
    @Expose public int transitionType;

    @Expose public Notification notification;

    public GeoNotification() {
    }

    public Geofence toGeofence() {
        return new Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radius)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                    | Geofence.GEOFENCE_TRANSITION_DWELL
                    | Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setLoiteringDelay(1)
            .setExpirationDuration(Geofence.NEVER_EXPIRE).build();
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public static GeoNotification fromJson(String json) {
        if (json == null) return null;
        return Gson.get().fromJson(json, GeoNotification.class);
    }
}

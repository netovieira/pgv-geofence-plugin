package com.pgv.cordova.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeoNotificationManager manager = new GeoNotificationManager(context);
        manager.loadFromStorageAndInitializeGeofences();
    }
}
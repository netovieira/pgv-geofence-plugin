package com.pgv.cordova.geofence;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class RemoveGeofenceCommand extends AbstractGoogleServiceCommand {
    private List<String> geofencesIds;

    public RemoveGeofenceCommand(Context context, List<String> geofencesIds) {
        super(context);
        this.geofencesIds = geofencesIds;
    }

    @Override
    protected void ExecuteCustomCode() {
        if (geofencesIds != null && geofencesIds.size() > 0) {
            Log.d(GeofencePlugin.TAG, , "Removing geofences...");
            LocationServices.GeofencingApi
                .removeGeofences(mGoogleApiClient, geofencesIds)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.d(GeofencePlugin.TAG, , "Geofences successfully removed");
                            CommandExecuted();
                        } else {
                            String message = "Removing geofences failed - " + status.getStatusMessage();
                            Log.e(GeofencePlugin.TAG, message);
                            CommandExecuted(new Error(message));
                        }
                    }
                });
        } else {
            Log.d(GeofencePlugin.TAG, , "Tried to remove Geofences when there were none");
            CommandExecuted();
        }
    }
}

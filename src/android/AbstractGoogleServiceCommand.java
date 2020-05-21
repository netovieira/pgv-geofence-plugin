package com.pgv.cordova.geofence;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGoogleServiceCommand implements
        ConnectionCallbacks, OnConnectionFailedListener{
    protected boolean connectionInProgress = false;
    protected List<IGoogleServiceCommandListener> listeners;
    protected Context context;
    protected GoogleApiClient mGoogleApiClient;

    public AbstractGoogleServiceCommand(Context context) {
        this.context = context;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
        logger = Logger.getLogger();
        listeners = new ArrayList<IGoogleServiceCommandListener>();
    }

    private void connectToGoogleServices() {
        if (!mGoogleApiClient.isConnected() ||
            (!mGoogleApiClient.isConnecting() && !connectionInProgress)) {
            connectionInProgress = true;
            Log.d(GeofencePlugin.TAG, , "Connecting location client");
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        connectionInProgress = false;
        Log.d(GeofencePlugin.TAG, , "Connecting to google services fail - "
                + connectionResult.toString());

        // TODO: invoke CommandExucuted with ERROR
    }

    @Override
    public void onConnected(Bundle arg0) {
        // TODO Auto-generated method stub
        Log.d(GeofencePlugin.TAG, , "Google play services connected");
        // Get the PendingIntent for the request
        ExecuteCustomCode();
    }

    @Override
    public void onConnectionSuspended(int arg) {

    }

    public void addListener(IGoogleServiceCommandListener listener) {
        listeners.add(listener);
    }

    public void Execute() {
        connectToGoogleServices();
    }

    protected void CommandExecuted() {
        CommandExecuted(null);
    }

    protected void CommandExecuted(Object error) {
        // Turn off the in progress flag and disconnect the client
        connectionInProgress = false;
        mGoogleApiClient.disconnect();
        for (IGoogleServiceCommandListener listener : listeners) {
            listener.onCommandExecuted(error);
        }
    }

    protected abstract void ExecuteCustomCode();
}

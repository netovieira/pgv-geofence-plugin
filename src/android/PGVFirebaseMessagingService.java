package com.pgv.cordova.geofence;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class PGVFirebaseMessagingService extends FirebaseMessagingService
{
    @Override public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(PGVApi.TAG, "Mensagem recebida!!");
    }
}
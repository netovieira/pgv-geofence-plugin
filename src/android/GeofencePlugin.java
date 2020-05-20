package com.pgv.cordova.geofence;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.Manifest;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

//CUSTOM
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

public class GeofencePlugin extends CordovaPlugin {
    public static final String TAG = "PGVGeofencePlugin";

    public static final String ERROR_UNKNOWN = "UNKNOWN";
    public static final String ERROR_PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String ERROR_GEOFENCE_NOT_AVAILABLE = "GEOFENCE_NOT_AVAILABLE";
    public static final String ERROR_GEOFENCE_LIMIT_EXCEEDED = "GEOFENCE_LIMIT_EXCEEDED";

    private GeoNotificationManager geoNotificationManager;

    private Context context;
    public static CordovaWebView webView = null;



    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, ">>>>>>> BroadcastReceiver Event Received!");
        }
    };

    private class Action {
        public String action;
        public JSONArray args;
        public CallbackContext callbackContext;

        public Action(String action, JSONArray args, CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }
    }

    //FIXME: what about many executedActions at once
    private Action executedAction;

    /**
     * @param cordova
     *            The context of the main Activity.
     * @param webView
     *            The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeofencePlugin.webView = webView;
        context = this.cordova.getActivity().getApplicationContext();
        Logger.setLogger(new Logger(TAG, context, false));
        geoNotificationManager = new GeoNotificationManager(context);
    }

    @Override
    public boolean execute(final String action, final JSONArray args,
                           final CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "GeofencePlugin execute action: " + action + " args: " + args.toString());
        executedAction = new Action(action, args, callbackContext);

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if (action.equals("addOrUpdate")) {
                    List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
                    for (int i = 0; i < args.length(); i++) {
                        GeoNotification not = parseFromJSONObject(args.optJSONObject(i));
                        if (not != null) {
                            geoNotifications.add(not);
                        }
                    }
                    geoNotificationManager.addGeoNotifications(geoNotifications, callbackContext);
                } else if (action.equals("remove")) {
                    List<String> ids = new ArrayList<String>();
                    for (int i = 0; i < args.length(); i++) {
                        ids.add(args.optString(i));
                    }
                    geoNotificationManager.removeGeoNotifications(ids, callbackContext);
                } else if (action.equals("removeAll")) {
                    geoNotificationManager.removeAllGeoNotifications(callbackContext);
                } else if (action.equals("getWatched")) {
                    List<GeoNotification> geoNotifications = geoNotificationManager.getWatched();
                    callbackContext.success(Gson.get().toJson(geoNotifications));
                } else if (action.equals("initialize")) {
                    initialize(callbackContext);
                } else if (action.equals("deviceReady")) {
                    deviceReady();
                }
            }
        });

        return true;
    }

    public boolean execute(Action action) throws JSONException {
        return execute(action.action, action.args, action.callbackContext);
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    /*Context context, */
    public static void onTransitionReceived(Context context, List<GeoNotification> geoNotifications, double latitude, double longitude) {
        Log.d(TAG, ">>>>>>> Transition Event Received!");
//
//        final String url = "https://api.localtarget.com.br/api/i-found-one";
//
//        RequestQueue requstQueue = Volley.newRequestQueue(context);
////                                RequestQueue requstQueue = Volley.newRequestQueue(getApplicationContext());
//
//        for (GeoNotification geoNotification : geoNotifications) {
//            try {
//                JSONObject obj = new JSONObject(geoNotification.notification.getDataJson());
//                obj.put("latitude",  latitude);
//                obj.put("longitude", longitude);
//
//                final JSONObject fobj = obj;
//
//                Log.d(TAG, "Prepare request ("+url+") and send data");
//                Log.d(TAG, fobj.toString());
//
//                JsonObjectRequest jsonObj = new JsonObjectRequest(Request.Method.POST, url, obj,
//                        new Response.Listener<JSONObject>() {
//                            @Override
//                            public void onResponse(JSONObject response) {
//                                Log.d(TAG, "******** SUCCESS! Request ("+url+") registered on success!");
//                                Log.d(TAG, "******** SUCCESS! Data: " + fobj.toString());
//                                Log.d(TAG, "******** SUCCESS! Response: "+response.toString());
//                            }
//                        }, new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        Log.d(TAG, "******** ERROR! Request ("+url+") returned error!");
//                        Log.d(TAG, "******** ERROR! Data: " + fobj.toString());
//                        Log.d(TAG, "******** ERROR! Error: " + error..getMessage());
//                    }
//                });
//                Log.d(TAG, "Request added on requstQueue");
//                requstQueue.add(jsonObj);
//            } catch (Exception e) {
//                Log.d(TAG, "******** Error on json instance");
//            }
//        }
//
//        String js = "setTimeout('geofence.onTransitionReceived("
//                + Gson.get().toJson(geoNotifications) + ")',0)";
//        if (webView == null) {
//            Log.d(TAG, "Webview is null");
//        } else {
//            webView.sendJavascript(js);
//        }
    }

    private void deviceReady() {
        Intent intent = cordova.getActivity().getIntent();
        String data = intent.getStringExtra("geofence.notification.data");
    }

    private void initialize(CallbackContext callbackContext) {
        callbackContext.success();
    }

    private boolean hasPermissions(String[] permissions) {
        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
    }

    @Override
    protected void onStart(){
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(MyIntentService.SERVICE_MSG));
    };

    @Override
    protected void onStop(){
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    };

}

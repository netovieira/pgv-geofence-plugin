package com.pgv.cordova.geofence;

import org.apache.cordova.CordovaInterface;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import android.util.Log;

import android.content.Context;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.device.Device;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.location.Location;

import android.content.Context;

public class PGVApi {

    private static PGVApi __instance = null;
    public static final String TAG = "PGVGeofencePlugin";
//    public static final String baseURL = "https://api.localtarget.com.br/api";
    public static final String baseURL = "http://192.168.0.139/teste-api/public/api";

    public JSONObject   userInfo;
    public Boolean      userEmpty;

    private CordovaInterface cordova;
    private Context context;

    private static final int UPDATE_INTERVAL  = 1000;
    private static final int FASTEST_INTERVAL = 900;

    private JSONArray GeofencesList;


    private Location userLocation;
    private FusedLocationProviderClient fusedLocationClient;

	/**
	 * Constructor.
	 */
    private PGVApi(CordovaInterface cordova) {
        this.userEmpty = true;
        this.cordova = cordova;
        this.context = cordova.getActivity().getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(cordova.getActivity());
	}

    // static method to create instance of Singleton class
    public static PGVApi get(CordovaInterface cordova)
    {
        if (__instance == null)
            __instance = new PGVApi(cordova);

        return __instance;
    }

    public void setDeviceInfo(String deviceInfo){
        try{
        writterData(this.context,"user_info.txt", deviceInfo);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getUserInfo(){
        return contentFile( this.context, "user_info.txt");
    }

    public static String teste(JSONObject object){
        return object.toString();
    }

    public void getCampaignGeofences(GeoNotificationManager geoNotificationManager, CallbackContext callbackContext)
    {

        String strUserInfo = contentFile( context, "user_info.txt");
        if(strUserInfo == "") return;

        final String url = baseURL + "/get-geolocation-campaigns";
        final GeoNotificationManager gNM = geoNotificationManager;
        final CallbackContext cc = callbackContext;

        try{
            JSONObject userInfo = new JSONObject(strUserInfo);


            final double lat  = userInfo.getDouble("latitude");
            final double lng  = userInfo.getDouble("logitude");

            final String uuid = userInfo.getString("uuid");
            StringRequest postRequest = new StringRequest(
                    Request.Method.POST,
                    url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(response);
                                try {
                                    // Loop through the array elements
                                    GeofencesList = jsonObject.getJSONArray("campaigns");

                                    List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
                                    for (int i = 0; i < GeofencesList.length(); i++) {
                                        GeoNotification not = parseFromJSONObject(GeofencesList.optJSONObject(i));
                                        if (not != null) {
                                            geoNotifications.add(not);
                                        }
                                    }
                                    gNM.addGeoNotifications(geoNotifications, cc);
                                }catch (JSONException e){
                                    e.printStackTrace();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            //NOTHING HERE
                        }
                    }
            ) {
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("UUID", uuid);
                    params.put("app_token", "94D118FA41B08F91E734A8E89C89521F");
                    params.put("latitude", Double.toString(lat));
                    params.put("longitude", Double.toString(lng));

                    return params;
                }
            };

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    public void setUserInfo(String userInfo) {
        String url = baseURL + "/app-user-data";

        try{
            JSONObject obj = new JSONObject(userInfo);
            obj.put("app_token", "94D118FA41B08F91E734A8E89C89521F");

            startLocationUpdates();

            obj.put("latitude",  userLocation.getLatitude());
            obj.put("longitude", userLocation.getLongitude());

            writterData(this.context,"user_info.txt", obj.toString());

            RequestQueue requstQueue = Volley.newRequestQueue(context);
            JsonObjectRequest jsonObj = new JsonObjectRequest(Request.Method.POST, url, obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });

            requstQueue.add(jsonObj);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void getLastKnownLocation() {
        fusedLocationClient.getLastLocation().
                addOnSuccessListener(this.cordova.getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            writeActualLocation(location);
                            startLocationUpdates();
                        } else {
                            startLocationUpdates();
                        }
                    }
                });
    }

    private void writeActualLocation(Location location) {
        if(location != null) {
            try{
                this.userLocation = location;
                userInfo = new JSONObject(contentFile( context, "user_info.txt"));
                userInfo.put("latitude",  location.getLatitude());
                userInfo.put("longitude", location.getLongitude());
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                writeActualLocation(locationResult.getLastLocation());
            }
        }, null);
    }






    public static void writterData(Context context, String fileName, String data)
    {
        File file = new File(context.getFilesDir() + File.pathSeparator + fileName);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String contentFile(Context context, String fileName) {
        File file = new File(context.getFilesDir() + File.pathSeparator + fileName);
        if(file.exists()) {
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                return bufferedReader.readLine();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return "";
    }

    public static void sendToServer(Context context, Location location) {

        String url = baseURL + "/i-found-one";

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        RequestQueue requstQueue = Volley.newRequestQueue(context);
        try {
            JSONObject obj = new JSONObject(contentFile(context,"user_info.txt"));
            obj.put("latitude",  latitude);
            obj.put("longitude", longitude);

            JsonObjectRequest jsonObj = new JsonObjectRequest(Request.Method.POST, url, obj,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "sendToServer().onResponse()");
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "sendToServer().onErrorResponse() - " + error.getMessage());
                }
            });
            requstQueue.add(jsonObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

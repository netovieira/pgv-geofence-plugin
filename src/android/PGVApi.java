package com.pgv.cordova.geofence;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.cordova.CallbackContext;

import org.apache.cordova.device.Device;

public class PGVApi {

    private static PGVApi __instance = null;

    public String       uuid;
    public JSONObject   userInfo;
    public float        latitude;
    public float        longitude;
    public Boolean      userEmpty;


    private JSONArray GeofencesList;

	/**
	 * Constructor.
	 */
    private PGVApi() {
        this.userEmpty = true;
        this.latitude  = 0;
        this.longitude = 0;
	    this.uuid = "AAAAAA-RRRRRR-YYYYYYY-OOOOOOO";
	}

    // static method to create instance of Singleton class
    public static PGVApi get()
    {
        if (__instance == null)
            __instance = new PGVApi();

        return __instance;
    }
    public void getCampaignGeofences(float latitude, float longitude, GeoNotificationManager geoNotificationManager, CallbackContext callbackContext) {

        final GeoNotificationManager gNM = GeoNotificationManager geoNotificationManager;

        final float  lat  = latitude;
        final float  lng  = longitude;
        final String uuid = this.uuid;

        String url = "https://api.localtarget.com.br/api/get-geolocation-campaigns";
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
                                gNM.addGeoNotifications(geoNotifications, callbackContext);
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
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo    = new JSONObject(userInfo);
        this.latitude    = userInfo.getDouble("latitude");
        this.longitude   = userInfo.getDouble("longitude");
        this.uuid        = userInfo.getDouble("uuid");
        this.userEmpty   = false;
        final String sui = userInfo;

        String url = "https://api.localtarget.com.br/api/app-user-data";
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
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
                params.put("app_token", "94D118FA41B08F91E734A8E89C89521F");
                params.put("data", sui);

                return params;
            }
        };
    }

    public void geofenceTriggered() {
        final String userInfo = this.userInfo.toString();
        final String url = "https://api.localtarget.com.br/api/i-found-one";
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //NOTHING HERE
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
                params.put("app_token", "94D118FA41B08F91E734A8E89C89521F");
                params.put("data", userInfo);

                return params;
            }
        };
    }
}

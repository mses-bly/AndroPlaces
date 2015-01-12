package com.android.aboutplaces.utils;

import android.content.Context;
import android.util.Log;

import com.android.aboutplaces.*;
import com.android.aboutplaces.model.Place;
import com.android.aboutplaces.model.System;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Moises on 12/22/2014.
 */

//Communications interface with the API
public class APIRequests {
    //synchronization token to use for the 1 sec restriction between calls
    private static Object synchronizationToken = new Object();

    //Obtain APIs available metro areas
    public static Hashtable<String, String> getAvailableMetros(Context context) {
        final String requestURL = context.getString(R.string.api_url) + "/metro/?api_key=" + context.getString(R.string.about_place_api_key);
        Log.d("INFO", "Requesting available metros " + requestURL);
        JSONArray response = makeHttpRequestArray(requestURL);
        if (response == null) {
            return null;
        }
        String[] metrosNames = new String[response.length()];
        Hashtable<String, String> availableMetros = new Hashtable<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject jsonObject = response.getJSONObject(i);
                String metro = (String) jsonObject.get("metro");
                metrosNames[i] = metro.substring(0, 1).toUpperCase() + metro.substring(1, metro.length());
                availableMetros.put(metro, jsonObject.getString("metroid"));
            }
            return availableMetros;
        } catch (JSONException e) {
            Log.d("ERROR", "Metros could not be resolved - JSON Exception" + " URL: > " + requestURL);
        }
        return null;
    }

    //Obtain APIs smartMapPid for a given metro area and desired system
    public static String getSmartMapPid(String metroID, System system, Context context) {
        String requestURL = null;
        try {
            requestURL = context.getString(R.string.api_url) + "/metro/" + metroID + "/smartmapp" + system.getSystemURL() + "?api_key=" + context.getString(R.string.about_place_api_key);
            Log.d("INFO", "Requesting SmartMapPid " + requestURL);
            JSONArray response = makeHttpRequestArray(requestURL);
            if (response == null) {
                return null;
            }
            return response.getJSONObject(0).getString("smartmappid");
        } catch (JSONException e) {
            Log.d("ERROR", "Smartmappid could not be resolved - JSON Exception" + " URL: > " + requestURL);
        }
        return null;
    }

    //Obtain top 1 PLACE within map bounds
    public static Place getBestPlaceInView(String mapViewBounds, String smartMapPid, System system, Context context) {
        String requestURL = context.getString(R.string.api_url) + "/smartmapp/" + smartMapPid + "/places/in/" + mapViewBounds + "?limit=1&api_key=" + context.getString(R.string.about_place_api_key);
        Log.d("INFO", "Requesting best place for: " + requestURL);
        JSONArray response = makeHttpRequestArray(requestURL);
        if (response == null) {
            return null;
        }
        Place bestPlace = new Place();
        JSONObject jsonObject = null;
        try {
            jsonObject = response.getJSONObject(0);
            bestPlace.setPlaceId((String) jsonObject.getString("placeid"));
            bestPlace.setPlacePulse((String) jsonObject.getString("pulse"));
            bestPlace.setSystem(system);
            return bestPlace;
        } catch (JSONException e) {
            Log.d("ERROR", "No places for " + system.getSystemId() + " obtained in this area - JSON Exception " + e.getMessage() + " URL: > " + requestURL);
        }
        return null;
    }
    //Obtain PLACE shape
    public static ArrayList<LatLng> getPlaceShape(Place place, Context context) {
        String requestURL = context.getString(R.string.api_url) + "/place/" + place.getPlaceId() + "/shape.geojson?api_key=" + context.getString(R.string.about_place_api_key);
        try {
            Log.d("INFO", "Requesting place shape " + requestURL);
            JSONObject response = makeHttpRequestObject(requestURL);
            if (response == null) {
                return null;
            }
            ArrayList<LatLng> shapeCoordinates = new ArrayList<>();
            JSONArray array = null;
            array = response.getJSONObject("geojson").getJSONArray("coordinates").getJSONArray(0).getJSONArray(0);
            for (int i = 0; i < array.length(); i++) {
                JSONArray coordinate = array.getJSONArray(i);
                //adapt to AboutPlaceAPI, Lng - Lat
                shapeCoordinates.add(new LatLng(coordinate.getDouble(1), coordinate.getDouble(0)));
            }
            return shapeCoordinates;
        } catch (JSONException e) {
            Log.d("ERROR", "No place shape " + place.getSystem().getSystemId() + " obtained in this area - JSON Exception" + e.getMessage() + " URL: > " + requestURL);
        }
        return null;
    }
    //Obtain PLACE count
    public static String getPlaceCount(Place place, Context context) {
        final String requestURL = context.getString(R.string.api_url) + "/place/" + place.getPlaceId() + place.getSystem().getSystemURL() + "?count&api_key=" + context.getString(R.string.about_place_api_key);
        try {
            Log.d("INFO", "Requesting place count " + requestURL);
            JSONObject response = makeHttpRequestObject(requestURL);
            if (response == null) {
                return null;
            }
            String count = response.getString("count");
            return count;
        } catch (JSONException e) {
            Log.d("ERROR", "Processing Error - Could not obtain the count for  " + place.getSystem().getSystemId() + " in this area - JSON Exception" + e.getMessage() + " URL: > " + requestURL);
        }
        return null;
    }
    //HTTP Get with JSON Array response
    private static JSONArray makeHttpRequestArray(String URL) {
        HttpResponse response = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(URL));
            //Obligatory to respect API restrictions - 1 call every second
            synchronized(synchronizationToken){
                response = client.execute(request);
                sleep(400);
            }
            if (response != null) {
                String json = EntityUtils.toString(response.getEntity());
                JSONArray jsonArray = new JSONArray(json);
                return jsonArray;
            }
        } catch (URISyntaxException e) {
            Log.d("ERROR", "URI Syntax Exception" + " " + e.getMessage() + " URL: > " + URL);
        } catch (ClientProtocolException e) {
            Log.d("ERROR", "Client protocol Exception" + " " + e.getMessage() + " URL: > " + URL);
        } catch (IOException e) {
            Log.d("ERROR", "IO Exception" + " " + e.getMessage() + " URL: > " + URL);
        } catch (JSONException e) {
            Log.d("ERROR", "JSON Exception" + " " + e.getMessage() + " URL: > " + URL);
        }
        return null;
    }
    //HTTP Get with JSON Object response
    private static JSONObject makeHttpRequestObject(String URL) {
        HttpResponse response = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(URL));
            //Obligatory to respect API restrictions - 1 call every second
            synchronized(synchronizationToken){
                response = client.execute(request);
                sleep(400);
            }
            if (response != null) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = new JSONObject(json);
                return jsonObject;
            }
        } catch (URISyntaxException e) {
            Log.d("ERROR", "URI Syntax Exception" + " " + e.getMessage() + " URL: > " + URL);
        } catch (ClientProtocolException e) {
            Log.d("ERROR", "Client protocol Exception" + " " + e.getMessage() + " URL: > " + URL);
        } catch (IOException e) {
            Log.d("ERROR", "IO Exception" + " " + e.getMessage() + " URL: > " + URL);
        } catch (JSONException e) {
            Log.d("ERROR", "JSON Exception" + " " + e.getMessage() + " URL: > " + URL);
        }
        return null;
    }

    private static void sleep(long millisec){
        try {
            Thread.sleep(millisec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

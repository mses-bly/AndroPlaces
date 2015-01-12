
package com.android.aboutplaces.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.aboutplaces.*;
import com.android.aboutplaces.model.Place;
import com.android.aboutplaces.model.System;
import com.android.aboutplaces.utils.APIRequests;
import com.android.aboutplaces.utils.Animations;
import com.android.aboutplaces.utils.MapHandler;
import com.android.aboutplaces.utils.MyListDialog;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

//Main UI fragment of the application
public class HomeScreen extends FragmentActivity implements MyListDialog.ListDialogListener {

    //map object for this fragment
    private GoogleMap mMap;
    //context of this fragment
    private Context context;
    //Map object toolbox class
    private MapHandler mapHandler;
    //Progress dialog to show in case of need (ie. getting the available metro areas)
    private ProgressDialog progressDialog;
    //Handler used to post actions to main UI thread
    private Handler handler;

    //Key: systemId (ie bars) Value: System
    private Hashtable<String, System> systemsTable;
    //Key: systemId Value: place Object
    private Hashtable<String,Place> bestPlaceInViewTable;
    //Key: metro name (ie miami) Value: metro id (ie 7)
    private Hashtable<String, String> availableMetrosTable;
    //current metro area selected
    private String currentMetro;
    //textview used to display average pulse in the visible bounding box
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        //set up handler
        handler = new Handler();
        //get the context
        context = this;
        //prepare systems table
        systemsTable = new Hashtable<>();
        //prepare best places table
        bestPlaceInViewTable = new Hashtable<>();
        //load the systems we are going to use. For this instance of the app: bars, bowling alleys, dance salons, casual dinning, movie theaters
        loadSystems();
        //load the map API object
        setUpMapIfNeeded();

        //Entry point
        getAvailableMetros();

        textView = (TextView)findViewById(R.id.textview_snippet);

        //define button click events handlers
        ImageButton refreshImageButton = (ImageButton)findViewById(R.id.refresh_button);
        refreshImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animations.fadeOut(textView);
                mapHandler.clearMap();
                updateMap();
            }
        });

        ImageButton metroButton = (ImageButton)findViewById(R.id.metro_button);
        metroButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animations.fadeOut(textView);
                mapHandler.clearMap();
                getAvailableMetros();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //set up the map if need be.
        setUpMapIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //sets up the map object, if not already done.
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    //Simulate GPS location button touch, since I am not in an available metro area
                    LatLng position = mapHandler.getLatLngFromAddress("Miami Beach, FL");
                    if (position == null) {
                        Toast.makeText(context, "Given address could not be resolved", Toast.LENGTH_LONG).show();
                        return true;
                    }
                    mapHandler.clearMap();
                    mapHandler.moveMapToPoint(position, 14);
                    //try to determine whether user's location is in one of available metro areas
                    //(current or otherwise)
                    String usersMetro = mapHandler.getLocalityFromLatLng(position);
                    if(usersMetro != null){
                        //the users is not in the currently selected metro
                        //we need to check if the user is in an available metro, and if so, change all
                        if(!usersMetro.toLowerCase().contains(currentMetro)){
                            currentMetro = null;
                            for(String metro : availableMetrosTable.keySet()){
                                if(usersMetro.toLowerCase().contains(metro)){
                                    currentMetro = metro;
                                    getSmartAppPids(metro);
                                    return true;
                                }
                            }
                            Toast.makeText(context, "Current location was not available", Toast.LENGTH_LONG).show();
                            return true;
                        }
                        updateMap();
                    }
                    else{
                        Toast.makeText(context, "Current location was not available", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });
            mapHandler = new MapHandler(mMap,this);
            mapHandler.initializeMapInformation();
        }
    }
    //load the systems to be used in this version of the app.
    //Any given set of systems could be loaded. I have chosen these as an example app for nightlife.
    private void loadSystems(){
        System bars = new System("bars","Bars",getString(R.string.bars_url),R.drawable.bar);
        System bowling_alleys = new System("bowling_alleys","Bowling Alleys",getString(R.string.bowling_alleys_url),R.drawable.bowling);
        System dance = new System("dance","Dance salons",getString(R.string.dance_url),R.drawable.dance);
        System casual_dinning = new System("casual_dinning_url","Casual Dinning Restaurants",getString(R.string.casual_dinning_url),R.drawable.casual);
        System movie_theaters= new System("movie_theaters","Movie Theaters",getString(R.string.movie_theaters_url),R.drawable.movie_theaters);
        systemsTable.put("bars", bars);
        //systemsTable.put("bowling_alleys", bowling_alleys);
        //systemsTable.put("dance", dance);
        //systemsTable.put("casual_dinning", casual_dinning);
        //systemsTable.put("movie_theaters", movie_theaters);
    }

    //event to handle when the user has selected an available metro area
    @Override
    public void onDialogClick(String selectedItem) {
        currentMetro = selectedItem.toLowerCase();
        getSmartAppPids(currentMetro);
        LatLng position = mapHandler.getLatLngFromAddress(selectedItem);
        if (position == null) {
            Toast.makeText(context, "Given address could not be resolved", Toast.LENGTH_LONG);
            return;
        }
        mapHandler.moveMapToPoint(position, 14);
    }
    //Updates the map information for a particular system.
    public void updateMap(final System system){
        handler.post(new Runnable() {
            @Override
            public void run() {
                String mapBounds = mapHandler.getMapViewBounds();
                getBestPlaceInView(system,mapBounds);
            }
        });

    }
    //Updates the map for all included systems.
    private void updateMap(){
        Animations.fadeOut(textView);
        String mapBounds = mapHandler.getMapViewBounds();
        Toast.makeText(this,"Getting PLACES in this area...",Toast.LENGTH_LONG).show();
        for(String systemId: systemsTable.keySet()){
            if (systemsTable.get(systemId).getSmartAppPid() != null) {
                getBestPlaceInView(systemsTable.get(systemId), mapBounds);
            }
        }
    }

    //Obtain the available metro areas from the API
    private void getAvailableMetros(){
        progressDialog = ProgressDialog.show(context,null,"Obtaining available metro areas");
        if(availableMetrosTable == null || availableMetrosTable.isEmpty()) {
            availableMetrosTable = APIRequests.getAvailableMetros(context);
        }
        if(availableMetrosTable == null){
            Toast.makeText(context,"No metro areas available", Toast.LENGTH_LONG);
        }
        else{
            String[] metroNames = new String[availableMetrosTable.size()];
            int i = 0;
            for(String metroName: availableMetrosTable.keySet()){
                metroNames[i++] = metroName.substring(0,1).toUpperCase() + metroName.substring(1,metroName.length());
            }
            Arrays.sort(metroNames);
            MyListDialog selectMetroDialog = new MyListDialog();
            Bundle params = new Bundle();
            params.putString("title", "Select a metro area");
            params.putStringArray("elements", metroNames);
            selectMetroDialog.setArguments(params);
            selectMetroDialog.show(getSupportFragmentManager(), "select_metro_dialog");
        }
        progressDialog.dismiss();
    }

    //Obtain the SmartAppPid of every included system for a particular metro area.
    private void getSmartAppPids(final String metroName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String systemId : systemsTable.keySet()) {
                    final System system = systemsTable.get(systemId);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String smartMapPid = APIRequests.getSmartMapPid(metroName, system, context);
                            if(smartMapPid != null){
                                System systemToUpdate = system;
                                systemToUpdate.setSmartAppPid(smartMapPid);
                                //Thread Safe
                                synchronized (systemsTable) {
                                    systemsTable.put(systemToUpdate.getSystemId(),systemToUpdate);
                                }
                                updateMap(systemToUpdate);
                            }
                        }
                    }).start();
                }
            }
        }).start();
    }
    //Updates the highest pulse ranking PLACE in the current map view bounds, for a given system.
    private void getBestPlaceInView(final System system, final String mapBounds){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Place place = APIRequests.getBestPlaceInView(mapBounds,system.getSmartAppPid(),system,context);
                //if could obtain a place for this coordinates and system
                if(place != null){
                    ArrayList<LatLng> thisPlaceShape = APIRequests.getPlaceShape(place,context);
                    //if could obtain a place shape
                    if(thisPlaceShape != null){
                        place.setShapeCoordinates(thisPlaceShape);
                        //Thread safe
                        synchronized (bestPlaceInViewTable){
                            bestPlaceInViewTable.put(system.getSystemId(),place);
                        }
                        updatePulseAverageText();
                        //we can draw
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mapHandler.drawPlaceMarker(bestPlaceInViewTable.get(system.getSystemId()));
                            }
                        });

                        //Get the count for this place.
                        String count = APIRequests.getPlaceCount(place,context);
                        //if could obtain count for this place
                        if(count != null){
                            place.setCount(count);
                            //Thread safe update
                            synchronized (bestPlaceInViewTable){
                                bestPlaceInViewTable.put(system.getSystemId(),place);
                            }
                        }
                    }
                }
            }
        }).start();
    }

    //Obtains the latest Best PLACE for a system. This function is used as a callback from the Map Handler.
    public Place getLatestPlaceVersion(String systemId){
        return bestPlaceInViewTable.get(systemId);
    }

    //Updates the text showing the average pulse for systems in the current view bound.
    private void updatePulseAverageText(){
        double average = 0;
        for (String systemId : bestPlaceInViewTable.keySet()){
            average += Double.valueOf(bestPlaceInViewTable.get(systemId).getPlacePulse());
        }
        average /= bestPlaceInViewTable.size();
        final String pulse = String.format("%.3g", average);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(textView.getVisibility() == View.VISIBLE){
                    Animations.fadeOut(textView);
                    textView.setText("Pulse for this area: "+pulse);
                    Animations.fadeIn(textView);
                }
                else{
                    textView.setText("Pulse for this area: "+pulse);
                    Animations.fadeIn(textView);
                }
            }
        });

    }



}

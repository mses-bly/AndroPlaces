package com.android.aboutplaces.utils;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.android.aboutplaces.ui.HomeScreen;
import com.android.aboutplaces.model.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Created by Moises on 12/22/2014.
 */
//Map UI handler class. Uses Google Maps v2 for Android API
public class MapHandler {

    //keep track of markers in the Map
    private Hashtable<Marker, String> markersTable;
    //polygon being drawn in the map
    private Polygon polygon;
    //map oject
    private GoogleMap gmap;
    //calling fragment class
    private HomeScreen parentFragment;

    public MapHandler(GoogleMap gmap, HomeScreen parentFragment) {
        this.gmap = gmap;
        this.parentFragment = parentFragment;
        markersTable = new Hashtable<>();
    }

    //obtain Latitude and Longitude from String address
    public LatLng getLatLngFromAddress(String addr) {
        Geocoder geocoder = new Geocoder(parentFragment);
        try {
            List<Address> addressList = geocoder.getFromLocationName(addr, 5);
            if (addressList == null) {
                return null;
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            return latLng;
        } catch (IOException e) {
            Log.d("ERROR", "Address not found");
            return null;
        }
    }

    //Obtain Locality (city) from Latitude and Longitude position
    public String getLocalityFromLatLng(LatLng position){
        Geocoder gcd = new Geocoder(parentFragment);
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(position.latitude, position.longitude, 1);
            if (addresses.size() > 0) {
                return addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            Log.d("ERROR", "Locality not resolved");
        }
        return null;
    }

    //move the map camera to a given position, with a given zoom. Performs a simple animation on movement.
    public void moveMapToPoint(LatLng latLng, float zoom) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();
        gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
    }

    //get the visible area bounds of the map, at the moment of call.
    public String getMapViewBounds() {
        LatLngBounds bounds = gmap.getProjection().getVisibleRegion().latLngBounds;
        LatLng ne = bounds.northeast;
        LatLng sw = bounds.southwest;
        double[] result = {sw.longitude, sw.latitude, ne.longitude, ne.latitude};
        String resultString = Arrays.toString(result);
        resultString = resultString.substring(1, resultString.length() - 1);
        resultString.replaceAll("\\s+", "");
        return resultString.replaceAll("\\s+", "");
    }

    public void drawPlace(Place place) {
        if (polygon != null) {
            polygon.remove();
        }
        PolygonOptions options = new PolygonOptions().fillColor(Color.argb(200, 0, 153, 153)).strokeWidth((float) 0.5);
        for (LatLng coordinates : place.getShapeCoordinates()) {
            options.add(coordinates);
        }
        if (place.getShapeCoordinates() != null && place.getShapeCoordinates().size() > 0) {
            polygon = gmap.addPolygon(options);
        }
    }
    //clear all objects from the map (markers, polygons etc.)
    public void clearMap(){
        gmap.clear();
        markersTable.clear();
    }
    //initializes the map object to a clean state. Defines the marker click event.
    public void initializeMapInformation() {
        gmap.clear();
        markersTable.clear();
        gmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (markersTable.get(marker) != null) {
                    Place place = parentFragment.getLatestPlaceVersion(markersTable.get(marker));
                    drawPlace(parentFragment.getLatestPlaceVersion(markersTable.get(marker)));
                    if(place.getCount() != null && Integer.valueOf(place.getCount()) > 0){
                        marker.setTitle(place.getSystem().getSystemName());
                        marker.setSnippet(place.getCount() + " " + place.getSystem().getSystemName() + " in this area");
                        marker.showInfoWindow();
                    }
                    else{
                        marker.setTitle(place.getSystem().getSystemName());
                        marker.showInfoWindow();
                    }
                    drawPlaceMarker(place);
                }
                return false;
            }
        });
    }

    //draws the polygon associated with a PLACE in the map
    public void drawPlaceMarker(Place place) {
        if (place.getShapeCoordinates() != null && place.getShapeCoordinates().size() > 0) {
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            for (int i = 1; i < place.getShapeCoordinates().size(); i++) {
                bounds = bounds.include(place.getShapeCoordinates().get(i));
            }
            LatLng markerPos = bounds.build().getCenter();
            MarkerOptions markerOptions = new MarkerOptions().position(markerPos).draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(place.getSystem().getSystemIcon()));
            Marker marker = gmap.addMarker(markerOptions);
            markersTable.put(marker, place.getSystem().getSystemId());
        }
    }
}
package com.android.aboutplaces.utils;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.android.aboutplaces.model.Place;
import com.android.aboutplaces.ui.HomeScreen;
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

/**
 * Created by Moises on 12/22/2014.
 * Map UI handler class. Uses Google Maps v2 for Android API.
 * Handles most of the maps interactions.
 */
public class MapHandler {

    //Table to keep track of markers in the map object.
    private Hashtable<Marker, String> markersTable;
    //Only one polygon will be drawn in the map at a particular time, when the user taps on a marker.
    private Polygon polygon;
    //Map object.
    private GoogleMap gmap;
    //Some interactions with the main UI thread need to be acceded from this class, so we need the calling fragment.
    private HomeScreen parentFragment;

    public MapHandler(GoogleMap gmap, HomeScreen parentFragment) {
        this.gmap = gmap;
        this.parentFragment = parentFragment;
        markersTable = new Hashtable<>();
    }

    //Obtain Latitude and Longitude from String address
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

    //Move the map camera to a given position, with a given zoom. Performs a simple animation on movement.
    public void moveMapToPoint(LatLng latLng, float zoom) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(zoom)
                .build();
        gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
    }

    //Get the visible area bounds of the map, at the moment of call.
    public String getMapViewBounds() {
        LatLngBounds bounds = gmap.getProjection().getVisibleRegion().latLngBounds;
        LatLng ne = bounds.northeast;
        LatLng sw = bounds.southwest;
        double[] result = {sw.longitude, sw.latitude, ne.longitude, ne.latitude};
        String resultString = Arrays.toString(result);
        //simple format of the String array - remove spaces and square brackets at the beginning and end to
        //provide the API with necessary set of BBox parameters.
        resultString = resultString.substring(1, resultString.length() - 1);
        return resultString.replaceAll("\\s+", "");
    }
    //Draw the shape of a place in the map.
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
    //Clear all objects from the map (markers, polygons etc.)
    public void clearMap(){
        gmap.clear();
        markersTable.clear();
    }
    //Initializes the map object to a clean state. Defines the marker click event.
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

    //Draw the marker for a place.
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
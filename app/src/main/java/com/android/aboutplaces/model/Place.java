package com.android.aboutplaces.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Moises on 12/22/2014.
 */
//PLACE basic model.
public class Place {
    //Id
    private String placeId;
    //Pulse
    private String placePulse;
    //Coordinates that define the place shape
    private ArrayList<LatLng> shapeCoordinates;
    //Count of relevant locations to this PLACE
    private String count = null;
    //System to which this place belongs to: i.e. cinemas, bars ...
    private System system;

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlacePulse() {
        return placePulse;
    }

    public void setPlacePulse(String placePulse) {
        this.placePulse = placePulse;
    }

    public ArrayList<LatLng> getShapeCoordinates() {
        return shapeCoordinates;
    }

    public void setShapeCoordinates(ArrayList<LatLng> shapeCoordinates) {
        this.shapeCoordinates = shapeCoordinates;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public System getSystem() {
        return system;
    }

    public void setSystem(System system) {
        this.system = system;
    }

    @Override
    public boolean equals(Object o) {
        Place other = (Place) o;
        return this.placeId.equals(other.placeId);
    }
}

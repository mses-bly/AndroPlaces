package com.android.aboutplaces.model;

/**
 * Created by Moises on 12/25/2014.
 */
//Basic Model for a System (i.e.  Bars)
public class System {
    //ID i.e. bowling_alleys: stored by string value rather than numeric for easy of use with API.
    private String systemId;
    //Custom field, display name: i.e. bowling_alleys = Bowling Alleys.
    private String systemName;
    //System URL in the API: i.e. /systems/entertainment/movie_theaters
    private String systemURL;
    //Icon to display when tapping a place that belongs to this system.
    private int systemIcon;
    //SmartAppPid recovered for this system in a particular metro area.
    private String smartAppPid;

    public System(String systemId, String systemName, String systemURL, int systemIcon) {
        this.systemId = systemId;
        this.systemName = systemName;
        this.systemURL = systemURL;
        this.systemIcon = systemIcon;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getSystemURL() {
        return systemURL;
    }

    public int getSystemIcon() {
        return systemIcon;
    }

    public String getSmartAppPid() {
        return smartAppPid;
    }

    public void setSmartAppPid(String smartAppPid) {
        this.smartAppPid = smartAppPid;
    }

    @Override
    public boolean equals(Object o) {
        System other = (System)o;
        return this.systemId.equals(other.getSystemId());
    }
}

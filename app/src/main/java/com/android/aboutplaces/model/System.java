package com.android.aboutplaces.model;

/**
 * Created by Moises on 12/25/2014.
 */
//Model for a System (IE.  Bars)
public class System {

    private String systemId;
    private String systemName;
    private String systemURL;
    private int systemIcon;
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

package com.example.hazardzoneawarness1;

public class HZALogData {
    //Need to send this data to the server: BeaconID, UserID, Entering or Exiting
    private String beaconID = "";
    private String userID = "";
    private Boolean isEntering = false;

    public String getBeaconID() {
        return beaconID;
    }

    public void setBeaconID(String beaconID) {
        this.beaconID = beaconID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public Boolean getEntering() {
        return isEntering;
    }

    public void setEntering(Boolean entering) {
        isEntering = entering;
    }
}

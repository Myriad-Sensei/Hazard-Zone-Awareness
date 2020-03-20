package com.example.hazardzoneawarness1;

import android.content.Context;

public class HazardZoneData {
    private String BeaconID;
    private String FacilityName;
    private String FacilityID;
    private String FacilityPhoneNumber;
    private String gblHazardSeverity;
    private String HazardSeverity;
    private String HazardZoneID;
    private String HazardZoneName;
    private String gblHazardZoneID;
    private String HazardLink;
    private Boolean gblHazardZoneIDChanged;

    private Double BeaconDistance;
    private Long BeaconBattery;
    private Double lat;
    private Double lon;
    private Double temperatureC;
    private Double lightSensor;
    private String nfcData;
    private Double humidity;
    private Double pressure;
    private Double airQuality;
    private Double absoluteOrientation;
    private Boolean enteringZone;

    public Double getAbsoluteOrientation() {
        return absoluteOrientation;
    }

    public void setAbsoluteOrientation(Double absoluteOrientation) {
        this.absoluteOrientation = absoluteOrientation;
    }

    // This defines a object to hold any hazard data or specific clues for other functions
    public HazardZoneData(Context context){
        BeaconID = context.getString(R.string.beacon_id);
        FacilityName = context.getString(R.string.facility_name);
        FacilityID = "";
        FacilityPhoneNumber = context.getString(R.string.facility_phone_number);
        gblHazardSeverity = "";
        HazardSeverity = context.getString(R.string.hazard_severity);
        HazardZoneID = "";
        HazardZoneName = context.getString(R.string.hazard_zone_name);
        gblHazardZoneID = "";
        HazardLink = "";
        gblHazardZoneIDChanged = false;
        BeaconBattery = 0L;
        BeaconDistance = 0.0;
        lat = null;
        lon = null;
        temperatureC = null;
        lightSensor = null;
        nfcData = "";
        humidity = null;
        pressure = null;
        airQuality = null;
        absoluteOrientation = null;
        enteringZone = null;

    }
    public HazardZoneData(){
        BeaconID = "";
        FacilityName = "";
        FacilityID = "";
        gblHazardSeverity = "";
        HazardSeverity = "";
        HazardZoneID = "";
        HazardZoneName = "";
        gblHazardZoneID = "";
        HazardLink = "";
        gblHazardZoneIDChanged = false;
        BeaconBattery = 0L;
        BeaconDistance = 0.0;
        lat = null;
        lon = null;
        temperatureC = null;
        lightSensor = null;
        nfcData = "";
        humidity = null;
        pressure = null;
        airQuality = null;
        absoluteOrientation = null;
        enteringZone = null;

    }


    public Double getBeaconDistance() { return BeaconDistance; }

    public void setBeaconDistance(Double beaconDistance) { BeaconDistance = beaconDistance; }

    public long getBeaconBattery() { return BeaconBattery; }

    public void setBeaconBattery(long beaconBattery) { BeaconBattery = beaconBattery; }

    public String getBeaconID() {
        return BeaconID;
    }

    public void setBeaconID(String beaconID) {
        BeaconID = beaconID;
    }

    public String getFacilityName() {
        return FacilityName;
    }

    public void setFacilityName(String FacilityName) {
        this.FacilityName = FacilityName;
    }

    public String getFacilityID() {
        return FacilityID;
    }

    public void setFacilityID(String facilityID) {
        FacilityID = facilityID;
    }

    public String getGblHazardSeverity() {
        return gblHazardSeverity;
    }

    public void setGblHazardSeverity(String gblHazardSeverity) {
        this.gblHazardSeverity = gblHazardSeverity;
    }

    public String getHazardSeverity() {
        return HazardSeverity;
    }

    public void setHazardSeverity(String hazardSeverity) {
        HazardSeverity = hazardSeverity;
    }

    public String getHazardZoneID() {
        return HazardZoneID;
    }

    public void setHazardZoneID(String hazardZoneID) {
        if (hazardZoneID != HazardZoneID) {
            HazardZoneID = hazardZoneID;
            gblHazardZoneIDChanged = true;
        }
        else{
            gblHazardZoneIDChanged = false;
        }
    }

    public String getHazardZoneName() {
        return HazardZoneName;
    }

    public void setHazardZoneName(String hazardZoneName) {
        HazardZoneName = hazardZoneName;
    }

    public String getGblHazardZoneID() {
        return gblHazardZoneID;
    }

    public void setGblHazardZoneID(String gblHazardZoneID) {
        this.gblHazardZoneID = gblHazardZoneID;
    }


    public String getHazardLink() {
        return HazardLink;
    }

    public void setHazardLink(String hazardLink) {
        HazardLink = hazardLink;
    }

    public Boolean getGblHazardZoneIDChanged() {
        return gblHazardZoneIDChanged;
    }


    public String getHazardName() {
        return HazardName;
    }

    public void setHazardName(String hazardName) {
        HazardName = hazardName;
    }

    private String HazardName;

    public void loadHazardData(String BeaconID){

    }

    public String getFacilityPhoneNumber() {
        return FacilityPhoneNumber;
    }

    public void setFacilityPhoneNumber(String facilityPhoneNumber) {
        FacilityPhoneNumber = facilityPhoneNumber;
    }

    public void setGblHazardZoneIDChanged(Boolean gblHazardZoneIDChanged) {
        this.gblHazardZoneIDChanged = gblHazardZoneIDChanged;
    }

    public void setBeaconBattery(Long beaconBattery) {
        BeaconBattery = beaconBattery;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(Double temperatureC) {
        this.temperatureC = temperatureC;
    }

    public Double getLightSensor() {
        return lightSensor;
    }

    public void setLightSensor(Double lightSensor) {
        this.lightSensor = lightSensor;
    }

    public String getNfcData() {
        return nfcData;
    }

    public void setNfcData(String nfcData) {
        this.nfcData = nfcData;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getPressure() {
        return pressure;
    }

    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }

    public Double getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(Double airQuality) {
        this.airQuality = airQuality;
    }

    public Boolean getEnteringZone() {
        return enteringZone;
    }

    public void setEnteringZone(Boolean enteringZone) {
        this.enteringZone = enteringZone;
    }
}


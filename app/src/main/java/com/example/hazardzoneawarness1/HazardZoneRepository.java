package com.example.hazardzoneawarness1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

//The repository is what allows functions to share data amongst themselves.
//This also allows the main UI to be aware of change
class HazardZoneRepository {
    private static MutableLiveData<HazardZoneData> data = new MutableLiveData<HazardZoneData>();

    public static LiveData<HazardZoneData> getHazardData(){
        return(data);
    }

    public static void postHazardData(HazardZoneData hazardData){
        data.postValue(hazardData);
    }
    public static void setHazardData(HazardZoneData hazardData){
        data.setValue(hazardData);
    }

    public static void setBeaconDistance(Double beaconDistance){
        HazardZoneData update = data.getValue();
        if (update != null) {
            update.setBeaconDistance(beaconDistance);
            data.setValue(update);
        }
    }

    public static void setBeaconBattery(long beaconBattery){
        HazardZoneData update = data.getValue();
        if (update != null) {
            update.setBeaconBattery(beaconBattery);
            data.setValue(update);
        }
    }
}

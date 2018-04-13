package com.example.a100541476.visimphelper;

public class Step {

    public String stepAction = "";
    public double stepLat;
    public double stepLon;

    public Step(String action, double lat, double lon){
        this.stepAction = action;
        this.stepLat = lat;
        this.stepLon = lon;
    }

    public String getStepAction(){return this.stepAction;}

    public double getStepLat() {return this.stepLat;}

    public double getStepLon() {return this.stepLon;}
}

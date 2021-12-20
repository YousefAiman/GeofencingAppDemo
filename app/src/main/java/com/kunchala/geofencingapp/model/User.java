package com.kunchala.geofencingapp.model;

import com.google.firebase.firestore.PropertyName;

public class User {

    @PropertyName("ID")
    private String ID;
    @PropertyName("username")
    private String username;
    @PropertyName("lat")
    private double lat;
    @PropertyName("lng")
    private double lng;
    @PropertyName("distanceAwayInMeters")
    private double distanceAwayInMeters;


    public User() {
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getDistanceAwayInMeters() {
        return distanceAwayInMeters;
    }


    public void setDistanceAwayInMeters(double distanceAwayInMeters) {
        this.distanceAwayInMeters = distanceAwayInMeters;
    }
}

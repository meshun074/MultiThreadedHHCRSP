package org.example.GA;

import org.example.Data.Caregiver;

import java.util.ArrayList;

public class Shift {
    private Caregiver caregiver;
    private ArrayList<String> route;
    private double currentTime;
    private double travelCost;
    private double tardiness;
    private double maxTardiness;
    private double load;

    public Shift(Caregiver caregiver, ArrayList<String> route, double currentTime) {
        this.caregiver = caregiver;
        this.route = route;
        this.currentTime = currentTime;
        travelCost = 0.0;
        load =0.0;
        tardiness =0.0;
        maxTardiness =tardiness;
    }

    public Caregiver getCaregiver() {
        return caregiver;
    }

    public void setCaregiver(Caregiver caregiver) {
        this.caregiver = caregiver;
    }

    public ArrayList<String> getRoute() {
        return route;
    }

    public void setRoute(ArrayList<String> route) {
        this.route = route;
    }
    public void updateRoute(String patient) {
        route.add(patient);
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(double currentTime) {
        this.currentTime = currentTime;
    }

    public double getLoad() {
        return load;
    }

    public void setLoad(double load) {
        this.load = load;
    }
    public void updateLoad(double load) {
        this.load += load;
    }

    public double getTardiness() {
        return tardiness;
    }

    public void setTardiness(double tardiness) {
        this.tardiness = tardiness;
    }
    public void updateTardiness(double tardiness) {
        this.tardiness += tardiness;
    }

    public double getMaxTardiness() {
        return maxTardiness;
    }

    public void setMaxTardiness(double maxTardiness) {
        this.maxTardiness = maxTardiness;
    }

    public double getTravelCost() {
        return travelCost;
    }

    public void setTravelCost(double travelCost) {
        this.travelCost = travelCost;
    }
    public void updateTravelCost(double travelCost) {
        this.travelCost += travelCost;
    }
}


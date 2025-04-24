package org.example.GA;

import java.util.ArrayList;

public class Process {
    private final ArrayList<String> route1;
    private final String patient;
    private final int insertPosition;
    private final int routeIndex;

    public Process(ArrayList<String> route1, String patient, int insertPosition, int routeIndex) {
        this.route1 = route1;
        this.patient = patient;
        this.insertPosition = insertPosition;
        this.routeIndex = routeIndex;
    }

    public ArrayList<String> getRoute() {
        return route1;
    }

    public String getPatient() {
        return patient;
    }

    public int getInsertPosition() {
        return insertPosition;
    }

    public int getRouteIndex() {
        return routeIndex;
    }
}

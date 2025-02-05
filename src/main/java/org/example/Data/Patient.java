package org.example.Data;

import java.util.ArrayList;

public class Patient {
    private String id;
    private double[] location;
    private double[] time_window;
    private Required_Caregiver[] required_caregivers;
    private Synchronization synchronization;

    public String getId() {
        return id;
    }

    public double[] getLocation() {
        return location;
    }

    public double[] getTime_window() {
        return time_window;
    }


    public Required_Caregiver[] getRequired_caregivers() {
        return required_caregivers;
    }

    public Synchronization getSynchronization() {
        return synchronization;
    }
}

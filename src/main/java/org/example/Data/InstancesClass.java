package org.example.Data;

import java.util.ArrayList;

public class InstancesClass {
    private Patient[] patients;
    private Service[] services;
    private Caregiver[] caregivers;
    private Offices[] central_offices;
    private double[][] distances;




    public double[][] getDistances() {
        return distances;
    }

    public Offices[] getCentral_offices() {
        return central_offices;
    }

    public Patient[] getPatients() {
        return patients;
    }

    public Caregiver[] getCaregivers() {
        return caregivers;
    }

    public Service[] getServices() {
        return services;
    }

}

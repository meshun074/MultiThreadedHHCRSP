package org.example.GA;

import java.util.ArrayList;

public class Move {
    private ArrayList<String> route1;
    private ArrayList<String> route2;
    private Chromosome chromosome;
    private String patient;
    private final int insertPosition1;
    private final int routeIndex1;
    private final int insertPosition2;
    private final int routeIndex2;
    private double fitness;

    public Move(ArrayList<String> route1, ArrayList<String> route2, String patient, int insertPosition1, int routeIndex1, int insertPosition2, int routeIndex2) {
        this.route1 = route1;
        this.route2 = route2;
        this.patient = patient;
        this.insertPosition1 = insertPosition1;
        this.insertPosition2 = insertPosition2;
        this.routeIndex1 = routeIndex1;
        this.routeIndex2 = routeIndex2;
        this.fitness = 0;
    }
    public Move(ArrayList<String> route1, String patient, int insertPosition1, int routeIndex1) {
        this.route1 = route1;
        this.patient = patient;
        this.insertPosition1 = insertPosition1;
        this.insertPosition2 = -1;
        this.routeIndex1 = routeIndex1;
        this.routeIndex2 = -1;
        this.fitness = 0;
    }

    public Chromosome getChromosome() {
        return chromosome;
    }

    public void setChromosome(Chromosome chromosome) {
        this.chromosome = chromosome;
    }

    public double getFitness() {
        return fitness;
    }


    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public ArrayList<String> getRoute1() {
        return route1;
    }
    public ArrayList<String> getRoute2() {
        return route2;
    }

    public void setRoute1(ArrayList<String> route1) {
        this.route1 = route1;
    }

    public String getPatient() {
        return patient;
    }

    public void setPatient(String patient) {
        this.patient = patient;
    }

    public int getInsertPosition1() {
        return insertPosition1;
    }

    public int getInsertPosition2() {
        return insertPosition2;
    }

    public int getRouteIndex1() {return routeIndex1;}
    public int getRouteIndex2() {return routeIndex2;}

}

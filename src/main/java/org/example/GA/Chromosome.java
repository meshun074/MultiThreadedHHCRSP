package org.example.GA;

import java.util.ArrayList;

public class Chromosome {
    private int rank;
    private int caregivers;
    private double fitness;
    private double totalTravelCost;
    private double totalTardiness;
    private double highestTardiness;
    private ArrayList[] genes;
    private Shift[] caregiversRoute;
    public Chromosome(int caregivers) {
        this.caregivers = caregivers;
        fitness = 0;
        genes = new ArrayList[caregivers];
        for (int i = 0; i < caregivers; i++) {
            genes[i] = new ArrayList();
        }
        caregiversRoute = new Shift[caregivers];
    }
    public Chromosome(ArrayList[] genes, double fitness, int rank) {
        this.rank = rank;
        this.genes = genes;
        this.caregivers = genes.length;
        this.fitness = fitness;
        caregiversRoute = new Shift[caregivers];
    }
    public Chromosome(ArrayList[] genes, double fitness) {
        this.genes = genes;
        this.caregivers = genes.length;
        this.fitness = fitness;
        caregiversRoute = new Shift[caregivers];
    }
    public Chromosome(ArrayList[] genes, double fitness, boolean newChromosome) {
        this.genes = new ArrayList[genes.length];
        for (int i = 0; i < genes.length; i++) {
            this.genes[i] = new ArrayList(genes[i]);
        }
        this.caregivers = genes.length;
        this.fitness = fitness;
        caregiversRoute = new Shift[caregivers];
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getCaregivers() {
        return caregivers;
    }

    public void setCaregivers(int caregivers) {
        this.caregivers = caregivers;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public ArrayList[] getGenes() {
        return genes;
    }

    public void setGenes(ArrayList[] genes) {
        this.genes = genes;
    }

    public Shift[] getCaregiversRoute() {
        return caregiversRoute;
    }

    public void setCaregiversRoute(Shift[] caregiversRoute) {
        this.caregiversRoute = caregiversRoute;
    }

    public double getTotalTravelCost() {
        return totalTravelCost;
    }

    public void setTotalTravelCost(double totalTravelCost) {
        this.totalTravelCost = totalTravelCost;
    }
    public void updateTotalTravelCost(double totalTravelCost) {
        this.totalTravelCost += totalTravelCost;
    }

    public double getTotalTardiness() {
        return totalTardiness;
    }

    public void setTotalTardiness(double totalTardiness) {
        this.totalTardiness = totalTardiness;
    }

    public double getHighestTardiness() {
        return highestTardiness;
    }

    public void setHighestTardiness(double highestTardiness) {
        this.highestTardiness = highestTardiness;
    }
    public void updateTotalTardiness(double totalTardiness) {
        this.totalTardiness += totalTardiness;
    }

    public void showSolution() {
        for (ArrayList route : genes) {
            System.out.print(route+" - ");
        }
        System.out.println("fitness: "+fitness);
    }
    public String toString() {
        StringBuilder genesStrings= new StringBuilder();
        for(ArrayList c :genes){
            genesStrings.append(c.toString());
        }
        return genesStrings.toString();
    }
}

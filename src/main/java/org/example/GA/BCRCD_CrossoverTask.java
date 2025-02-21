package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;

import static org.example.GA.EvaluationFunction.EvaluateFitness;
import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.GeneticAlgorithm.conflictCheck;

public class BCRCD_CrossoverTask implements Runnable {
    private final GeneticAlgorithm ga;
    private final int identity;
    private final float mutRate;
    private final int r1;
    private final int r2;
    private final Chromosome p1, p2;
    private final InstancesClass data;

    public BCRCD_CrossoverTask(GeneticAlgorithm ga, int identity, float mutRate, Chromosome p1, Chromosome p2, int r1, int r2, InstancesClass data) {
        this.ga = ga;
        this.identity = identity;
        this.mutRate = mutRate;
        this.r1 = r1;
        this.r2 = r2;
        this.p1 = p1;
        this.p2 = p2;
        this.data = data;
    }

    @Override
    public void run() {
        ga.getCrossoverChromosomes().add(Crossover());
    }

    private Chromosome Crossover() {
        Chromosome c1 = p2, c1Temp;
        Random rand = new Random(System.currentTimeMillis() + identity);
        ArrayList[] p1Routes, c1Routes;
        ArrayList<String> selectRoute, route, tempRoute1,
                tempRoute2, currentRoute1, currentRoute2, bestroute1, bestroute2;
        LinkedHashSet<String> route1;
        int bestRoute1Index = 0, bestRoute2Index = 0;
        String patient;
        Patient p;
        double bestCost;
        selectRoute = new ArrayList(p2.getGenes()[r1]);
        selectRoute.addAll(p1.getGenes()[r2]);
        p1Routes = p1.getGenes();
        c1Routes = new ArrayList[p1.getGenes().length];
        //removing patients of selected route from parent routes
        for (int i = 0; i < p1Routes.length; i++) {
            route = new ArrayList<>();
            for (int j = 0; j < p1Routes[i].size(); j++) {
                patient = (String) p1Routes[i].get(j);
                if (!selectRoute.contains(patient)) {
                    route.add(patient);
                }
            }
            c1Routes[i] = new ArrayList<>(route);
        }
        // inserting removed route.
        Collections.shuffle(selectRoute, rand);
        route1 = new LinkedHashSet<>(selectRoute);

        String service1, service2;
        ArrayList<Integer> caregivers1, caregivers2;

        for (String s : route1) {
            bestCost = Double.MAX_VALUE;
            bestroute1 = null;
            bestroute2 = null;
            p = data.getPatients()[getIdOfObject(s)];
            service1 = p.getRequired_caregivers()[0].getService();
            if (p.getRequired_caregivers().length > 1) {
                service2 = p.getRequired_caregivers()[1].getService();
                caregivers1 = getQualifiedCaregiver(service1);
                caregivers2 = getQualifiedCaregiver(service2);

//                for (int k = 0; k < c1Routes.length; k++) {
//                    if (caregivers1.contains(k)) {
//                        for (int l = 0; l < c1Routes.length; l++) {
//                            if (caregivers2.contains(l) && k != l) {
                for (int k : caregivers1) {
                    for (int l : caregivers2) {
                        if (k != l) {

                            for (int m = 0; m <= c1Routes[k].size(); m++) {
                                for (int n = 0; n <= c1Routes[l].size(); n++) {
                                    if (noEvaluationConflicts(c1Routes[k], c1Routes[l], m, n)) {
                                        tempRoute1 = new ArrayList<>(c1Routes[k]);
                                        tempRoute2 = new ArrayList<>(c1Routes[l]);
                                        tempRoute1.add(m, s);
                                        tempRoute2.add(n, s);
                                        currentRoute1 = c1Routes[k];
                                        currentRoute2 = c1Routes[l];
                                        c1Routes[k] = tempRoute1;
                                        c1Routes[l] = tempRoute2;
                                        c1Temp = new Chromosome(c1Routes, 0.0);
                                        EvaluateFitness(Collections.singletonList(c1Temp), data);
                                        if (c1Temp.getFitness() <= bestCost) {
                                            bestCost = c1Temp.getFitness();
                                            bestroute1 = tempRoute1;
                                            bestroute2 = tempRoute2;
                                            bestRoute1Index = k;
                                            bestRoute2Index = l;
                                            c1 = c1Temp;
                                        }
                                        c1Routes[k] = currentRoute1;
                                        c1Routes[l] = currentRoute2;
                                    }
                                }
                            }
                        }
                    }
                }
                try {
                    assert bestroute1 != null;
                    c1Routes[bestRoute1Index] = new ArrayList<>(bestroute1);
                    assert bestroute2 != null;
                    c1Routes[bestRoute2Index] = new ArrayList<>(bestroute2);
                } catch (Exception e) {
                    System.out.println("Exception \n" + s);
                    for (ArrayList m : c1Routes) {
                        System.out.println(m);
                    }
                    System.out.println("Caregivers1" + caregivers1);
                    System.out.println("Caregivers2" + caregivers2);
                    throw new RuntimeException(e);
                }

            } else {
                caregivers1 = getQualifiedCaregiver(service1);
                for (int j = 0; j < c1Routes.length; j++) {
                    if (caregivers1.contains(j)) {
                        for (int k = 0; k <= c1Routes[j].size(); k++) {
                            tempRoute1 = new ArrayList(c1Routes[j]);
                            tempRoute1.add(k, s);
                            currentRoute1 = c1Routes[j];
                            c1Routes[j] = tempRoute1;
                            c1Temp = new Chromosome(c1Routes, 0.0);
                            EvaluateFitness(Collections.singletonList(c1Temp), data);
                            if (c1Temp.getFitness() <= bestCost) {
                                bestCost = c1Temp.getFitness();
                                bestroute1 = tempRoute1;
                                bestRoute1Index = j;
                                c1 = c1Temp;
                            }
                            c1Routes[j] = currentRoute1;
                        }
                    }
                }
                assert bestroute1 != null;
                c1Routes[bestRoute1Index] = new ArrayList<>(bestroute1);
            }
        }
        if (mutRate > 0) {
            if (Math.random() < mutRate) {
                c1 = ga.mutationSelection(c1);
            }
        }
        return c1;
    }

    private ArrayList<Integer> getQualifiedCaregiver(String service) {
        ArrayList<Integer> caregivers = new ArrayList<>();
        for (Caregiver c : data.getCaregivers()) {
            if (c.getAbilities().contains(service)) {
                caregivers.add(getIdOfObject(c.getId()));
            }
        }
        return caregivers;
    }

    private boolean noEvaluationConflicts(ArrayList<String> c1Route, ArrayList<String> c2Route, int m, int n) {
        return conflictCheck(c1Route, c2Route, m, n);
    }

}

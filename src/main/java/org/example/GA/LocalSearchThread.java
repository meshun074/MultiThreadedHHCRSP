package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static org.example.GA.EvaluationFunction.EvaluateFitness;
import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.GeneticAlgorithm.conflictCheck;
import static org.example.GA.GeneticAlgorithm.swapPatients;


public class LocalSearchThread implements Runnable{
    private GeneticAlgorithm ga;
    private Chromosome ch;
    private final Random rand;
    private final int r;
    private final ArrayList<Integer> searchedPatients;
    private final InstancesClass data;

    public LocalSearchThread(GeneticAlgorithm ga, Chromosome ch, Random rand, int r, InstancesClass data) {
        this.ga = ga;
        this.ch = ch;
        this.rand = rand;
        this.r = r;
        this.data = data;
        searchedPatients = new ArrayList<>();
    }

    private Chromosome localSearch(){
        Chromosome newCh;
        int sp;
        do {
            newCh = ch;
            do {
                sp = rand.nextInt(data.getPatients().length);
            } while (searchedPatients.contains(sp));
            ch = search(ch, sp);
            searchedPatients.add(sp);
        } while (ch.getFitness() < newCh.getFitness() || searchedPatients.size() < Math.ceilDiv(data.getPatients().length, 4));
//        newPopulation.set(r, newCh);
        return newCh;
    }
    @Override
    public void run() {
        ga.getLSChromosomes().put(r,localSearch());
    }
    public Chromosome search(Chromosome ch, int sp) {
        Chromosome tempCh;
        Chromosome bestCh = ch;
        Patient patient = data.getPatients()[sp];
        ArrayList<String> route;
        String p;
        double bestCost;
        String service1, service2;
        ArrayList<String> tempRoute1, tempRoute2, currentRoute1, currentRoute2, currentRoute3, currentRoute4;
        ArrayList<Integer> caregivers1, caregivers2;
        ArrayList[] routes = new ArrayList[ch.getGenes().length];
        //Removing selected patient
        for (int i = 0; i < ch.getGenes().length; i++) {
            route = new ArrayList<>();
            for (int j = 0; j < ch.getGenes()[i].size(); j++) {
                p = (String) ch.getGenes()[i].get(j);
                if (!patient.getId().equals(p)) {
                    route.add(p);
                }
            }
            routes[i] = new ArrayList<>(route);
        }
        bestCost = Double.MAX_VALUE;
        service1 = patient.getRequired_caregivers()[0].getService();
        // double patient local search
        if (patient.getRequired_caregivers().length > 1) {
            //Relocate
            service2 = patient.getRequired_caregivers()[1].getService();
            caregivers1 = getQualifiedCaregiver(service1);
            caregivers2 = getQualifiedCaregiver(service2);

            for (int k = 0; k < routes.length; k++) {
                if (caregivers1.contains(k)) {
                    for (int l = 0; l < routes.length; l++) {
                        if (caregivers2.contains(l) && k != l) {

                            for (int m = 0; m <= routes[k].size(); m++) {
                                for (int n = 0; n <= routes[l].size(); n++) {
                                    if (noEvaluationConflicts(routes[k], routes[l], m, n)) {
                                        tempRoute1 = new ArrayList<>(routes[k]);
                                        tempRoute2 = new ArrayList<>(routes[l]);
                                        tempRoute1.add(m, patient.getId());
                                        tempRoute2.add(n, patient.getId());
                                        currentRoute1 = routes[k];
                                        currentRoute2 = routes[l];
                                        routes[k] = tempRoute1;
                                        routes[l] = tempRoute2;
                                        tempCh = new Chromosome(routes, 0.0, true);
                                        EvaluateFitness(Collections.singletonList(tempCh), data);
                                        if (tempCh.getFitness() < bestCost) {
                                            bestCost = tempCh.getFitness();
                                            bestCh = tempCh;
                                        }
                                        routes[k] = currentRoute1;
                                        routes[l] = currentRoute2;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //Swap
            ArrayList<Integer> b = new ArrayList<>();
            ArrayList<Integer> r = new ArrayList<>();
            int b1, b2, r1, r2;
            String s;
            Patient p2;
            routes = new ArrayList[ch.getGenes().length];
            // getting initial routes for swap operation
            for (int i = 0; i < ch.getGenes().length; i++) {
                route = new ArrayList<>();
                if (ch.getGenes()[i].contains(patient.getId())) {
                    r.add(i);
                    b.add(ch.getGenes()[i].indexOf(patient.getId()));
                }
                for (int j = 0; j < ch.getGenes()[i].size(); j++) {
                    p = (String) ch.getGenes()[i].get(j);
                    route.add(p);
                }
                routes[i] = new ArrayList<>(route);
            }
            //find position of service 1 and 2
            if (caregivers1.contains(r.getLast()) && !caregivers2.contains(r.getLast())
                    || caregivers2.contains(r.getFirst()) && !caregivers1.contains(r.getFirst())
                    || caregivers1.contains(r.getLast()) && !caregivers1.contains(r.getFirst())) {
                r1 = r.getLast();
                r2 = r.getFirst();
                b1 = b.getLast();
                b2 = b.getFirst();
            } else {
                r1 = r.getFirst();
                r2 = r.getLast();
                b1 = b.getFirst();
                b2 = b.getLast();
            }

            for (int j : caregivers1) {
                // swapping first service in route and second service in route or outside it current route
                if (routes[j].contains(patient.getId()) && j == r1) {
                    for (int w = 0; w < routes[j].size(); w++) {
                        if (b1 != w) {
                            tempRoute1 = new ArrayList(routes[j]);
                            swapPatients(tempRoute1, b1, w);
                            currentRoute1 = routes[j];
                            routes[j] = tempRoute1;
                            if (noEvaluationConflicts(tempRoute1, routes[r2], w, b2)) {
                                tempCh = new Chromosome(routes, 0.0, true);
                                EvaluateFitness(Collections.singletonList(tempCh), data);
                                if (tempCh.getFitness() < bestCost) {
                                    bestCost = tempCh.getFitness();
                                    bestCh = tempCh;
                                }

                                //second service swap
                                for (int k : caregivers2) {
                                    if (routes[k].contains(patient.getId()) && k == r2) {
                                        for (int l = 0; l < routes[k].size(); l++) {
                                            if (b2 != l) {
                                                tempRoute1 = new ArrayList(routes[k]);
                                                swapPatients(tempRoute1, b2, l);
                                                currentRoute3 = routes[k];
                                                routes[k] = tempRoute1;
                                                if (noEvaluationConflicts(tempRoute1, routes[r1], l, b1)) {
                                                    tempCh = new Chromosome(routes, 0.0, true);
                                                    EvaluateFitness(Collections.singletonList(tempCh), data);
                                                    if (tempCh.getFitness() < bestCost) {
                                                        bestCost = tempCh.getFitness();
                                                        bestCh = tempCh;
                                                    }
                                                }
                                                routes[k] = currentRoute3;
                                            }
                                        }

                                    }
                                    else {
                                        if(!routes[k].contains(patient.getId())) {
                                            for (int l = 0; l < routes[k].size(); l++) {
                                                p2 = data.getPatients()[getIdOfObject((String) routes[k].get(l))];
                                                if (!routes[r2].contains(p2.getId())) {
                                                    s = p2.getRequired_caregivers()[0].getService();
                                                    if (!getQualifiedCaregiver(s).contains(k) && p2.getRequired_caregivers().length > 1)
                                                        s = p2.getRequired_caregivers()[1].getService();
                                                    if (getQualifiedCaregiver(s).contains(r2)) {
                                                        tempRoute1 = new ArrayList(routes[k]);
                                                        tempRoute2 = new ArrayList(routes[r2]);
                                                        tempRoute1.set(l, patient.getId());
                                                        tempRoute2.set(b2, p2.getId());
                                                        currentRoute3 = routes[k];
                                                        currentRoute4 = routes[r2];
                                                        routes[k] = tempRoute1;
                                                        routes[r2] = tempRoute2;
                                                        if (noEvaluationConflicts(tempRoute1, routes[r1], l, b1)){
                                                            tempCh = new Chromosome(routes, 0.0, true);
                                                            EvaluateFitness(Collections.singletonList(tempCh), data);
                                                            if (tempCh.getFitness() < bestCost) {
                                                                bestCost = tempCh.getFitness();
                                                                bestCh = tempCh;
                                                            }
                                                        }
                                                        routes[k] = currentRoute3;
                                                        routes[r2] = currentRoute4;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                }

                            }
                            routes[j] = currentRoute1;
                        }
                    }
                }
                // swapping first service in route/ outside it current route and second service in route or outside it current route
                else {
                    if (!routes[j].contains(patient.getId())){
                        for ( int w = 0; w < routes[j].size(); w++) {
                            p2 = data.getPatients()[getIdOfObject((String) routes[j].get(w))];
                            if (!routes[r1].contains(p2.getId())) {
                                s = p2.getRequired_caregivers()[0].getService();
                                if (!getQualifiedCaregiver(s).contains(j) && p2.getRequired_caregivers().length > 1)
                                    s = p2.getRequired_caregivers()[1].getService();
                                if (getQualifiedCaregiver(s).contains(r1)) {
                                    tempRoute1 = new ArrayList(routes[j]);
                                    tempRoute2 = new ArrayList(routes[r1]);
                                    tempRoute1.set(w, patient.getId());
                                    tempRoute2.set(b1, p2.getId());
                                    currentRoute1 = routes[j];
                                    currentRoute2 = routes[r1];
                                    routes[j] = tempRoute1;
                                    routes[r1] = tempRoute2;
                                    if (noEvaluationConflicts(tempRoute1, routes[r2], w, b2)){
                                        tempCh = new Chromosome(routes, 0.0, true);
                                        EvaluateFitness(Collections.singletonList(tempCh), data);
                                        if (tempCh.getFitness() < bestCost) {
                                            bestCost = tempCh.getFitness();
                                            bestCh = tempCh;
                                        }

                                        //second service swap
                                        for (int k : caregivers2) {
                                            if (routes[k].contains(patient.getId()) && k == r2) {
                                                for (int l = 0; l < routes[k].size(); l++) {
                                                    if (b2 != l) {
                                                        tempRoute1 = new ArrayList(routes[k]);
                                                        swapPatients(tempRoute1, b2, l);
                                                        currentRoute3 = routes[k];
                                                        routes[k] = tempRoute1;
                                                        if (noEvaluationConflicts(tempRoute1, routes[r1], l, b1)) {
                                                            tempCh = new Chromosome(routes, 0.0, true);
                                                            EvaluateFitness(Collections.singletonList(tempCh), data);
                                                            if (tempCh.getFitness() < bestCost) {
                                                                bestCost = tempCh.getFitness();
                                                                bestCh = tempCh;
                                                            }
                                                        }
                                                        routes[k] = currentRoute3;
                                                    }
                                                }

                                            }
                                            else {
                                                if(!routes[k].contains(patient.getId())) {
                                                    for (int l = 0; l < routes[k].size(); l++) {
                                                        p2 = data.getPatients()[getIdOfObject((String) routes[k].get(l))];
                                                        if (!routes[r2].contains(p2.getId())) {
                                                            s = p2.getRequired_caregivers()[0].getService();
                                                            if (!getQualifiedCaregiver(s).contains(k) && p2.getRequired_caregivers().length > 1)
                                                                s = p2.getRequired_caregivers()[1].getService();
                                                            if (getQualifiedCaregiver(s).contains(r2)) {
                                                                tempRoute1 = new ArrayList(routes[k]);
                                                                tempRoute2 = new ArrayList(routes[r2]);
                                                                tempRoute1.set(l, patient.getId());
                                                                tempRoute2.set(b2, p2.getId());
                                                                currentRoute3 = routes[k];
                                                                currentRoute4 = routes[r2];
                                                                routes[k] = tempRoute1;
                                                                routes[r2] = tempRoute2;
                                                                if (noEvaluationConflicts(tempRoute1, routes[r1], l, b1)){
                                                                    tempCh = new Chromosome(routes, 0.0, true);
                                                                    EvaluateFitness(Collections.singletonList(tempCh), data);
                                                                    if (tempCh.getFitness() < bestCost) {
                                                                        bestCost = tempCh.getFitness();
                                                                        bestCh = tempCh;
                                                                    }
                                                                }
                                                                routes[k] = currentRoute3;
                                                                routes[r2] = currentRoute4;
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                        }

                                    }
                                    routes[j] = currentRoute1;
                                    routes[r1] = currentRoute2;
                                }
                            }
                        }
                    }
                }
            }

        }
        //Single service patient local search
        else {
            caregivers1 = getQualifiedCaregiver(service1);
            //relocate
            for (int j = 0; j < routes.length; j++) {
                if (caregivers1.contains(j)) {
                    for (int k = 0; k <= routes[j].size(); k++) {
                        tempRoute1 = new ArrayList(routes[j]);
                        tempRoute1.add(k, patient.getId());
                        currentRoute1 = routes[j];
                        routes[j] = tempRoute1;
                        tempCh = new Chromosome(routes, 0.0, true);
                        EvaluateFitness(Collections.singletonList(tempCh), data);
                        if (tempCh.getFitness() < bestCost) {
                            bestCost = tempCh.getFitness();
                            bestCh = tempCh;
                        }
                        routes[j] = currentRoute1;
                    }
                }
            }
            //Swap
            int b = 0, r = 0;
            String s;
            Patient p2;
            routes = new ArrayList[ch.getGenes().length];
            // getting initial routes for swap operation
            for (int i = 0; i < ch.getGenes().length; i++) {
                route = new ArrayList<>();
                if (ch.getGenes()[i].contains(patient.getId())) {
                    r = i;
                    b = ch.getGenes()[i].indexOf(patient.getId());
                }
                for (int j = 0; j < ch.getGenes()[i].size(); j++) {
                    p = (String) ch.getGenes()[i].get(j);
                    route.add(p);
                }
                routes[i] = new ArrayList<>(route);
            }
//
            for (int k : caregivers1) {
                if (routes[k].contains(patient.getId())) {
                    for (int l = 0; l < routes[k].size(); l++) {
                        if (b != l) {
                            tempRoute1 = new ArrayList(routes[k]);
                            swapPatients(tempRoute1, b, l);
                            currentRoute1 = routes[k];
                            routes[k] = tempRoute1;
                            tempCh = new Chromosome(routes, 0.0, true);
                            EvaluateFitness(Collections.singletonList(tempCh), data);
                            if (tempCh.getFitness() < bestCost) {
                                bestCost = tempCh.getFitness();
                                bestCh = tempCh;
                            }
                            routes[k] = currentRoute1;
                        }
                    }

                } else {
                    for (int l = 0; l < routes[k].size(); l++) {
                        p2 = data.getPatients()[getIdOfObject((String) routes[k].get(l))];
                        if (!routes[r].contains(p2.getId())) {
                            s = p2.getRequired_caregivers()[0].getService();
                            if (!getQualifiedCaregiver(s).contains(k) && p2.getRequired_caregivers().length > 1)
                                s = p2.getRequired_caregivers()[1].getService();
                            if (getQualifiedCaregiver(s).contains(r)) {
                                tempRoute1 = new ArrayList(routes[k]);
                                tempRoute2 = new ArrayList(routes[r]);
                                tempRoute1.set(l, patient.getId());
                                tempRoute2.set(b, p2.getId());
                                currentRoute1 = routes[k];
                                currentRoute2 = routes[r];
                                routes[k] = tempRoute1;
                                routes[r] = tempRoute2;
                                tempCh = new Chromosome(routes, 0.0, true);
                                EvaluateFitness(Collections.singletonList(tempCh), data);
                                if (tempCh.getFitness() < bestCost) {
                                    bestCost = tempCh.getFitness();
                                    bestCh = tempCh;
                                }
                                routes[k] = currentRoute1;
                                routes[r] = currentRoute2;
                            }
                        }
                    }
                }
            }
        }
        return bestCh;
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

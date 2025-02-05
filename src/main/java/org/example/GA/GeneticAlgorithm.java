package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;

import static org.example.GA.EvaluationFunction.EvaluateFitness;
import static org.example.GA.EvaluationFunction.getIdOfObject;

public class GeneticAlgorithm implements Runnable {
    private final int popSize;
    private final int gen;
    private final int identity;
    private final int localSearchRate;
    private final int numOfEliteSearch;
    private final float elitismRate;
    private final InstancesClass data;
    private Chromosome bestChromosome;
    private List<Chromosome> nextPopulation;
    private List<Chromosome> newPopulation;
    public static List<Chromosome> bestChromosomes;

    public GeneticAlgorithm(int identity, int numOfEliteSearch, int localSearchRate, int popSize, int gen, float elitismRate, InstancesClass data) {
        this.identity = identity;
        this.numOfEliteSearch = numOfEliteSearch;
        this.localSearchRate = localSearchRate;
        this.popSize = popSize;
        this.gen = gen;
        this.elitismRate = elitismRate;
        this.data = data;
    }

    public Chromosome start() {
        bestChromosome = null;
        //initialize and evaluate fitness of chromosome
        newPopulation = Population.initialize(popSize, data.getPatients().length);
        //Sort population
        sortPopulation(newPopulation);
        //Localsearch();
        performanceUpdate(newPopulation, 0);
        for (int i = 1; i <= gen; i++) {
            maintainElitism();
            //System.out.println(i);
            bestCostRouteCrossover1();
            //System.out.println(i);
//            if (i % localSearchRate == 0)
//                Localsearch();
            updatePopulation1();
            //System.out.println("Size: " + newPopulation.size());
            performanceUpdate(newPopulation, i);
        }

        return bestChromosome;
    }

    private void updatePopulation1() {
        newPopulation.clear();
        newPopulation.addAll(nextPopulation);
    }

    private void updatePopulation() {
        int index = 0;
        sortPopulation(newPopulation);
        while (nextPopulation.size() < popSize) {
            if (!nextPopulation.contains(newPopulation.get(index))) {
                nextPopulation.add(newPopulation.get(index));
            }
            index++;
            //System.out.println("ths");
        }
        newPopulation.clear();
        newPopulation.addAll(nextPopulation);
    }


    private void bestCostRouteCrossover() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome p1, p2, c1, c2;
        int c = 0, r1, r2;
        int count;
        while (c < popSize) {
            //p1 = newPopulation.get(rand.nextInt((int) (popSize * elitismRate)));
            p1 = newPopulation.get(rand.nextInt(popSize));
//            p2 = newPopulation.get(tournamentSelection(3));
            p2 = newPopulation.get(rand.nextInt(popSize));
            count = 0;
            while (count < 10 && p1.getFitness() == p2.getFitness() && p1.getTotalTravelCost() == p2.getTotalTravelCost() && p1.getHighestTardiness() == p2.getHighestTardiness() && p1.getTotalTardiness() == p2.getTotalTardiness()) {
                p2 = newPopulation.get(rand.nextInt(popSize));
                //System.out.println(c);
                count++;
            }
            r1 = rand.nextInt(p2.getGenes().length);
            r2 = rand.nextInt(p1.getGenes().length);
            //c1 = Crossover(p1, p2);
            c1 = CrossoverD(p1, p2, r1, r2);
            //System.out.println("yiees");
            newPopulation.add(c1);
            //c2 = Crossover(p2, p1);
            c2 = CrossoverD(p2, p1, r2, r1);
            newPopulation.add(c2);
            c += 2;

        }
    }
    private void bestCostRouteCrossover1() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome p1, p2, c1, c2;
        int r1, r2;
        int count;
        while (nextPopulation.size() < popSize) {
            //p1 = newPopulation.get(rand.nextInt((int) (popSize * elitismRate)));
            p1 = newPopulation.get(rand.nextInt(popSize));
//            p2 = newPopulation.get(tournamentSelection(3));
            p2 = newPopulation.get(rand.nextInt(popSize));
            count = 0;
            while (count < 10 && p1.getFitness() == p2.getFitness() && p1.getTotalTravelCost() == p2.getTotalTravelCost() && p1.getHighestTardiness() == p2.getHighestTardiness() && p1.getTotalTardiness() == p2.getTotalTardiness()) {
                p2 = newPopulation.get(rand.nextInt(popSize));
                //System.out.println(c);
                count++;
            }
            do{
                r1 = rand.nextInt(p2.getGenes().length);
                r2 = rand.nextInt(p1.getGenes().length);
            }while(p1.getGenes()[r2].isEmpty()&&p2.getGenes()[r1].isEmpty());
            //c1 = Crossover(p1, p2);
            c1 = CrossoverD(p1, p2, r1, r2);
            //System.out.println("yiees");
            nextPopulation.add(c1);
            //c2 = Crossover(p2, p1);
            if (nextPopulation.size() < popSize) {
                c2 = CrossoverD(p2, p1, r2, r1);
                nextPopulation.add(c2);
            }
        }
    }
    private void bestCostRouteCrossover2() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome p1, p2, p3, c1, c2, c3;
        int r1, r2, r3;
        int count;
        List<String> uniqueParents = new ArrayList<>();
        while (nextPopulation.size() < popSize) {
            //p1 = newPopulation.get(rand.nextInt((int) (popSize * elitismRate)));
            p1 = newPopulation.get(rand.nextInt(popSize));
//            p2 = newPopulation.get(tournamentSelection(3));
            uniqueParents.add(p1.toString());
            p2 = newPopulation.get(rand.nextInt(popSize));
            count = 0;
            while (count < 10 && uniqueParents.contains(p2.toString())) {
                p2 = newPopulation.get(rand.nextInt(popSize));
                count++;
            }
            p3 = newPopulation.get(rand.nextInt(popSize));
            count = 0;
            while (count < 10 && uniqueParents.contains(p3.toString())) {
                p3 = newPopulation.get(rand.nextInt(popSize));
                count++;
            }
            //c1 = Crossover(p1, p2);
            do {
                r1 = rand.nextInt(p2.getGenes().length);
                r2 = rand.nextInt(p3.getGenes().length);
                r3 = rand.nextInt(p1.getGenes().length);
            } while (p1.getGenes()[r3].isEmpty() && p2.getGenes()[r1].isEmpty() && p3.getGenes()[r2].isEmpty());

            c1 = CrossoverMD(p1, p2, p3, r1, r2,r3);
            //System.out.println("yiees");
            nextPopulation.add(c1);
            //c2 = Crossover(p2, p1);
            if (nextPopulation.size() < popSize) {
                c2 = CrossoverMD(p2, p3, p1, r2,r3, r1);
                nextPopulation.add(c2);
            }
            if (nextPopulation.size() < popSize) {
                c3 = CrossoverMD(p3, p1, p2, r3,r1, r2);
                nextPopulation.add(c3);
            }
        }
    }

    private int tournamentSelection(int k) {
        ArrayList<Integer> list = new ArrayList<>();
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < k; i++) {
            list.add(rand.nextInt(popSize));
        }
        if (Math.random() < 0.8) {
            list.sort(Comparator.comparingInt(Integer::intValue));
            return list.getFirst();
        }
        return list.get(rand.nextInt(list.size()));
    }
    private void Localsearch() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome ch;
        Chromosome newCh;
        ArrayList<Integer> searchedPatients;
        int r, sp;
        ArrayList<Integer> keys = new ArrayList<>();
        for (int i = 1; i <= numOfEliteSearch; i++) {
            r = rand.nextInt((int) (popSize * elitismRate));
            while (keys.contains(r)) {
                r = rand.nextInt(popSize);
            }
            keys.add(r);
            ch = newPopulation.get(r);
            //System.out.println("Chromosome before: " + ch.getFitness());
            searchedPatients = new ArrayList<>();
            do {
                newCh = ch;
                do {
                    sp = rand.nextInt(data.getPatients().length);
                } while (searchedPatients.contains(sp));
                ch = search(ch, sp);
                searchedPatients.add(sp);
            } while (ch.getFitness() < newCh.getFitness() || searchedPatients.size() < Math.ceilDiv(data.getPatients().length, 4));
            //System.out.println("Chromosome after: " + newCh.getFitness());
            newPopulation.set(r, newCh);
        }
    }

    private Chromosome search(Chromosome ch, int sp) {
        Random rand = new Random(System.currentTimeMillis());
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

    public static void swapPatients(ArrayList<String> base, int b, int g) {
        String keyB = base.get(b);
        base.set(b, base.get(g));
        base.set(g, keyB);
    }

    private Chromosome CrossoverMD(Chromosome p1, Chromosome p2, Chromosome p3, int r1, int r2, int r3) {
        Chromosome c1 = p2, c1Temp;
        Random rand = new Random(System.currentTimeMillis());
        ArrayList[] p1Routes, c1Routes;
        ArrayList<String> selectRoute, route, tempRoute1,
                tempRoute2, currentRoute1, currentRoute2, bestroute1, bestroute2;
        LinkedHashSet<String> route1;
        int bestRoute1Index = 0, bestRoute2Index = 0;
        String patient;
        Patient p;
        double bestCost;
        selectRoute = new ArrayList(p1.getGenes()[r3]);
        selectRoute.addAll(p2.getGenes()[r1]);
        selectRoute.addAll(p3.getGenes()[r2]);
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
        //Testing
//        for(ArrayList m: p1.getGenes()) {
//            System.out.println(m);
//        }
//        System.out.println("----- i "+ route1);
//        for(ArrayList m: c1Routes) {
//            System.out.println(m);
//        }
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

                for (int k = 0; k < c1Routes.length; k++) {
                    if (caregivers1.contains(k)) {
                        for (int l = 0; l < c1Routes.length; l++) {
                            if (caregivers2.contains(l) && k != l) {

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
        return c1;
    }
    private Chromosome CrossoverD(Chromosome p1, Chromosome p2, int r1, int r2) {
        Chromosome c1 = p2, c1Temp;
        Random rand = new Random(System.currentTimeMillis());
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
        //Testing
//        for(ArrayList m: p1.getGenes()) {
//            System.out.println(m);
//        }
//        System.out.println("----- i "+ route1);
//        for(ArrayList m: c1Routes) {
//            System.out.println(m);
//        }
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

                for (int k = 0; k < c1Routes.length; k++) {
                    if (caregivers1.contains(k)) {
                        for (int l = 0; l < c1Routes.length; l++) {
                            if (caregivers2.contains(l) && k != l) {

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
//            System.out.println("----- "+s);
//            for(ArrayList m: c1Routes) {
//                System.out.println(m);
//            }
        }
        return c1;
    }

    private Chromosome Crossover(Chromosome p1, Chromosome p2) {
        Chromosome c1 = p2, c1Temp;
        Random rand = new Random(System.currentTimeMillis());
        ArrayList[] p1Routes, c1Routes;
        ArrayList<String> selectRoute, route, route1, tempRoute1,
                tempRoute2, currentRoute1, currentRoute2, bestroute1, bestroute2;
        int r, bestRoute1Index = 0, bestRoute2Index = 0;
        String patient;
        Patient p;
        double bestCost;
        r = rand.nextInt(p2.getGenes().length);
        selectRoute = new ArrayList(p2.getGenes()[r]);
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
        route1 = new ArrayList<>(selectRoute);
        Collections.shuffle(route1);
        String service1, service2;
        ArrayList<Integer> caregivers1, caregivers2;
        //Testing
//        for(ArrayList m: p1.getGenes()) {
//            System.out.println(m);
//        }
//        System.out.println("----- i "+ route1);
//        for(ArrayList m: c1Routes) {
//            System.out.println(m);
//        }
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

                for (int k = 0; k < c1Routes.length; k++) {
                    if (caregivers1.contains(k)) {
                        for (int l = 0; l < c1Routes.length; l++) {
                            if (caregivers2.contains(l) && k != l) {

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
                                            if (c1Temp.getFitness() < bestCost) {
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
                            if (c1Temp.getFitness() < bestCost) {
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
//            System.out.println("----- "+s);
//            for(ArrayList m: c1Routes) {
//                System.out.println(m);
//            }
        }
        return c1;
    }

    private boolean noEvaluationConflicts(ArrayList<String> c1Route, ArrayList<String> c2Route, int m, int n) {
        int index1;
        int index2;
        for (int i = 0; i < c1Route.size(); i++) {
            if (c2Route.contains(c1Route.get(i))) {
                index1 = c1Route.indexOf(c1Route.get(i));
                index2 = c2Route.indexOf(c1Route.get(i));
                if (m <= index1 && n > index2 || m > index1 && n <= index2) {
                    return false;
                }
            }
        }
        return true;
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

    private void maintainElitism() {
        nextPopulation = new ArrayList<>();
        sortPopulation(newPopulation);
        for (int i = 0; i < popSize * elitismRate; i++) {
            nextPopulation.add(newPopulation.get(i));
        }
    }

    private void sortPopulation(List<Chromosome> population) {
        population.sort(Comparator.comparingDouble(Chromosome::getFitness));
    }

    private void performanceUpdate(List<Chromosome> population, int iterations) {
        sortPopulation(population);
        if (iterations == gen) {
//            double averageFitness = population.stream().mapToDouble(Chromosome::getFitness).sum();
//            System.out.print("Index " + identity + " Average fitness: " + averageFitness + " ** ");
           // population.getFirst().showSolution();
            bestChromosome = population.getFirst();
            System.out.println(" Iteration " + iterations + " Fitness: " + bestChromosome.getFitness() + " Total Distance: " + bestChromosome.getTotalTravelCost() + " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness());
        }
        System.out.println("Index " + identity +" Iteration " +iterations + " Best fitness: " + population.getFirst().getFitness());
    }

    @Override
    public void run() {
        bestChromosomes.add(start());
    }
}

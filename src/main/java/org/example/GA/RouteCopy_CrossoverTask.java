package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;

import static org.example.GA.EvaluationFunction.EvaluateFitness;
import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.GeneticAlgorithm.conflictCheck;

public class RouteCopy_CrossoverTask implements Runnable {
    private final GeneticAlgorithm ga;
    private final long identity;
    private final float mutRate;
    private final ArrayList<Double> r;
    private final Chromosome p1, p2;
    private final InstancesClass data;

    public RouteCopy_CrossoverTask(GeneticAlgorithm ga, long identity, float mutRate, Chromosome p1, Chromosome p2, ArrayList<Double> r, InstancesClass data) {
        this.ga = ga;
        this.identity = identity;
        this.mutRate = mutRate;
        this.p1 = p1;
        this.p2 = p2;
        this.r = r;
        this.data = data;
    }

    private Chromosome Crossover() {
        Chromosome c1 = p2, c1Temp;
        Random rand = new Random(System.currentTimeMillis());
        ArrayList[] p1Routes, c1Routes, p2Routes;
        ArrayList<String> selectRoute, route, route1, tempRoute1,
                tempRoute2, currentRoute1, currentRoute2, bestroute1, bestroute2;
        int bestRoute1Index = 0, bestRoute2Index = 0;
        String patient;
        Patient p;
        double bestCost;
        int[] patients = new int[data.getPatients().length];
        selectRoute = new ArrayList();
        p1Routes = p1.getGenes();
        p2Routes = p2.getGenes();
        c1Routes = new ArrayList[p1.getGenes().length];
        for (int i = 0; i < p2Routes.length; i++) {
            if (r.get(i) < 0.5) {
                for (int j = 0; j < p2Routes[i].size(); j++) {
                    patient = (String) p2Routes[i].get(j);
                    patients[getIdOfObject(patient)]++;
                }
//                System.out.print(i+" - "+p2Routes[i]);
            }
        }
//        for(int i : patients) {
//            System.out.print(i+"_");
//        }
//        System.out.println();

        //swapping patients of selected route from parent routes
        for (int i = 0; i < p1Routes.length; i++) {
            route = new ArrayList<>();
            if (r.get(i) < 0.5) {
                for (int j = 0; j < p2Routes[i].size(); j++) {
                    patient = (String) p2Routes[i].get(j);
                    route.add(patient);
                }
            }
            c1Routes[i] = new ArrayList<>(route);
        }
//        for(ArrayList c: c1Routes){
//            System.out.println(c);
//        }

        for (int i = 0; i < p1Routes.length; i++) {
            route = new ArrayList();
            if (r.get(i) > 0.5) {
//                System.out.println(i+" -p1- "+p1Routes[i]);
                for (int j = 0; j < p1Routes[i].size(); j++) {
                    patient = (String) p1Routes[i].get(j);
                    p = data.getPatients()[getIdOfObject(patient)];
                    if (p.getRequired_caregivers().length > 1) {
                        if (patients[getIdOfObject(patient)] == 0) {
                            if (validEntry(p, r, c1Routes, p1Routes)) {
                                route.add(patient);
                                patients[getIdOfObject(patient)]++;
                            }
                        } else if (patients[getIdOfObject(patient)] == 1) {
                            if (getOtherValidCaregiver(p, i, c1Routes, p1Routes)) {
                                route.add(patient);
                                patients[getIdOfObject(patient)]++;
                            }
                        }
                    } else {
                        if (patients[getIdOfObject(patient)] == 0) {
                            route.add(patient);
                            patients[getIdOfObject(patient)]++;
                        }
                    }
                }
                c1Routes[i] = new ArrayList<>(route);
            }
        }

//        for(ArrayList c: c1Routes){
//            System.out.println(c);
//        }
//
//        for(int i : patients) {
//            System.out.print(i+"_");
//        }
//        System.out.println();

        //Removing double patient a single insertion
        for (int i = 0; i < patients.length; i++) {
            if (patients[i] == 1) {
                p = data.getPatients()[i];
                //System.out.println(p.getId());
                if (p.getRequired_caregivers().length > 1) {
                    for (ArrayList c1Route : c1Routes) {
                        if (c1Route.contains(p.getId())) {
//                            System.out.println("Patient " + p.getId());
//                            System.out.println("Before" + c1Route);
                            c1Route.remove(p.getId());
                            patients[i] = 0;
//                            System.out.println("after" + c1Route);
                            break;
                        }
                    }
                }
            }
        }

//        System.out.println("After removal of single caregiver of double patient");
//        for(ArrayList c: c1Routes){
//            System.out.println(c);
//        }

//        for(int i : patients) {
//            System.out.print(i+"_");
//        }
//        System.out.println();
        //check left out patients
        int v;
        for (int i = 0; i < patients.length; i++) {
            v = i + 1;
            if (patients[i] == 0) {
                selectRoute.add("p" + v);
            }
//            else if (patients[i] == 1) {
//                p = data.getPatients()[getIdOfObject("p" + v)];
//                if (p.getRequired_caregivers().length > 1) {
//                    selectRoute.add("p" + v);
//                }
//            }
        }

//        System.out.println("Select "+selectRoute);
        //System.exit(1);


        // inserting removed route.
        route1 = new ArrayList<>(selectRoute);
        Collections.shuffle(route1, rand);
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

                for (int k : caregivers1) {
                    for (int l : caregivers2) {
                        if (k != l) {
                            int m = c1Routes[k].size();
                            int n = c1Routes[l].size();
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
                if (bestroute1 != null) {
                    c1Routes[bestRoute1Index] = new ArrayList<>(bestroute1);
                }
                if (bestroute2 != null) {
                    c1Routes[bestRoute2Index] = new ArrayList<>(bestroute2);
                }

            } else {
                caregivers1 = getQualifiedCaregiver(service1);
                for (int j = 0; j < c1Routes.length; j++) {
                    if (caregivers1.contains(j)) {
                        int k = c1Routes[j].size();
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
                if (bestroute1 != null) {
                    c1Routes[bestRoute1Index] = new ArrayList<>(bestroute1);
                }
            }

        }
        if (mutRate > 0) {
            if (Math.random() < mutRate) {
                c1 = ga.mutationSelection(c1);
            }
        }
        return c1;
    }

    private boolean getOtherValidCaregiver(Patient p, int c2, ArrayList[] c1Routes, ArrayList[] p1Routes) {
        int c1 = getCaregiver(p, c1Routes);
        String service1 = p.getRequired_caregivers()[0].getService();
        String service2 = p.getRequired_caregivers()[1].getService();
        ArrayList<Integer> caregivers1 = getQualifiedCaregiver(service1);
        ArrayList<Integer> caregivers2 = getQualifiedCaregiver(service2);
        return caregivers1.contains(c1) && caregivers2.contains(c2) || caregivers1.contains(c2) && caregivers2.contains(c1);
    }


    //private Chromosome Crossover() {
//    Chromosome c1 = p2, c1Temp;
//    Random rand = new Random(System.currentTimeMillis() );
//    ArrayList[] p1Routes, c1Routes;
//    ArrayList<String> selectRoute, route, route1, tempRoute1,
//            tempRoute2, currentRoute1, currentRoute2, bestroute1, bestroute2;
//    int bestRoute1Index = 0, bestRoute2Index = 0;
//    String patient;
//    Patient p;
//    double bestCost;
//    selectRoute = new ArrayList(p2.getGenes()[rand.nextInt(p2.getGenes().length)]);
//    p1Routes = p1.getGenes();
//    c1Routes = new ArrayList[p1.getGenes().length];
//    //removing patients of selected route from parent routes
//    for (int i = 0; i < p1Routes.length; i++) {
//        route = new ArrayList<>();
//        for (int j = 0; j < p1Routes[i].size(); j++) {
//            patient = (String) p1Routes[i].get(j);
//            if (!selectRoute.contains(patient)) {
//                route.add(patient);
//            }
//        }
//        c1Routes[i] = new ArrayList<>(route);
//    }
//    // inserting removed route.
//    route1 = new ArrayList<>(selectRoute);
//    Collections.shuffle(route1, rand);
//    String service1, service2;
//    ArrayList<Integer> caregivers1, caregivers2;
//    for (String s : route1) {
//        bestCost = Double.MAX_VALUE;
//        bestroute1 = null;
//        bestroute2 = null;
//        p = data.getPatients()[getIdOfObject(s)];
//        service1 = p.getRequired_caregivers()[0].getService();
//        if (p.getRequired_caregivers().length > 1) {
//            service2 = p.getRequired_caregivers()[1].getService();
//            caregivers1 = getQualifiedCaregiver(service1);
//            caregivers2 = getQualifiedCaregiver(service2);
//
//            for (int k : caregivers1) {
//                for (int l : caregivers2) {
//                    if (k != l) {
//
//                        for (int m = 0; m <= c1Routes[k].size(); m++) {
//                            for (int n = 0; n <= c1Routes[l].size(); n++) {
//                                if (noEvaluationConflicts(c1Routes[k], c1Routes[l], m, n)) {
//                                    tempRoute1 = new ArrayList<>(c1Routes[k]);
//                                    tempRoute2 = new ArrayList<>(c1Routes[l]);
//                                    tempRoute1.add(m, s);
//                                    tempRoute2.add(n, s);
//                                    currentRoute1 = c1Routes[k];
//                                    currentRoute2 = c1Routes[l];
//                                    c1Routes[k] = tempRoute1;
//                                    c1Routes[l] = tempRoute2;
//                                    c1Temp = new Chromosome(c1Routes, 0.0);
//                                    EvaluateFitness(Collections.singletonList(c1Temp), data);
//                                    if (c1Temp.getFitness() < bestCost) {
//                                        bestCost = c1Temp.getFitness();
//                                        bestroute1 = tempRoute1;
//                                        bestroute2 = tempRoute2;
//                                        bestRoute1Index = k;
//                                        bestRoute2Index = l;
//                                        c1 = c1Temp;
//                                    }
//                                    c1Routes[k] = currentRoute1;
//                                    c1Routes[l] = currentRoute2;
//                                }
//                            }
//                        }
//
//                    }
//                }
//            }
//            if (bestroute1 != null) {
//                c1Routes[bestRoute1Index] = new ArrayList<>(bestroute1);
//            }
//            if (bestroute2 != null) {
//                c1Routes[bestRoute2Index] = new ArrayList<>(bestroute2);
//            }
//
//        } else {
//            caregivers1 = getQualifiedCaregiver(service1);
//            for (int j = 0; j < c1Routes.length; j++) {
//                if (caregivers1.contains(j)) {
//                    for (int k = 0; k <= c1Routes[j].size(); k++) {
//                        tempRoute1 = new ArrayList(c1Routes[j]);
//                        tempRoute1.add(k, s);
//                        currentRoute1 = c1Routes[j];
//                        c1Routes[j] = tempRoute1;
//                        c1Temp = new Chromosome(c1Routes, 0.0);
//                        EvaluateFitness(Collections.singletonList(c1Temp), data);
//                        if (c1Temp.getFitness() < bestCost) {
//                            bestCost = c1Temp.getFitness();
//                            bestroute1 = tempRoute1;
//                            bestRoute1Index = j;
//                            c1 = c1Temp;
//                        }
//                        c1Routes[j] = currentRoute1;
//                    }
//                }
//            }
//            if (bestroute1 != null) {
//                c1Routes[bestRoute1Index] = new ArrayList<>(bestroute1);
//            }
//        }
//
//    }
//    if (mutRate > 0) {
//        if (Math.random() < mutRate) {
//            c1 = ga.mutationSelection(c1);
//        }
//    }
//    return c1;
//}
    private boolean validEntry(Patient p, ArrayList<Double> r, ArrayList[] c1Routes, ArrayList[] p1Routes) {
        ArrayList<Integer> count = new ArrayList<>();
        for (int i = 0; i < c1Routes.length; i++) {
            if (r.get(i) > 0.5) {
                if (p1Routes[i].contains(p.getId()))
                    count.add(i);
            }
        }
        return count.size() >= 2;
    }


    private int getCaregiver(Patient p, ArrayList[] c1Routes) {
        ArrayList<String> route;
        int counter = 0;
        for (ArrayList c1Route : c1Routes) {
            route = new ArrayList<>(c1Route);
            if (route.contains(p.getId())) {
                return counter;
            }
            counter++;
        }
        return counter;
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

    private boolean isvalid(ArrayList[] routes) {
        HashMap<String, Integer> index = new HashMap<>();
        HashMap<String, Integer> index2 = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            if (i < 8)
                index.put("p" + i, 1);
            else
                index.put("p" + i, 2);
        }
        for (ArrayList route : routes) {
            for (Object o : route) {
                if (index2.containsKey(o.toString())) {
                    index2.replace(o.toString(), index2.get(o.toString()) + 1);
                } else {
                    index2.put(o.toString(), 1);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : index.entrySet()) {
            if (!Objects.equals(entry.getValue(), index2.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        ga.getCrossoverChromosomes().add(Crossover());
    }
}

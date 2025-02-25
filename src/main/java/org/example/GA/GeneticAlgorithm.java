package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.GA.EvaluationFunction.EvaluateFitness;
import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.Main.startTime;

public class GeneticAlgorithm implements Runnable {
    private final int popSize;
    private final int gen;
    private final int identity;
    private final int LSRate;
    private final int TSRate;
    private final char selectTechnique;
    private final String crossType;
    private final String mutType;
    private final int numOfEliteSearch;
    private final float elitismRate;
    private final float mutRate;
    private final InstancesClass data;
    private Chromosome bestChromosome;
    private List<Chromosome> nextPopulation;
    private List<Chromosome> newPopulation;
    public static List<Chromosome> bestChromosomes;
    private List<Chromosome> crossoverChromosomes;
    private final double[] popProbabilities;
    private Map<Integer, Chromosome> LSChromosomes;

    public GeneticAlgorithm(int identity, int numOfEliteSearch, int LSRate, int TSRate, int popSize, int gen,   float elitismRate,  Parameters p, InstancesClass data) {
        this.identity = identity;
        this.numOfEliteSearch = numOfEliteSearch;
        this.LSRate = LSRate;
        this.TSRate = TSRate;
        this.popSize = popSize;
        this.gen = gen;
        selectTechnique = p.selectionTechnique();
        mutType = p.mutationType();
        crossType = p.crossoverType();
        this.elitismRate = elitismRate;
        mutRate = p.mutationRate();
        this.data = data;
        popProbabilities = new double[popSize];
    }

    public Chromosome start() {
        bestChromosome = null;
        //initialize and evaluate fitness of chromosome
        newPopulation = Population.initialize(popSize, data.getPatients().length);
        //Sort population
        sortPopulation(newPopulation);
        //printing output
        performanceUpdate(newPopulation, 0);
        for (int i = 1; i <= gen; i++) {
            maintainElitism();
            //select appropriate crossover;
            crossoverSelection();
            //Local search();
            if(!crossType.equals("MP")&&mutRate==-1f){
                if (i % LSRate == 0)
                    LocalSearch();
            }
            updatePopulation1();
            performanceUpdate(newPopulation, i);
        }
        return bestChromosome;
    }

    public List<Chromosome> getCrossoverChromosomes() {
        return crossoverChromosomes;
    }

    private void updatePopulation1() {
        newPopulation.clear();
        newPopulation.addAll(nextPopulation);
    }

    private void crossoverSelection(){
        if(crossType.equals("MP"))
            MultiParentBCRCD();
        else if(crossType.equals("BD"))
            bestCostRouteCrossoverDestruction();
        else
            bestCostRouteCrossover();
    }
    private void bestCostRouteCrossover() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome p1, p2;
        int r1, r2;
        int count;
        int index =nextPopulation.size();
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        crossoverChromosomes = Collections.synchronizedList(new ArrayList<>());
        List<Callable<Void>> crossoverTasks = new ArrayList<>();
        if(selectTechnique=='R')
            rouletteWheelSetup();
        while (index < popSize) {
            p1 = newPopulation.get(selectionTechnique(rand));
            count = 0;
            do {
                p2 = newPopulation.get(selectionTechnique(rand));
                count++;
            }
            while (count < 10 && p2.toString().equals(p1.toString()));
            do {
                r1 = rand.nextInt(p2.getGenes().length);
                r2 = rand.nextInt(p1.getGenes().length);
            }while (p1.getGenes()[r2].isEmpty() && p2.getGenes()[r1].isEmpty());

            Chromosome finalP1 = p1;
            Chromosome finalP2 = p2;
            int finalR1 = r1;
            int finalR2 = r2;
            crossoverTasks.add(() -> {
                new BCRC_CrossoverTask(this,identity,mutRate,finalP1, finalP2, finalR1,data).run();
                return null;
            });
            index++;


            if (index < popSize) {
                crossoverTasks.add(() -> {
                    new BCRC_CrossoverTask(this,identity,mutRate,finalP2, finalP1, finalR2,data).run();
                    return null;
                });
                index++;
            }
        }
        invokeThreads(service, crossoverTasks);
    }

    private void invokeThreads(ExecutorService service, List<Callable<Void>> crossoverTasks) {
        try {
            service.invokeAll(crossoverTasks);
            List<Chromosome> xChromosomes = crossoverChromosomes;
            synchronized (xChromosomes){
                nextPopulation.addAll(xChromosomes);
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }finally {
            service.shutdown();
        }
    }

    private void bestCostRouteCrossoverDestruction() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome p1, p2;
        int r1, r2;
        int count;
        int index =nextPopulation.size();
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        crossoverChromosomes = Collections.synchronizedList(new ArrayList<>());
        List<Callable<Void>> crossoverTasks = new ArrayList<>();
        if(selectTechnique=='R')
            rouletteWheelSetup();
        while (index < popSize) {
            p1 = newPopulation.get(selectionTechnique(rand));
            count = 0;
            do {
                p2 = newPopulation.get(selectionTechnique(rand));
                count++;
            }
            while (count < 10 && p2.toString().equals(p1.toString()));
            do {
                r1 = rand.nextInt(p2.getGenes().length);
                r2 = rand.nextInt(p1.getGenes().length);
            }while (p1.getGenes()[r2].isEmpty() && p2.getGenes()[r1].isEmpty());

            Chromosome finalP1 = p1;
            Chromosome finalP2 = p2;
            int finalR1 = r1;
            int finalR2 = r2;

            crossoverTasks.add(() -> {
                new BCRCD_CrossoverTask(this,identity,mutRate,finalP1, finalP2, finalR1,finalR2, data).run();
                return null;
            });
            index++;


            if (index < popSize) {
                crossoverTasks.add(() -> {
                    new BCRCD_CrossoverTask(this,identity,mutRate,finalP2, finalP1, finalR2,finalR1, data).run();
                    return null;
                });
                index++;
            }
        }
        invokeThreads(service, crossoverTasks);
    }

    private void MultiParentBCRCD() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome p1, p2, p3;
        int r1, r2, r3;
        int count;
        int index =nextPopulation.size();
        List<String> uniqueParents = new ArrayList<>();
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        crossoverChromosomes = Collections.synchronizedList(new ArrayList<>());
        List<Callable<Void>> crossoverTasks = new ArrayList<>();
        if(selectTechnique=='R')
            rouletteWheelSetup();
        while (index < popSize) {
            p1 = newPopulation.get(selectionTechnique(rand));
            uniqueParents.add(p1.toString());
            count = 0;
            do {
                p2 = newPopulation.get(selectionTechnique(rand));
                count++;
            }
            while (count < 10 && uniqueParents.contains(p2.toString()));

            count = 0;
            do {
                p3 = newPopulation.get(selectionTechnique(rand));
                count++;
            }
            while (count < 10 && uniqueParents.contains(p3.toString()));
            do {
                r1 = rand.nextInt(p2.getGenes().length);
                r2 = rand.nextInt(p3.getGenes().length);
                r3 = rand.nextInt(p1.getGenes().length);
            } while (p1.getGenes()[r3].isEmpty() && p2.getGenes()[r1].isEmpty() && p3.getGenes()[r2].isEmpty());

            Chromosome finalP1 = p1;
            Chromosome finalP2 = p2;
            Chromosome finalP3 = p3;
            int finalR1 = r1;
            int finalR2 = r2;
            int finalR3 = r3;
            crossoverTasks.add(() -> {
                new MPBCRCD_CrossoverTask(this,identity,mutRate,finalP1, finalP2, finalP3, finalR1, finalR2,finalR3,data).run();
                return null;
            });
            index++;


            if (index < popSize) {
                crossoverTasks.add(() -> {
                    new MPBCRCD_CrossoverTask(this,identity,mutRate,finalP2, finalP3, finalP1, finalR2, finalR3, finalR1,data).run();
                    return null;
                });
                index++;
            }
            if (index < popSize) {
                crossoverTasks.add(() -> {
                    new MPBCRCD_CrossoverTask( this,identity,mutRate,finalP3, finalP1, finalP2,  finalR3, finalR1, finalR2, data).run();
                    return null;
                });
                index++;
            }
        }
        invokeThreads(service, crossoverTasks);
    }
    private int selectionTechnique(Random rand) {
        if (selectTechnique=='W'){
            return rouletteWheelSelection();
        }else if (selectTechnique=='T'){
            return tournamentSelection(TSRate);
        }else
            return rand.nextInt(popSize);
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
    private void rouletteWheelSetup(){
        double total = 0.0;
        for (int i = 0; i < newPopulation.size(); i++){
            popProbabilities[i] = 1 / newPopulation.get(i).getFitness();
            total+=popProbabilities[i];
        }
        for(int i = 0; i < popProbabilities.length; i++){
            popProbabilities[i] = popProbabilities[i] / total;
        }
    }
    private int rouletteWheelSelection(){
        double rand = Math.random();
      for(int i = 0; i < newPopulation.size(); i++){
          if(rand<popProbabilities[i])
              return i;
      }
      return (int)(rand*popSize);
    }

    public Chromosome mutationSelection(Chromosome c){
        if(mutType.equals("M"))
            return mutation(c);
        else return mutation1(c);
    }
    private Chromosome mutation(Chromosome c){
        Random rand = new Random(System.currentTimeMillis());
        Chromosome newCh = search(c, rand.nextInt(data.getPatients().length));
        if(newCh.getFitness()<c.getFitness())
            return newCh;
        return c;
    }
    private Chromosome mutation1(Chromosome ch){
        Random rand = new Random(System.currentTimeMillis());
        Chromosome c = new Chromosome(ch.getGenes(),ch.getFitness(),true);
        int selectedRoute = rand.nextInt(c.getGenes().length);
        ArrayList<String> route  = new ArrayList<>(c.getGenes()[selectedRoute]);
        int index;
        int limit;
        String holder;
        if(route.size()>1){
            if(route.size()==2){
                route.reversed();
            }else {
                index = rand.nextInt(route.size());
                limit = route.size()-index;
                if (limit==1){
                    if(index==1){
                        holder = route.getFirst();
                        route.set(0,route.get(index));
                        route.set(index, holder);
                    }else{
                        if(rand.nextBoolean()){
                            holder = route.get(index);
                            route.set(index, route.get(index-1));
                            route.set(index-1, holder);
                        }else {
                            holder = route.get(index);
                            route.set(index, route.get(index-2));
                            route.set(index-2, holder);
                        }
                    }
                }else if(limit==2){
                    holder = route.get(index);
                    route.set(index,route.get(index+1));
                    route.set(index+1, holder);
                }else {
                    if(rand.nextBoolean()){
                        holder = route.get(index);
                        route.set(index, route.get(index+1));
                        route.set(index+1, holder);
                    }else {
                        holder = route.get(index);
                        route.set(index, route.get(index+2));
                        route.set(index+2, holder);
                    }
                }
            }
            c.getGenes()[selectedRoute] = new ArrayList(route);
        }
        EvaluateFitness(Collections.singletonList(c), data);
        if(c.getFitness()==Double.POSITIVE_INFINITY)
            return c;
        return ch;
    }

    public Map<Integer, Chromosome> getLSChromosomes() {
        return LSChromosomes;
    }

    private void LocalSearch() {
        Random rand = new Random(System.currentTimeMillis());
        Chromosome ch;
        int r;
        ArrayList<Integer> keys = new ArrayList<>();

        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        HashMap<Integer, Chromosome> newMap = new HashMap<>();
        LSChromosomes =Collections.synchronizedMap(newMap);
        List<Callable<Void>> LSTasks = new ArrayList<>();
        for (int i = 1; i <= numOfEliteSearch; i++) {
            r = rand.nextInt((int) (popSize * elitismRate));
            while (keys.contains(r)) {
                r = rand.nextInt(popSize);
            }
            keys.add(r);
            ch = newPopulation.get(r);
//            System.out.println("Chromosome before: "+ r+" -"+ ch.getFitness());

            Chromosome finalCh = ch;
            int finalR = r;
            LSTasks.add(() -> {
                new LocalSearchThread(this, finalCh,rand, finalR, data).run();
                return null;
            });
        }
        try {
            service.invokeAll(LSTasks);
            Map<Integer, Chromosome> sChromosomes = LSChromosomes;
            synchronized (sChromosomes){
                for(Map.Entry<Integer, Chromosome> entry : sChromosomes.entrySet()){
                    newPopulation.set(entry.getKey(), entry.getValue());
//                    System.out.println("Chromosome after: "+ entry.getKey()+" -"+ entry.getValue().getFitness());
                }
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }finally {
            service.shutdown();
        }
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

    public static void swapPatients(ArrayList<String> base, int b, int g) {
        String keyB = base.get(b);
        base.set(b, base.get(g));
        base.set(g, keyB);
    }


    private boolean noEvaluationConflicts(ArrayList<String> c1Route, ArrayList<String> c2Route, int m, int n) {
        return conflictCheck(c1Route, c2Route, m, n);
    }

    static boolean conflictCheck(ArrayList<String> c1Route, ArrayList<String> c2Route, int m, int n) {
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
        double averageFitness = population.stream().mapToDouble(Chromosome::getFitness).sum();
        long time = (System.currentTimeMillis() - startTime) / (1000);
        System.out.println("Time at: " + time + " Index " + identity +" Iteration " +iterations + " Best fitness: " + population.getFirst().getFitness() + " Average fitness: " + averageFitness/popSize );
        if (iterations == gen) {
            population.getFirst().showSolution(identity);
            bestChromosome = population.getFirst();
            System.out.println( "Time at: " + time+" Index " + identity +" Iteration " + iterations + " Fitness: " + bestChromosome.getFitness() + " Total Distance: " + bestChromosome.getTotalTravelCost() + " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness());
        }
    }

    @Override
    public void run() {
        bestChromosomes.add(start());
    }
}

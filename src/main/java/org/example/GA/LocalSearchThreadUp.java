package org.example.GA;

import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;

import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.EvaluationFunctionUp.patientAssignment;
import static org.example.GA.GeneticAlgorithm.conflictCheck;
import static org.example.GA.GeneticAlgorithm.swapPatients;


public class LocalSearchThreadUp implements Runnable {
    private GeneticAlgorithm ga;
    private Chromosome ch;
    private final Random rand;
    private final int r;
    private final Set<Integer> searchedPatients;
    private final InstancesClass data;
    private final int max;
    private final int gen;
    private final Patient[] allPatients;
    private final double[][] distanceMatrix;

    public LocalSearchThreadUp(GeneticAlgorithm ga, Chromosome ch, Random rand, int max, int r, int gen, InstancesClass data) {
        this.ga = ga;
        this.ch = ch;
        this.rand = rand;
        this.r = r;
        this.gen = gen;
        this.data = data;
        this.max = max;
        searchedPatients = new HashSet<>();
        this.allPatients = data.getPatients();
        this.distanceMatrix = data.getDistances();
    }

    private Chromosome localSearch() {
        Chromosome newCh;
        int sp;
        int patientLength = allPatients.length;
        boolean continueLS = false;
        boolean genCont =  gen >= 300;
        do {
            newCh = ch;
            do {
                sp = rand.nextInt(patientLength);
            } while (searchedPatients.contains(sp) && searchedPatients.size() < max);
            ch = search(ch, sp);
            //System.out.println("Local Search Thread Up: " + ch.getFitness());
            searchedPatients.add(sp);
            if(genCont)
                continueLS = ch.getFitness() < newCh.getFitness();
        } while (continueLS && searchedPatients.size() < max);
        //System.exit(0);
        return newCh;
    }

    @Override
    public void run() {
        ga.getLSChromosomes().put(r, localSearch());
    }

    public Chromosome search(Chromosome ch, int sp) {
        Chromosome tempCh;
        Chromosome bestCh = ch;
        Patient patient = allPatients[sp];
        ArrayList<String> route;
        String p;
        String service1, service2;
        MoveUp move, bestMove = null;
        ArrayList<Process> processes;
        Process process;
        String moveSign1, moveSign2;
        Set<String> listOfMoves;
        ArrayList<String> tempRoute1, tempRoute2, tempRoute3, tempRoute4, currentRoute1, currentRoute2, currentRoute3, currentRoute4;
        Set<Integer> caregivers1, caregivers2, caregivers3;
        ArrayList<String>[] routes;
        //Removing selected patient
        routes = removePatientFromRoutes(ch, patient.getId());
        listOfMoves = new HashSet<>();
        service1 = patient.getRequired_caregivers()[0].getService();
        tempCh = new Chromosome(routes, 0.0, true);
        EvaluationFunctionUp.EvaluateFitness(Collections.singletonList(tempCh), data);
        tempCh.buildPatientRouteMap();
        // double patient local search
        if (patient.getRequired_caregivers().length > 1) {
            //Relocate
            service2 = patient.getRequired_caregivers()[1].getService();
            caregivers1 = data.getQualifiedCaregiver(service1);
            caregivers2 = data.getQualifiedCaregiver(service2);

            for (int k : caregivers1) {
                for (int l : caregivers2) {
                    if (k != l) {

                        for (int m = 0; m <= routes[k].size(); m++) {
                            for (int n = 0; n <= routes[l].size(); n++) {
                                if (noEvaluationConflicts(routes[k], routes[l], m, n, data)) {
                                    tempRoute1 = new ArrayList<>(routes[k]);
                                    tempRoute2 = new ArrayList<>(routes[l]);
                                    tempRoute1.add(m, patient.getId());
                                    tempRoute2.add(n, patient.getId());
                                    moveSign1 = tempRoute1 + " - " + tempRoute2;
                                    moveSign2 = tempRoute2 + " - " + tempRoute1;
                                    if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(moveSign2)) {
                                        processes = new ArrayList<>();
                                        process = new Process(tempRoute1, patient.getId(), m, k);
                                        processes.add(process);
                                        process = new Process(tempRoute2, patient.getId(), n, l);
                                        processes.add(process);
                                        move = new MoveUp(processes);
                                        bestMove = evaluateMove(move, bestMove, tempCh);
                                        listOfMoves.add(moveSign1);
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
                            moveSign1 = String.valueOf(tempRoute1);
                            if (!listOfMoves.contains(moveSign1)) {
                                processes = new ArrayList<>();
                                process = new Process(tempRoute1, patient.getId(), Math.min(b1, w), j);
                                processes.add(process);
                                move = new MoveUp(processes);
                                bestMove = evaluateMove(move, bestMove, tempCh);
                                listOfMoves.add(moveSign1);
                            }

                            //second service swap
                            for (int k : caregivers2) {
                                //swapping second patient in the same second route
                                if (routes[k].contains(patient.getId()) && k == r2) {
                                    for (int l = 0; l < routes[k].size(); l++) {
                                        if (b2 != l) {
                                            tempRoute2 = new ArrayList(routes[k]);
                                            swapPatients(tempRoute2, b2, l);

                                            if (noEvaluationConflicts(tempRoute2, tempRoute1, l, b1,data)) {
                                                moveSign1 = tempRoute1 + " - " + tempRoute2;
                                                processes = new ArrayList<>();
                                                if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                                    process = new Process(tempRoute1, patient.getId(), Math.min(b1, w), j);
                                                    processes.add(process);
                                                    process = new Process(tempRoute2, patient.getId(), Math.min(b2, l), k);
                                                    processes.add(process);
                                                    move = new MoveUp(processes);
                                                    bestMove = evaluateMove(move, bestMove, tempCh);
                                                    listOfMoves.add(moveSign1);
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    if (!routes[k].contains(patient.getId())) {
                                        for (int l = 0; l < routes[k].size(); l++) {
                                            p2 = allPatients[getIdOfObject((String) routes[k].get(l))];
                                            if (!routes[r2].contains(p2.getId())) {
                                                s = p2.getRequired_caregivers()[0].getService();
                                                caregivers3 = data.getQualifiedCaregiver(s);
                                                if (!caregivers3.contains(k) && p2.getRequired_caregivers().length > 1) {
                                                    s = p2.getRequired_caregivers()[1].getService();
                                                    caregivers3 = data.getQualifiedCaregiver(s);
                                                }
                                                if (caregivers3.contains(r2)) {
                                                    tempRoute2 = new ArrayList(routes[k]);
                                                    tempRoute3 = new ArrayList(routes[r2]);
                                                    tempRoute2.set(l, patient.getId());
                                                    tempRoute3.set(b2, p2.getId());
                                                    if (noEvaluationConflicts(tempRoute2, tempRoute1, l, b1,data)) {
                                                        moveSign1 = tempRoute1 + " - " + tempRoute2 + " - " + tempRoute3;
                                                        if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(tempRoute2 + " - " + tempRoute3 + " - " + tempRoute1)) {
                                                            processes = new ArrayList<>();
                                                            process = new Process(tempRoute1, patient.getId(), Math.min(b1, w), j);
                                                            processes.add(process);
                                                            process = new Process(tempRoute2, patient.getId(), l, k);
                                                            processes.add(process);
                                                            process = new Process(tempRoute3, p2.getId(), b2, r2);
                                                            processes.add(process);
                                                            move = new MoveUp(processes);
                                                            bestMove = evaluateMove(move, bestMove, tempCh);
                                                            listOfMoves.add(moveSign1);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // swapping first service in route/ outside it current route and second service in route or outside it current route
                else {
                    if (!routes[j].contains(patient.getId())) {
                        for (int w = 0; w < routes[j].size(); w++) {
                            p2 = allPatients[getIdOfObject((String) routes[j].get(w))];
                            if (!routes[r1].contains(p2.getId())) {
                                s = p2.getRequired_caregivers()[0].getService();
                                caregivers3 = data.getQualifiedCaregiver(s);
                                if (!caregivers3.contains(j) && p2.getRequired_caregivers().length > 1) {
                                    s = p2.getRequired_caregivers()[1].getService();
                                    caregivers3 = data.getQualifiedCaregiver(s);
                                }
                                if (caregivers3.contains(r1)) {
                                    tempRoute1 = new ArrayList(routes[j]);
                                    tempRoute2 = new ArrayList(routes[r1]);
                                    tempRoute1.set(w, patient.getId());
                                    tempRoute2.set(b1, p2.getId());
                                    if (noEvaluationConflicts(tempRoute1, routes[r2], w, b2,data)) {
                                        moveSign1 = tempRoute1 + " - " + tempRoute2;
                                        if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                            processes = new ArrayList<>();
                                            process = new Process(tempRoute1, patient.getId(), w, j);
                                            processes.add(process);
                                            process = new Process(tempRoute2, p2.getId(), b1, r1);
                                            processes.add(process);
                                            move = new MoveUp(processes);
                                            bestMove = evaluateMove(move, bestMove, tempCh);
                                            listOfMoves.add(moveSign1);
                                        }

                                        //second service swap
                                        for (int k : caregivers2) {
                                            if (routes[k].contains(patient.getId()) && k == r2) {
                                                for (int l = 0; l < routes[k].size(); l++) {
                                                    if (b2 != l) {
                                                        tempRoute3 = new ArrayList(routes[k]);
                                                        swapPatients(tempRoute3, b2, l);
                                                        if (noEvaluationConflicts(tempRoute3, tempRoute1, l, b1,data)) {
                                                            moveSign1 = tempRoute1 + " - " + tempRoute2 + " - " + tempRoute3;
                                                            if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(tempRoute3 + " - " + tempRoute1 + " - " + tempRoute2)) {
                                                                processes = new ArrayList<>();
                                                                process = new Process(tempRoute1, patient.getId(), w, j);
                                                                processes.add(process);
                                                                process = new Process(tempRoute2, p2.getId(), b1, r1);
                                                                processes.add(process);
                                                                process = new Process(tempRoute3, patient.getId(), Math.min(b2, l), k);
                                                                processes.add(process);
                                                                //check the order in which movesign is created also for all to remove duplicates
                                                                move = new MoveUp(processes);
                                                                bestMove = evaluateMove(move, bestMove, tempCh);
                                                                listOfMoves.add(moveSign1);
                                                            }
                                                        }
                                                    }
                                                }

                                            } else {
                                                if (!routes[k].contains(patient.getId())) {
                                                    for (int l = 0; l < routes[k].size(); l++) {
                                                        p2 = allPatients[getIdOfObject((String) routes[k].get(l))];
                                                        if (!routes[r2].contains(p2.getId())) {
                                                            s = p2.getRequired_caregivers()[0].getService();
                                                            caregivers3 = data.getQualifiedCaregiver(s);
                                                            if (!caregivers3.contains(k) && p2.getRequired_caregivers().length > 1) {
                                                                s = p2.getRequired_caregivers()[1].getService();
                                                                caregivers3 = data.getQualifiedCaregiver(s);
                                                            }
                                                            if (caregivers3.contains(r2)) {
                                                                tempRoute3 = new ArrayList(routes[k]);
                                                                tempRoute4 = new ArrayList(routes[r2]);
                                                                tempRoute3.set(l, patient.getId());
                                                                tempRoute4.set(b2, p2.getId());
                                                                if (noEvaluationConflicts(tempRoute3, tempRoute1, l, b1,data)) {
                                                                    moveSign1 = tempRoute1 + " - " + tempRoute2 + " - " + tempRoute3 + " - " + tempRoute4;
                                                                    if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(tempRoute3 + " - " + tempRoute4 + " - " + tempRoute1 + " - " + tempRoute2)) {
                                                                        processes = new ArrayList<>();
                                                                        process = new Process(tempRoute1, patient.getId(), w, j);
                                                                        processes.add(process);
                                                                        process = new Process(tempRoute2, p2.getId(), b1, r1);
                                                                        processes.add(process);
                                                                        process = new Process(tempRoute3, patient.getId(), l, k);
                                                                        processes.add(process);
                                                                        process = new Process(tempRoute4, p2.getId(), b2, r2);
                                                                        processes.add(process);
                                                                        move = new MoveUp(processes);
                                                                        bestMove = evaluateMove(move, bestMove, tempCh);
                                                                        listOfMoves.add(moveSign1);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        //Single service patient local search
        else {
            caregivers1 = data.getQualifiedCaregiver(service1);
            //relocate
            for (int j :caregivers1) {
                    for (int k = 0; k <= routes[j].size(); k++) {
                        tempRoute1 = new ArrayList(routes[j]);
                        tempRoute1.add(k, patient.getId());
                        moveSign1 = String.valueOf(tempRoute1);
                        if (!listOfMoves.contains(moveSign1)) {
                            processes = new ArrayList<>();
                            process = new Process(tempRoute1, patient.getId(), k, j);
                            processes.add(process);
                            move = new MoveUp(processes);
                            bestMove = evaluateMove(move, bestMove, tempCh);
                            listOfMoves.add(moveSign1);
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
                            moveSign1 = String.valueOf(tempRoute1);
                            if (!listOfMoves.contains(moveSign1)) {
                                processes = new ArrayList<>();
                                process = new Process(tempRoute1, patient.getId(), Math.min(b, l), k);
                                processes.add(process);
                                move = new MoveUp(processes);
                                bestMove = evaluateMove(move, bestMove, tempCh);
                                listOfMoves.add(moveSign1);
                            }
                        }
                    }

                } else {
                    for (int l = 0; l < routes[k].size(); l++) {
                        p2 = allPatients[getIdOfObject((String) routes[k].get(l))];
                        if (!routes[r].contains(p2.getId())) {
                            s = p2.getRequired_caregivers()[0].getService();
                            caregivers2 = data.getQualifiedCaregiver(s);
                            if (!caregivers2.contains(k) && p2.getRequired_caregivers().length > 1) {
                                s = p2.getRequired_caregivers()[1].getService();
                                caregivers2 = data.getQualifiedCaregiver(s);
                            }
                            if (caregivers2.contains(r)) {
                                tempRoute1 = new ArrayList(routes[k]);
                                tempRoute2 = new ArrayList(routes[r]);
                                tempRoute1.set(l, patient.getId());
                                tempRoute2.set(b, p2.getId());
                                moveSign1 = tempRoute1 + " - " + tempRoute2;
                                if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                    processes = new ArrayList<>();
                                    process = new Process(tempRoute1, patient.getId(), l, k);
                                    processes.add(process);
                                    process = new Process(tempRoute2, p2.getId(), b, r);
                                    processes.add(process);
                                    move = new MoveUp(processes);
                                    bestMove = evaluateMove(move, bestMove, tempCh);
                                    listOfMoves.add(moveSign1);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (bestMove != null && bestMove.getChromosome().getFitness() < bestCh.getFitness()) {
            return bestMove.getChromosome();
        }
        return bestCh;
    }

    private ArrayList<String>[] removePatientFromRoutes(Chromosome ch, String patientId) {
        @SuppressWarnings("unchecked")
        ArrayList<String>[] routes = (ArrayList<String>[]) new ArrayList[ch.getGenes().length];
        for (int i = 0; i < ch.getGenes().length; i++) {
            ArrayList originalRoute = ch.getGenes()[i];
            // Fast path - if patient not in this route, just copy
            if (!originalRoute.contains(patientId)) {
                routes[i] = new ArrayList<>(originalRoute);
                continue;
            }

            // Slow path - only if patient is in this route
            ArrayList<String> newRoute = new ArrayList<>(originalRoute.size());
            for (int j = 0; j < originalRoute.size(); j++) {
                Object p = originalRoute.get(j);
                if (!patientId.equals(p)) {
                    newRoute.add((String) p);
                }
            }
            routes[i] = newRoute;
        }
        return routes;
    }

    private MoveUp evaluateMove(MoveUp m, MoveUp bestMove, Chromosome c) {
        Chromosome tempCh = new Chromosome(c.getGenes(), 0.0, true);
        int[] routeEndPoint = new int[c.getGenes().length];
        Arrays.fill(routeEndPoint, -1);
        for (Process process : m.getProcesses()) {
            routeEndPoint[process.getRouteIndex()] = process.getInsertPosition();
        }


        removeAffectedPatients(m, c, routeEndPoint);
        int index;
        for (int i = 0; i < routeEndPoint.length; i++) {
            ArrayList<String> route;
            ArrayList<Double> currentTime;
            ArrayList<Double> travelCost;
            ArrayList<Double> tardiness;
            ArrayList<Double> maxTardiness;
            route = new ArrayList<>(c.getCaregiversRouteUp()[i].getRoute());
            currentTime = new ArrayList<>(c.getCaregiversRouteUp()[i].getCurrentTime());
            travelCost = new ArrayList<>(c.getCaregiversRouteUp()[i].getTravelCost());
            travelCost.removeLast();
            tardiness = new ArrayList<>(c.getCaregiversRouteUp()[i].getTardiness());
            maxTardiness = new ArrayList<>(c.getCaregiversRouteUp()[i].getMaxTardiness());
            if (routeEndPoint[i] != -1) {
                index = routeEndPoint[i] + 1;
                route.subList(index, route.size()).clear();
                travelCost.subList(index, travelCost.size()).clear();
                currentTime.subList(index, currentTime.size()).clear();
                tardiness.subList(index, tardiness.size()).clear();
                maxTardiness.subList(index, maxTardiness.size()).clear();
                tempCh.getCaregiversRouteUp()[i] = new ShiftUp(c.getCaregiversRouteUp()[i].getCaregiver(), route, currentTime, travelCost, tardiness, maxTardiness);
            } else {
                tempCh.getCaregiversRouteUp()[i] = new ShiftUp(c.getCaregiversRouteUp()[i].getCaregiver(), route, currentTime, travelCost, tardiness, maxTardiness);
            }
        }
        //changing routes with move routes
        for (Process process : m.getProcesses()) {
            tempCh.getGenes()[process.getRouteIndex()] = process.getRoute();
        }

        double totalTravelCost = 0;
        double totalTardiness = 0;
        double highestTardiness = 0;
        for (ShiftUp s : tempCh.getCaregiversRouteUp()) {
            totalTravelCost += s.getTravelCost().getLast();
            totalTardiness += s.getTardiness().getLast();
            highestTardiness = Math.max(highestTardiness, s.getMaxTardiness().getLast());
        }

        tempCh.setTotalTravelCost(totalTravelCost);
        tempCh.setTotalTardiness(totalTardiness);
        tempCh.setHighestTardiness(highestTardiness);
        tempCh.setFitness(0.0);


        evaluate(tempCh, routeEndPoint, bestMove);
        if (bestMove == null || tempCh.getFitness() < bestMove.getFitness()) {
            m.setChromosome(tempCh);
            m.setFitness(tempCh.getFitness());
            return m;
        }
        return bestMove;
    }

    private void removeAffectedPatients(MoveUp r, Chromosome c, int[] affectedRoutes) {
        int startPos;
        String patientId;
        Map<String, Set<Integer>> patientToRoutesMap = c.getPatientToRoutesMap();
        for (Process process : r.getProcesses()) {
            ArrayList<String> currentRoute = c.getGenes()[process.getRouteIndex()];
            startPos = process.getInsertPosition();
            for (int i = startPos; i < currentRoute.size(); i++) {
                patientId = currentRoute.get(i);
                Patient p = allPatients[getIdOfObject(patientId)];
                if (p.getRequired_caregivers().length > 1) {
                    int routeIndex = getRouteIndexMethod(process.getRouteIndex(), patientToRoutesMap.get(p.getId()));
                    int patientIndex = c.getGenes()[routeIndex].indexOf(p.getId());
                    ArrayList<Process> processBuffer = new ArrayList<>();

                    if (affectedRoutes[routeIndex] == -1 || affectedRoutes[routeIndex] > patientIndex) {
                        affectedRoutes[routeIndex] = patientIndex;
                        processBuffer.add(new Process(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), patientIndex, routeIndex));
                        removeAffectedPatients(new MoveUp(processBuffer), c, affectedRoutes);
                    }

                }
            }
        }
    }

    private int getRouteIndexMethod(int route1, Set<Integer> routes) {
        if (routes == null) return -1; // Patient not found
        for (int route : routes) {
            if (route != route1) {
                return route; // Return the first alternative route
            }
        }
        return -1; // No alternative route found
    }

    private void evaluate(Chromosome ch, int[] routeEndPoint, MoveUp bestMove) {
        ArrayList<String> route;
        ShiftUp[] routes = ch.getCaregiversRouteUp();
        ShiftUp caregiver1;
        int routeEnd;
        Set<String> track = new LinkedHashSet<>();
        Map<String,List<Integer>> sycTrack = new HashMap<>();
        int simCounter = 0;
        for (int i = 0; i < routeEndPoint.length; i++) {
            route = new ArrayList<>(ch.getGenes()[i]);
            caregiver1 = routes[i];
            routeEnd = routeEndPoint[i];
            if (routeEnd != -1) {
                for (int j = routeEnd; j < route.size(); j++) {
                    String patient = route.get(j);
                    if (!caregiver1.getRoute().contains(patient)) {
                        if (!patientAssignment(ch, patient, caregiver1, routes, i, track, sycTrack,simCounter)) {
                            ch.setFitness(Double.POSITIVE_INFINITY);
                            return;
                        }
                        UpdateCost(ch);
                        if (bestMove != null && ch.getFitness() > bestMove.getFitness()) {
                            return;
                        }
                        track.clear();
                        sycTrack.clear();
                    }
                }
            }
        }
        for (ShiftUp s : routes) {
            ch.updateTotalTravelCost(distanceMatrix[getIdOfObjectLocation(s.getRoute().getLast())][0]);
            s.updateTravelCost(distanceMatrix[getIdOfObjectLocation(s.getRoute().getLast())][0]);
        }
        UpdateCost(ch);
    }

    private static void UpdateCost(Chromosome ch) {
        ch.setFitness((1 / 3d * ch.getTotalTravelCost()) + (1 / 3d * ch.getTotalTardiness()) + (1 / 3d * ch.getHighestTardiness()));
    }

    private static int getIdOfObjectLocation(String s) {
        return Integer.parseInt(s.substring(1));
    }


    private boolean noEvaluationConflicts(ArrayList<String> c1Route, ArrayList<String> c2Route, int m, int n, InstancesClass data) {
        return conflictCheck(c1Route, c2Route, m, n);
    }
}

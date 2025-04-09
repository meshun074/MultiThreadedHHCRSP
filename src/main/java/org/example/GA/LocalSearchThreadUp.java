package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.GA.EvaluationFunction.EvaluateFitness;
import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.EvaluationFunctionUp.patientAssignment;
import static org.example.GA.GeneticAlgorithm.conflictCheck;
import static org.example.GA.GeneticAlgorithm.swapPatients;


public class LocalSearchThreadUp implements Runnable {
    private GeneticAlgorithm ga;
    private Chromosome ch;
    private final Random rand;
    private final int r;
    private final ArrayList<Integer> searchedPatients;
    private final InstancesClass data;

    public LocalSearchThreadUp(GeneticAlgorithm ga, Chromosome ch, Random rand, int r, InstancesClass data) {
        this.ga = ga;
        this.ch = ch;
        this.rand = rand;
        this.r = r;
        this.data = data;
        searchedPatients = new ArrayList<>();
    }

    private Chromosome localSearch() {
        Chromosome newCh;
        int sp;
        int max = (data.getPatients().length < 75 ? Math.ceilDiv(data.getPatients().length, 6) : 10);
        do {
            newCh = ch;
            do {
                sp = rand.nextInt(data.getPatients().length);
            } while (searchedPatients.contains(sp));
            ch = search(ch, sp);
            searchedPatients.add(sp);
        } while (ch.getFitness() < newCh.getFitness() || searchedPatients.size() < max);
//        newPopulation.set(r, newCh);
        return newCh;
    }

    @Override
    public void run() {
        ga.getLSChromosomes().put(r, localSearch());
    }

    public Chromosome search(Chromosome ch, int sp) {
        Chromosome tempCh;
        Chromosome bestCh = ch;
        Patient patient = data.getPatients()[sp];
        ArrayList<String> route;
        String p;
        String service1, service2;
        MoveUp move, bestMove = null;
        ArrayList<Process> processes;
        Process process;
        String moveSign;
        ArrayList<MoveUp> possibleMoves = new ArrayList<>();
        ArrayList<String> listOfMoves;
        ArrayList<String> tempRoute1, tempRoute2,tempRoute3,tempRoute4, currentRoute1, currentRoute2, currentRoute3, currentRoute4;
        ArrayList<Integer> caregivers1, caregivers2, caregivers3;
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
        listOfMoves = new ArrayList<>();
        service1 = patient.getRequired_caregivers()[0].getService();
        tempCh = new Chromosome(routes, 0.0, true);
        EvaluationFunctionUp.EvaluateFitness(Collections.singletonList(tempCh), data);
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
                                        processes = new ArrayList<>();
                                        moveSign = tempRoute1 + " - " + tempRoute2;
                                        if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                            process = new Process(tempRoute1,patient.getId(),m,k);
                                            processes.add(process);
                                            process = new Process(tempRoute2,patient.getId(),n,l);
                                            processes.add(process);
                                            move = new MoveUp(processes);
                                            possibleMoves.add(move);
                                            listOfMoves.add(moveSign);
                                        }
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
                            moveSign = String.valueOf(tempRoute1);
                            if (!listOfMoves.contains(moveSign)) {
                                processes = new ArrayList<>();
                                process = new Process(tempRoute1, patient.getId(), Math.min(b1, w), j);
                                processes.add(process);
                                move = new MoveUp(processes);
                                possibleMoves.add(move);
                                listOfMoves.add(moveSign);
                            }

                            //second service swap
                            for (int k : caregivers2) {
                                //swapping second patient in the same second route
                                if (routes[k].contains(patient.getId()) && k == r2) {
                                    for (int l = 0; l < routes[k].size(); l++) {
                                        if (b2 != l) {
                                            tempRoute2 = new ArrayList(routes[k]);
                                            swapPatients(tempRoute2, b2, l);

                                            if (noEvaluationConflicts(tempRoute2, tempRoute1, l, b1)) {
                                                moveSign = tempRoute1 + " - " + tempRoute2;
                                                processes = new ArrayList<>();
                                                if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                                    process = new Process(tempRoute1,patient.getId(),Math.min(b1, w), j);
                                                    processes.add(process);
                                                    process = new Process(tempRoute2,patient.getId(),Math.min(b2, l), k);
                                                    processes.add(process);
                                                    move = new MoveUp(processes);
                                                    possibleMoves.add(move);
                                                    listOfMoves.add(moveSign);
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    if (!routes[k].contains(patient.getId())) {
                                        for (int l = 0; l < routes[k].size(); l++) {
                                            p2 = data.getPatients()[getIdOfObject((String) routes[k].get(l))];
                                            if (!routes[r2].contains(p2.getId())) {
                                                s = p2.getRequired_caregivers()[0].getService();
                                                caregivers3 = getQualifiedCaregiver(s);
                                                if (!caregivers3.contains(k) && p2.getRequired_caregivers().length > 1) {
                                                    s = p2.getRequired_caregivers()[1].getService();
                                                    caregivers3 = getQualifiedCaregiver(s);
                                                }
                                                if (caregivers3.contains(r2)) {
                                                    tempRoute2 = new ArrayList(routes[k]);
                                                    tempRoute3 = new ArrayList(routes[r2]);
                                                    tempRoute2.set(l, patient.getId());
                                                    tempRoute3.set(b2, p2.getId());
                                                    if (noEvaluationConflicts(tempRoute2, tempRoute1, l, b1)) {
                                                        moveSign = tempRoute1 + " - " + tempRoute2+ " - " + tempRoute3;
                                                        if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute2 + " - " + tempRoute3+ " - " + tempRoute1)) {
                                                            processes = new ArrayList<>();
                                                            process = new Process(tempRoute1,patient.getId(),Math.min(b1, w), j);
                                                            processes.add(process);
                                                            process = new Process(tempRoute2,patient.getId(), l, k);
                                                            processes.add(process);
                                                            process = new Process(tempRoute3,p2.getId(),b2, r2);
                                                            processes.add(process);
                                                            move = new MoveUp(processes);
                                                            possibleMoves.add(move);
                                                            listOfMoves.add(moveSign);
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
                            p2 = data.getPatients()[getIdOfObject((String) routes[j].get(w))];
                            if (!routes[r1].contains(p2.getId())) {
                                s = p2.getRequired_caregivers()[0].getService();
                                caregivers3 = getQualifiedCaregiver(s);
                                if (!caregivers3.contains(j) && p2.getRequired_caregivers().length > 1) {
                                    s = p2.getRequired_caregivers()[1].getService();
                                    caregivers3 = getQualifiedCaregiver(s);
                                }
                                if (caregivers3.contains(r1)) {
                                    tempRoute1 = new ArrayList(routes[j]);
                                    tempRoute2 = new ArrayList(routes[r1]);
                                    tempRoute1.set(w, patient.getId());
                                    tempRoute2.set(b1, p2.getId());
                                    if (noEvaluationConflicts(tempRoute1, routes[r2], w, b2)) {
                                        moveSign = tempRoute1 + " - " + tempRoute2;
                                        if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                            processes = new ArrayList<>();
                                            process = new Process(tempRoute1,patient.getId(), w, j);
                                            processes.add(process);
                                            process = new Process(tempRoute2,p2.getId(), b1, r1);
                                            processes.add(process);
                                            move = new MoveUp(processes);
                                            possibleMoves.add(move);
                                            listOfMoves.add(moveSign);
                                        }

                                        //second service swap
                                        for (int k : caregivers2) {
                                            if (routes[k].contains(patient.getId()) && k == r2) {
                                                for (int l = 0; l < routes[k].size(); l++) {
                                                    if (b2 != l) {
                                                        tempRoute3 = new ArrayList(routes[k]);
                                                        swapPatients(tempRoute3, b2, l);
                                                        if (noEvaluationConflicts(tempRoute3, tempRoute1, l, b1)) {
                                                            moveSign = tempRoute1 + " - " + tempRoute2+ " - " + tempRoute3;
                                                            if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute3 + " - " + tempRoute1+ " - " + tempRoute2)) {
                                                                processes = new ArrayList<>();
                                                                process = new Process(tempRoute1,patient.getId(), w, j);
                                                                processes.add(process);
                                                                process = new Process(tempRoute2,p2.getId(), b1, r1);
                                                                processes.add(process);
                                                                process = new Process(tempRoute3,patient.getId(), Math.min(b2,l), k);
                                                                processes.add(process);
                                                                //check the order in which movesign is created also for all to remove duplicates
                                                                move = new MoveUp(processes);
                                                                possibleMoves.add(move);
                                                                listOfMoves.add(moveSign);
                                                            }
                                                        }
                                                    }
                                                }

                                            } else {
                                                if (!routes[k].contains(patient.getId())) {
                                                    for (int l = 0; l < routes[k].size(); l++) {
                                                        p2 = data.getPatients()[getIdOfObject((String) routes[k].get(l))];
                                                        if (!routes[r2].contains(p2.getId())) {
                                                            s = p2.getRequired_caregivers()[0].getService();
                                                            caregivers3 = getQualifiedCaregiver(s);
                                                            if (!caregivers3.contains(k) && p2.getRequired_caregivers().length > 1) {
                                                                s = p2.getRequired_caregivers()[1].getService();
                                                                caregivers3 = getQualifiedCaregiver(s);
                                                            }
                                                            if (caregivers3.contains(r2)) {
                                                                tempRoute3 = new ArrayList(routes[k]);
                                                                tempRoute4 = new ArrayList(routes[r2]);
                                                                tempRoute3.set(l, patient.getId());
                                                                tempRoute4.set(b2, p2.getId());
                                                                if (noEvaluationConflicts(tempRoute3, tempRoute1, l, b1)) {
                                                                    moveSign = tempRoute1 + " - " + tempRoute2+ " - " + tempRoute3 + " - "+ tempRoute4;
                                                                    if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute3 + " - " + tempRoute4+ " - " + tempRoute1 + " - " + tempRoute2)) {
                                                                        processes = new ArrayList<>();
                                                                        process = new Process(tempRoute1,patient.getId(), w, j);
                                                                        processes.add(process);
                                                                        process = new Process(tempRoute2,p2.getId(), b1, r1);
                                                                        processes.add(process);
                                                                        process = new Process(tempRoute3,patient.getId(), l, k);
                                                                        processes.add(process);
                                                                        process = new Process(tempRoute4,p2.getId(), b2, r2);
                                                                        processes.add(process);
                                                                        move = new MoveUp(processes);
                                                                        possibleMoves.add(move);
                                                                        listOfMoves.add(moveSign);
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
            caregivers1 = getQualifiedCaregiver(service1);
            //relocate
            for (int j = 0; j < routes.length; j++) {
                if (caregivers1.contains(j)) {
                    for (int k = 0; k <= routes[j].size(); k++) {
                        tempRoute1 = new ArrayList(routes[j]);
                        tempRoute1.add(k, patient.getId());
                        moveSign = String.valueOf(tempRoute1);
                        if (!listOfMoves.contains(moveSign)) {
                            processes = new ArrayList<>();
                            process = new Process(tempRoute1,patient.getId(), k, j);
                            processes.add(process);
                            move = new MoveUp(processes);
                            possibleMoves.add(move);
                            listOfMoves.add(moveSign);
                        }
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
                            moveSign = String.valueOf(tempRoute1);
                            if (!listOfMoves.contains(moveSign)) {
                                processes = new ArrayList<>();
                                process = new Process(tempRoute1, patient.getId(), Math.min(b, l), k);
                                move = new MoveUp(processes);
                                possibleMoves.add(move);
                                listOfMoves.add(moveSign);
                            }
                        }
                    }

                } else {
                    for (int l = 0; l < routes[k].size(); l++) {
                        p2 = data.getPatients()[getIdOfObject((String) routes[k].get(l))];
                        if (!routes[r].contains(p2.getId())) {
                            s = p2.getRequired_caregivers()[0].getService();
                            caregivers2 = getQualifiedCaregiver(s);
                            if (!caregivers2.contains(k) && p2.getRequired_caregivers().length > 1) {
                                s = p2.getRequired_caregivers()[1].getService();
                                caregivers2 = getQualifiedCaregiver(s);
                            }
                            if (caregivers2.contains(r)) {
                                tempRoute1 = new ArrayList(routes[k]);
                                tempRoute2 = new ArrayList(routes[r]);
                                tempRoute1.set(l, patient.getId());
                                tempRoute2.set(b, p2.getId());
                                moveSign = tempRoute1 + " - " + tempRoute2;
                                if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                    processes = new ArrayList<>();
                                    process = new Process(tempRoute1,patient.getId(), l, k);
                                    processes.add(process);
                                    process = new Process(tempRoute2,p2.getId(), b, r);
                                    processes.add(process);
                                    move = new MoveUp(processes);
                                    possibleMoves.add(move);
                                    listOfMoves.add(moveSign);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (MoveUp m : possibleMoves) {
//                    System.out.println("Move "+count);
//                    System.out.println("Routes to change "+m.getRoute1()+" -> "+m.getRoute2());
            bestMove = evaluateMove(m, bestMove, tempCh);
            // count ++;
        }
        if(bestMove != null&&bestMove.getChromosome().getFitness()<bestCh.getFitness()){
            return bestMove.getChromosome();
        }
        return bestCh;
    }

    private MoveUp evaluateMove(MoveUp m, MoveUp bestMove, Chromosome c) {
        Chromosome tempCh = new Chromosome(c.getGenes(), 0.0, true);
        int[] routeEndPoint = new int[c.getGenes().length];
        Arrays.fill(routeEndPoint, -1);
        Map<Integer, Integer> affectedRoutes = new ConcurrentHashMap<>();
        for(Process process: m.getProcesses()){
            affectedRoutes.put(process.getRouteIndex(), process.getInsertPosition());
            routeEndPoint[process.getRouteIndex()] = process.getInsertPosition();
        }

        removeAffectedPatients(m, c, affectedRoutes);

        for (Map.Entry<Integer, Integer> entry : affectedRoutes.entrySet()) {
            routeEndPoint[entry.getKey()] = entry.getValue();
            //System.out.println(entry.getKey() + " yaya " + routeEndPoint[entry.getKey()]);
        }
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
//
        }
//        System.out.println(" After endpoint");
        //changing routes with move routes
        for(Process process: m.getProcesses()){
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

//        System.out.println(" After objectives setup");
//        tempCh.showSolution(57);
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

    private void removeAffectedPatients(MoveUp r, Chromosome c, Map<Integer, Integer> affectedRoutes) {
        int routeIndex;
        Patient p;
        ArrayList<Process> process1;
        for(Process process : r.getProcesses()){
            for (int i = process.getInsertPosition(); i < c.getGenes()[process.getRouteIndex()].size(); i++) {
                p = data.getPatients()[getIdOfObject(c.getGenes()[process.getRouteIndex()].get(i).toString())];
                if (p.getRequired_caregivers().length > 1) {
                    routeIndex = getRouteIndex(p.getId(), process.getRouteIndex(), c.getGenes());
                    process1 = new ArrayList<>();
                    if (affectedRoutes.containsKey(routeIndex) && affectedRoutes.get(routeIndex) > c.getGenes()[routeIndex].indexOf(p.getId())) {
                        affectedRoutes.replace(routeIndex, c.getGenes()[routeIndex].indexOf(p.getId()));
                        process1.add(new Process(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(),c.getGenes()[routeIndex].indexOf(p.getId()), routeIndex));
                        removeAffectedPatients(new MoveUp(process1), c, affectedRoutes);
                    } else if (!affectedRoutes.containsKey(routeIndex)) {
                        int index = getPatientIndexInRoute(p.getId(), routeIndex, c);
                        affectedRoutes.put(routeIndex, index);
                        process1.add(new Process(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), c.getGenes()[routeIndex].indexOf(p.getId()), routeIndex));
                        removeAffectedPatients(new MoveUp(process1), c, affectedRoutes);
                    }
                }
            }
        }
    }

    private int getRouteIndex(String p, int route1, ArrayList[] genes) {
        for (int i = 0; i < genes.length; i++) {
            if (genes[i].contains(p) && i != route1) {
                return i;
            }
        }
        return -1;
    }
    private int getPatientIndexInRoute(String p, int routeIndex, Chromosome c) {
        return c.getGenes()[routeIndex].indexOf(p);
    }
    private void evaluate(Chromosome ch, int[] routeEndPoint, MoveUp bestMove) {
        ArrayList<String> route;
        ShiftUp[] routes = ch.getCaregiversRouteUp();
        ShiftUp caregiver1;
        ArrayList<String> track = new ArrayList<>();
        for (int i = 0; i < routeEndPoint.length; i++) {
            route = new ArrayList<>(ch.getGenes()[i]);
            caregiver1 = routes[i];
            if (routeEndPoint[i] != -1) {
                for (int j = routeEndPoint[i]; j < route.size(); j++) {
                    String patient = route.get(j);
                    if (!caregiver1.getRoute().contains(patient)) {
                        if (!patientAssignment(ch, patient, caregiver1, routes, i, track)) {
                            ch.setFitness(Double.POSITIVE_INFINITY);
                            return;
                        }
                        UpdateCost(ch);
                        if (bestMove != null && ch.getFitness() > bestMove.getFitness()) {
                            return;
                        }
                        track = new ArrayList<>();
                    }
                }
            }
        }
        for (ShiftUp s : routes) {
            ch.updateTotalTravelCost(data.getDistances()[getIdOfObjectLocation(s.getRoute().getLast())][0]);
            s.updateTravelCost(data.getDistances()[getIdOfObjectLocation(s.getRoute().getLast())][0]);
        }
        UpdateCost(ch);
    }
    private static void UpdateCost(Chromosome ch) {
        ch.setFitness((1 / 3d * ch.getTotalTravelCost()) + (1 / 3d * ch.getTotalTardiness()) + (1 / 3d * ch.getHighestTardiness()));
    }
    private static int getIdOfObjectLocation(String s) {
        return Integer.parseInt(s.substring(1));
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

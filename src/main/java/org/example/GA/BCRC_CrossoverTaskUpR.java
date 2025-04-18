package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.EvaluationFunctionUp.EvaluateFitness;
import static org.example.GA.EvaluationFunctionUp.patientAssignment;
import static org.example.GA.GeneticAlgorithm.conflictCheck;

public class BCRC_CrossoverTaskUpR implements Runnable {
    private final GeneticAlgorithm ga;
    private final int identity;
    private final boolean cross;
    private final float mutRate;
    private final int r;
    private final Chromosome p1, p2;
    private final InstancesClass data;

    public BCRC_CrossoverTaskUpR(GeneticAlgorithm ga, int identity, float mutRate, Chromosome p1, Chromosome p2, int r, boolean cross, InstancesClass data) {
        this.ga = ga;
        this.identity = identity;
        this.mutRate = mutRate;
        this.cross = cross;
        this.p1 = p1;
        this.p2 = p2;
        this.r = r;
        this.data = data;
    }

    private Chromosome Crossover() {
        if (!cross) {
            return p1;
        }
        Chromosome c2Temp;
        Random rand = new Random(System.currentTimeMillis());
        ArrayList[] p1Routes, c1Routes;
        ArrayList<String> selectRoute, route, route1, tempRoute1,
                tempRoute2;
        String patient;
        Patient p;
        int num;
        selectRoute = new ArrayList<>();
        while (selectRoute.size() < 20) {
            num = rand.nextInt(data.getPatients().length);
            if(!selectRoute.contains(data.getPatients()[num].getId())){
                selectRoute.add(data.getPatients()[num].getId());
            }
        }
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
        Collections.shuffle(route1, rand);
        String service1, service2;
        ArrayList<Integer> caregivers1, caregivers2;
        MoveUp move1, bestMove;
        ArrayList<String> listOfMoves;
        String moveSign;
        ArrayList<Process> processes;
        Process process;
        ArrayList<MoveUp> possibleMoves;
        c2Temp = new Chromosome(c1Routes, 0.0, true);
        EvaluateFitness(Collections.singletonList(c2Temp), data);
        //c2Temp.showSolution(54);
        //System.out.println("Selected patients: " + route1);
//        System.exit(1);
        for (String s : route1) {
//            System.out.println("start ");
            possibleMoves = new ArrayList<>();
            bestMove = null;
            p = data.getPatients()[getIdOfObject(s)];
            service1 = p.getRequired_caregivers()[0].getService();
            if (p.getRequired_caregivers().length > 1) {
                listOfMoves = new ArrayList<>();
                service2 = p.getRequired_caregivers()[1].getService();
                caregivers1 = getQualifiedCaregiver(service1);
                caregivers2 = getQualifiedCaregiver(service2);

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
                                        processes = new ArrayList<>();
                                        moveSign = tempRoute1 + " - " + tempRoute2;
                                        if (!listOfMoves.contains(moveSign)&&!listOfMoves.contains(tempRoute2 + " - " + tempRoute1)) {
                                            process = new Process(tempRoute1,s,m,k);
                                            processes.add(process);
                                            process = new Process(tempRoute2,s,n,l);
                                            processes.add(process);
                                            move1 = new MoveUp(processes);
                                            possibleMoves.add(move1);
                                            listOfMoves.add(moveSign);
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

//                System.out.println("end11 ");
//                System.out.println("Number of Moves: " + possibleMoves.size());
//                int count =1;
                for (MoveUp m : possibleMoves) {
//                    System.out.println("Move "+count);
//                    System.out.println("Routes to change "+m.getRoute1()+" -> "+m.getRoute2());
                    bestMove = evaluateMoveUp(m, bestMove, c2Temp);
                    // count ++;
                }
//                System.out.println("bestMove 2");
//                assert bestMove != null;
//                bestMove.getChromosome().showSolution(2);
//                for (ShiftUp sh : bestMove.getChromosome().getCaregiversRouteUp()) {
//                    System.out.println("Route " + sh.getRoute());
//                    System.out.println("time " + sh.getCurrentTime());
//                    System.out.println("travel " + sh.getTravelCost());
//                    System.out.println("tard " + sh.getTardiness());
//                    System.out.println("Max " + sh.getMaxTardiness());
//                }
                //System.exit(1);
//                System.out.println("end12 ");
                if (bestMove != null) {
                    for (Process process1: bestMove.getProcesses()) {
                        c1Routes[process1.getRouteIndex()] = new ArrayList<>(process1.getRoute());
                    }
                    c2Temp = bestMove.getChromosome();
//                    System.out.println("Before " + c2Temp.getFitness());
//                    EvaluationFunction.EvaluateFitness(Collections.singletonList(c2Temp), data);
//                    System.out.println("After " + c2Temp.getFitness());
                }

            }
            else {
                caregivers1 = getQualifiedCaregiver(service1);
                for (int j : caregivers1) {
                    for (int k = 0; k <= c1Routes[j].size(); k++) {
                        tempRoute1 = new ArrayList(c1Routes[j]);
                        tempRoute1.add(k, s);
                        processes = new ArrayList<>();
                        process = new Process(tempRoute1,s,k,j);
                        processes.add(process);
                        move1 = new MoveUp(processes);
                        possibleMoves.add(move1);
                    }

                }
//                System.out.println("end21 ");
//                System.out.println("Number of Moves: " + possibleMoves.size());
//                int count =1;
                for (MoveUp m : possibleMoves) {
//                    System.out.println("Move "+count);
//                    System.out.println("Routes to change "+ m.getRoute1());
                    bestMove = evaluateMoveUp(m, bestMove, c2Temp);
//                    count ++;
                }
//                System.out.println("bestMove 1");
//                assert bestMove != null;
//                bestMove.getChromosome().showSolution(1);
//                for (ShiftUp sh : bestMove.getChromosome().getCaregiversRouteUp()) {
//                    System.out.println("Route " + sh.getRoute());
//                    System.out.println("time " + sh.getCurrentTime());
//                    System.out.println("travel " + sh.getTravelCost());
//                    System.out.println("tard " + sh.getTardiness());
//                    System.out.println("Max " + sh.getMaxTardiness());
//                }
                //System.exit(1);
//                System.out.println("end22 ");
                if (bestMove != null) {
                    for (Process process1: bestMove.getProcesses()) {
                        c1Routes[process1.getRouteIndex()] = new ArrayList<>(process1.getRoute());
                    }
                    c2Temp = bestMove.getChromosome();
                    //EvaluationFunction.EvaluateFitness(Collections.singletonList(c2Temp), data);
                }
            }
        }
        if (mutRate > 0) {
            if (Math.random() < mutRate) {
                c2Temp = ga.mutationSelection(c2Temp);
            }
        }
//        System.out.println("Same");
//        c1.showSolution(-1);
        //System.exit(1);
        return c2Temp;
    }

    private MoveUp evaluateMoveUp(MoveUp m, MoveUp bestMove, Chromosome c) {
        Chromosome tempCh = new Chromosome(c.getGenes(), 0.0, true);
        int[] routeEndPoint = new int[c.getGenes().length];
        Arrays.fill(routeEndPoint, -1);
        Map<Integer, Integer> affectedRoutes = new ConcurrentHashMap<>();
        for(Process process: m.getProcesses()){
            affectedRoutes.put(process.getRouteIndex(), process.getInsertPosition());
            routeEndPoint[process.getRouteIndex()] = process.getInsertPosition();
        }

//        System.out.println(Arrays.toString(routeEndPoint) + " endpoint 1");
//        System.out.println("In evaluate move ");
//        c.showSolution(78);
        removeAffectedPatientsUp(m, c, affectedRoutes);
//        System.out.println(m.getRoute1() + " " + m.getRouteIndex2());
//        System.out.println("Affected routes: " + affectedRoutes);
        for (Map.Entry<Integer, Integer> entry : affectedRoutes.entrySet()) {
            routeEndPoint[entry.getKey()] = entry.getValue();
            //System.out.println(entry.getKey() + " yaya " + routeEndPoint[entry.getKey()]);
        }
//        System.out.println(Arrays.toString(routeEndPoint) + " endpoint 2");
//        tempCh.showSolution(90);
//        for(ShiftUp s : c.getCaregiversRouteUp()) {
//            System.out.println("Route "+s.getRoute());
//            System.out.println("time "+s.getCurrentTime());
//            System.out.println("travel "+s.getTravelCost());
//            System.out.println("tard "+s.getTardiness());
//            System.out.println("Max "+s.getMaxTardiness());
//        }
//        System.out.println(" Masa aden ");
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

//        tempCh.showSolution(95);
//        for (ShiftUp s : tempCh.getCaregiversRouteUp()) {
//            System.out.println("Route " + s.getRoute());
//            System.out.println("time " + s.getCurrentTime());
//            System.out.println("travel " + s.getTravelCost());
//            System.out.println("tard " + s.getTardiness());
//            System.out.println("Max " + s.getMaxTardiness());
//        }


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


//        System.out.println("About to evaluate");
//        tempCh.showSolution(-1);
//        for(ShiftUp s : tempCh.getCaregiversRouteUp()) {
//            System.out.println("Route "+s.getRoute());
//            System.out.println("time "+s.getCurrentTime());
//            System.out.println("travel "+s.getTravelCost());
//            System.out.println("tard "+s.getTardiness());
//            System.out.println("Max "+s.getMaxTardiness());
//        }

        evaluateUp(tempCh, routeEndPoint, bestMove);
//        System.out.println("evaluateMove ");
//        tempCh.showSolution(76);
//        for(ShiftUp s : tempCh.getCaregiversRouteUp()) {
//            System.out.println("Route "+s.getRoute());
//            System.out.println("time "+s.getCurrentTime());
//            System.out.println("travel "+s.getTravelCost());
//            System.out.println("tard "+s.getTardiness());
//            System.out.println("Max "+s.getMaxTardiness());
//        }
        //System.exit(1);
        if (bestMove == null || tempCh.getFitness() < bestMove.getFitness()) {
            m.setChromosome(tempCh);
            m.setFitness(tempCh.getFitness());
            return m;
        }
        return bestMove;
    }

    private Move evaluateMove(Move m, Move bestMove, Chromosome c) {
        Chromosome tempCh = new Chromosome(c.getGenes(), 0.0, true);
        int[] routeEndPoint = new int[c.getGenes().length];
        Arrays.fill(routeEndPoint, -1);
        Map<Integer, Integer> affectedRoutes = new ConcurrentHashMap<>();
        affectedRoutes.put(m.getRouteIndex1(), m.getInsertPosition1());
        routeEndPoint[m.getRouteIndex1()] = m.getInsertPosition1();
        if (m.getRouteIndex2() != -1) {
            routeEndPoint[m.getRouteIndex2()] = m.getInsertPosition2();
            affectedRoutes.put(m.getRouteIndex2(), m.getInsertPosition2());
        }
//        System.out.println(Arrays.toString(routeEndPoint) + " endpoint 1");
//        System.out.println("In evaluate move ");
//        c.showSolution(78);
        removeAffectedPatients(m, c, affectedRoutes);
//        System.out.println(m.getRoute1() + " " + m.getRouteIndex2());
//        System.out.println("Affected routes: " + affectedRoutes);
        for (Map.Entry<Integer, Integer> entry : affectedRoutes.entrySet()) {
            routeEndPoint[entry.getKey()] = entry.getValue();
            //System.out.println(entry.getKey() + " yaya " + routeEndPoint[entry.getKey()]);
        }
//        System.out.println(Arrays.toString(routeEndPoint) + " endpoint 2");
//        tempCh.showSolution(90);
//        for(ShiftUp s : c.getCaregiversRouteUp()) {
//            System.out.println("Route "+s.getRoute());
//            System.out.println("time "+s.getCurrentTime());
//            System.out.println("travel "+s.getTravelCost());
//            System.out.println("tard "+s.getTardiness());
//            System.out.println("Max "+s.getMaxTardiness());
//        }
//        System.out.println(" Masa aden ");
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
        tempCh.getGenes()[m.getRouteIndex1()] = m.getRoute1();
        if (m.getRouteIndex2() != -1) {
            tempCh.getGenes()[m.getRouteIndex2()] = m.getRoute2();
        }
//        tempCh.showSolution(95);
//        for (ShiftUp s : tempCh.getCaregiversRouteUp()) {
//            System.out.println("Route " + s.getRoute());
//            System.out.println("time " + s.getCurrentTime());
//            System.out.println("travel " + s.getTravelCost());
//            System.out.println("tard " + s.getTardiness());
//            System.out.println("Max " + s.getMaxTardiness());
//        }


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


//        System.out.println("About to evaluate");
//        tempCh.showSolution(-1);
//        for(ShiftUp s : tempCh.getCaregiversRouteUp()) {
//            System.out.println("Route "+s.getRoute());
//            System.out.println("time "+s.getCurrentTime());
//            System.out.println("travel "+s.getTravelCost());
//            System.out.println("tard "+s.getTardiness());
//            System.out.println("Max "+s.getMaxTardiness());
//        }

        evaluate(tempCh, routeEndPoint, bestMove);
//        System.out.println("evaluateMove ");
//        tempCh.showSolution(76);
//        for(ShiftUp s : tempCh.getCaregiversRouteUp()) {
//            System.out.println("Route "+s.getRoute());
//            System.out.println("time "+s.getCurrentTime());
//            System.out.println("travel "+s.getTravelCost());
//            System.out.println("tard "+s.getTardiness());
//            System.out.println("Max "+s.getMaxTardiness());
//        }
        //System.exit(1);
        if (bestMove == null || tempCh.getFitness() < bestMove.getFitness()) {
            m.setChromosome(tempCh);
            m.setFitness(tempCh.getFitness());
            return m;
        }
        return bestMove;
    }

    private void evaluateUp(Chromosome ch, int[] routeEndPoint, MoveUp bestMove) {
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
    private void evaluate(Chromosome ch, int[] routeEndPoint, Move bestMove) {
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


    private void removeAffectedPatientsUp(MoveUp r, Chromosome c, Map<Integer, Integer> affectedRoutes) {
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
                        removeAffectedPatientsUp(new MoveUp(process1), c, affectedRoutes);
                    } else if (!affectedRoutes.containsKey(routeIndex)) {
                        int index = getPatientIndexInRoute(p.getId(), routeIndex, c);
                        affectedRoutes.put(routeIndex, index);
                        process1.add(new Process(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), c.getGenes()[routeIndex].indexOf(p.getId()), routeIndex));
                        removeAffectedPatientsUp(new MoveUp(process1), c, affectedRoutes);
                    }
                }
            }
        }
    }

    private void removeAffectedPatients(Move m, Chromosome c, Map<Integer, Integer> affectedRoutes) {
        int routeIndex;
        Patient p;
//        System.out.println("Executing removeAffectedPatients");
//        c.showSolution(89);
//        System.out.println(m.getRoute1().subList(m.getInsertPosition1(), m.getRoute1().size()));
        for (int i = m.getInsertPosition1(); i < c.getGenes()[m.getRouteIndex1()].size(); i++) {
            p = data.getPatients()[getIdOfObject(c.getGenes()[m.getRouteIndex1()].get(i).toString())];
            if (p.getRequired_caregivers().length > 1) {
                routeIndex = getRouteIndex(p.getId(), m.getRouteIndex1(), c.getGenes());
                if (affectedRoutes.containsKey(routeIndex) && affectedRoutes.get(routeIndex) > c.getGenes()[routeIndex].indexOf(p.getId())) {
                    affectedRoutes.replace(routeIndex, c.getGenes()[routeIndex].indexOf(p.getId()));
//                    System.out.println("Removing affected patients from caregivers route 1");
//                    System.exit(1);
                    removeAffectedPatients(new Move(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), c.getGenes()[routeIndex].indexOf(p.getId()), routeIndex), c, affectedRoutes);
                } else if (!affectedRoutes.containsKey(routeIndex)) {
                    int index = getPatientIndexInRoute(p.getId(), routeIndex, c);
                    affectedRoutes.put(routeIndex, index);
//                    System.out.println("Removing affected patients from caregivers route 2");
//                    System.exit(1);
                    removeAffectedPatients(new Move(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), c.getGenes()[routeIndex].indexOf(p.getId()), routeIndex), c, affectedRoutes);
                }
            }
//            System.out.println("Removing affected top");
//            System.exit(1);
        }
        if (m.getRouteIndex2() != -1) {
            for (int i = m.getInsertPosition2(); i < c.getGenes()[m.getRouteIndex2()].size(); i++) {
                p = data.getPatients()[getIdOfObject(c.getGenes()[m.getRouteIndex2()].get(i).toString())];
                if (p.getRequired_caregivers().length > 1) {
                    routeIndex = getRouteIndex(p.getId(), m.getRouteIndex2(), c.getGenes());
                    if (affectedRoutes.containsKey(routeIndex) && affectedRoutes.get(routeIndex) > c.getGenes()[routeIndex].indexOf(p.getId())) {
                        affectedRoutes.replace(routeIndex, c.getGenes()[routeIndex].indexOf(p.getId()));
//                    System.out.println("Removing affected patients from caregivers route 1");
//                    System.exit(1);
                        removeAffectedPatients(new Move(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), c.getGenes()[routeIndex].indexOf(p.getId()), routeIndex), c, affectedRoutes);
                    } else if (!affectedRoutes.containsKey(routeIndex)) {
                        int index = getPatientIndexInRoute(p.getId(), routeIndex, c);
                        affectedRoutes.put(routeIndex, index);
//                    System.out.println("Removing affected patients from caregivers route 2");
//                    System.exit(1);
                        removeAffectedPatients(new Move(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), c.getGenes()[routeIndex].indexOf(p.getId()), routeIndex), c, affectedRoutes);
                    }
                }
//            System.out.println("Removing affected top");
//            System.exit(1);
            }
        }
    }

    private int getPatientIndexInRoute(String p, int routeIndex, Chromosome c) {
        return c.getGenes()[routeIndex].indexOf(p);
    }

    private int getRouteIndex(String p, int route1, ArrayList[] genes) {
        for (int i = 0; i < genes.length; i++) {
            if (genes[i].contains(p) && i != route1) {
                return i;
            }
        }
        return -1;
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

    @Override
    public void run() {
        ga.getCrossoverChromosomes().add(Crossover());
    }
}

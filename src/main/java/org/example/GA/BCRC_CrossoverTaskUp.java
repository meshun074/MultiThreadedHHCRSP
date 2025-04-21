package org.example.GA;

import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.GA.EvaluationFunctionUp.EvaluateFitness;
import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.EvaluationFunctionUp.patientAssignment;
import static org.example.GA.GeneticAlgorithm.conflictCheck;

public class BCRC_CrossoverTaskUp implements Runnable {
    private final GeneticAlgorithm ga;
    private final int identity;
    private final boolean cross;
    private final float mutRate;
    private final int r;
    private final Chromosome p1, p2;
    private final InstancesClass data;
    private final Patient[] allPatients;
    private final double[][] distanceMatrix;

    public BCRC_CrossoverTaskUp(GeneticAlgorithm ga, int identity, float mutRate, Chromosome p1, Chromosome p2, int r, boolean cross, InstancesClass data) {
        this.ga = ga;
        this.identity = identity;
        this.mutRate = mutRate;
        this.cross = cross;
        this.p1 = p1;
        this.p2 = p2;
        this.r = r;
        this.data = data;
        this.allPatients = data.getPatients();
        this.distanceMatrix = data.getDistances();
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
        Collections.shuffle(route1, rand);
        String service1, service2;
        Set<Integer> caregivers1, caregivers2;
        MoveUp move1, bestMove;
        Set<String> listOfMoves;
        String moveSign1, moveSign2;
        ArrayList<Process> processes;
        Process process;
        ArrayList<MoveUp> possibleMoves;
        c2Temp = new Chromosome(c1Routes, 0.0, true);
        EvaluateFitness(Collections.singletonList(c2Temp), data);

        for (String s : route1) {
            possibleMoves = new ArrayList<>();
            bestMove = null;
            p = allPatients[getIdOfObject(s)];
            service1 = p.getRequired_caregivers()[0].getService();
            if (p.getRequired_caregivers().length > 1) {
                listOfMoves = new HashSet<>();
                service2 = p.getRequired_caregivers()[1].getService();
                caregivers1 = data.getQualifiedCaregiver(service1);
                caregivers2 = data.getQualifiedCaregiver(service2);

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
                                        moveSign1 = tempRoute1 + " - " + tempRoute2;
                                        moveSign2 = tempRoute2 + " - " + tempRoute1;
                                        if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(moveSign2)) {
                                            process = new Process(tempRoute1, s, m, k);
                                            processes.add(process);
                                            process = new Process(tempRoute2, s, n, l);
                                            processes.add(process);
                                            move1 = new MoveUp(processes);
                                            possibleMoves.add(move1);
                                            listOfMoves.add(moveSign1);
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

                //set the hashset of the chromosome genes
                c2Temp.buildPatientRouteMap();
                for (MoveUp m : possibleMoves) {
                    bestMove = evaluateMoveUp(m, bestMove, c2Temp, allPatients,distanceMatrix);
                }

                if (bestMove != null) {
                    for (Process process1 : bestMove.getProcesses()) {
                        c1Routes[process1.getRouteIndex()] = new ArrayList<>(process1.getRoute());
                    }
                    c2Temp = bestMove.getChromosome();
                }

            } else {
                caregivers1 = data.getQualifiedCaregiver(service1);
                for (int j : caregivers1) {
                    for (int k = 0; k <= c1Routes[j].size(); k++) {
                        tempRoute1 = new ArrayList(c1Routes[j]);
                        tempRoute1.add(k, s);
                        processes = new ArrayList<>();
                        process = new Process(tempRoute1, s, k, j);
                        processes.add(process);
                        move1 = new MoveUp(processes);
                        possibleMoves.add(move1);
                    }

                }
                //set the Map patient ot route of the chromosome genes
                c2Temp.buildPatientRouteMap();
                for (MoveUp m : possibleMoves) {
                    bestMove = evaluateMoveUp(m, bestMove, c2Temp, allPatients,distanceMatrix);
                }
                if (bestMove != null) {
                    for (Process process1 : bestMove.getProcesses()) {
                        c1Routes[process1.getRouteIndex()] = new ArrayList<>(process1.getRoute());
                    }
                    c2Temp = bestMove.getChromosome();
                }
            }
        }
        if (mutRate > 0) {
            if (Math.random() < mutRate) {
                c2Temp = ga.mutationSelection(c2Temp);
            }
        }
        return c2Temp;
        //return p1;
    }

    private MoveUp evaluateMoveUp(MoveUp m, MoveUp bestMove, Chromosome c, Patient[] allPatients, double [][] distanceMatrix) {
        Chromosome tempCh = new Chromosome(c.getGenes(), 0.0, true);
        int[] routeEndPoint = new int[c.getGenes().length];
        Arrays.fill(routeEndPoint, -1);
        Map<Integer, Integer> affectedRoutes = new ConcurrentHashMap<>();
        for (Process process : m.getProcesses()) {
            affectedRoutes.put(process.getRouteIndex(), process.getInsertPosition());
            routeEndPoint[process.getRouteIndex()] = process.getInsertPosition();
        }


        removeAffectedPatientsUp(m, c, affectedRoutes, allPatients);
//        System.out.println(m.getRoute1() + " " + m.getRouteIndex2());
//        System.out.println("Affected routes: " + affectedRoutes);
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

//        System.out.println(" After objectives setup");
//        tempCh.showSolution(57);
        tempCh.setTotalTravelCost(totalTravelCost);
        tempCh.setTotalTardiness(totalTardiness);
        tempCh.setHighestTardiness(highestTardiness);
        tempCh.setFitness(0.0);


        evaluateUp(tempCh, routeEndPoint, bestMove, distanceMatrix);
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
        tempCh.getGenes()[m.getRouteIndex1()] = m.getRoute1();
        if (m.getRouteIndex2() != -1) {
            tempCh.getGenes()[m.getRouteIndex2()] = m.getRoute2();
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

    private void evaluateUp(Chromosome ch, int[] routeEndPoint, MoveUp bestMove, double[][]distanceMatrix) {
        //ch.buildPatientRouteMap();
        ArrayList<String> route;
        ShiftUp[] routes = ch.getCaregiversRouteUp();
        ShiftUp caregiver1;
        int routeEnd;
        Set<String> track = new HashSet<>();
        for (int i = 0; i < routeEndPoint.length; i++) {
            route = new ArrayList<>(ch.getGenes()[i]);
            caregiver1 = routes[i];
            routeEnd = routeEndPoint[i];
            if (routeEnd != -1) {
                for (int j = routeEnd; j < route.size(); j++) {
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
                        track.clear();
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

    private void evaluate(Chromosome ch, int[] routeEndPoint, Move bestMove) {
        ArrayList<String> route;
        ShiftUp[] routes = ch.getCaregiversRouteUp();
        ShiftUp caregiver1;
        Set<String> track = new HashSet<>();
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
                        track.clear();
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

    private void removeAffectedPatientsUp(MoveUp r, Chromosome c, Map<Integer, Integer> affectedRoutes, Patient[] allPatients) {
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
                    int routeIndex = getRouteIndexMethod(process.getRouteIndex(),  patientToRoutesMap.get(p.getId()));
                    int patientIndex = c.getGenes()[routeIndex].indexOf(p.getId());
                    ArrayList<Process> processBuffer = new ArrayList<>();

                    if (!affectedRoutes.containsKey(routeIndex) || affectedRoutes.get(routeIndex) > patientIndex) {
                        affectedRoutes.put(routeIndex, patientIndex);
                        processBuffer.add(new Process(new ArrayList<>(c.getGenes()[routeIndex]), p.getId(), patientIndex, routeIndex));
                        removeAffectedPatientsUp(new MoveUp(processBuffer), c, affectedRoutes, allPatients);
                    }

                }
            }
        }
    }

    private void removeAffectedPatients(Move m, Chromosome c, Map<Integer, Integer> affectedRoutes) {
        int routeIndex;
        Patient p;
        Map<String, Set<Integer>> patientToRoutesMap = c.getPatientToRoutesMap();

        for (int i = m.getInsertPosition1(); i < c.getGenes()[m.getRouteIndex1()].size(); i++) {
            p = allPatients[getIdOfObject(c.getGenes()[m.getRouteIndex1()].get(i).toString())];
            if (p.getRequired_caregivers().length > 1) {
                routeIndex = getRouteIndexMethod(m.getRouteIndex1(), patientToRoutesMap.get(p.getId()));
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
                p = allPatients[getIdOfObject(c.getGenes()[m.getRouteIndex2()].get(i).toString())];
                if (p.getRequired_caregivers().length > 1) {
                    routeIndex = getRouteIndexMethod(m.getRouteIndex2(), patientToRoutesMap.get(p.getId()));
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

    private int getRouteIndexMethod( int route1, Set<Integer> routes) {
        if (routes == null) return -1; // Patient not found
        for (int route : routes) {
            if (route != route1) {
                return route; // Return the first alternative route
            }
        }
        return -1; // No alternative route found
    }

//    private ArrayList<Integer> getQualifiedCaregiver(String service) {
//        ArrayList<Integer> caregivers = new ArrayList<>();
//        Set<String> abilities;
//        for (Caregiver c : data.getCaregivers()) {
//            abilities = new HashSet<>(c.getAbilities());
//            if (abilities.contains(service)) {
//                caregivers.add(c.getCacheId());
//            }
//        }
//        return caregivers;
//    }

    private boolean noEvaluationConflicts(ArrayList<String> c1Route, ArrayList<String> c2Route, int m, int n) {
        return conflictCheck(c1Route, c2Route, m, n);
    }

    @Override
    public void run() {
        ga.getCrossoverChromosomes().add(Crossover());
    }
}

package org.example.Tabu;

import org.example.Data.InstancesClass;
import org.example.Data.Patient;
import org.example.GA.*;
import org.example.GA.Process;

import java.util.*;

import static org.example.GA.EvaluationFunction.getIdOfObject;
import static org.example.GA.EvaluationFunctionUp.patientAssignment;
import static org.example.GA.GeneticAlgorithm.conflictCheck;
import static org.example.Main.startTime;
import static org.example.Main.timer;

public class TabuSearch {
    private final long seed;
    private final int chromosomeLength;
    private final int caregiverCount;
    private final int maxGenerations;
    private final int minTabuLength;
    private final int maxTabuLength;
    private Chromosome bestChromosome;
    private final InstancesClass data;
    private final Random rand;
    private final int[] moveTabu;
    private final Map<Long, Integer> swapTabu;
    private final int[] inRouteSwapTabu;
    //    private final List<Integer> moveList;
//    private final List<Integer> swapList;
    private final List<Integer> inRouteSwapList;
    private final Queue<Integer> moveTabuSet;
    private final Set<Long> swapTabuSet;
    private final List<Integer> inRouteSwapTabuList;
    private final Patient[] allPatients;
    private final double[][] distanceMatrix;
    private final Queue<Long> tabuList;
    private int terminator = 0;

    public TabuSearch(long seed, int maxGenerations, int minTabuLength, int maxTabuLength, InstancesClass instance) {
        this.seed = seed;
        this.maxGenerations = maxGenerations;
        this.minTabuLength = minTabuLength;
        this.maxTabuLength = maxTabuLength;
        this.data = instance;
        this.allPatients = data.getPatients();
        this.distanceMatrix = data.getDistances();
        this.chromosomeLength = allPatients.length;
        this.caregiverCount = data.getCaregivers().length;
        this.rand = new Random(seed);
        this.moveTabu = new int[chromosomeLength];
        this.swapTabu = new HashMap<>();
        this.inRouteSwapTabu = new int[chromosomeLength];
//        this.moveList = new ArrayList<>(chromosomeLength);
//        this.swapList = new ArrayList<>(chromosomeLength);
        this.inRouteSwapList = new ArrayList<>(chromosomeLength);
        this.moveTabuSet = new LinkedList<>();
        this.swapTabuSet = new HashSet<>(chromosomeLength);
        this.inRouteSwapTabuList = new ArrayList<>(chromosomeLength);
        this.tabuList = new LinkedList<>();
//        for (int i = 0; i < chromosomeLength; i++) {
//            moveList.add(i);
//            swapList.add(i);
//            inRouteSwapList.add(i);
//        }
    }

    public Chromosome start() {


        Chromosome tempChromosome = Population.initialize(1, chromosomeLength, seed).getFirst();
        //Evaluate fitness
        EvaluationFunctionUp.EvaluateFitness(tempChromosome, data);
        bestChromosome = tempChromosome;

        boolean improved = false;
        double bestFitness = bestChromosome.getFitness();
        for (int i = 0; i < maxGenerations; i++) {
//            System.out.println("Before "+tempChromosome.getGenes());
            Chromosome newChromosome = neighbourhoodSearch(tempChromosome, bestFitness);
//            System.out.println("New "+newChromosome.getGenes());
            if (newChromosome == null) {
                System.out.println("New Chromosome is null");
                break;
            }
            tempChromosome = newChromosome;
            if (tempChromosome.getFitness() < bestFitness) {
                bestChromosome = tempChromosome;
                improved = true;
                bestFitness = bestChromosome.getFitness();
            } else {
                improved = false;
            }
            tabuList.add(tempChromosome.getMoveID());
            if (tabuList.size() > maxTabuLength) {
                tabuList.poll();
            }
            performanceUpdate(i, improved, tempChromosome);
            if (chromosomeLength <= 100) {
                if (terminator == chromosomeLength / 2) break;
            } else {
                if (terminator == 60) break;
            }
        }
        return bestChromosome;
    }

    private void performanceUpdate(int iterations, boolean improved, Chromosome tempChromosome) {
        if (iterations > 0 && !improved) {
            terminator++;
        } else {
            terminator = 0;
        }
        long time = (System.currentTimeMillis() - startTime) / (1000);
        System.out.println("Time at: " + time + " CPU Timer " + String.format("%.3f", timer.getTotalCPUTimeSeconds()) + " seconds Index " + seed + " Generation " + iterations + " Best fitness: " + bestChromosome.getFitness() + " Temp fitness: " + tempChromosome.getFitness());
        if (iterations == maxGenerations) {
            bestChromosome.showSolution((int) seed);
            System.out.println("Time at: " + time + " CPU Timer " + String.format("%.3f", timer.getTotalCPUTimeSeconds()) + " seconds Index " + seed + " Generation " + iterations + " Fitness: " + bestChromosome.getFitness() + " Total Distance: " + bestChromosome.getTotalTravelCost() + " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness());
        }
    }

    private Chromosome neighbourhoodSearch(Chromosome tempChromosome, double fitness) {

//        System.out.println("before "+tempChromosome.getGenes());
        Chromosome c1 = moveNeighbourhood(tempChromosome, fitness);
//        if(c1 == null)
//            System.out.println("c1 is null");
//        assert c1 != null;
//        System.out.println("after "+c1.getGenes());
        return c1;
//        Chromosome c1 = moveNeighbourhood(tempChromosome,fitness);
//        Chromosome c2 = swapNeighbourhood(tempChromosome,fitness);
//        if(c1!=null && c2!=null){
//            if(c1.getFitness()<c2.getFitness()){
//                return c1;
//            }else {
//                return c2;
//            }
//        }else if(c1==null && c2==null){
//            return tempChromosome;
//        }else return Objects.requireNonNullElse(c1, c2);
    }

    private Chromosome moveNeighbourhood(Chromosome tempChromosome, double bestFitness) {
        ArrayList<String>[] parentGenes = tempChromosome.getGenes();
        ArrayList<String>[] childGenes;
        MoveUp bestMove = null;
        Set<String> listOfMoves;
        long bestUnique = 0, tempUnique;
        for (int gen = 0; gen < chromosomeLength; gen++) {
            Patient patient = allPatients[gen];
            String pIndex = patient.getId();
            childGenes = new ArrayList[parentGenes.length];

            Set<Integer> allPossibleCaregivers = patient.getRequired_caregivers().length > 1 ? patient.getAllCaregiversForDoubleService() : patient.getPossibleFirstCaregiver();
            for (int i = 0; i < parentGenes.length; i++) {
                ArrayList<String> route;
                if (allPossibleCaregivers.contains(i) && parentGenes[i].contains(pIndex)) {
                    route = new ArrayList<>();
                    for (int j = 0; j < parentGenes[i].size(); j++) {
                        String p = parentGenes[i].get(j);
                        if (!p.equals(pIndex)) {
                            route.add(p);
                        }
                    }
                } else {
                    route = new ArrayList<>(parentGenes[i]);
                }
                childGenes[i] = route;
            }

            Chromosome c2Temp = new Chromosome(childGenes, 0.0, true);
            EvaluationFunctionUp.EvaluateFitness(c2Temp, data);


            if (patient.getRequired_caregivers().length > 1) {
                listOfMoves = new HashSet<>();

                boolean isSeq = patient.getSynchronization().getType().equals("sequential");
                //set the hashset of the chromosome genes
                c2Temp.buildPatientRouteMap();

                for (CaregiverPair caregiverPair : patient.getAllPossibleCaregiverCombinations()) {
                    int k = caregiverPair.getFirst();
                    int l = caregiverPair.getSecond();
                    for (int m = 0; m <= childGenes[k].size(); m++) {
                        for (int n = 0; n <= childGenes[l].size(); n++) {
                            if (isSeq || noEvaluationConflicts(childGenes[k], childGenes[l], m, n)) {
                                ArrayList<String> tempRoute1 = new ArrayList<>(childGenes[k]);
                                ArrayList<String> tempRoute2 = new ArrayList<>(childGenes[l]);
                                tempRoute1.add(m, pIndex);
                                tempRoute2.add(n, pIndex);
                                ArrayList<Process> processes = new ArrayList<>();
                                String moveSign1 = tempRoute1 + " - " + tempRoute2;
                                String moveSign2 = tempRoute2 + " - " + tempRoute1;
                                if (!listOfMoves.contains(moveSign1) && !listOfMoves.contains(moveSign2)) {
                                    Process process = new org.example.GA.Process(tempRoute1, pIndex, m, k);
                                    processes.add(process);
                                    process = new org.example.GA.Process(tempRoute2, pIndex, n, l);
                                    processes.add(process);
                                    MoveUp move1 = new MoveUp(processes);
                                    bestMove = evaluateMoveUp(move1, bestMove, c2Temp,bestFitness);
                                    listOfMoves.add(moveSign1);
                                }
                            }
                        }
                    }
                }


//                if (bestMove != null) {
//                    ArrayList<org.example.GA.Process> processesList = bestMove.getProcesses();
//                    for (int z = 0; z < processesList.size(); z++) {
//                        org.example.GA.Process process1 = processesList.get(z);
//                        c1Routes[process1.getRouteIndex()] = new ArrayList<>(process1.getRoute());
//                    }
//                    c2Temp = bestMove.getChromosome();
//                }

            } else {
                 Set<Integer> caregivers1 = patient.getPossibleFirstCaregiver();
                //set the Map patient ot route of the chromosome genes
                c2Temp.buildPatientRouteMap();
                for (int j : caregivers1) {
                    for (int k = 0; k <= childGenes[j].size(); k++) {
                        ArrayList<String>tempRoute1 = new ArrayList(childGenes[j]);
                        tempRoute1.add(k, pIndex);
                        ArrayList<Process>processes = new ArrayList<>();
                        Process process = new org.example.GA.Process(tempRoute1, pIndex, k, j);
                        processes.add(process);
                        MoveUp move1 = new MoveUp(processes);
                        bestMove = evaluateMoveUp(move1, bestMove, c2Temp,bestFitness);
                    }

                }

//                if (bestMove != null) {
//                    ArrayList<org.example.GA.Process> processesList = bestMove.getProcesses();
//                    for (int z = 0; z < processesList.size(); z++) {
//                        Process process1 = processesList.get(z);
//                        c1Routes[process1.getRouteIndex()] = new ArrayList<>(process1.getRoute());
//                    }
//                    c2Temp = bestMove.getChromosome();
//                }
            }
        }
        if (bestMove != null) {
            return bestMove.getChromosome();
        } else {
            System.out.println("Tabu does not exist: ");
            return null;
        }
    }

    private MoveUp evaluateMoveUp(MoveUp m, MoveUp bestMove, Chromosome c, double bestFitness) {
        Chromosome tempCh = new Chromosome(c.getGenes(), 0.0, true);
        int[] routeEndPoint = new int[c.getGenes().length];
        Arrays.fill(routeEndPoint, -1);
        ArrayList<Process> processesList = m.getProcesses();
        int[] pIndex = new int[processesList.size()];
        for (int z = 0; z < processesList.size(); z++) {
            Process process = processesList.get(z);
            routeEndPoint[process.getRouteIndex()] = process.getInsertPosition();
            pIndex[z]=getIdOfObject(process.getPatient());
        }


        removeAffectedPatientsUp(m, c, routeEndPoint);
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
        for (int z = 0; z < processesList.size(); z++) {
            Process process = processesList.get(z);
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

        long unique =getUniqueNumber(pIndex[0],pIndex[0],"move");
        tempCh.setMoveID(unique);

        evaluateUp(tempCh, routeEndPoint, bestMove);
//        System.out.println("P"+(pIndex[0]+1)+" - "+tempCh);
        if(!tabuList.contains(tempCh.getMoveID())||tempCh.getFitness()<bestFitness) {
            if (bestMove == null || tempCh.getFitness() < bestMove.getFitness()||tempCh.getFitness() == bestMove.getFitness()&&rand.nextBoolean()) {
                m.setChromosome(tempCh);
                m.setFitness(tempCh.getFitness());
                return m;
            }
        }

        return bestMove;
    }

    private void removeAffectedPatientsUp(MoveUp r, Chromosome c, int[] affectedRoutes) {
        int startPos;
        String patientId;
        Map<String, Set<Integer>> patientToRoutesMap = c.getPatientToRoutesMap();
        ArrayList<Process> processList = r.getProcesses();
        for (int z = 0; z < processList.size(); z++) {
            Process process = processList.get(z);
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
                        removeAffectedPatientsUp(new MoveUp(processBuffer), c, affectedRoutes);
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
    private boolean noEvaluationConflicts(ArrayList<String> c1Route, ArrayList<String> c2Route, int m, int n) {
        return conflictCheck(c1Route, c2Route, m, n);
    }

    private void evaluateUp(Chromosome ch, int[] routeEndPoint, MoveUp bestMove) {
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
                        if (!patientAssignment(ch, patient, caregiver1, routes, i, track)) {
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


//    private Chromosome swapNeighbourhood(Chromosome tempChromosome, double bestFitness) {
//        List<Integer> parentGenes = tempChromosome.getGenes();
//        CaregiverPair[] parentCaregivers = tempChromosome.getCaregivers();
//        List<Integer> genes = new ArrayList<>(parentGenes);
//        CaregiverPair[] caregivers = new CaregiverPair[chromosomeLength];
//        System.arraycopy(parentCaregivers, 0, caregivers, 0, chromosomeLength);
//
//        int bestGene1 = -1, bestGene2 = -1, bestPosition1 = -1, bestPosition2 = -1;
//        CaregiverPair bestCaregiver1 = null, bestCaregiver2 = null;
//        long bestUnique = 0, tempUnique = 0;
//        double localBestFitness = Double.MAX_VALUE;
//        Chromosome c2Temp = new Chromosome(genes, 0.0, caregivers, caregiverCount, false, true);
//        for (int i = 0; i < chromosomeLength - 1; i++) {
//            int gene1 = genes.get(i);
//            for (int y = i + 1; y < chromosomeLength; y++) {
//                int gene2 = genes.get(y);
////                System.out.println(gene1+" - "+gene2);
////                System.out.println("before: "+genes);
//                Patient patient1 = allPatients[gene1];
//                Patient patient2 = allPatients[gene2];
//                List<CaregiverPair> caregiverPairs1 = patient1.getAllPossibleCaregiverCombinations();
//                List<CaregiverPair> caregiverPairs2 = patient2.getAllPossibleCaregiverCombinations();
//                CaregiverPair oldCaregiver1 = caregivers[gene1];
//                CaregiverPair oldCaregiver2 = caregivers[gene2];
//                genes.set(i, gene2);
//                genes.set(y, gene1);
//                for (int x = 0; x < caregiverPairs1.size(); x++) {
//                    CaregiverPair caregiverPair1 = caregiverPairs1.get(x);
//                    for (int j = 0; j < caregiverPairs2.size(); j++) {
//                        CaregiverPair caregiverPair2 = caregiverPairs2.get(j);
//                        c2Temp.setGenes(genes);
//                        c2Temp.setCaregivers(caregiverPair1, gene1);
//                        c2Temp.setCaregivers(caregiverPair2, gene2);
//                        tempUnique = getUniqueNumber(gene1, gene2, "swap");
//                        EvaluationFunction.EvaluateFitness(c2Temp);
//                        if (!tabuList.contains(c2Temp.getMoveID()) || c2Temp.getFitness() < bestFitness) {
//                            if (bestGene1 == -1 || c2Temp.getFitness() < localBestFitness) {
//                                bestGene1 = gene1;
//                                bestGene2 = gene2;
//                                bestPosition1 = i;
//                                bestPosition2 = y;
//                                bestCaregiver1 = caregiverPair1;
//                                bestCaregiver2 = caregiverPair2;
//                                bestUnique = tempUnique;
//                                localBestFitness = c2Temp.getFitness();
//                            }
//                        }
//                    }
//                }
//                genes.set(i, gene1);
//                genes.set(y, gene2);
//                c2Temp.setCaregivers(oldCaregiver1, gene1);
//                c2Temp.setCaregivers(oldCaregiver2, gene2);
////                System.out.println(genes);
////                System.exit(1);
//            }
//        }
//
//        if (bestCaregiver1 != null) {
//            genes.set(bestPosition1, bestGene2);
//            genes.set(bestPosition2, bestGene1);
//            c2Temp.setGenes(genes);
//            c2Temp.setCaregivers(bestCaregiver1, bestGene1);
//            c2Temp.setCaregivers(bestCaregiver2, bestGene2);
//            EvaluationFunction.EvaluateFitness(c2Temp);
//            c2Temp.setMoveID(bestUnique);
//            return c2Temp;
//        } else {
//            System.out.println("Tabu does not exist: ");
//            return null;
//        }
//    }


//    private Chromosome swapLocalSearch(Chromosome tempChromosome, int tabuLength, double bestFitness) {
//        List<Integer> parentGenes = tempChromosome.getGenes();
//        CaregiverPair[] parentCaregivers = tempChromosome.getCaregivers();
//        List<Integer> genes = new ArrayList<>(parentGenes);
//        CaregiverPair[] caregivers = new CaregiverPair[chromosomeLength];
//        System.arraycopy(parentCaregivers, 0, caregivers, 0, chromosomeLength);
//        long bestUnique = -1;
//        int bestGene1 = -1, bestGene2 = -1, bestPosition1 = -1, bestPosition2 = -1;
//        CaregiverPair bestCaregiver1 = null;
//        double localBestFitness = Double.MAX_VALUE;
//        boolean isTabu;
//        Chromosome c2Temp = new Chromosome(genes, 0.0, caregivers, caregiverCount, false, true);
//        for (int i = 0; i < chromosomeLength - 1; i++) {
//            int gene1 = genes.get(i);
//            for (int y = i + 1; y < chromosomeLength; y++) {
//                int gene2 = genes.get(y);
//                long index = getUniqueNumber(gene1, gene2, "swap");
//                isTabu = swapTabuSet.contains(index);
//                Patient patient = allPatients[gene1];
//                List<CaregiverPair> caregiverPairs = patient.getAllPossibleCaregiverCombinations();
//                CaregiverPair oldCaregiver = caregivers[gene1];
//                genes.set(i, gene2);
//                genes.set(y, gene1);
//                c2Temp.setGenes(genes);
//                for (int x = 0; x < caregiverPairs.size(); x++) {
//                    CaregiverPair caregiverPair = caregiverPairs.get(x);
////                    if(position>0){
////                        CaregiverPair tempCaregiverPair = caregivers[genes.get(position-1)];
////                        int first = tempCaregiverPair.getFirst();
////                        int cFirst = caregiverPair.getFirst();
////                        int second = tempCaregiverPair.getSecond();
////                        int cSecond = caregiverPair.getSecond();
////                        boolean firstCheck = first!=cFirst && first!=cSecond;
////                        boolean secondCheck = second!=cFirst && second!=cSecond;
////                        boolean thirdCheck = second==cSecond&&second==-1;
////                        if(firstCheck&&secondCheck||firstCheck&&thirdCheck){
////                            continue;
////                        }
////
//                    c2Temp.setCaregivers(caregiverPair, gene1);
//                    EvaluationFunction.EvaluateFitness(c2Temp);
//                    if (isTabu && c2Temp.getFitness() < bestFitness || !isTabu && c2Temp.getFitness() < localBestFitness) {
//                        bestGene1 = gene1;
//                        bestGene2 = gene2;
//                        bestPosition1 = i;
//                        bestUnique = index;
//                        bestPosition2 = y;
//                        bestCaregiver1 = caregiverPair;
//                    }
//                }
//                c2Temp.setCaregivers(oldCaregiver, gene1);
//                genes.set(i, gene1);
//                genes.set(y, gene2);
//            }
//        }
//
//        //Update tabu
//        List<Long> swapList = new ArrayList<>();
//        for (long p : swapTabuSet) {
//            int value = swapTabu.get(p);
//            value--;
//            if (value == 0) {
//                swapList.add(p);
//            }
//            swapTabu.put(p, value);
//        }
////        System.out.println("Set: "+moveTabuSet);
////            System.exit(1);
//        for (int i = 0; i < swapList.size(); i++) {
//            swapTabuSet.remove(swapList.get(i));
//        }
//
//        if (bestCaregiver1 != null) {
////            System.out.println("Best caregiver: ");
//            genes.set(bestPosition1, bestGene2);
//            genes.set(bestPosition2, bestGene1);
//            c2Temp.setGenes(genes);
//            c2Temp.setCaregivers(bestCaregiver1, bestGene1);
//            EvaluationFunction.EvaluateFitness(c2Temp);
//
//            swapTabuSet.add(bestUnique);
//            swapTabu.put(bestUnique, tabuLength);
//
//            return c2Temp;
//        } else {
////            System.out.println("Tabu does not exist");
//            return tempChromosome;
//        }
//    }

    private Chromosome inRouteSwapSearch(Chromosome tempChromosome, int tabuLength) {
        return tempChromosome;
    }

    private long getUniqueNumber(int a, int b, String name) {
        if (name.equals("move")) {
            return -1 - a;
        } else {
            int x = Math.min(a, b);
            int y = Math.max(a, b);
            return ((long) (x + y) * (x + y + 1)) / 2 + y;
        }
    }

    private int mapToNonNegative(int a) {
        return a >= 0 ? 2 * a : -2 * a - 1;
    }
}

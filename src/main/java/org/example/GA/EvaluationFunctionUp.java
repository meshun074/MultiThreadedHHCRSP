package org.example.GA;

import org.example.Data.InstancesClass;
import org.example.Data.Patient;
import org.example.Data.Required_Caregiver;

import java.util.*;

public class EvaluationFunctionUp {
    private static InstancesClass dataset;

    public static void EvaluateFitness(List<Chromosome> population, InstancesClass data) {
        dataset = data;
        for (Chromosome chromosome : population) {
            Evaluate(chromosome);
        }
    }

    private static void Evaluate(Chromosome ch) {
        // Testing
//        ch.showSolution(78);
//        System.out.println("Fitness: yieeee" );

        ShiftUp[] routes = new ShiftUp[ch.getCaregivers()];
        //initializing caregivers shift.
        initializeRoutes(routes);
        ch.setCaregiversRouteUp(routes);
        ch.setHighestTardiness(0);
        ch.setTotalTardiness(0);
        ch.setTotalTravelCost(0);
        ch.setFitness(Double.POSITIVE_INFINITY);
        ArrayList<String> route;
        ShiftUp caregiver1;
        Set<String> track = new LinkedHashSet<>();
        Map<String,List<Integer>> sycTrack = new HashMap<>();
        int simCounter = 0;
        for (int i = 0; i < routes.length; i++) {
            route = new ArrayList<>(ch.getGenes()[i]);
            caregiver1 = routes[i];
            for (String patient : route) {
                if (!caregiver1.getRoute().contains(patient)) {
                    if (!patientAssignment(ch, patient, caregiver1, routes, i, track, sycTrack,simCounter)) {
                        ch.setFitness(Double.POSITIVE_INFINITY);
                        return;
                    }
                    track.clear();
                    sycTrack.clear();
                }
            }
        }
        for (ShiftUp s : routes) {
            ch.updateTotalTravelCost(dataset.getDistances()[getIdOfObjectLocation(s.getRoute().getLast())][0]);
            s.updateTravelCost(dataset.getDistances()[getIdOfObjectLocation(s.getRoute().getLast())][0]);
        }
        UpdateCost(ch);
//        ch.showSolution(89);
//        int c=0;
//        for(ShiftUp s : ch.getCaregiversRouteUp()) {
//            System.out.println("Caregiver "+ c);
//            s.showInfo();
//            c++;
//        }
//        System.exit(1);
    }

    private static void UpdateCost(Chromosome ch) {
        ch.setFitness((1 / 3d * ch.getTotalTravelCost()) + (1 / 3d * ch.getTotalTardiness()) + (1 / 3d * ch.getHighestTardiness()));
    }

    static boolean patientAssignment(Chromosome ch, String patient, ShiftUp caregiver1, ShiftUp[] routes, int i, Set<String> track, Map<String,List<Integer>> sycTrack,int simCounter) {
        double maxTardiness;
        double travelCost;
        double tardiness1, tardiness2;
        Patient[] allPatients = dataset.getPatients();
        double[][] distancesMatrix = dataset.getDistances();
        int index;
        ShiftUp caregiver2;
        Patient p = allPatients[getIdOfObject(patient)];
        double[] timeWindow = p.getTime_window();
        String lastLocation =caregiver1.getRoute().getLast();
        int currentLocation1 = getIdOfObjectLocation(lastLocation);
        int nextLocation = getIdOfObjectLocation(p.getId());
        double arrivalTime1 = caregiver1.getCurrentTime().getLast() + dataset.getDistances()[currentLocation1][nextLocation];
        double startTime1 = Math.max(arrivalTime1, timeWindow[0]);
        if (p.getRequired_caregivers().length > 1) {
            String synchronousServiceType = p.getSynchronization().getType();
            if (track.contains(patient)) {
//                if(simCounter==track.size()){
//                    //System.out.println("Schedule conflict");
//                    return false;
//                }
//                if(track.size()>3)
//                    return false;
//                Map<String, Integer> map = verifyFirstSequentialService(track, sycTrack,allPatients, ch);
//                if(map==null) {
//                    return false;
//                }
//                //System.out.println("Schedule conflict");
//                return  solveScheduleConflict(track,map, routes,sycTrack, ch,allPatients,distancesMatrix);
//                //System.out.println(ch.toString()+" - "+ch.getFitness());
//                //System.out.println("yh");
//                //System.exit(1);
                return false;
            }

//            sycTrack.put(patient,new ArrayList<>(2));
//            if (synchronousServiceType.equals("simultaneous")) {
//                simCounter++;
//                sycTrack.get(patient).add(0);
//            }else
//                sycTrack.get(patient).add(1);
            track.add(patient);
            index = findSecondCaregiver(p, i, routes, ch, track,sycTrack,simCounter);
            if (index > dataset.getCaregivers().length - 1) {
                return false;
            }
            caregiver2 = routes[index];

//            boolean route1Assigned = caregiver1.getRoute().contains(patient);
//            boolean route2Assigned = caregiver2.getRoute().contains(patient);
//
//            if(route1Assigned && route2Assigned){
//                return true;
//            }

            //check for the first and second route based on assignment
            //find position of service 1 and 2
            Required_Caregiver[] requiredCaregiverServices = p.getRequired_caregivers();
            String service1 = requiredCaregiverServices[0].getService();
            String service2 = requiredCaregiverServices[1].getService();
            Set<Integer> service1RoutesList = dataset.getQualifiedCaregiver(service1);
            Set<Integer> service2RoutesList = dataset.getQualifiedCaregiver(service2);

            int caregiver2Id = caregiver2.getCaregiver().getCacheId();
            int caregiver1Id = caregiver1.getCaregiver().getCacheId();

            boolean condition1 = service1RoutesList.contains(caregiver2Id) && !service2RoutesList.contains(caregiver2Id);
            boolean condition2 = service2RoutesList.contains(caregiver1Id) && !service1RoutesList.contains(caregiver1Id);
            boolean condition3 = service1RoutesList.contains(caregiver2Id) && !service1RoutesList.contains(caregiver1Id);

            if (condition1 || condition2 || condition3) {
                ShiftUp temp = caregiver1;
                caregiver1 = caregiver2;
                caregiver2 = temp;

                //Recalculate since you have made a swap
                lastLocation = caregiver1.getRoute().getLast();
                currentLocation1 = getIdOfObjectLocation(lastLocation);
                arrivalTime1 = caregiver1.getCurrentTime().getLast() + distancesMatrix[currentLocation1][nextLocation];
                startTime1 = Math.max(arrivalTime1, timeWindow[0]);
            }


            int currentLocation2 = getIdOfObjectLocation(caregiver2.getRoute().getLast());
            double arrivalTime2 = caregiver2.getCurrentTime().getLast() + distancesMatrix[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, timeWindow[0]);

            if (p.getSynchronization().getType().equals("sequential")) {
                double[] syncDistances = p.getSynchronization().getDistance();

//                if(route1Assigned ^ route2Assigned){
////                    System.out.println("Aye asem oo ");
//                    ArrayList<String> c1Routes = caregiver1.getRoute();
//                    ArrayList<Double> c1Time = caregiver1.getCurrentTime();
//                    int indexOfFirstAssigned =c1Routes.size() - c1Routes.indexOf(patient)-1;
//                    indexOfFirstAssigned = c1Time.size()- 1 - indexOfFirstAssigned;
//                    startTime1 = c1Time.get(indexOfFirstAssigned) - requiredCaregiverServices[0].getDuration();
//                    startTime2 = Math.max(startTime2, startTime1 + syncDistances[0]);
//                    if (startTime2 - startTime1 > syncDistances[1]) {
//                        return false;
//                    }
//                    tardiness2 = Math.max(0, startTime2 - timeWindow[1]);
//                    ch.updateTotalTardiness(tardiness2);
//                    ch.setHighestTardiness(Math.max(tardiness2, ch.getHighestTardiness()));
//                    caregiver2.setCurrentTime(startTime2 + requiredCaregiverServices[1].getDuration());
//                    caregiver2.updateTardiness(tardiness2);
//                    travelCost = distancesMatrix[currentLocation2][nextLocation];
//                    ch.updateTotalTravelCost(travelCost);
//                    caregiver2.updateRoute(p.getId());
//                    caregiver2.updateTravelCost(distancesMatrix[currentLocation2][nextLocation]);
//                    return true;
//                }


                startTime2 = Math.max(startTime2, startTime1 + syncDistances[0]);
                if (startTime2 - startTime1 > syncDistances[1]) {
                    startTime1 = startTime2 - syncDistances[1];
                }
                tardiness1 = Math.max(0, startTime1 - timeWindow[1]);
                tardiness2 = Math.max(0, startTime2 - timeWindow[1]);
                maxTardiness = Math.max(tardiness1, tardiness2);
                ch.updateTotalTardiness(tardiness1 + tardiness2);
                ch.setHighestTardiness(Math.max(maxTardiness, ch.getHighestTardiness()));
                caregiver1.setCurrentTime(startTime1 + requiredCaregiverServices[0].getDuration());
                caregiver1.updateTardiness(tardiness1);
                caregiver2.setCurrentTime(startTime2 + requiredCaregiverServices[1].getDuration());
                caregiver2.updateTardiness(tardiness2);
            } else {
                double startTime = Math.max(startTime1, startTime2);
                tardiness1 = Math.max(0, startTime - timeWindow[1]);
                tardiness2 = tardiness1;
                ch.setHighestTardiness(Math.max(tardiness1, ch.getHighestTardiness()));
                caregiver1.setCurrentTime(startTime + requiredCaregiverServices[0].getDuration());
                caregiver1.updateTardiness(tardiness1);
                caregiver2.setCurrentTime(startTime + requiredCaregiverServices[1].getDuration());
                caregiver2.updateTardiness(tardiness2);
                ch.updateTotalTardiness(tardiness1 + tardiness2);
            }
            travelCost = distancesMatrix[currentLocation1][nextLocation] + distancesMatrix[currentLocation2][nextLocation];
            ch.updateTotalTravelCost(travelCost);
            caregiver1.updateRoute(p.getId());
            caregiver1.updateTravelCost(distancesMatrix[currentLocation1][nextLocation]);
            caregiver2.updateRoute(p.getId());
            caregiver2.updateTravelCost(distancesMatrix[currentLocation2][nextLocation]);
        } else {
            tardiness1 = Math.max(0, startTime1 - timeWindow[1]);
            ch.setHighestTardiness(Math.max(tardiness1, ch.getHighestTardiness()));
            ch.updateTotalTardiness(tardiness1);
            travelCost = distancesMatrix[currentLocation1][nextLocation];
            ch.updateTotalTravelCost(travelCost);
            caregiver1.setCurrentTime(startTime1 + p.getRequired_caregivers()[0].getDuration());
            caregiver1.updateRoute(p.getId());
            caregiver1.updateTravelCost(travelCost);
            caregiver1.updateTardiness(tardiness1);
        }
        return true;
    }

    private static boolean solveScheduleConflict(Set<String> track, Map<String, Integer> startRoute, ShiftUp[] routes, Map<String, List<Integer>> sycTrack, Chromosome ch, Patient[] allPatients,double[][] distancesMatrix) {

        String patient = startRoute.keySet().iterator().next();
        ShiftUp caregiver1 = routes[startRoute.get(patient)];
        Patient p = allPatients[getIdOfObject(patient)];
        double[] timeWindow = p.getTime_window();
        String lastLocation =caregiver1.getRoute().getLast();
        int currentLocation1 = getIdOfObjectLocation(lastLocation);
        int nextLocation = getIdOfObjectLocation(p.getId());
        double arrivalTime1 = caregiver1.getCurrentTime().getLast() + dataset.getDistances()[currentLocation1][nextLocation];
        double startTime1 = Math.max(arrivalTime1, timeWindow[0]);

        double tardiness1 = Math.max(0, startTime1 - timeWindow[1]);
        ch.setHighestTardiness(Math.max(tardiness1, ch.getHighestTardiness()));
        ch.updateTotalTardiness(tardiness1);
        double travelCost = distancesMatrix[currentLocation1][nextLocation];
        ch.updateTotalTravelCost(travelCost);
        caregiver1.setCurrentTime(startTime1 + p.getRequired_caregivers()[0].getDuration());
        caregiver1.updateRoute(p.getId());
        caregiver1.updateTravelCost(travelCost);
        caregiver1.updateTardiness(tardiness1);

        //testing
////        ch.trail();
//        System.out.println("Schedule conflict resolving");
        List<Integer> patientRoutes;
        Set<String> newTrack = new LinkedHashSet<>();
        Map<String,List<Integer>> newSycTrack = new HashMap<>();
        int simCounter = 0;
        for(String s : track) {
            patientRoutes = sycTrack.get(s);
            caregiver1 = routes[patientRoutes.get(1)];
            ShiftUp caregiver2 = routes[patientRoutes.get(2)];
            if (!caregiver1.getRoute().contains(s)||!caregiver2.getRoute().contains(s)) {
                System.out.println("patient "+s+" route "+patientRoutes.get(1));
                if (!patientAssignment(ch, s, caregiver1, routes, patientRoutes.get(1), newTrack, newSycTrack,simCounter)) {
                    return false;
                }
                newTrack.clear();
                newSycTrack.clear();
            }
        }
//        System.out.println("Schedule conflict resolved");
//        ch.showSolution(0);
//        ch.trail();
//        System.exit(1);
        return true;
    }


    private static Map<String,Integer> verifyFirstSequentialService(Set<String> track, Map<String,List<Integer>>sycTrack, Patient[] allPatients,Chromosome ch) {
        for(String p : track) {
            if(sycTrack.get(p).getFirst()==1) {
//                System.out.println("Verify conflict: "+p);
                Patient patient = allPatients[getIdOfObject(p)];

                Required_Caregiver[] requiredCaregiverServices = patient.getRequired_caregivers();
                String service1 = requiredCaregiverServices[0].getService();
                String service2 = requiredCaregiverServices[1].getService();
                Set<Integer> service1RoutesList = dataset.getQualifiedCaregiver(service1);
                Set<Integer> service2RoutesList = dataset.getQualifiedCaregiver(service2);

                int caregiver1Id = sycTrack.get(p).get(1);
                int caregiver2Id = sycTrack.get(p).get(2);

                boolean condition1 = service1RoutesList.contains(caregiver2Id) && !service2RoutesList.contains(caregiver2Id);
                boolean condition2 = service2RoutesList.contains(caregiver1Id) && !service1RoutesList.contains(caregiver1Id);
                boolean condition3 = service1RoutesList.contains(caregiver2Id) && !service1RoutesList.contains(caregiver1Id);
                if (condition1 || condition2 || condition3){
                    caregiver1Id = caregiver2Id;
                }
                ArrayList<String> route = ch.getGenes()[caregiver1Id];
                int count=0;
                for(int i=0;i<route.indexOf(p);i++) {
                    if(track.contains(route.get(i))){
                        count++;
                        break;
                    }
                }
                if(count==0) {
                    Map<String,Integer> map = new HashMap<>();
                    map.put(p,caregiver1Id);
                    return map;
                }
            }
        }
        return null;
    }

    private static int findSecondCaregiver(Patient p, int route1, ShiftUp[] routes, Chromosome ch, Set<String> track, Map<String,List<Integer>> sycTrack, int simCounter) {
        ArrayList<String> route = null;
        ArrayList[] genes = ch.getGenes();
        int routeIndex = 0;
        int patientPositionInRoute;
        for (int i = 0; i < genes.length; i++) {
            if (i != route1&&genes[i].contains(p.getId())) {
                route = genes[i];
                routeIndex = i;
                break;
            }
        }
        if (route == null) {
            return Integer.MAX_VALUE;
        }
//        sycTrack.get(p.getId()).add(route1);
//        sycTrack.get(p.getId()).add(routeIndex);
        ShiftUp caregiver = routes[routeIndex];
        patientPositionInRoute = route.indexOf(p.getId());
        int i = caregiver.getRoute().size() - 1;
        while (caregiver.getRoute().size() - 1 < patientPositionInRoute && i < route.size()) {
            String patient = route.get(i);
            if (!patientAssignment(ch, patient, caregiver, routes, routeIndex, track, sycTrack,simCounter))
                return Integer.MAX_VALUE;
            i++;
        }
        return routeIndex;
    }
    private static int findSecondCaregiver1(Patient p, int route1, ShiftUp[] routes, Chromosome ch, Set<String> track,Map<String,List<Integer>> sycTrack, int simCounter) {
        ArrayList[] genes = ch.getGenes();
        int routeIndex = 0;
        for (int i = 0; i < genes.length; i++) {
            if (i != route1&&genes[i].contains(p.getId())) {
                ArrayList<String> route = genes[i];
                ShiftUp caregiver = routes[i];
                int patientPositionInRoute = route.indexOf(p.getId());
                routeIndex = i;
                int start = caregiver.getRoute().size() - 1;
                // Check if patient is at the end of route (no need to process)
                if (patientPositionInRoute == start) {
                    return i;
                }
                for(int j = start; j<route.size();j++) {
                    String patient = route.get(j);
                    if (!patientAssignment(ch, patient, caregiver, routes, routeIndex, track, sycTrack,simCounter))
                        return Integer.MAX_VALUE;
                    if (patientPositionInRoute == caregiver.getRoute().size() - 1) {
                        return i;
                    }
                }
                return i;
            }
        }

        return Integer.MAX_VALUE;
    }

    private static void initializeRoutes(ShiftUp[] routes) {
        for (int s = 0; s < routes.length; s++)
            //Initialize the shift of the caregivers
            routes[s] = new ShiftUp(dataset.getCaregivers()[s], new ArrayList<>() {{
                add("d0");
            }}, 0.0);
    }

    //    static int getIdOfObject(String s) {
//        return Integer.parseInt(s.substring(1)) - 1;
//    }
    static int getIdOfObject(String s) {
        // Skip first char and parse the rest
        int id = 0;
        for (int i = 1; i < s.length(); i++) {
            id = id * 10 + (s.charAt(i) - '0');
        }
        return id - 1;
    }

    //gets the index of a location of a patient or depot
    private static int getIdOfObjectLocation(String s) {
        return Integer.parseInt(s.substring(1));
    }

}


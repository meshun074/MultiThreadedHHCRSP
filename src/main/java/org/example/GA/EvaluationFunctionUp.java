package org.example.GA;

import org.example.Data.Caregiver;
import org.example.Data.InstancesClass;
import org.example.Data.Patient;

import java.util.ArrayList;
import java.util.List;

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
        ArrayList<String> track = new ArrayList<>();
        for (int i = 0; i < routes.length; i++) {
            route = new ArrayList<>(ch.getGenes()[i]);
            caregiver1 = routes[i];
            for (String patient : route) {
                if (!caregiver1.getRoute().contains(patient)) {
                    if (!patientAssignment(ch, patient, caregiver1, routes, i, track)) {
                        ch.setFitness(Double.POSITIVE_INFINITY);
                        return;
                    }
                    track = new ArrayList<>();
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

    static boolean patientAssignment(Chromosome ch, String patient, ShiftUp caregiver1, ShiftUp[] routes, int i, ArrayList<String> track) {
        double maxTardiness;
        double travelCost;
        double tardiness1;
        double tardiness2;
        int index;
        ShiftUp caregiver2;
        Patient p = dataset.getPatients()[getIdOfObject(patient)];
        int currentLocation1 = getIdOfObjectLocation(caregiver1.getRoute().getLast());
        int nextLocation = getIdOfObjectLocation(p.getId());
        double arrivalTime1 = caregiver1.getCurrentTime().getLast() + dataset.getDistances()[currentLocation1][nextLocation];
        double startTime1 = Math.max(arrivalTime1, p.getTime_window()[0]);
        if (p.getRequired_caregivers().length > 1) {
            if (track.contains(patient)) {
                return false;
            }
            track.add(patient);
            index = findSecondCaregiver(p, i, routes, ch, track);
            if (index > dataset.getCaregivers().length - 1) {
                return false;
            }
            caregiver2 = routes[index];
            //check for the first and second route based on assignment
            //find position of service 1 and 2
            String service1 = p.getRequired_caregivers()[0].getService();
            String service2 = p.getRequired_caregivers()[1].getService();
            ArrayList<String> service1RoutesList = getQualifiedCaregiver(service1);
            ArrayList<String> service2RoutesList = getQualifiedCaregiver(service2);

            if (service1RoutesList.contains(caregiver2.getCaregiver().getId()) && !service2RoutesList.contains(caregiver2.getCaregiver().getId())
                    || service2RoutesList.contains(caregiver1.getCaregiver().getId()) && !service1RoutesList.contains(caregiver1.getCaregiver().getId())
                    || service1RoutesList.contains(caregiver2.getCaregiver().getId()) && !service1RoutesList.contains(caregiver1.getCaregiver().getId())) {
                ShiftUp temp = caregiver1;
                caregiver1 = caregiver2;
                caregiver2 = temp;

                //initialize again since you have made a swap
                currentLocation1 = getIdOfObjectLocation(caregiver1.getRoute().getLast());
                arrivalTime1 = caregiver1.getCurrentTime().getLast() + dataset.getDistances()[currentLocation1][nextLocation];
                startTime1 = Math.max(arrivalTime1, p.getTime_window()[0]);
            }


            int currentLocation2 = getIdOfObjectLocation(caregiver2.getRoute().getLast());
            double arrivalTime2 = caregiver2.getCurrentTime().getLast() + dataset.getDistances()[currentLocation2][nextLocation];
            double startTime2 = Math.max(arrivalTime2, p.getTime_window()[0]);

            if (p.getSynchronization().getType().equals("sequential")) {
                startTime2 = Math.max(startTime2, startTime1 + p.getSynchronization().getDistance()[0]);
                if (startTime2 - startTime1 > p.getSynchronization().getDistance()[1])
                    startTime1 = startTime2 - p.getSynchronization().getDistance()[1];
                tardiness1 = Math.max(0, startTime1 - p.getTime_window()[1]);
                tardiness2 = Math.max(0, startTime2 - p.getTime_window()[1]);
                maxTardiness = Math.max(tardiness1, tardiness2);
                ch.updateTotalTardiness(tardiness1 + tardiness2);
                ch.setHighestTardiness(Math.max(maxTardiness, ch.getHighestTardiness()));
                caregiver1.setCurrentTime(startTime1 + p.getRequired_caregivers()[0].getDuration());
                caregiver1.updateTardiness(tardiness1);
                caregiver2.setCurrentTime(startTime2 + p.getRequired_caregivers()[1].getDuration());
                caregiver2.updateTardiness(tardiness2);
            } else {
                double startTime = Math.max(startTime1, startTime2);
                tardiness1 = Math.max(0, startTime - p.getTime_window()[1]);
                tardiness2 = tardiness1;
                ch.setHighestTardiness(Math.max(tardiness1, ch.getHighestTardiness()));
                caregiver1.setCurrentTime(startTime + p.getRequired_caregivers()[0].getDuration());
                caregiver1.updateTardiness(tardiness1);
                caregiver2.setCurrentTime(startTime + p.getRequired_caregivers()[1].getDuration());
                caregiver2.updateTardiness(tardiness2);
                ch.updateTotalTardiness(tardiness1 + tardiness2);
            }
            travelCost = dataset.getDistances()[currentLocation1][nextLocation] + dataset.getDistances()[currentLocation2][nextLocation];
            ch.updateTotalTravelCost(travelCost);
            caregiver1.updateRoute(p.getId());
            caregiver1.updateTravelCost(dataset.getDistances()[currentLocation1][nextLocation]);
            caregiver2.updateRoute(p.getId());
            caregiver2.updateTravelCost(dataset.getDistances()[currentLocation2][nextLocation]);
        } else {
            tardiness1 = Math.max(0, startTime1 - p.getTime_window()[1]);
            ch.setHighestTardiness(Math.max(tardiness1, ch.getHighestTardiness()));
            ch.updateTotalTardiness(tardiness1);
            travelCost = dataset.getDistances()[currentLocation1][nextLocation];
            ch.updateTotalTravelCost(travelCost);
            caregiver1.setCurrentTime(startTime1 + p.getRequired_caregivers()[0].getDuration());
            caregiver1.updateRoute(p.getId());
            caregiver1.updateTravelCost(travelCost);
            caregiver1.updateTardiness(tardiness1);
        }
        return true;
    }

    private static int findSecondCaregiver(Patient p, int route1, ShiftUp[] routes, Chromosome ch, ArrayList<String> track) {
        ArrayList<String> route = null;
        ArrayList[] genes = ch.getGenes();
        int routeIndex = 0;
        int patientPositionInRoute;
        for (int i = 0; i < genes.length; i++) {
            if (i != route1) {
                if (genes[i].contains(p.getId())) {
                    route = new ArrayList<>(genes[i]);
                    routeIndex = i;
                    break;
                }
            }
        }
        assert route != null;
        ShiftUp caregiver = routes[routeIndex];
        patientPositionInRoute = route.indexOf(p.getId());
        int i = caregiver.getRoute().size() - 1;
        while (caregiver.getRoute().size() - 1 != patientPositionInRoute && i < route.size()) {
            String patient = route.get(i);
            if (!patientAssignment(ch, patient, caregiver, routes, routeIndex, track))
                return Integer.MAX_VALUE;
            i++;
        }

        return routeIndex;
    }

    private static void initializeRoutes(ShiftUp[] routes) {
        for (int s = 0; s < routes.length; s++)
            //Initialize the shift of the caregivers
            routes[s] = new ShiftUp(dataset.getCaregivers()[s], new ArrayList<>() {{
                add("d0");
            }}, 0.0);
    }

    static int getIdOfObject(String s) {
        return Integer.parseInt(s.substring(1)) - 1;
    }

    //gets the index of a location of a patient or depot
    private static int getIdOfObjectLocation(String s) {
        return Integer.parseInt(s.substring(1));
    }

    private static ArrayList<String> getQualifiedCaregiver(String service) {
        ArrayList<String> caregivers = new ArrayList<>();
        for (Caregiver c : dataset.getCaregivers()) {
            if (c.getAbilities().contains(service)) {
                caregivers.add(c.getId());
            }
        }
        return caregivers;
    }
}

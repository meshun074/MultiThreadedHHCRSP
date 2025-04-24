package org.example.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstancesClass {
    private Patient[] patients;
    private Service[] services;
    private Caregiver[] caregivers;
    private Offices[] central_offices;
    private double[][] distances;
    private static final Map<String, Set<Integer>> SERVICE_CAREGIVER_CACHE = new ConcurrentHashMap<>();

    public double[][] getDistances() {
        return distances;
    }

    public Offices[] getCentral_offices() {
        return central_offices;
    }

    public Patient[] getPatients() {
        return patients;
    }

    public Caregiver[] getCaregivers() {
        return caregivers;
    }

    //The method should be called by the jackson package after deserialization
    public void setCaregivers(Caregiver[] caregivers) {
        this.caregivers = caregivers;
        initializeCaregiverCache();
    }
    // Initialize the cache at startup or when dataset changes
    private void initializeCaregiverCache() {
        SERVICE_CAREGIVER_CACHE.clear();
        for (Caregiver c : caregivers) {
            for (String ability : c.getAbilities()) {
                SERVICE_CAREGIVER_CACHE
                        .computeIfAbsent(ability, k -> new HashSet<>())
                        .add(c.getCacheId());
            }
        }
        // Make cache immutable
        SERVICE_CAREGIVER_CACHE.replaceAll((k, v) -> Collections.unmodifiableSet(v));
    }
    public Set<Integer> getQualifiedCaregiver(String service) {
        // Return cached result if available
        Set<Integer> cached = SERVICE_CAREGIVER_CACHE.get(service);
        if (cached != null) {
            return cached;
        }

        // Fallback to computation if not in cache (shouldn't happen if cache was initialized)
        Set<Integer> caregiverList = new HashSet<>();
        for (Caregiver c : caregivers) {
            if (c.getAbilities().contains(service)) {
                caregiverList.add(c.getCacheId());
            }
        }
        Set<Integer> immutableSet = Collections.unmodifiableSet(caregiverList);
        SERVICE_CAREGIVER_CACHE.put(service, immutableSet);
        return immutableSet;
    }

    public Service[] getServices() {
        return services;
    }

}

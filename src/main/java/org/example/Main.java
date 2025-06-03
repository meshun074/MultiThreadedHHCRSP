package org.example;

import org.example.Data.InstancesClass;
import org.example.Data.ReadData;
import org.example.GA.*;

import java.io.File;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static InstancesClass instance;
    public static long startTime;
    public static CPUTimer timer =new CPUTimer();
    /*
    Usage: main arg1-int:parameter settings (0-65) arg2-int:problem size (0-6) arg3-int:instance number (1-10) arg4-int:seed
     */

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java GeneticAlgorithmRunner <config-file>");
            return;
        }
        for (int i = 0; i < 1; i++) {
            timer.start();
            startTime = System.currentTimeMillis();
            long endTime;
            long averageTime;
            long randomSeed;
            long instanceNumber;
            String instanceName;
            Parameters p;

            try {
                // Read configuration file
                File configFile = new File(args[0]);
                if(args[0].contains("HHCRSP")){
                    Config1 config1 = Config1.read(configFile);
                    instanceNumber = config1.getInstanceIndex();
                    instanceName = config1.getInstanceName();
                    int runCount = Integer.parseInt(args[1]);
                    randomSeed = System.currentTimeMillis() + runCount;
                    List<Parameters> parameters = getParametersList();
                    p = parameters.get(44);
                    //create result directory
                    String resultDir = "src/main/java/org/example/Config_" + instanceName + "_" + instanceNumber + "_results";
                    new File(resultDir).mkdirs();
                    // Read dataset
                    PrintStream fileout = new PrintStream(resultDir + "/Result_" + instanceName + "_" + instanceNumber + "_" + runCount + "_" + p.selectionTechnique() + "_" + p.crossoverType() + "_" + p.mutationType() + "_" + p.mutationRate() + "_" + randomSeed + ".txt");
                    System.setOut(fileout);
                    System.out.printf("Config Parameters: parameterIndex=44, instanceNumber=%d, seed=%d\n", instanceNumber, randomSeed);

                    instance = ReadData.read(new File("src/main/java/org/example/Data/kummer/" + instanceName));

                }else
                {
                    Config config = Config.read(configFile);

                    // Extract parameters from JSON
                    int paramIndex = config.getParameterIndex();
                    int problemSize = config.getProblemSize();
                    instanceNumber = config.getInstanceIndex();

                    int runCount = Integer.parseInt(args[1]);
                    randomSeed = System.currentTimeMillis() + runCount;

                    List<Parameters> parameters = getParametersList();
                    p = parameters.get(paramIndex);
                    String[] Instances = {"10", "25", "50", "75", "100", "200", "300"};
                    instanceName = Instances[problemSize];

                    //create result directory
                    String resultDir = "src/main/java/org/example/Config_" + paramIndex + "_" + problemSize + "_" + instanceNumber + "_results";
                    new File(resultDir).mkdirs();

                    // Read dataset
                    PrintStream fileout = new PrintStream(resultDir + "/Result_" + instanceName + "_" + instanceNumber + "_" + runCount + "_" + p.selectionTechnique() + "_" + p.crossoverType() + "_" + p.mutationType() + "_" + p.mutationRate() + "_" + randomSeed + ".txt");
                    System.setOut(fileout);
                    System.out.printf("Config Parameters: parameterIndex=%d, ProblemSize=%d, instanceNumber=%d, seed=%d\n", paramIndex, problemSize, instanceNumber, randomSeed);

                    instance = ReadData.read(new File("src/main/java/org/example/Data/instance/" + instanceName + "_" + instanceNumber + ".json"));
                }

                // GA execution setup
                double total = 0;
                double best = Double.MAX_VALUE;

                GeneticAlgorithm ga = new GeneticAlgorithm(randomSeed, 6, 10, 4, 300, 600, 0.1f, 1.0f, p, instance);
                Chromosome bestChromosome = ga.start();
                // Execute GA tasks

                endTime = System.currentTimeMillis();
                averageTime = (endTime - startTime) / 1000;

                assert bestChromosome != null;
                System.out.println("----------------- Solution ----------------------");
                System.out.println("Instance_" + instanceName + "_" + instanceNumber + " Best Fitness: " + bestChromosome.getFitness() + " Time: " + averageTime + "s");
                System.out.println("Total Distance: " + bestChromosome.getTotalTravelCost() + " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness());
                bestChromosome.showSolution(0);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<Parameters> getParametersList() {
        char[] selectType = {'r', 'T', 'W'};
        String[] crossType = {"BC", "BD", "MP"};
        String[] mutType = {"M", "M1"};
        //-1f for local search
        float[] mutRate = {0.0f, 0.05f, 0.1f, -1f};
        List<Parameters> parameters = new ArrayList<>();
        for (char s : selectType) {
            for (String c : crossType) {
                for (String m : mutType) {
                    for (float n : mutRate) {
                        if (c.equals("MP") && n == -1f)
                            continue;
                        parameters.add(new Parameters(s, c, m, n));
                    }
                }
            }
        }
        return parameters;
    }
}

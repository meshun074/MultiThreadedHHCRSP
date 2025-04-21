package org.example;

import org.example.Data.InstancesClass;
import org.example.Data.ReadData;
import org.example.GA.Chromosome;
import org.example.GA.Config;
import org.example.GA.GeneticAlgorithm;
import org.example.GA.Parameters;

import java.io.File;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static InstancesClass instance;
    public static long startTime;
    /*
    Usage: main arg1-int:parameter settings (0-65) arg2-int:problem size (0-6) arg3-int:instance number (1-10) arg4-int:seed
     */

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java GeneticAlgorithmRunner <config-file>");
            return;
        }
        for (int i = 0; i < 1; i++) {
            startTime = System.currentTimeMillis();
            long endTime;
            long averageTime;
            SecureRandom random = new SecureRandom();
            int randomSeed = random.nextInt(Integer.MAX_VALUE);

            try {
                // Read configuration file
                File configFile = new File(args[0]);
                Config config = Config.read(configFile);

                // Extract parameters from JSON
                int paramIndex = config.getParameterIndex();
                int problemSize = config.getProblemSize();
                int instanceNumber = config.getInstanceIndex();

                List<Parameters> parameters = getParametersList();
                Parameters p = parameters.get(paramIndex);
                String[] Instances = {"10", "25", "50", "75", "100", "200", "300"};
                String instanceName = Instances[problemSize];

                //create result directory
                String resultDir = "src/main/java/org/example/Config_" + paramIndex + "_" + problemSize + "_" + instanceNumber + "_results";
                new File(resultDir).mkdirs();

                // Read dataset
                PrintStream fileout = new PrintStream(resultDir + "/Result_" + instanceName + "_" + instanceNumber + "_" + p.selectionTechnique() + "_" + p.crossoverType() + "_" + p.mutationType() + "_" + p.mutationRate() + "_" + randomSeed + ".txt");
                System.setOut(fileout);
                System.out.printf("Config Parameters: parameterIndex=%d, ProblemSize=%d, instanceNumber=%d, seed=%d\n", paramIndex, problemSize, instanceNumber, randomSeed);

                instance = ReadData.read(new File("src/main/java/org/example/Data/instance/" + instanceName + "_" + instanceNumber + ".json"));
//                System.out.println(instance.getQualifiedCaregiver("s5"));
//                System.exit(1);

                // GA execution setup
                double total = 0;
                double best = Double.MAX_VALUE;
                Chromosome bestChromosome = null;
                ExecutorService executor = Executors.newFixedThreadPool(1);
                GeneticAlgorithm.bestChromosomes = Collections.synchronizedList(new ArrayList<>());
                List<Callable<Void>> gaTasks = new ArrayList<>();

                gaTasks.add(() -> {
                    new GeneticAlgorithm(randomSeed, 6, 10, 4, 200, 600, 0.1f, 1.0f, p, instance).run();
                    return null;
                });

                // Execute GA tasks
                executor.invokeAll(gaTasks);
                List<Chromosome> gaChromosomes = GeneticAlgorithm.bestChromosomes;
                synchronized (gaChromosomes) {
                    for (Chromosome ch : gaChromosomes) {
                        if (ch.getFitness() < best) {
                            best = ch.getFitness();
                            bestChromosome = ch;
                        }
                        total += ch.getFitness();
                    }
                }

                double mean = total;
                endTime = System.currentTimeMillis();
                averageTime = (endTime - startTime) / 1000;

                assert bestChromosome != null;
                System.out.println("----------------- Solution ----------------------");
                System.out.println("Instance_" + instanceName + "_" + instanceNumber + " Best Fitness: " + best + " Average Fitness: " + mean + " Average Time: " + averageTime + "s");
                System.out.println("Total Distance: " + bestChromosome.getTotalTravelCost() + " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness());
                bestChromosome.showSolution(0);
                System.out.println("All GA tasks completed. " + gaTasks.size());

                executor.shutdown();
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

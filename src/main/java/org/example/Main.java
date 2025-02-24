package org.example;

import org.example.Data.InstancesClass;
import org.example.Data.ReadData;
import org.example.GA.Chromosome;
import org.example.GA.GeneticAlgorithm;
import org.example.GA.Parameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
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
        long endTime;
        long averageTime;
        //Parameter settings
        List<Parameters> parameters = getParametersList();
        String[] Instances = {"10", "25", "50", "75", "100", "200", "300"};
        try {
            //for parameter settings
            Parameters p = parameters.get(Integer.parseInt(args[0]));
            //for instance size

            String instanceName = Instances[Integer.parseInt(args[1])];
            //for instance type
            int y = Integer.parseInt(args[2]);
            //Read dataset
            startTime = System.currentTimeMillis();
            PrintStream fileout = new PrintStream("src/main/java/org/example/InstanceResults/Result_" + instanceName + "_" + y + "_" + p.selectionTechnique() +
                    "_" + p.crossoverType() + "_" + p.mutationType() + "_" + p.mutationRate() +"_" + args[3]+ ".txt");
            System.setOut(fileout);
            System.out.printf("Usage: main arg1-int:parameter settings %s arg2-int:problem size %s arg3-int:instance number %s arg4-int:seed %s\n",args[0],args[1],args[2],args[3]);
            instance = ReadData.read(new File("src/main/java/org/example/Data/instance/" + instanceName + "_" + y + ".json"));
            //GA start here
            double total = 0;
            double best = Double.MAX_VALUE;
            Chromosome bestChromosome = null;
//                        int coreNumber = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(1);
            GeneticAlgorithm.bestChromosomes = Collections.synchronizedList(new ArrayList<>());
            List<Callable<Void>> gaTasks = new ArrayList<>();
            double mean;
            int n = 1;
            for (int i = 1; i <= n; i++) {
                int finalI = Integer.parseInt(args[3]);
                gaTasks.add(() -> {
                    new GeneticAlgorithm(finalI, 6, 10, 4, 300, 800, 0.1f, p, instance).run();
                    return null;
                });
            }
            try {
                // Submit all tasks and wait for completion
                executor.invokeAll(gaTasks);
                List<Chromosome> gaChromosomes = GeneticAlgorithm.bestChromosomes;
                synchronized (gaChromosomes) {
                    for (Chromosome ch : gaChromosomes) {
                        if (ch.getFitness() < best) {
                            best = ch.getFitness();
                            bestChromosome = ch;
                        }
                        //System.out.println("Fitness: "+ch.getFitness());
                        total += ch.getFitness();
                    }
                    mean = total / n;
                }

                endTime = System.currentTimeMillis();
                averageTime = (endTime - startTime) / (1000 * n);
                assert bestChromosome != null;
                System.out.println("----------------- Solution ----------------------");
                System.out.println("Instance_" + instanceName + "_" + y + " Best Fitness: " + best + " Average Fitness: " + mean + " Average Time: " + averageTime + "s");
                System.out.println("Total Distance: " + bestChromosome.getTotalTravelCost() + " Total Tardiness: " + bestChromosome.getTotalTardiness() + " Highest Tardiness: " + bestChromosome.getHighestTardiness());
                bestChromosome.showSolution(0);
                System.out.println("All GA tasks completed. " + gaTasks.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                executor.shutdown();
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
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

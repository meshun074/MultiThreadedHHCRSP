package org.example;

import org.example.Data.InstancesClass;
import org.example.Data.ReadData;
import org.example.GA.Chromosome;
import org.example.GA.GeneticAlgorithm;
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
    public static void main(String[] args) {
        long startTime;
        long endTime;
        long averageTime;
        try {
            for (int y = 1; y <= 1; y++) {
                //Read dataset
                String instanceName = "25";
                startTime = System.currentTimeMillis();
                PrintStream fileout = new PrintStream("src/main/java/org/example/Result_"+instanceName+"_"+y+"_MP_.txt");
                System.setOut(fileout);
                instance = ReadData.read(new File("src/main/java/org/example/Data/instance/" + instanceName + "_" + y + ".json"));
                //GA start here
                double total = 0;
                double best = Double.MAX_VALUE;
                Chromosome bestChromosome = null;
                int coreNumber = Runtime.getRuntime().availableProcessors();
                ExecutorService executor = Executors.newFixedThreadPool(2);
                GeneticAlgorithm.bestChromosomes = Collections.synchronizedList(new ArrayList<>());
                List<Callable<Void>> gaTasks = new ArrayList<>();
                double mean;
                int n = 8;
                for (int i = 1; i <= n; i++) {
                    int finalI = i;
                    gaTasks.add(() -> {
                        new GeneticAlgorithm(finalI, 6, 10, 300, 400, 0.1f, instance).run();
                        return null;
                    });
//                GeneticAlgorithm ga = new GeneticAlgorithm(200, 600, 0.1f, instance);
//                result = ga.start();
//                if (result.getFitness() < best) {
//                    best = result.getFitness();
//                }
//                total += result.getFitness();
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
                    bestChromosome.showSolution();
                    System.out.println("Instance_" + instanceName + "_" + y + " Best Fitness: " + best + " Average Fitness: " + mean + " Average Time: " + averageTime + "s");
                    System.out.println("All GA tasks completed. " + gaTasks.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    executor.shutdown();
                }

            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }
}

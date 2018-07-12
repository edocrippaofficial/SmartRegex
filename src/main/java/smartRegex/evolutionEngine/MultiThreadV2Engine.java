package smartRegex.evolutionEngine;

import smartRegex.MainClass;
import smartRegex.utils.MutationThread;

import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MultiThreadV2Engine extends EvolutionEngine {

    MutationThread[] mutThreads;
    CyclicBarrier mutBarrierStart, mutBarrierEnd;
    int N_HOM_THREADS;
    public static volatile boolean finish = false;

    public MultiThreadV2Engine() {
        super();
        offspring = Collections.synchronizedList(offspring);
        N_HOM_THREADS = MainClass.N_HOM_THREADS;
    }

    @Override
    public double[] run() {
        mutThreads = new MutationThread[N_PARENTS];
        mutBarrierStart = new CyclicBarrier(N_PARENTS + 1);
        mutBarrierEnd = new CyclicBarrier(N_PARENTS + 1);
        for (int i = 0; i < N_PARENTS; i++) {
            mutThreads[i] = new MutationThread(offspring, USE_HOM, N_HOM_THREADS, HOM_PERCENTAGE, mutBarrierStart, mutBarrierEnd);
            new Thread(mutThreads[i]).start();
        }
        double[] profData = new double[2];
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("******************************** MT-v2 Iteration " + (i+1) + " ********************************");
            Long time1 = System.nanoTime();
            selectParents();
            try {
                mutBarrierStart.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                e.printStackTrace();
            }
            try {
                mutBarrierEnd.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                e.printStackTrace();
            }
            replaceWorst();
            profData[1] += offspring.size();
            System.out.println("Parents: " + parents.size() + " Offspring: " + offspring.size());
            System.out.println("Generation best: " + pop.get(pop.size()-1).regex + " with fitness " + pop.get(pop.size()-1).fitness);
            parents.clear();
            offspring.clear();
            Long time2 = System.nanoTime();
            System.out.println("Time this generation: " + (time2 - time1)/1e9f + " sec\n");

            double lastnum = fri.numFinalFaults;
            fri.computeRatio(pop.get(pop.size()-1).regex);
            System.out.println("Fault index this generation over last one: " + fri.numFinalFaults + " / " + lastnum + " --> Ratio: " + (fri.numFinalFaults/lastnum));
            System.out.println("Fault index this generation over initial one: " + fri.numFinalFaults + " / " + fri.numInitialFaults + " --> Ratio: " + (fri.numFinalFaults/fri.numInitialFaults));

            profData[0] += ((time2 - time1)/1e6f);
            if (pop.get(pop.size()-1).fitness > 0.96) {
                break;
            }
        }
        finish = true;
        mutBarrierStart.reset();
        mutBarrierEnd.reset();
        if (SPECIALIZE)
            specializeFinalRegex();
        else {
            MainClass.finalRegex = pop.get(pop.size() - 1);
            MainClass.finalFri = fri.numFinalFaults;
        }
        return profData;
    }

    @Override
    void selectParents() {
        super.selectParents();
        for (int i = 0; i < mutThreads.length; i++) {
            // Giving all threads their own regex
            mutThreads[i].regex = parents.get(i).regex;
        }
    }
}

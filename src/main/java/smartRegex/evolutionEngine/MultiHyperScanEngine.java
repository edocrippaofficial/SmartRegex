package smartRegex.evolutionEngine;

import com.gliwka.hyperscan.wrapper.Database;
import com.gliwka.hyperscan.wrapper.Expression;
import smartRegex.MainClass;
import smartRegex.utils.HyperScanThread;
import smartRegex.utils.LabeledString;
import smartRegex.utils.MutationThread;
import smartRegex.utils.RegexCandidate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MultiHyperScanEngine extends MultiThreadV2Engine {

    private List<Expression> hyperOffspring;
    public static Database regexDatabase;
    private static double MAX_FIT;

    public MultiHyperScanEngine() {
        super();
        hyperOffspring = new LinkedList<>();
        hyperOffspring = Collections.synchronizedList(hyperOffspring);
        int maxLength = 0;
        for (LabeledString l: EvolutionEngine.strings) {
            if (l.accepted && l.string.length() > maxLength) {
                maxLength = l.string.length();
            }
        }
        double maxFit = 0;
        for (int i = 1; i <= maxLength; i++) {
            maxFit += i * i;
        }
        MAX_FIT = maxFit * EvolutionEngine.strings.size() / 2;
        for (RegexCandidate r: pop) {
            r.normalizeFitness(5);
        }
    }

    @Override
    public double[] run() {
        mutThreads = new MutationThread[N_PARENTS];
        mutBarrierStart = new CyclicBarrier(N_PARENTS + 1);
        mutBarrierEnd = new CyclicBarrier(N_PARENTS + 1);
        for (int i = 0; i < N_PARENTS; i++) {
            mutThreads[i] = new MutationThread(offspring, hyperOffspring, USE_HOM, N_HOM_THREADS, HOM_PERCENTAGE, mutBarrierStart, mutBarrierEnd);
            new Thread(mutThreads[i]).start();
        }
        HyperScanThread[] hyperThreads = new HyperScanThread[N_STRINGS];
        CyclicBarrier hyperBarrierStart = new CyclicBarrier(N_STRINGS + 1);
        CyclicBarrier hyperBarrierEnd = new CyclicBarrier(N_STRINGS + 1);
        for (int i = 0; i < N_STRINGS; i++) {
            hyperThreads[i] = new HyperScanThread(strings.get(i), offspring, hyperBarrierStart, hyperBarrierEnd);
            new Thread(hyperThreads[i]).start();
        }
        double[] profData = new double[2];
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("******************************** MT-Hyper Iteration " + (i+1) + " ********************************");
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
            System.out.println("Compiling Database...");
            try {
                regexDatabase = Database.compile(hyperOffspring);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            System.out.println("Done.");
            try {
                hyperBarrierStart.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                e.printStackTrace();
            }
            try {
                hyperBarrierEnd.await();
            } catch (BrokenBarrierException | InterruptedException e) {
                e.printStackTrace();
            }
            for (RegexCandidate r: offspring) {
                r.normalizeFitness(MAX_FIT);
            }
            replaceWorst();
            profData[1] += offspring.size();
            System.out.println("Parents: " + parents.size() + " Offspring: " + offspring.size());
            System.out.println("Generation best: " + pop.get(pop.size()-1).regex + " with fitness " + pop.get(pop.size()-1).fitness);
            parents.clear();
            offspring.clear();
            hyperOffspring.clear();
            Long time2 = System.nanoTime();
            System.out.println("Time this generation: " + (time2 - time1)/1e9f + " sec\n");

            double lastnum = fri.numFinalFaults;
            fri.computeRatio(pop.get(pop.size()-1).regex);
            System.out.println("Fault index this generation over last one: " + fri.numFinalFaults + " / " + lastnum + " --> Ratio: " + (fri.numFinalFaults/lastnum));
            System.out.println("Fault index this generation over initial one: " + fri.numFinalFaults + " / " + fri.numInitialFaults + " --> Ratio: " + (fri.numFinalFaults/fri.numInitialFaults));

            profData[0] += ((time2 - time1)/1e6f);
        }
        finish = true;
        hyperBarrierStart.reset();
        hyperBarrierEnd.reset();
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
}

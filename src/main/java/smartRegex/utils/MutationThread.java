package smartRegex.utils;

import com.gliwka.hyperscan.wrapper.Expression;
import com.gliwka.hyperscan.wrapper.ExpressionFlag;
import dk.brics.automaton.RegExp;
import regex.operators.AllMutators;
import regex.operators.RegexMutator;
import smartRegex.evolutionEngine.MultiThreadV2Engine;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

    //A threads for each parent are executed at the beginning and go to sleep while
    // the main thread does the calculations for the next generation

public class MutationThread implements Runnable {

    public RegExp regex;
    private boolean hyperScan;
    private CyclicBarrier homBarrierStart, homBarrierEnd;
    private HomThread[] homThreads;
    private ArrayList<RegExp> mutatedRegex;
    private List<RegexCandidate> offspring;
    private List<Expression> hyperOffspring;
    private SplittableRandom rnd;
    private boolean USE_HOM;
    private int N_HOM_THREADS;
    private float HOM_PERC;
    private CyclicBarrier mutBarrierStart, mutBarrierEnd;

    private class HomThread implements Runnable {

        private ArrayList<RegExp> regex = new ArrayList<>();

        @Override
        public void run() {
            while (!MultiThreadV2Engine.finish) {
                try {
                    homBarrierStart.await();
                } catch (InterruptedException | BrokenBarrierException ignored) {}
                if (MultiThreadV2Engine.finish) {
                    return;
                }
                for (RegExp r : regex) {
                    RegExp regMutata;
                    Iterator<RegexMutator.MutatedRegExp> it = AllMutators.mutator.mutate(r);
                    while (it.hasNext()) {
                        regMutata = it.next().mutatedRexExp;
                        String s = regMutata.toString();
                        if (!s.contains("~")) {
                            if (!hyperScan || (!s.contains("^") && !s.contains("{0,") && !s.contains("*") && !s.contains("?"))) {
                                RegexCandidate c = new RegexCandidate(regMutata);
                                if (hyperScan) {
                                    String regex = c.regex.toString().replace("\\", "");
                                    hyperOffspring.add(new Expression(regex, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
                                } else {
                                    c.fitness();
                                }
                                offspring.add(c);
                            }
                        }
                    }
                }
                if (MultiThreadV2Engine.finish) {
                    break;
                }
                try {
                    homBarrierEnd.await();
                } catch (InterruptedException | BrokenBarrierException ignored) {}
                regex.clear();
            }
        }
    }

    public MutationThread(List<RegexCandidate> offspring, boolean USE_HOM, int N_HOM_THREADS, float HOM_PERC, CyclicBarrier mutBarrierStart, CyclicBarrier mutBarrierEnd) {
        this.mutatedRegex = new ArrayList<>();
        this.offspring = offspring;
        this.rnd = new SplittableRandom();
        this.USE_HOM = USE_HOM;
        this.N_HOM_THREADS = N_HOM_THREADS;
        this.HOM_PERC = HOM_PERC;
        this.mutBarrierStart = mutBarrierStart;
        this.mutBarrierEnd = mutBarrierEnd;
        if (USE_HOM) {
            this.homThreads = new HomThread[N_HOM_THREADS];
            this.homBarrierStart = new CyclicBarrier(N_HOM_THREADS + 1);
            this.homBarrierEnd = new CyclicBarrier(N_HOM_THREADS + 1);
            for (int i = 0; i < homThreads.length; i++) {
                homThreads[i] = new HomThread();
                new Thread(homThreads[i]).start();
            }
        }
    }

    public MutationThread(List<RegexCandidate> offspring, List<Expression> hyperOffspring, boolean USE_HOM, int N_HOM_THREADS, float HOM_PERC, CyclicBarrier mutBarrierStart, CyclicBarrier mutBarrierEnd) {
        this(offspring, USE_HOM, N_HOM_THREADS, HOM_PERC, mutBarrierStart, mutBarrierEnd);
        this.hyperOffspring = hyperOffspring;
        this.hyperScan = true;
    }

    @Override
    public void run() {
        while (!MultiThreadV2Engine.finish) {
            try {
                mutBarrierStart.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {}
            if (MultiThreadV2Engine.finish) {
                break;
            }
            mutatedRegex.clear();
            mutation();
            if (MultiThreadV2Engine.finish) {
                break;
            }
            try {
                mutBarrierEnd.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {}
        }
        if (USE_HOM) {
            homBarrierEnd.reset();
            homBarrierStart.reset();
        }
    }

    private void mutation() {
        RegExp regMutata;
        Iterator<RegexMutator.MutatedRegExp> it = AllMutators.mutator.mutate(regex);
        while (it.hasNext()) {
            regMutata = it.next().mutatedRexExp;
            String s = regMutata.toString();
            if (!s.contains("~")) {
                if (!hyperScan || (!s.contains("^") && !s.contains("{0,") && !s.contains("*") && !s.contains("?"))) {
                    RegexCandidate c = new RegexCandidate(regMutata);
                    if (hyperScan) {
                        String regex = c.regex.toString().replace("\\", "");
                        hyperOffspring.add(new Expression(regex, EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
                    } else {
                        c.fitness();
                    }
                    mutatedRegex.add(c.regex);
                    offspring.add(c);
                }
            }
        }
        if (USE_HOM) {
            int size = mutatedRegex.size();
            int n = (int) (size * HOM_PERC);
            int nRegexAssigned = 0;
            int homThreadIndex = 0;
            ArrayList<Integer> chosen = new ArrayList<>();
            while (nRegexAssigned < n) {
                int index;
                do {
                    index = rnd.nextInt(size);
                } while (chosen.contains(index));
                chosen.add(index);
                homThreads[homThreadIndex].regex.add(mutatedRegex.get(index));
                homThreadIndex++;
                nRegexAssigned++;
                if (homThreadIndex == N_HOM_THREADS) {
                    homThreadIndex = 0;
                }
            }
            try {
                homBarrierStart.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            try {
                homBarrierEnd.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}
package smartRegex.evolutionEngine;

import dk.brics.automaton.RegExp;
import regex.operators.AllMutators;
import regex.operators.RegexMutator;
import smartRegex.MainClass;
import smartRegex.utils.RegexCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MultiThreadEngine extends EvolutionEngine {

    private List<Thread> mutationThreads = new ArrayList<>();
    private List<Thread> HOMThreads = new ArrayList<>();

    public MultiThreadEngine() {
        super();
        offspring = Collections.synchronizedList(offspring);
    }

    @Override
    public double[] run() {
        double[] profData = new double[2];
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("******************************** MT Iteration " + (i+1) + " ********************************");
            Long time1 = System.nanoTime();
            selectParents();
            multiMutation();
            for (Thread t : mutationThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (USE_HOM)
                multiHOM(offspring);
            for (Thread t : HOMThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            replaceWorst();
            profData[1] += offspring.size();
            System.out.println("Parents: " + parents.size() + " Offspring: " + offspring.size());
            System.out.println("Generation best: " + pop.get(pop.size()-1).regex + " with fitness " + pop.get(pop.size()-1).fitness);
            parents.clear();
            offspring.clear();
            mutationThreads.clear();
            HOMThreads.clear();
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
        if (SPECIALIZE)
            specializeFinalRegex();
        else {
            MainClass.finalRegex = pop.get(pop.size() - 1);
            MainClass.finalFri = fri.numFinalFaults;
        }
        return profData;
    }

    private  void multiMutation() {
        for (RegexCandidate r : parents) {
            mutationThreads.add(new Thread(() -> {
                RegExp regMutata;
                Iterator<RegexMutator.MutatedRegExp> it;
                it = AllMutators.mutator.mutate(r.regex);
                while (it.hasNext()) {
                    //if it contains '~' I do not want it because it breaks the evolution by taking a high score only by having a correct side
                    if (!(regMutata = it.next().mutatedRexExp).toString().contains("~")) {
                        RegexCandidate c = new RegexCandidate(regMutata);
                        c.fitness();
                        offspring.add(c);
                    }
                }
            }));
        }
        for (Thread t : mutationThreads){
            t.start();
        }
    }

    private void multiHOM(final List<RegexCandidate> offspring) {
        int size = offspring.size();
        int n = (int) (size * HOM_PERCENTAGE);
        int totalRegexAssigned = 0;
        ArrayList<Integer> chosen = new ArrayList<>();
        while (totalRegexAssigned < n) {
            int index;
            do {
                index = rnd.nextInt(size);
            } while (chosen.contains(index));
            chosen.add(index);
            totalRegexAssigned++;
            final RegexCandidate r = offspring.get(index);
            HOMThreads.add(new Thread(() -> {
                RegExp regMutata;
                Iterator<RegexMutator.MutatedRegExp> it;
                it = AllMutators.mutator.mutate(r.regex);
                while (it.hasNext()) {
                    if (!(regMutata = it.next().mutatedRexExp).toString().contains("~")) {
                        RegexCandidate c = new RegexCandidate(regMutata);
                        c.fitness();
                        offspring.add(c);
                    }
                }
            }));
        }
        for (Thread t : HOMThreads){
            t.start();
        }
    }
}

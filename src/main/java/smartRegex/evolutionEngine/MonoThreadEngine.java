package smartRegex.evolutionEngine;

import dk.brics.automaton.RegExp;
import regex.operators.AllMutators;
import regex.operators.RegexMutator;
import smartRegex.MainClass;
import smartRegex.utils.RegexCandidate;

import java.util.ArrayList;
import java.util.Iterator;

public class MonoThreadEngine extends EvolutionEngine {

    @Override
    public double[] run() {
        double[] profData = new double[2];
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("******************************** ST Iteration " + (i+1) + " ********************************");
            Long time1 = System.nanoTime();
            selectParents();
            mutation();
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
        if (SPECIALIZE)
            specializeFinalRegex();
        else {
            MainClass.finalRegex = pop.get(pop.size() - 1);
            MainClass.finalFri = fri.numFinalFaults;
        }
        return profData;
    }


    private void mutation() {
        RegExp regMutata;
        Iterator<RegexMutator.MutatedRegExp> it;
        for (RegexCandidate r : parents) {
            it = AllMutators.mutator.mutate(r.regex);
            while (it.hasNext()) {
                //if it contains '~' I do not want it because it breaks the evolution by taking a high score only by having a correct side
                if (!(regMutata = it.next().mutatedRexExp).toString().contains("~")) {
                    RegexCandidate c = new RegexCandidate(regMutata);
                    c.fitness();
                    offspring.add(c);
                }
            }
        }
        if (USE_HOM) {
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
                Iterator<RegexMutator.MutatedRegExp> it1 = AllMutators.mutator.mutate(offspring.get(index).regex);
                while (it1.hasNext()) {
                    if (!(regMutata = it1.next().mutatedRexExp).toString().contains("~")) {
                        RegexCandidate c = new RegexCandidate(regMutata);
                        c.fitness();
                        offspring.add(c);
                    }
                }
            }
        }
    }
}

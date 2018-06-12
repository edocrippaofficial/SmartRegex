package smartRegex.utils;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import smartRegex.evolutionEngine.EvolutionEngine;

public class RegexCandidate {

    private Automaton automaton;
    public RegExp regex;
    public double fitness;

    public RegexCandidate(String regex) {
        this.regex = new RegExp(regex);
        automaton = this.regex.toAutomaton();
        fitness();
    }

    public RegexCandidate(RegExp regex) {
        this.regex = regex;
        automaton = regex.toAutomaton();
    }


    public void fitness() {
        double fit = 0;
        for (LabeledString l : EvolutionEngine.strings) {
            boolean result = automaton.run(l.string);
            if (result == l.accepted)
                fit++;
        }
        fitness = fit / EvolutionEngine.strings.size();
        int count = regex.toString().length() - regex.toString().replace("|", "").length();
        fitness -= 0.04 * count;

    }

    public void normalizeFitness(double max) {
        fitness /= max;
        int count = regex.toString().length() - regex.toString().replace("|", "").length();
        fitness -= 0.04 * count;
    }

    public void replace(RegexCandidate sub) {
        this.regex = sub.regex;
        this.automaton = sub.automaton;
        this.fitness = sub.fitness;
    }
}
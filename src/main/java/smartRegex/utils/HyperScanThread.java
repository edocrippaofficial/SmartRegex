package smartRegex.utils;

import com.gliwka.hyperscan.wrapper.Match;
import com.gliwka.hyperscan.wrapper.Scanner;
import smartRegex.evolutionEngine.MultiHyperScanEngine;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class HyperScanThread implements Runnable {

    private LabeledString matching;
    private List<RegexCandidate> offspring;
    private CyclicBarrier startBarrier, endBarrier;

    public HyperScanThread(LabeledString matching, List<RegexCandidate> offspring, CyclicBarrier startBarrier, CyclicBarrier endBarrier) {
        this.matching = matching;
        this.offspring = offspring;
        this.startBarrier = startBarrier;
        this.endBarrier = endBarrier;
    }

    @Override
    public void run() {
        // This thread is waken by the main entering the start barrier. While the main thread goes to sleep in the
        // end barrier, the matches for the LabelledString are found. At the end it goes in the end barrier, when
        // all threads are done the main thread is waken and the cycle is repeated for the next generation
        while (!MultiHyperScanEngine.finish) {
            try {
                startBarrier.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {}
            if (MultiHyperScanEngine.finish) {
                break;
            }
            findMatches();
            if (MultiHyperScanEngine.finish) {
                break;
            }
            try {
                endBarrier.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {}
        }
    }

    private void findMatches() {
        try {
            Scanner scanner = new Scanner();
            scanner.allocScratch(MultiHyperScanEngine.regexDatabase);
            List<Match> matches = scanner.scan(MultiHyperScanEngine.regexDatabase, matching.string);
            // The regex that matches this string (if accepted) earns points
            // proportionally to the match length, otherwise it loses points.
            for (Match m: matches) {
                long length = 1 + (m.getEndPosition() - m.getStartPosition());
                if (matching.accepted) {
                    offspring.get(m.regexIndex).fitness += length * length;
                } else {
                    offspring.get(m.regexIndex).fitness -= (length * length) / 2;
                }
            }
            scanner.close();
        } catch (Throwable ignored) { }
    }
}
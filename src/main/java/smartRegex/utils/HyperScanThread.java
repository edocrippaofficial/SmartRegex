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
    private CyclicBarrier hyperBarrierStart, hyperBarrierEnd;

    public HyperScanThread(LabeledString matching, List<RegexCandidate> offspring, CyclicBarrier hyperBarrierStart, CyclicBarrier hyperBarrierEnd) {
        this.matching = matching;
        this.offspring = offspring;
        this.hyperBarrierStart = hyperBarrierStart;
        this.hyperBarrierEnd = hyperBarrierEnd;
    }

    @Override
    public void run() {
        while (!MultiHyperScanEngine.finish) {
            try {
                hyperBarrierStart.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {}
            if (MultiHyperScanEngine.finish) {
                break;
            }
            findMatches();
            if (MultiHyperScanEngine.finish) {
                break;
            }
            try {
                hyperBarrierEnd.await();
            } catch (InterruptedException | BrokenBarrierException ignored) {}
        }
    }

    private void findMatches() {
        try {
            Scanner scanner = new Scanner();
            scanner.allocScratch(MultiHyperScanEngine.regexDatabase);
            List<Match> matches = scanner.scan(MultiHyperScanEngine.regexDatabase, matching.string);
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
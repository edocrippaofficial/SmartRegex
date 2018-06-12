package smartRegex.utils;

import counters.StringCounterLengthDouble;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import regexrepair.process.quality.RepairStats;

import java.io.OutputStream;
import java.io.PrintStream;

// computes the index by considering the number of strings accepted by the xor (that are evaluated differently)
public class FailureResidualIndex extends RepairStats {
	private static int MAX_LENGTH = 68;
	private Automaton oa;
	private Automaton noa;

	public double numInitialFaults;
	public double numFinalFaults;

	public FailureResidualIndex(RegExp oracleRegex, RegExp startingRegex) {
		super(oracleRegex, startingRegex);
		System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {}
        }));
		// symmetric difference between the oracle and the starting regex
		oa = oracleRegex.toAutomaton();
		noa = oa.complement();
		Automaton sa = startingRegex.toAutomaton();
		Automaton nsa = sa.complement();
		Automaton sd = oa.intersection(nsa).union(sa.intersection(noa));
		if (sd.isEmpty()) {
			// starting and oracle are the same
			numInitialFaults = 0;
		} else {
			String shortestExample = sd.getShortestExample(true);
			if (shortestExample.length() > MAX_LENGTH) {
				numInitialFaults = 0;
				System.out.println("> MAX_LENGTH ");
			} else {
				StringCounterLengthDouble sr = new StringCounterLengthDouble(sd, MAX_LENGTH);
				numInitialFaults = sr.count();
			}
		}
	}

	@Override
	public void computeRatio(RegExp finalRegex) {
		Automaton sa = finalRegex.toAutomaton();
		Automaton nsa = sa.complement();
		Automaton sd = oa.intersection(nsa).union(sa.intersection(noa));
		if (sd.isEmpty()) {
			numFinalFaults = 0;
		} else {
			StringCounterLengthDouble sr = new StringCounterLengthDouble(sd, MAX_LENGTH);
			numFinalFaults = sr.count();
		}
	}

	@Override
	public void printInitStats() {
		System.out.println(numInitialFaults);
	}

	@Override
	public void printFinalStats() {
		System.out.println(numFinalFaults + "/" + numInitialFaults);
		System.out.println(numFinalFaults / numInitialFaults);
	}
}
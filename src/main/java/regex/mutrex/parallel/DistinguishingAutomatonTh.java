package regex.mutrex.parallel;

import regex.distinguishing.DistinguishingString;
import regex.mutrex.ds.DSSet;
import regex.mutrex.ds.DistinguishingAutomaton;
import regex.operators.RegexMutator.MutatedRegExp;

import java.util.List;

public class DistinguishingAutomatonTh extends Thread {
	private DistinguishingAutomaton da;
	private MutantsManager mutantsManager;
	private boolean run;
	private DSSet dsS;

	public DistinguishingAutomatonTh(DistinguishingAutomaton da, MutantsManager mutantsManager, DSSet dsS) {
		this.da = da;
		this.mutantsManager = mutantsManager;
		run = true;
		this.dsS = dsS;
		assert da.getMutants().size() == 1;
	}

	@Override
	public void run() {
		while (run) {
			MutantForDasParallelCollector mutant = mutantsManager.getMutant(this);
			if (mutant != null) {
				if (da.add(mutant.description,mutant.getRegexWithAutomata())) {
					assert da.getMutants().size() > 1;
					mutantsManager.coverMutant(mutant);
				}
				mutant.unlock();
			}
			mutantsManager.mutantConsidered();
		}
		List<MutatedRegExp> daCoveredMuts = da.getMutants();
		assert daCoveredMuts.size() > 0;
		dsS.add(new DistinguishingString(da.getExample(), da.positive), daCoveredMuts);
		da = null;
	}

	public void stopThread() {
		run = false;
	}
}
package regex.mutrex.parallel.mutant;

import dk.brics.automaton.RegExp;
import regex.distinguishing.DistinguishingString;
import regex.mutrex.ds.DSSet;
import regex.mutrex.ds.DSSetGenerator;
import regex.mutrex.ds.DistinguishingAutomaton;
import regex.mutrex.ds.RegexWAutomata;
import regex.operators.RegexMutator.MutatedRegExp;

import java.util.*;

public class MutantParallelCollectDSSetGenerator extends DSSetGenerator {
	public static DSSetGenerator generator = new MutantParallelCollectDSSetGenerator();
	int numRunningMutants = 0;

	public synchronized MutantForMutParallelCollector getMutant(Iterator<MutatedRegExp> mutants) {
		while (mutants.hasNext()) {
			if (numRunningMutants >= Runtime.getRuntime().availableProcessors()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			} else {
				numRunningMutants++;
				return new MutantForMutParallelCollector(mutants.next());
			}
		}
		return null;
	}

	public synchronized void decreaseRunningMuts() {
		numRunningMutants--;
		notifyAll();
	}

	@Override
	public void addStringsToDSSet(DSSet dsS, RegExp regex, Iterator<MutatedRegExp> mutants) {
		DasManager dasManager = new DasManager(regex);
		MutantForMutParallelCollector mutant = null;
		Set<MutTh> mutThs = new HashSet<MutTh>();
		while ((mutant = getMutant(mutants)) != null) {
			MutTh mutTh = new MutTh(mutant, dasManager, this);
			mutTh.start();
			mutThs.add(mutTh);
		}
		for (MutTh mutTh : mutThs) {
			try {
				mutTh.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for (DistinguishingAutomatonClass dac : dasManager.daS) {
			DistinguishingString ds = new DistinguishingString(dac.da.getExample(), dac.da.isPositive());
			dsS.add(ds, dac.da.getMutants());
		}
	}
}

class MutTh extends Thread {
	MutantForMutParallelCollector mutant;
	DasManager dasManager;
	MutantParallelCollectDSSetGenerator gen;

	public MutTh(MutantForMutParallelCollector mutant, DasManager dasManager, MutantParallelCollectDSSetGenerator gen) {
		this.mutant = mutant;
		this.dasManager = dasManager;
		this.gen = gen;
	}

	@Override
	public void run() {
		List<Boolean> trueFalse = Arrays.asList(true, false);
		boolean isCovered = false;
		while (!isCovered) {
			DistinguishingAutomatonClass dac = dasManager.getDA(mutant);
			if (dac == null) {
				Collections.shuffle(trueFalse);
				for (boolean b : trueFalse) {
					DistinguishingAutomaton newDa = new DistinguishingAutomaton(dasManager.regexWithAutomata, b);
					if (newDa.add(mutant.description, mutant.mutant)) {
						DistinguishingAutomatonClass newDaC = new DistinguishingAutomatonClass(newDa);
						dasManager.addDA(newDaC);
						break;
					}
				}
				isCovered = true;
				mutant.isEquivalent = true;
			} else {
				if (dac.da.add(mutant.description, mutant.mutant)) {
					isCovered = true;
				}
				mutant.visited.add(dac);
				dasManager.unlock(dac);
			}
		}
		gen.decreaseRunningMuts();
	}

}

class DasManager {
	Set<DistinguishingAutomatonClass> daS = new HashSet<DistinguishingAutomatonClass>();
	List<Boolean> trueFalse = Arrays.asList(true, false);
	RegexWAutomata regexWithAutomata;

	public DasManager(RegExp regex) {
		regexWithAutomata = new RegexWAutomata(regex);
	}

	public synchronized void addDA(DistinguishingAutomatonClass da) {
		daS.add(da);
		notifyAll();
	}

	public synchronized void unlock(DistinguishingAutomatonClass da) {
		da.locked = false;
		notifyAll();
	}

	public synchronized DistinguishingAutomatonClass getDA(MutantForMutParallelCollector mutant) {
		Set<DistinguishingAutomatonClass> checkedByMut = mutant.visited;
		while (true) {
			boolean addNewDa = true;
			for (DistinguishingAutomatonClass da : daS) {
				if (!checkedByMut.contains(da)) {
					addNewDa = false;
					if (!da.locked) {
						da.locked = true;
						return da;
					}
				}
			}
			if (addNewDa) {
				return null;
			} else {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class DistinguishingAutomatonClass {
	DistinguishingAutomaton da;
	boolean locked = false;

	public DistinguishingAutomatonClass(DistinguishingAutomaton da) {
		this.da = da;
	}
}

class MutantForMutParallelCollector {
	RegexWAutomata mutant;
	Set<DistinguishingAutomatonClass> visited;
	boolean isEquivalent;
	String description;

	public MutantForMutParallelCollector(MutatedRegExp mutatedRegExp) {
		this.mutant = new RegexWAutomata(mutatedRegExp.mutatedRexExp);
		visited = new HashSet<DistinguishingAutomatonClass>();
		description = mutatedRegExp.description;
	}

	public RegExp getRegex() {
		return mutant.getMutant();
	}
}
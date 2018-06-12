package regex.mutrex;

import dk.brics.automaton.RegExp;
import regex.distinguishing.DistinguishingString;
import regex.mutrex.ds.DSSet;
import regex.mutrex.ds.DSSetGenerator;
import regex.mutrex.ds.DistinguishingAutomaton;
import regex.mutrex.ds.RegexWAutomata;
import regex.operators.RegexMutator.MutatedRegExp;

import java.util.*;

/**
 * generates a ds that tries to kill as many mutants as possible
 * 
 * @author garganti
 *
 */
abstract class CollectDSSetGenerator extends DSSetGenerator {
	List<Integer> coveredMutsNum = new ArrayList<Integer>();

	@Override
	public void addStringsToDSSet(DSSet result, RegExp regex, Iterator<MutatedRegExp> mutants) {
		List<Boolean> trueFalse = Arrays.asList(true, false);
		//Automaton rexAut = regex.toAutomaton();
		RegexWAutomata r = new RegexWAutomata(regex);
		List<DistinguishingAutomaton> das = new ArrayList<>();
		nextMut: while (mutants.hasNext()) {
			MutatedRegExp mutant = mutants.next();
			sortDAs(das);
			RegexWAutomata m = new RegexWAutomata(mutant.mutatedRexExp);
			Iterator<DistinguishingAutomaton> dasIt = das.iterator();
			while (dasIt.hasNext()) {
				DistinguishingAutomaton da = dasIt.next();
				// solution 2: invalidating the da
				/*if(!da.isActive) {
					continue;
				}*/

				if (da.add(mutant.description,m)) {
					if (stop(da)) {
						// solution 1: removing the da
						genTest(result, da);
						dasIt.remove();

						//solution 2: invalidating the da
						//da.isActive = false;
					}
					continue nextMut;
				}
			}
			// it is not collectable
			// try to collect rexAut
			Collections.shuffle(trueFalse);
			for (boolean b : trueFalse) {
				DistinguishingAutomaton newDa = new DistinguishingAutomaton(r, b);
				if (newDa.add(mutant.description,m)) {
					das.add(newDa);
					continue nextMut;
				}
			}
		}
		// now get the remaining DS
		for (DistinguishingAutomaton da : das) {
			genTest(result, da);
		}
		//System.out.print(das.size() + "\t");
		/*System.out.println();
		for(Integer c: coveredMutsNum) {
			System.out.print(c + " ");
		}
		System.out.println();*/
		//System.out.print(coveredMutsNum.size() + "\t");
	}

	private void sortDAs(List<DistinguishingAutomaton> das) {
		Collections.shuffle(das);
		//smallest ones first
		//das.sort((DistinguishingAutomaton o1, DistinguishingAutomaton o2) -> o1.mutatedRegexes.size() - o2.mutatedRegexes.size());
		//biggest ones first
		//das.sort((DistinguishingAutomaton o1, DistinguishingAutomaton o2) -> o2.mutatedRegexes.size() - o1.mutatedRegexes.size());
	}

	private void genTest(DSSet result, DistinguishingAutomaton da) {
		DistinguishingString ds = new DistinguishingString(da.getExample(), da.isPositive());
		result.add(ds, da.getMutants());
		coveredMutsNum.add(da.size());
	}

	abstract boolean stop(DistinguishingAutomaton da);
}
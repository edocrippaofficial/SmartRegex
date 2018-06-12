package regex.mutrex;

import dk.brics.automaton.RegExp;
import regex.mutrex.ds.DSSet;
import regex.mutrex.ds.DSSetGenerator;
import regex.operators.RegexMutator.MutatedRegExp;
import regex.utils.IteratorUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * generates a ds that tries to kill as many mutants as possible, in random
 * order SHUFFLE
 * 
 */
public final class CollectRandomDSSetGenerator extends CollectDSSetGeneratorNoLimit {
	public static DSSetGenerator generator = new CollectRandomDSSetGenerator();

	private CollectRandomDSSetGenerator() {
	}

	@Override
	public void addStringsToDSSet(DSSet dsset, RegExp regex, Iterator<MutatedRegExp> mutants) {
		List<MutatedRegExp> iteratorToList = IteratorUtils.iteratorToList(mutants);
		Collections.shuffle(iteratorToList);
		super.addStringsToDSSet(dsset, regex, iteratorToList.iterator());
	}
}
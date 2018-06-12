package regex.operators;

import dk.brics.automaton.oo.REGEXP_CHAR_RANGE;
import dk.brics.automaton.oo.ooregex;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * the user has used a char range but has a wrong boundary (but the new
 * mutation will be a char again)
 * 
 * In MUTATION 2017 is RMN
 *
 * modified in order to have a random mutation between bounds instead of +- 1
 */

public class RangeModificationNew extends RegexMutator {

    private static SplittableRandom rnd = new SplittableRandom();
	public static RangeModificationNew mutator = new RangeModificationNew();

	private RangeModificationNew() {
		super(new RangeModificationVisitor());
	}

	static class RangeModificationVisitor extends RegexVisitorAdapterList {

		@Override
		public List<ooregex> visit(REGEXP_CHAR_RANGE r) {
            List<ooregex> result = new ArrayList<>();
            char fromV = vary(r.from, false);
            char toV = vary(r.to, true);
            if (fromV > 0) {
                result.add(new REGEXP_CHAR_RANGE(fromV, r.to));
            }
            if (toV > 0) {
                result.add(new REGEXP_CHAR_RANGE(r.from, toV));
            }
            return result;
		}

		// varia c in modo + o -1 ma che sia ancora un carattere leggibile
		private char vary(char c, boolean to) {
			if (c >= 'a' && c <= 'z' && ((to && c < 'z') || (!to && c > 'a'))) {
			    if (to) {
                    return (char) (c + rnd.nextInt(1 + ('z' - c)));
                } else {
                    return (char) ('a' + rnd.nextInt(c - 'a'));
                }
            }
            if (c >= 'A' && c <= 'Z' && ((to && c < 'Z') || (!to && c > 'A'))) {
                if (to) {
                    return (char) (c + rnd.nextInt(1 + ('Z' - c)));
                } else {
                    return (char) ('A' + rnd.nextInt(c - 'A'));
                }
            }
            if (c >= '0' && c <= '9' && ((to && c < '9') || (!to && c > '0'))) {
                if (to) {
                    return (char) (c + rnd.nextInt(1 + ('9' - c)));
                } else {
                    return (char) ('0' + rnd.nextInt(c - '0'));
                }
            }
			return 0;
		}

		@Override
		public String getCode() {
			return "RMN";
		}
	}

	@Override
	public String getCode() {
		return "RMN";
	}
}
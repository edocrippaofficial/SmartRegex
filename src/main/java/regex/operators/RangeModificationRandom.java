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

public class RangeModificationRandom extends RegexMutator {

    private static SplittableRandom rnd = new SplittableRandom();
	public static RangeModificationRandom mutator = new RangeModificationRandom();

	private RangeModificationRandom() {
		super(new RangeModificationVisitor());
	}

	static class RangeModificationVisitor extends RegexVisitorAdapterList {

		@Override
		public List<ooregex> visit(REGEXP_CHAR_RANGE r) {
            List<ooregex> result = new ArrayList<>();
            char extFrom = extend(r.from, false);
            char extTo = extend(r.to, true);
            char redFrom = reduce(r.from, r.to, false);
            char redTo = reduce(r.from, r.to, true);
            if (extFrom > 0) result.add(new REGEXP_CHAR_RANGE(extFrom, r.to));
            if (extTo > 0) result.add(new REGEXP_CHAR_RANGE(r.from, extTo));
            if (redFrom > 0) result.add(new REGEXP_CHAR_RANGE(redFrom, r.to));
            if (redTo > 0) result.add(new REGEXP_CHAR_RANGE(r.from, redTo));
            return result;
		}

		private char extend(char c, boolean to) {
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

        private char reduce(char from, char to, boolean t) {
            if (from >= 'a' && from <= 'z' && to >= 'a' && to <= 'z' && to > from) {
                if (t) {
                    return (char) (to - (1 + rnd.nextInt(to - from)));
                } else {
                    return (char) (from + (1 + rnd.nextInt(to - from)));
                }
            }
            if (from >= 'A' && from <= 'Z' && to >= 'A' && to <= 'Z' && to > from) {
                if (t) {
                    return (char) (to - (1 + rnd.nextInt(to - from)));
                } else {
                    return (char) (from + (1 + rnd.nextInt(to - from)));
                }
            }
            if (from >= '0' && from <= '9' && to >= '0' && to <= '9' && to > from) {
                if (t) {
                    return (char) (to - (1 + rnd.nextInt(to - from)));
                } else {
                    return (char) (from + (1 + rnd.nextInt(to - from)));
                }
            }
            return 0;
        }

		@Override
		public String getCode() {
			return "RMR";
		}
	}

	@Override
	public String getCode() {
		return "RMR";
	}
}
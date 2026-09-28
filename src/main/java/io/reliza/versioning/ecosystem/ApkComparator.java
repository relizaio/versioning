/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orders Alpine Linux (apk) package versions. A version is one or more numbers separated by
 * dots, then optionally a run of letters, then any number of suffixes (an underscore, a suffix
 * name and an optional number), then optionally a tilde and a lowercase hex commit hash, and
 * finally an optional package revision, written as -r followed by a number.
 * <p>
 * Sources: the version format is the one documented for pkgver and pkgrel on the Alpine wiki
 * (APKBUILD Reference); the comparison rules follow the Gentoo Package Manager Specification
 * (section 3.3, "Version comparison"), extended with Alpine's extra suffixes and commit hash.
 * <p>
 * Parts are compared in the order below and the first difference decides:
 * <ol>
 * <li>Numbers. The first number compares by value. A later number also compares by value,
 * unless either side starts with 0; then both are compared as plain ASCII text, so
 * {@code 1.0 < 1.00 < 1.001 < 1.01 < 1.1}, {@code 1.05 < 1.5} and {@code 8.2.0015 < 8.2.002}.
 * When one list of numbers is a prefix of the other, the longer one is higher.</li>
 * <li>Letters. The letters right after the numbers compare as ASCII text and no letters is
 * lowest, so {@code 1.0 < 1.0a < 1.0bc}. Gentoo allows a single letter; a run is accepted.</li>
 * <li>Suffixes. Read the suffixes as one sequence of items (each suffix name, followed by its
 * number when it has one) and compare item by item from the left. At any one position a
 * pre-release name (alpha, beta, pre, rc, in that order) is lowest, then the end of the
 * suffixes, then a number (by value), then a post-release name (cvs, svn, git, hg, p, in that
 * order). So a missing number is not the same as 0, and what follows the bare name decides:
 * {@code 1.0_alpha < 1.0_alpha0 < 1.0_alpha1}, {@code 1.0_rc_beta < 1.0_rc0 < 1.0_rc_p} and
 * {@code 1.0_alpha_pre2 < 1.0_alpha < 1.0 < 1.0_p1}.</li>
 * <li>Commit hash (lowercase hex after {@code ~}). No hash is lowest; two hashes compare as
 * ASCII text.</li>
 * <li>Revision {@code -rN} by value. Unlike Gentoo, a missing revision is its own lowest
 * state rather than {@code -r0}, so {@code 1.0 < 1.0-r0 < 1.0-r1}.</li>
 * </ol>
 * <p>
 * Malformed input still gets a consistent total order. A string that does not start with a
 * digit is lower than every version, and such strings compare with each other as plain text.
 * Otherwise the longest well-formed prefix is compared with the rules above, and whatever is
 * left over breaks any remaining tie as plain text, with nothing left over being lowest. For
 * example {@code 23_foo} is read as {@code 23} plus the leftover {@code _foo}.
 */
final class ApkComparator implements Comparator<String> {

	@Override
	public int compare(String a, String b) {
		Parsed x = Parsed.parse(a);
		Parsed y = Parsed.parse(b);
		boolean xValid = !x.numbers().isEmpty();
		boolean yValid = !y.numbers().isEmpty();
		if (xValid != yValid) return xValid ? 1 : -1;
		int c = compareNumbers(x.numbers(), y.numbers());
		if (c == 0) c = Integer.signum(x.letters().compareTo(y.letters()));
		if (c == 0) c = compareSuffixes(x.suffixes(), y.suffixes());
		if (c == 0) c = compareHashes(x.hash(), y.hash());
		if (c == 0) c = compareRevisions(x.revision(), y.revision());
		if (c == 0) c = Integer.signum(x.rest().compareTo(y.rest()));
		return c;
	}

	/**
	 * Checks that the whole string is a version in the format described on the class, that is
	 * it starts with a digit and nothing is left over after the well-formed part.
	 * @param version version, not null
	 * @return true when the version is well formed
	 */
	static boolean isValid(String version) {
		Parsed p = Parsed.parse(version);
		return !p.numbers().isEmpty() && p.rest().isEmpty();
	}

	private static int compareNumbers(List<String> a, List<String> b) {
		if (a.isEmpty()) return 0;
		int c = NumericStrings.compare(a.get(0), b.get(0));
		for (int i = 1; c == 0 && i < a.size() && i < b.size(); i++) {
			c = compareLaterNumber(a.get(i), b.get(i));
		}
		return c != 0 ? c : Integer.compare(a.size(), b.size());
	}

	private static int compareLaterNumber(String a, String b) {
		if (a.charAt(0) == '0' || b.charAt(0) == '0') return Integer.signum(a.compareTo(b));
		return NumericStrings.compare(a, b);
	}

	/**
	 * Item-by-item comparison of the suffix sequences, as described in the class javadoc.
	 * Only the first differing item matters, so a number is only ever weighed against the item
	 * that follows the same bare suffix name on the other side.
	 */
	private static int compareSuffixes(List<Suffix> a, List<Suffix> b) {
		int common = Math.min(a.size(), b.size());
		for (int i = 0; i < common; i++) {
			Suffix x = a.get(i);
			Suffix y = b.get(i);
			int c = x.type().compareTo(y.type());
			if (c != 0) return Integer.signum(c);
			boolean xNumber = !x.number().isEmpty();
			boolean yNumber = !y.number().isEmpty();
			if (xNumber && yNumber) {
				c = NumericStrings.compare(x.number(), y.number());
				if (c != 0) return c;
			} else if (xNumber) {
				return isPostRelease(b, i + 1) ? -1 : 1;
			} else if (yNumber) {
				return isPostRelease(a, i + 1) ? 1 : -1;
			}
		}
		if (a.size() > common) return isPostRelease(a, common) ? 1 : -1;
		if (b.size() > common) return isPostRelease(b, common) ? -1 : 1;
		return 0;
	}

	private static boolean isPostRelease(List<Suffix> suffixes, int index) {
		return index < suffixes.size() && suffixes.get(index).type().postRelease;
	}

	private static int compareHashes(String a, String b) {
		if (a.isEmpty() || b.isEmpty()) return Boolean.compare(!a.isEmpty(), !b.isEmpty());
		return Integer.signum(a.compareTo(b));
	}

	private static int compareRevisions(String a, String b) {
		if (a.isEmpty() || b.isEmpty()) return Boolean.compare(!a.isEmpty(), !b.isEmpty());
		return NumericStrings.compare(a, b);
	}

	private static boolean isLowerLetter(char c) {
		return c >= 'a' && c <= 'z';
	}

	private static boolean isHexDigit(char c) {
		return NumericStrings.isDigit(c) || (c >= 'a' && c <= 'f');
	}

	private static int skipDigits(String s, int from) {
		int i = from;
		while (i < s.length() && NumericStrings.isDigit(s.charAt(i))) i++;
		return i;
	}

	/**
	 * Suffix types in ascending order. A missing suffix sorts between the last pre-release
	 * type and the first post-release type.
	 */
	private enum SuffixType {
		ALPHA("alpha", false),
		BETA("beta", false),
		PRE("pre", false),
		RC("rc", false),
		CVS("cvs", true),
		SVN("svn", true),
		GIT("git", true),
		HG("hg", true),
		P("p", true);

		private final String label;
		private final boolean postRelease;

		SuffixType(String label, boolean postRelease) {
			this.label = label;
			this.postRelease = postRelease;
		}

		static SuffixType of(String label) {
			for (SuffixType t : values()) {
				if (t.label.equals(label)) return t;
			}
			return null;
		}
	}

	/**
	 * One suffix; number is empty when the suffix has no number.
	 */
	private record Suffix(SuffixType type, String number) {
	}

	/**
	 * Parsed form of a version string. An empty number list marks a string that does not
	 * start with a digit; its whole text is then kept in rest. An empty hash or revision means
	 * that part is absent.
	 */
	private record Parsed(List<String> numbers, String letters, List<Suffix> suffixes, String hash,
			String revision, String rest) {

		static Parsed parse(String s) {
			int n = s.length();
			if (n == 0 || !NumericStrings.isDigit(s.charAt(0))) {
				return new Parsed(List.of(), "", List.of(), "", "", s);
			}
			List<String> numbers = new ArrayList<>();
			int i = skipDigits(s, 0);
			numbers.add(s.substring(0, i));
			while (i + 1 < n && s.charAt(i) == '.' && NumericStrings.isDigit(s.charAt(i + 1))) {
				int start = i + 1;
				i = skipDigits(s, start);
				numbers.add(s.substring(start, i));
			}

			int letterStart = i;
			while (i < n && NumericStrings.isLetter(s.charAt(i))) i++;
			String letters = s.substring(letterStart, i);

			List<Suffix> suffixes = new ArrayList<>();
			while (i < n && s.charAt(i) == '_') {
				int j = i + 1;
				while (j < n && isLowerLetter(s.charAt(j))) j++;
				SuffixType type = SuffixType.of(s.substring(i + 1, j));
				if (type == null) break;
				int numberStart = j;
				j = skipDigits(s, j);
				suffixes.add(new Suffix(type, s.substring(numberStart, j)));
				i = j;
			}

			String hash = "";
			if (i + 1 < n && s.charAt(i) == '~' && isHexDigit(s.charAt(i + 1))) {
				int start = i + 1;
				i = start;
				while (i < n && isHexDigit(s.charAt(i))) i++;
				hash = s.substring(start, i);
			}

			String revision = "";
			if (i + 2 < n && s.charAt(i) == '-' && s.charAt(i + 1) == 'r' && NumericStrings.isDigit(s.charAt(i + 2))) {
				int start = i + 2;
				i = skipDigits(s, start);
				revision = s.substring(start, i);
			}
			return new Parsed(numbers, letters, suffixes, hash, revision, s.substring(i));
		}
	}
}

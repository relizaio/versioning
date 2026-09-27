/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.Comparator;

/**
 * Debian version ordering as specified by Debian Policy section 5.6.12: versions have the form
 * [epoch:]upstream_version[-debian_revision]. Epochs compare numerically (missing epoch is 0);
 * upstream versions and revisions (missing revision is "0") compare by alternating non-digit
 * and digit runs. Non-digit runs compare character by character with "~" sorting before
 * everything including the end of the run, then letters, then all other characters.
 */
final class DpkgComparator implements Comparator<String> {

	private static final int END_OF_RUN = -1;

	private record Parsed(String epoch, String upstream, String revision) {}

	@Override
	public int compare(String a, String b) {
		Parsed pa = parse(a);
		Parsed pb = parse(b);
		int c = NumericStrings.compare(pa.epoch(), pb.epoch());
		if (c != 0) return c;
		c = compareFragment(pa.upstream(), pb.upstream());
		if (c != 0) return c;
		return compareFragment(pa.revision(), pb.revision());
	}

	private static Parsed parse(String version) {
		String rest = version;
		String epoch = "0";
		int colon = rest.indexOf(':');
		if (colon > 0 && NumericStrings.isDigits(rest.substring(0, colon))) {
			epoch = rest.substring(0, colon);
			rest = rest.substring(colon + 1);
		}
		String revision = "0";
		int hyphen = rest.lastIndexOf('-');
		if (hyphen >= 0) {
			revision = rest.substring(hyphen + 1);
			rest = rest.substring(0, hyphen);
		}
		return new Parsed(epoch, rest, revision);
	}

	/**
	 * Compares upstream versions or revisions per Debian Policy 5.6.12: alternately the leading
	 * run of non-digits (compared character by character) and the leading run of digits
	 * (compared numerically, an empty run counting as zero) of what remains of each string.
	 */
	private static int compareFragment(String a, String b) {
		int i = 0;
		int j = 0;
		while (i < a.length() || j < b.length()) {
			int ei = runEnd(a, i, false);
			int ej = runEnd(b, j, false);
			int c = compareNonDigitRuns(a.substring(i, ei), b.substring(j, ej));
			if (c != 0) return c;
			i = ei;
			j = ej;
			ei = runEnd(a, i, true);
			ej = runEnd(b, j, true);
			c = NumericStrings.compare(i == ei ? "0" : a.substring(i, ei), j == ej ? "0" : b.substring(j, ej));
			if (c != 0) return c;
			i = ei;
			j = ej;
		}
		return 0;
	}

	private static int runEnd(String s, int from, boolean digits) {
		int k = from;
		while (k < s.length() && NumericStrings.isDigit(s.charAt(k)) == digits) k++;
		return k;
	}

	private static int compareNonDigitRuns(String a, String b) {
		int n = Math.max(a.length(), b.length());
		for (int k = 0; k < n; k++) {
			int c = comparePositions(k < a.length() ? a.charAt(k) : END_OF_RUN, k < b.length() ? b.charAt(k) : END_OF_RUN);
			if (c != 0) return c;
		}
		return 0;
	}

	/**
	 * Policy order for one position of a non-digit run: a tilde first, then the end of the run,
	 * then letters, then all other characters, letters and others each in ASCII order.
	 */
	private static int comparePositions(int a, int b) {
		int c = Integer.compare(tier(a), tier(b));
		return c != 0 ? c : Integer.compare(a, b);
	}

	private static int tier(int c) {
		if (c == '~') return 0;
		if (c == END_OF_RUN) return 1;
		if (NumericStrings.isLetter((char) c)) return 2;
		return 3;
	}
}

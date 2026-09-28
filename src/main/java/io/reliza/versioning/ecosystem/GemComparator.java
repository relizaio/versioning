/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * RubyGems version ordering, following Gem::Version
 * (https://docs.ruby-lang.org/en/master/Gem/Version.html) and its canonical segments, and checked
 * against Ruby 3.3 / RubyGems 3.5.22:
 * <ul>
 * <li>A version is read as segments: maximal runs of ASCII digits and maximal runs of ASCII
 * letters. Dots, and every other character, only separate segments, so "1.0.a1" is read as
 * 1, 0, "a", 1. A hyphen is read as ".pre.", so "1.0.0-rc1" is 1, 0, 0, "pre", "rc", 1.</li>
 * <li>Zero padding is then removed as Gem::Version#canonical_segments does: trailing zero
 * segments are dropped, and so is the first run of zero segments that directly precedes a letter
 * segment and starts the version or follows a dot. So zero padding of the release does not matter
 * even before a pre-release: "1.0.a" equals "1.a", "1.0-rc1" equals "1.0.0-rc1" and
 * "5.0.0.beta1" &lt; "5.0.rc1". Only the first such run goes: "1.a.0.b" equals "1.a.b", but
 * "1.0.a.0.b" &gt; "1.a.b", and "1.a0b" keeps its 0.</li>
 * <li>Canonical segments compare pairwise from the left, a missing segment counting as 0, so
 * "1.0" equals "1.0.0".</li>
 * <li>Two numeric segments compare by value, ignoring leading zeros and never overflowing.</li>
 * <li>A letter segment sorts before any numeric segment, which is how a pre-release sorts below
 * its release: 1.0.a &lt; 1.0 = 1.0.0 and 1.0.b1 &lt; 1.0.</li>
 * <li>Two letter segments compare lexically by ASCII code, so upper case sorts before lower
 * case.</li>
 * </ul>
 * Examples, ascending: 0.9 &lt; 1.0.a.2 &lt; 1.0.a9 &lt; 1.0.a10 &lt; 1.0.b1 &lt; 1.0 &lt; 1.0.1.
 * A version with a platform suffix, such as "1.13.10-arm64-darwin", is a valid Gem::Version
 * string: the hyphens make it a pre-release, so it sorts below "1.13.10", as RubyGems itself
 * orders it. Every string gets a place in the same total order; a string without digits or
 * letters reads as version 0.
 */
final class GemComparator implements Comparator<String> {

	/**
	 * What a hyphen is read as.
	 */
	private static final String HYPHEN = ".pre.";

	/**
	 * Trailing zero segments, after a letter or a dot.
	 */
	private static final Pattern TRAILING_ZEROS = Pattern.compile("(?<=[A-Za-z.])[.0]+\\z");

	/**
	 * A run of zero segments right before a letter, starting the version or after a dot.
	 */
	private static final Pattern ZEROS_BEFORE_LETTER = Pattern.compile("(?:(?<=\\.)|^)[0.]+(?=[A-Za-z])");

	private static final String ZERO = "0";

	@Override
	public int compare(String a, String b) {
		List<String> x = canonicalSegments(a);
		List<String> y = canonicalSegments(b);
		int n = Math.max(x.size(), y.size());
		for (int i = 0; i < n; i++) {
			int c = compareSegments(i < x.size() ? x.get(i) : ZERO, i < y.size() ? y.get(i) : ZERO);
			if (c != 0) return c;
		}
		return 0;
	}

	/**
	 * Numbers by value, letters lexically, letters before numbers.
	 */
	private static int compareSegments(String a, String b) {
		boolean aNumeric = isNumeric(a);
		boolean bNumeric = isNumeric(b);
		if (aNumeric && bNumeric) return NumericStrings.compare(a, b);
		if (aNumeric != bNumeric) return aNumeric ? 1 : -1;
		return Integer.signum(a.compareTo(b));
	}

	private static boolean isNumeric(String segment) {
		return NumericStrings.isDigit(segment.charAt(0));
	}

	/**
	 * Splits a version into its canonical segments, as described on the class.
	 * @param version version, not null
	 * @return canonical segments, each made of ASCII digits only or ASCII letters only
	 */
	static List<String> canonicalSegments(String version) {
		String v = version.replace("-", HYPHEN);
		v = TRAILING_ZEROS.matcher(v).replaceFirst("");
		v = ZEROS_BEFORE_LETTER.matcher(v).replaceFirst("");
		return segments(v);
	}

	/**
	 * Splits a version into its digit and letter runs; every other character only separates them.
	 * @param version version, not null
	 * @return non-empty segments, each made of ASCII digits only or ASCII letters only
	 */
	private static List<String> segments(String version) {
		List<String> segments = new ArrayList<>();
		int i = 0;
		int n = version.length();
		while (i < n) {
			char ch = version.charAt(i);
			if (NumericStrings.isDigit(ch)) {
				int start = i;
				while (i < n && NumericStrings.isDigit(version.charAt(i))) i++;
				segments.add(version.substring(start, i));
			} else if (NumericStrings.isLetter(ch)) {
				int start = i;
				while (i < n && NumericStrings.isLetter(version.charAt(i))) i++;
				segments.add(version.substring(start, i));
			} else {
				i++;
			}
		}
		return segments;
	}
}

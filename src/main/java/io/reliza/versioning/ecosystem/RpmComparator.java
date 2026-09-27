/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orders RPM package versions written as {@code [epoch:]version[-release]}.
 *
 * <p>The rules follow the prose description of RPM version ordering in the Fedora Packaging
 * Guidelines, "Versioning" page
 * (https://docs.fedoraproject.org/en-US/packaging-guidelines/Versioning/), and were checked
 * against black-box observations of rpm itself.
 *
 * <p>Splitting the string:
 * <ul>
 * <li>An epoch exists only when the string opens with one or more ASCII digits immediately
 * followed by a colon. It is compared as a non-negative integer of any size; when absent it
 * counts as 0, so {@code 0:1.0-1} equals {@code 1.0-1}. A string such as {@code :1.0} has no
 * epoch, and its colon is just a separator inside the version.</li>
 * <li>The release is whatever follows the last hyphen; the version is what sits between the
 * epoch and that hyphen. Without a hyphen there is no release.</li>
 * <li>Epochs are compared first, then versions, then releases.</li>
 * </ul>
 *
 * <p>Comparing a version (or a release) string:
 * <ul>
 * <li>The string is read as a sequence of pieces: a maximal run of ASCII digits, a maximal run
 * of ASCII letters, a single tilde, or a single caret. Every other character, including
 * punctuation and non-ASCII characters, only separates pieces and is otherwise ignored, so
 * {@code 2.0} equals {@code 2_0} and {@code 1.0.} equals {@code 1.0}.</li>
 * <li>The two piece sequences are walked side by side. A finished sequence behaves as if it
 * continued with an endless run of "end" markers, and the first position where the two differ
 * decides the result.</li>
 * <li>At a single position the ranking from lowest to highest is: tilde, end, caret, letter
 * run, digit run. So a tilde sorts before everything, even the end of the string
 * ({@code 1.0~rc1 < 1.0}); a caret sorts after the end of the string but before any other
 * continuation ({@code 1.0 < 1.0^git1 < 1.0.1}); and a digit run beats a letter run
 * ({@code 1.0a < 1.0.1}).</li>
 * <li>Two digit runs compare by numeric value, ignoring leading zeros and never overflowing
 * ({@code 10.0001} equals {@code 10.1}). Two letter runs compare by ASCII code, so upper case
 * sorts before lower case. Two tildes, or two carets, are equal.</li>
 * </ul>
 *
 * <p>Design decision of this library: rpm's dependency matching ignores the release when only
 * one side carries one. Here a missing release is instead treated exactly like an empty release
 * string, which keeps the ordering a consistent total order. As a result {@code 1.0 < 1.0-1},
 * while {@code 1.0} and {@code 1.0-} are equal.
 */
final class RpmComparator implements Comparator<String> {

	/**
	 * Kinds of piece a version string is read into. The declaration order is the ranking used
	 * when two different kinds meet at the same position, lowest first.
	 */
	private enum Kind {
		TILDE,
		END,
		CARET,
		LETTERS,
		DIGITS
	}

	/**
	 * One piece of a version string.
	 * @param kind what the piece is
	 * @param text the characters of a digit or letter run, empty for the other kinds
	 */
	private record Token(Kind kind, String text) {
	}

	private static final Token END_TOKEN = new Token(Kind.END, "");

	/**
	 * An EVR string split into its three parts.
	 * @param epoch the epoch digits, "0" when absent
	 * @param version the version part
	 * @param release the release part, empty when absent
	 */
	private record Parsed(String epoch, String version, String release) {

		/**
		 * Splits an EVR string into epoch, version and release.
		 * @param evr trimmed, non-null {@code [epoch:]version[-release]} string
		 * @return the parts
		 */
		static Parsed parse(String evr) {
			String epoch = "0";
			String rest = evr;
			int digitsEnd = 0;
			while (digitsEnd < evr.length() && NumericStrings.isDigit(evr.charAt(digitsEnd))) {
				digitsEnd++;
			}
			if (digitsEnd > 0 && digitsEnd < evr.length() && evr.charAt(digitsEnd) == ':') {
				epoch = evr.substring(0, digitsEnd);
				rest = evr.substring(digitsEnd + 1);
			}
			int hyphen = rest.lastIndexOf('-');
			if (hyphen < 0) return new Parsed(epoch, rest, "");
			return new Parsed(epoch, rest.substring(0, hyphen), rest.substring(hyphen + 1));
		}
	}

	@Override
	public int compare(String a, String b) {
		Parsed x = Parsed.parse(a);
		Parsed y = Parsed.parse(b);
		int c = NumericStrings.compare(x.epoch(), y.epoch());
		if (c != 0) return c;
		c = compareSegments(x.version(), y.version());
		if (c != 0) return c;
		return compareSegments(x.release(), y.release());
	}

	/**
	 * Compares two version or release strings piece by piece, as described on the class.
	 * @param a version or release string
	 * @param b version or release string
	 * @return -1, 0 or 1 as a sorts before, together with or after b
	 */
	private static int compareSegments(String a, String b) {
		List<Token> x = tokenize(a);
		List<Token> y = tokenize(b);
		int n = Math.max(x.size(), y.size());
		for (int i = 0; i < n; i++) {
			Token p = i < x.size() ? x.get(i) : END_TOKEN;
			Token q = i < y.size() ? y.get(i) : END_TOKEN;
			int c = compareTokens(p, q);
			if (c != 0) return c;
		}
		return 0;
	}

	private static int compareTokens(Token p, Token q) {
		if (p.kind() != q.kind()) return Integer.signum(p.kind().compareTo(q.kind()));
		return switch (p.kind()) {
			case DIGITS -> NumericStrings.compare(p.text(), q.text());
			case LETTERS -> Integer.signum(p.text().compareTo(q.text()));
			default -> 0;
		};
	}

	private static List<Token> tokenize(String s) {
		List<Token> tokens = new ArrayList<>();
		int i = 0;
		while (i < s.length()) {
			char ch = s.charAt(i);
			if (ch == '~') {
				tokens.add(new Token(Kind.TILDE, ""));
				i++;
			} else if (ch == '^') {
				tokens.add(new Token(Kind.CARET, ""));
				i++;
			} else if (NumericStrings.isDigit(ch)) {
				int start = i;
				while (i < s.length() && NumericStrings.isDigit(s.charAt(i))) i++;
				tokens.add(new Token(Kind.DIGITS, s.substring(start, i)));
			} else if (NumericStrings.isLetter(ch)) {
				int start = i;
				while (i < s.length() && NumericStrings.isLetter(s.charAt(i))) i++;
				tokens.add(new Token(Kind.LETTERS, s.substring(start, i)));
			} else {
				i++;
			}
		}
		return tokens;
	}
}

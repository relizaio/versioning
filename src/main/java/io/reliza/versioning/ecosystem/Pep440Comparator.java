/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Python version ordering as specified by PEP 440 (https://peps.python.org/pep-0440/), using the
 * spec's normalisation rules: epoch, then release (trailing zeros ignored), then pre-release
 * (a &lt; b &lt; rc; alpha, beta, c, pre and preview are aliases), post-release, development
 * release and local version label. A version with only a dev segment sorts before its
 * pre-releases, so 1.0.dev0 &lt; 1.0a1 &lt; 1.0 &lt; 1.0.post1.
 *
 * <p>A string that is not a valid PEP 440 version is read as its longest prefix that is one,
 * cut where a run of letters or digits ends, followed by a local version label made of the
 * alphanumeric runs of the rest: "2.0rc1-foo" sorts like 2.0rc1+foo, below 2.0, and
 * "0.9.8-final" like 0.9.8+final, just above 0.9.8. Without such a prefix the release counts
 * as zero.</p>
 */
final class Pep440Comparator implements Comparator<String> {

	// canonical pattern from PEP 440, Appendix B
	private static final Pattern PEP440 = Pattern.compile(
			"^\\s*v?"
			+ "(?:(?<epoch>[0-9]+)!)?"
			+ "(?<release>[0-9]+(?:\\.[0-9]+)*)"
			+ "(?<pre>[-_.]?(?<preL>alpha|a|beta|b|preview|pre|c|rc)[-_.]?(?<preN>[0-9]+)?)?"
			+ "(?<post>(?:-(?<postN1>[0-9]+))|(?:[-_.]?(?<postL>post|rev|r)[-_.]?(?<postN2>[0-9]+)?))?"
			+ "(?<dev>[-_.]?(?<devL>dev)[-_.]?(?<devN>[0-9]+)?)?"
			+ "(?:\\+(?<local>[a-z0-9]+(?:[-_.][a-z0-9]+)*))?"
			+ "\\s*$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Where a missing optional segment sorts relative to a present one.
	 */
	private enum AbsentSorts {
		FIRST,
		LAST
	}

	/**
	 * Normalised pre-release labels, in precedence order.
	 */
	private enum PreLabel {
		A,
		B,
		RC
	}

	/**
	 * Normalised version. Absent segments are null.
	 */
	private record Parsed(String epoch, String[] release, PreLabel preLabel, String preNumber,
			String post, String dev, String[] local) {}

	@Override
	public int compare(String a, String b) {
		Parsed pa = parse(a);
		Parsed pb = parse(b);
		int c = NumericStrings.compare(pa.epoch(), pb.epoch());
		if (c == 0) c = NumericStrings.compareParts(pa.release(), pb.release());
		if (c == 0) c = Integer.compare(preRank(pa), preRank(pb));
		if (c == 0 && pa.preLabel() != null) {
			c = Integer.signum(pa.preLabel().compareTo(pb.preLabel()));
			if (c == 0) c = NumericStrings.compare(pa.preNumber(), pb.preNumber());
		}
		if (c == 0) c = compareOptional(pa.post(), pb.post(), AbsentSorts.FIRST);
		if (c == 0) c = compareOptional(pa.dev(), pb.dev(), AbsentSorts.LAST);
		if (c == 0) c = compareLocal(pa.local(), pb.local());
		return c;
	}

	/**
	 * Checks for a valid PEP 440 version, as matched by the Appendix B pattern, without the
	 * fallback reading of other strings.
	 * @param version version, not null
	 * @return true when the version is valid PEP 440
	 */
	static boolean isValid(String version) {
		return PEP440.matcher(version).matches();
	}

	private static Parsed parse(String version) {
		Parsed strict = parseStrict(version);
		return strict != null ? strict : parseLenient(version);
	}

	private static Parsed parseStrict(String version) {
		Matcher m = PEP440.matcher(version);
		if (!m.matches()) return null;
		String epoch = m.group("epoch") == null ? "0" : m.group("epoch");
		PreLabel preLabel = null;
		String preNumber = null;
		if (m.group("pre") != null) {
			preLabel = switch (m.group("preL").toLowerCase(Locale.ROOT)) {
				case "alpha", "a" -> PreLabel.A;
				case "beta", "b" -> PreLabel.B;
				default -> PreLabel.RC;
			};
			preNumber = orZero(m.group("preN"));
		}
		String post = null;
		if (m.group("post") != null) {
			post = m.group("postN1") != null ? m.group("postN1") : orZero(m.group("postN2"));
		}
		String dev = m.group("dev") == null ? null : orZero(m.group("devN"));
		String[] local = m.group("local") == null ? null : m.group("local").toLowerCase(Locale.ROOT).split("[-_.]");
		return new Parsed(epoch, m.group("release").split("\\."), preLabel, preNumber, post, dev, local);
	}

	private static Parsed parseLenient(String version) {
		for (int end = version.length() - 1; end > 0; end--) {
			if (!isRunBoundary(version, end)) continue;
			Parsed p = parseStrict(version.substring(0, end));
			if (p != null) {
				String[] local = Stream.concat(p.local() == null ? Stream.<String>empty() : Arrays.stream(p.local()),
						Arrays.stream(alphanumericRuns(version.substring(end)))).toArray(String[]::new);
				return new Parsed(p.epoch(), p.release(), p.preLabel(), p.preNumber(), p.post(), p.dev(),
						local.length == 0 ? null : local);
			}
		}
		String[] local = alphanumericRuns(version);
		return new Parsed("0", new String[0], null, null, null, null, local.length == 0 ? null : local);
	}

	/**
	 * Whether position i of s ends a run of letters or a run of digits.
	 */
	private static boolean isRunBoundary(String s, int i) {
		char prev = s.charAt(i - 1);
		char next = s.charAt(i);
		boolean prevAlnum = NumericStrings.isDigit(prev) || NumericStrings.isLetter(prev);
		boolean nextAlnum = NumericStrings.isDigit(next) || NumericStrings.isLetter(next);
		return !prevAlnum || !nextAlnum || NumericStrings.isDigit(prev) != NumericStrings.isDigit(next);
	}

	private static String[] alphanumericRuns(String s) {
		return Arrays.stream(s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
				.filter(run -> !run.isEmpty())
				.toArray(String[]::new);
	}

	private static String orZero(String digits) {
		return digits == null ? "0" : digits;
	}

	/**
	 * Where the pre-release segment places the version relative to its release: a dev-only
	 * version sorts before every pre-release (-1), a pre-release is ranked by label and number
	 * (0), anything else sorts after all pre-releases (1).
	 */
	private static int preRank(Parsed p) {
		if (p.preLabel() != null) return 0;
		if (p.post() == null && p.dev() != null) return -1;
		return 1;
	}

	/**
	 * Compares optional numeric segments.
	 */
	private static int compareOptional(String a, String b, AbsentSorts absent) {
		if (a == null && b == null) return 0;
		int absentSign = absent == AbsentSorts.FIRST ? -1 : 1;
		if (a == null) return absentSign;
		if (b == null) return -absentSign;
		return NumericStrings.compare(a, b);
	}

	/**
	 * Local labels: absent sorts first; segments compare pairwise with numeric segments above
	 * alphanumeric ones, numbers numerically and strings lexically; a shorter label that is a
	 * prefix of a longer one sorts first.
	 */
	private static int compareLocal(String[] a, String[] b) {
		if (a == null || b == null) return a == b ? 0 : (a == null ? -1 : 1);
		int n = Math.min(a.length, b.length);
		for (int i = 0; i < n; i++) {
			boolean an = NumericStrings.isDigits(a[i]);
			boolean bn = NumericStrings.isDigits(b[i]);
			int c;
			if (an && bn) {
				c = NumericStrings.compare(a[i], b[i]);
			} else if (an != bn) {
				c = an ? 1 : -1;
			} else {
				c = Integer.signum(a[i].compareTo(b[i]));
			}
			if (c != 0) return c;
		}
		return Integer.compare(a.length, b.length);
	}
}

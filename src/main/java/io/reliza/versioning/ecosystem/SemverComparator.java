/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic Versioning 2.0.0 precedence (https://semver.org/#spec-item-11), made lenient for
 * ecosystems that use semver-like versions. A version is read as an optional "v" before a
 * dotted numeric release of any length (missing parts compare as zero, so "1.2" equals "1.2.0"
 * and NuGet's "1.2.3.4" works) and everything after it, less one leading "-", "." or "_"
 * separator, as dot-separated pre-release identifiers. Build metadata from the first "+" on is
 * ignored. For valid SemVer this is exactly SemVer precedence; any other string still gets a
 * place in the same total order, with a trailing suffix ranking like a pre-release.
 */
final class SemverComparator implements Comparator<String> {

	private static final Pattern RELEASE_AND_REST = Pattern.compile("^(?:[vV](?=\\d))?(\\d+(?:\\.\\d+)*)?(.*)$", Pattern.DOTALL);

	/**
	 * How alphanumeric pre-release identifiers compare: in ASCII order as SemVer specifies, or
	 * ignoring case as NuGet does.
	 */
	enum LabelCase {
		SENSITIVE,
		INSENSITIVE
	}

	private final LabelCase labelCase;

	/**
	 * @param labelCase how alphanumeric pre-release identifiers compare
	 */
	SemverComparator(LabelCase labelCase) {
		this.labelCase = labelCase;
	}

	private record Parsed(String[] release, String[] preRelease) {}

	@Override
	public int compare(String a, String b) {
		Parsed pa = parse(a);
		Parsed pb = parse(b);
		int c = NumericStrings.compareParts(pa.release(), pb.release());
		if (c != 0) return c;
		return comparePreRelease(pa.preRelease(), pb.preRelease());
	}

	private static Parsed parse(String version) {
		int plus = version.indexOf('+');
		Matcher m = RELEASE_AND_REST.matcher(plus < 0 ? version : version.substring(0, plus));
		m.matches();
		String[] release = m.group(1) == null ? new String[0] : m.group(1).split("\\.");
		String rest = m.group(2);
		if (!rest.isEmpty() && "-._".indexOf(rest.charAt(0)) >= 0) rest = rest.substring(1);
		String[] preRelease = rest.isEmpty() ? new String[0] : rest.split("\\.", -1);
		return new Parsed(release, preRelease);
	}

	private int comparePreRelease(String[] a, String[] b) {
		// a version without pre-release identifiers has higher precedence
		if (a.length == 0 || b.length == 0) return Integer.compare(b.length, a.length);
		int n = Math.min(a.length, b.length);
		for (int i = 0; i < n; i++) {
			int c = compareIdentifier(a[i], b[i]);
			if (c != 0) return c;
		}
		return Integer.compare(a.length, b.length);
	}

	private int compareIdentifier(String a, String b) {
		boolean aNumeric = NumericStrings.isDigits(a);
		boolean bNumeric = NumericStrings.isDigits(b);
		if (aNumeric && bNumeric) return NumericStrings.compare(a, b);
		// numeric identifiers have lower precedence than alphanumeric ones
		if (aNumeric) return -1;
		if (bNumeric) return 1;
		if (labelCase == LabelCase.INSENSITIVE) return Integer.signum(a.toUpperCase(Locale.ROOT).compareTo(b.toUpperCase(Locale.ROOT)));
		return Integer.signum(a.compareTo(b));
	}
}

/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * Package ecosystems whose version strings follow distinct ordering rules. Each constant
 * carries a comparator implementing that ecosystem's precedence rules, so that versions
 * of third-party components (as identified by a package URL) can be ordered and checked
 * against vulnerable version ranges.
 *
 * <p>Every comparator except {@link #MAVEN} defines a consistent total order over all strings,
 * including strings that are not well-formed versions of the ecosystem, so it is safe for
 * sorting. Surrounding whitespace is ignored, and only the first {@value #MAX_COMPARED_LENGTH}
 * characters of a version take part in the comparison, which bounds the cost of hostile input.
 * The comparators are stateless and thread-safe,
 * and they are not consistent with {@link String#equals(Object)}: for example under SEMVER
 * "1.0" and "1.0.0" compare as equal.</p>
 *
 * <p>Because the order is total, a comparison alone cannot tell a real ordering from a guess
 * about a string that is not a version of the ecosystem at all. {@link #canParse(String)} is the
 * strict test for that, and {@link EcosystemVersions} combines the two, answering "cannot say"
 * instead of ordering such strings.</p>
 */
public enum Ecosystem {
	/**
	 * Semantic Versioning 2.0.0 precedence. Lenient: a leading "v" is ignored, any number of
	 * numeric release parts is accepted (missing parts count as zero) and whatever follows the
	 * release is treated as pre-release identifiers. Build metadata is ignored, which Dart's pub
	 * does not do (it orders "1.0.0+1" after "1.0.0").
	 */
	SEMVER(new SemverComparator(SemverComparator.LabelCase.SENSITIVE), VersionSyntax::isSemver,
			Set.of("npm", "cargo", "golang", "hex", "pub", "swift", "cocoapods")),
	/**
	 * NuGet: SEMVER precedence with case-insensitive pre-release labels.
	 */
	NUGET(new SemverComparator(SemverComparator.LabelCase.INSENSITIVE), VersionSyntax::isNuget, Set.of("nuget")),
	/**
	 * Apache Maven ComparableVersion ordering, reproduced exactly. Like Maven's own, it is not
	 * transitive for some unusual versions, such as a qualifier between numbers:
	 * 1.foo.2 &lt; 1-rc &lt; 1 &lt; 1.foo.2. Sorting a list that mixes such versions can fail.
	 */
	MAVEN(new MavenComparator(), VersionSyntax::isDigitFirst, Set.of("maven")),
	/**
	 * PEP 440 ordering. A string that is not a valid PEP 440 version is read as its leading
	 * release number followed by a local version label made of the rest.
	 */
	PYPI(new Pep440Comparator(), VersionSyntax::isPep440, Set.of("pypi")),
	/**
	 * Debian dpkg ordering, with epoch and revision.
	 */
	DEBIAN(new DpkgComparator(), VersionSyntax::isDebian, Set.of("deb")),
	/**
	 * RPM rpmvercmp ordering, with epoch and release.
	 */
	RPM(new RpmComparator(), VersionSyntax::isRpm, Set.of("rpm")),
	/**
	 * Alpine apk ordering.
	 */
	ALPINE(new ApkComparator(), VersionSyntax::isApk, Set.of("apk")),
	/**
	 * RubyGems Gem::Version ordering: pre-releases, marked by a letter segment such as
	 * "1.0.0.rc1" or "1.0.0-rc1", sort below their release.
	 */
	GEM(new GemComparator(), VersionSyntax::isGem, Set.of("gem")),
	/**
	 * Generic dotted-number ordering based on Dependency-Track's ComponentVersion. Used for every
	 * package type without dedicated rules.
	 */
	GENERIC(new GenericComparator(), VersionSyntax::isDigitFirst, Set.of("generic"));

	/**
	 * Versions are compared on at most this many characters, after stripping whitespace.
	 */
	public static final int MAX_COMPARED_LENGTH = 256;

	private static final Map<String, Ecosystem> purlTypeLookupMap;
	static {
		HashMap<String, Ecosystem> purlTypeLookupMapBuild = new HashMap<>();
		for (Ecosystem e : Ecosystem.values()) {
			e.getPurlTypes().forEach(type -> purlTypeLookupMapBuild.put(type, e));
		}
		purlTypeLookupMap = Collections.unmodifiableMap(purlTypeLookupMapBuild);
	}

	private final Comparator<String> comparator;
	private final Predicate<String> syntax;
	private final Set<String> purlTypes;

	/**
	 * Private Ecosystem enum constructor
	 * @param rules comparator implementing the ecosystem's ordering on stripped, non-null versions
	 * @param syntax strict validity test on stripped, non-empty versions
	 * @param purlTypes lowercase package URL types that use this ecosystem's ordering
	 */
	private Ecosystem(Comparator<String> rules, Predicate<String> syntax, Set<String> purlTypes) {
		this.comparator = (a, b) -> rules.compare(bounded(a), bounded(b));
		this.syntax = syntax;
		this.purlTypes = purlTypes;
	}

	private static String bounded(String version) {
		String v = strip(version);
		return v.length() > MAX_COMPARED_LENGTH ? v.substring(0, MAX_COMPARED_LENGTH) : v;
	}

	/**
	 * Removes surrounding whitespace, including Unicode spaces such as the no-break space that
	 * String.strip() keeps.
	 * @param s string, not null
	 * @return s without leading and trailing whitespace
	 */
	static String strip(String s) {
		int start = 0;
		int end = s.length();
		while (start < end && isSpace(s.charAt(start))) start++;
		while (end > start && isSpace(s.charAt(end - 1))) end--;
		return s.substring(start, end);
	}

	private static boolean isSpace(char c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c);
	}

	/**
	 * Gets the comparator implementing this ecosystem's version precedence. It does not accept null.
	 * @return comparator of version strings
	 */
	public Comparator<String> getComparator() {
		return comparator;
	}

	/**
	 * Gets the package URL types, lowercase, that resolve to this ecosystem
	 * @return purl types set
	 */
	public Set<String> getPurlTypes() {
		return purlTypes;
	}

	/**
	 * Compares two version strings under this ecosystem's precedence rules.
	 * @param a first version, not null
	 * @param b second version, not null
	 * @return negative, zero or positive as a is lower than, equal to or higher than b
	 */
	public int compare(String a, String b) {
		return comparator.compare(a, b);
	}

	/**
	 * Checks whether a string is a well-formed version of this ecosystem, so that its place in
	 * this ecosystem's order is meaningful. This is stricter than {@link #compare(String, String)},
	 * which orders any string. Surrounding whitespace is ignored, as in comparisons. The tests are:
	 * <ul>
	 * <li>SEMVER: the SemVer 2.0.0 grammar (no leading zeros, non-empty identifiers), with an
	 * optional leading "v" or "V" and optional minor and patch, as in "v1" or "4.7". A Go
	 * pseudo-version such as "v0.0.0-20230101000000-abcdef123456" passes.</li>
	 * <li>NUGET: as SEMVER, also allowing a fourth numeric part, as in "1.2.3.4".</li>
	 * <li>MAVEN, GENERIC: the first character is a digit. This is only a plausibility test. A
	 * leading "v" fails it, because these comparators do not skip the "v" and would order the
	 * version as text: under MAVEN "v2.0" is a qualifier followed by 2.0 and sorts below "1.0", as
	 * in Maven, and under GENERIC "v10" is one text part, so "v9.0" sorts above "v10.0", as in
	 * Dependency-Track.</li>
	 * <li>PYPI: a valid PEP 440 version, as matched by the pattern of PEP 440 Appendix B.</li>
	 * <li>DEBIAN: [epoch:]upstream_version[-debian_revision] per Debian Policy 5.6.12. The epoch
	 * is digits; the upstream version starts with a digit and contains only ASCII letters, digits
	 * and ". + ~ -", plus ":" when there is an epoch; the revision after the last hyphen is
	 * non-empty and contains only ASCII letters, digits and ". + ~".</li>
	 * <li>RPM: an optional digits-only epoch and colon, then a version starting with a digit and
	 * containing only ASCII letters, digits and ". _ + ~ ^", then optionally one hyphen and a
	 * non-empty release made of the same characters.</li>
	 * <li>ALPINE: the whole string is an apk version: dot-separated numbers, optional letters,
	 * any number of suffixes (_alpha, _beta, _pre, _rc, _cvs, _svn, _git, _hg or _p, each with an
	 * optional number), an optional "~" and lowercase hex commit hash, and an optional revision
	 * "-r" followed by a number.</li>
	 * <li>GEM: the Gem::Version pattern
	 * {@code [0-9]+(\.[0-9a-zA-Z]+)*(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?}.</li>
	 * </ul>
	 * A version longer than {@value #MAX_COMPARED_LENGTH} characters after stripping is not
	 * parseable, because comparisons would only read part of it.
	 * @param version version to check; may be null
	 * @return true when the version is well formed; false for null, blank, over-long or malformed
	 * versions. Never throws.
	 */
	public boolean canParse(String version) {
		if (version == null) return false;
		String v = strip(version);
		return !v.isEmpty() && v.length() <= MAX_COMPARED_LENGTH && syntax.test(v);
	}

	/**
	 * Resolves the ecosystem for a package URL type, e.g. "npm" or "maven".
	 * @param purlType purl type, case-insensitive; may be null
	 * @return matching ecosystem, {@link #GENERIC} when the type is null, blank or has no dedicated rules
	 */
	public static Ecosystem fromPurlType(String purlType) {
		if (StringUtils.isBlank(purlType)) return GENERIC;
		return purlTypeLookupMap.getOrDefault(purlType.trim().toLowerCase(Locale.ROOT), GENERIC);
	}

	/**
	 * Resolves the ecosystem from a full package URL, e.g. "pkg:maven/org.example/lib@1.0".
	 * Only the type segment is inspected.
	 * @param purl package URL; may be null
	 * @return matching ecosystem, {@link #GENERIC} when the purl is null or malformed
	 */
	public static Ecosystem fromPurl(String purl) {
		if (StringUtils.isBlank(purl)) return GENERIC;
		String rest = purl.trim();
		if (!rest.regionMatches(true, 0, "pkg:", 0, 4)) return GENERIC;
		rest = StringUtils.stripStart(rest.substring(4), "/");
		int slash = rest.indexOf('/');
		return slash > 0 ? fromPurlType(rest.substring(0, slash)) : GENERIC;
	}
}

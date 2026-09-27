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
 */
public enum Ecosystem {
	/**
	 * Semantic Versioning 2.0.0 precedence. Lenient: a leading "v" is ignored, any number of
	 * numeric release parts is accepted (missing parts count as zero) and whatever follows the
	 * release is treated as pre-release identifiers. Build metadata is ignored, which Dart's pub
	 * does not do (it orders "1.0.0+1" after "1.0.0").
	 */
	SEMVER(new SemverComparator(SemverComparator.LabelCase.SENSITIVE), Set.of("npm", "cargo", "golang", "hex", "pub", "swift", "cocoapods")),
	/**
	 * NuGet: SEMVER precedence with case-insensitive pre-release labels.
	 */
	NUGET(new SemverComparator(SemverComparator.LabelCase.INSENSITIVE), Set.of("nuget")),
	/**
	 * Apache Maven ComparableVersion ordering, reproduced exactly. Like Maven's own, it is not
	 * transitive for some unusual versions, such as a qualifier between numbers:
	 * 1.foo.2 &lt; 1-rc &lt; 1 &lt; 1.foo.2. Sorting a list that mixes such versions can fail.
	 */
	MAVEN(new MavenComparator(), Set.of("maven")),
	/**
	 * PEP 440 ordering. A string that is not a valid PEP 440 version is read as its leading
	 * release number followed by a local version label made of the rest.
	 */
	PYPI(new Pep440Comparator(), Set.of("pypi")),
	/**
	 * Debian dpkg ordering, with epoch and revision.
	 */
	DEBIAN(new DpkgComparator(), Set.of("deb")),
	/**
	 * RPM rpmvercmp ordering, with epoch and release.
	 */
	RPM(new RpmComparator(), Set.of("rpm")),
	/**
	 * Alpine apk ordering.
	 */
	ALPINE(new ApkComparator(), Set.of("apk")),
	/**
	 * Generic dotted-number ordering based on Dependency-Track's ComponentVersion. Used for every
	 * package type without dedicated rules.
	 */
	GENERIC(new GenericComparator(), Set.of("generic"));

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
	private final Set<String> purlTypes;

	/**
	 * Private Ecosystem enum constructor
	 * @param rules comparator implementing the ecosystem's ordering on stripped, non-null versions
	 * @param purlTypes lowercase package URL types that use this ecosystem's ordering
	 */
	private Ecosystem(Comparator<String> rules, Set<String> purlTypes) {
		this.comparator = (a, b) -> rules.compare(bounded(a), bounded(b));
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

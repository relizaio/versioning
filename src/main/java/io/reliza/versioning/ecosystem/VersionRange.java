/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.Objects;

/**
 * An immutable interval of versions with optional inclusive or exclusive bounds, such as the
 * affected range of a vulnerability ("from 1.2.0 inclusive, up to 1.2.5 exclusive"). Bounds are
 * kept as strings; ordering is supplied by an {@link Ecosystem} when checking membership, so one
 * range can be evaluated under the rules of the package it describes.
 */
public final class VersionRange {

	private static final VersionRange ALL = new VersionRange(null, false, null, false);

	private final String lower;
	private final boolean lowerInclusive;
	private final String upper;
	private final boolean upperInclusive;

	private VersionRange(String lower, boolean lowerInclusive, String upper, boolean upperInclusive) {
		this.lower = lower;
		this.lowerInclusive = lower != null && lowerInclusive;
		this.upper = upper;
		this.upperInclusive = upper != null && upperInclusive;
	}

	/**
	 * Range matching every version.
	 * @return unbounded range
	 */
	public static VersionRange all() {
		return ALL;
	}

	/**
	 * Range matching a single version, compared under the ecosystem's rules (so under SEMVER
	 * exact("1.0") also matches "1.0.0"). The wildcard "*" matches every version.
	 * @param version the version, not blank
	 * @return range containing only that version
	 * @throws IllegalArgumentException when the version is blank
	 */
	public static VersionRange exact(String version) {
		String v = version == null ? "" : Ecosystem.strip(version);
		if (v.isEmpty()) throw new IllegalArgumentException("Exact version must not be blank");
		if (v.equals("*")) return ALL;
		return new VersionRange(v, true, v, true);
	}

	/**
	 * Range between two optional bounds. A lower bound above the upper bound gives an empty range.
	 * @param lower lower bound, null, blank or "*" for none
	 * @param lowerInclusive whether the lower bound itself is in the range
	 * @param upper upper bound, null, blank or "*" for none
	 * @param upperInclusive whether the upper bound itself is in the range
	 * @return the range
	 */
	public static VersionRange between(String lower, boolean lowerInclusive, String upper, boolean upperInclusive) {
		return new VersionRange(bound(lower), lowerInclusive, bound(upper), upperInclusive);
	}

	/**
	 * Range from the four optional bounds used by NVD CPE matches and by Dependency-Track's
	 * affected components (versionStartIncluding, versionStartExcluding, versionEndIncluding,
	 * versionEndExcluding). With no bounds at all the range matches every version.
	 * @param startIncluding inclusive lower bound, null, blank or "*" for none
	 * @param startExcluding exclusive lower bound, null, blank or "*" for none
	 * @param endIncluding inclusive upper bound, null, blank or "*" for none
	 * @param endExcluding exclusive upper bound, null, blank or "*" for none
	 * @return the range
	 * @throws IllegalArgumentException when both an inclusive and an exclusive bound are given for the same end
	 */
	public static VersionRange fromBounds(String startIncluding, String startExcluding, String endIncluding, String endExcluding) {
		String si = bound(startIncluding);
		String se = bound(startExcluding);
		String ei = bound(endIncluding);
		String ee = bound(endExcluding);
		if (si != null && se != null) throw new IllegalArgumentException("Both inclusive and exclusive start given: " + si + ", " + se);
		if (ei != null && ee != null) throw new IllegalArgumentException("Both inclusive and exclusive end given: " + ei + ", " + ee);
		return new VersionRange(si != null ? si : se, si != null, ei != null ? ei : ee, ei != null);
	}

	/**
	 * Normalises a bound: stripped, with null, blank and the wildcard "*" meaning no bound.
	 */
	private static String bound(String value) {
		String v = value == null ? "" : Ecosystem.strip(value);
		return v.isEmpty() || v.equals("*") ? null : v;
	}

	/**
	 * Checks whether a version lies within this range under the given ecosystem's ordering.
	 * @param ecosystem ecosystem whose ordering applies, not null
	 * @param version version to check; null or blank is never contained
	 * @return true when the version satisfies both bounds
	 */
	public boolean contains(Ecosystem ecosystem, String version) {
		Objects.requireNonNull(ecosystem, "ecosystem");
		if (version == null || Ecosystem.strip(version).isEmpty()) return false;
		if (lower != null) {
			int c = ecosystem.compare(version, lower);
			if (c < 0 || (c == 0 && !lowerInclusive)) return false;
		}
		if (upper != null) {
			int c = ecosystem.compare(version, upper);
			if (c > 0 || (c == 0 && !upperInclusive)) return false;
		}
		return true;
	}

	/**
	 * Getter for the lower bound.
	 * @return lower bound, null when unbounded below
	 */
	public String getLower() {
		return lower;
	}

	/**
	 * Whether the lower bound itself is in the range.
	 * @return true for an inclusive lower bound; false when exclusive or unbounded below
	 */
	public boolean isLowerInclusive() {
		return lowerInclusive;
	}

	/**
	 * Getter for the upper bound.
	 * @return upper bound, null when unbounded above
	 */
	public String getUpper() {
		return upper;
	}

	/**
	 * Whether the upper bound itself is in the range.
	 * @return true for an inclusive upper bound; false when exclusive or unbounded above
	 */
	public boolean isUpperInclusive() {
		return upperInclusive;
	}

	/**
	 * Whether the range holds a single version, as created by {@link #exact(String)}.
	 * @return true when both bounds are the same inclusive version
	 */
	public boolean isExact() {
		return lower != null && lower.equals(upper) && lowerInclusive && upperInclusive;
	}

	/**
	 * Structural equality on the bounds as given: exact("1.0") and exact("1.0.0") are different
	 * ranges even where an ecosystem treats the two versions as equal.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof VersionRange other)) return false;
		return lowerInclusive == other.lowerInclusive && upperInclusive == other.upperInclusive
				&& Objects.equals(lower, other.lower) && Objects.equals(upper, other.upper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lower, lowerInclusive, upper, upperInclusive);
	}

	/**
	 * Interval notation as used by Maven version ranges: "[1.0,2.0)", "(,1.5]", "[1.0]" for an
	 * exact version, "(,)" for all versions.
	 */
	@Override
	public String toString() {
		if (isExact()) return "[" + lower + "]";
		return (lowerInclusive ? "[" : "(") + Objects.toString(lower, "") + ","
				+ Objects.toString(upper, "") + (upperInclusive ? "]" : ")");
	}
}

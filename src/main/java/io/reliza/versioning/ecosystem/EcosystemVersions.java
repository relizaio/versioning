/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.Optional;

/**
 * Strict version comparisons that only answer when the answer is meaningful. The comparators of
 * {@link Ecosystem} are total, so they order any string, including ones that are not versions of
 * the ecosystem at all ("latest", a commit hash, a typo). The methods here first check every
 * version they are given with {@link Ecosystem#canParse(String)} and answer "cannot say" (an
 * empty Optional, or {@link RangeMembership#UNKNOWN}) when one of them is not a well-formed
 * version; the caller decides what an uncomparable version means for it. When all versions are
 * well formed the answer is the one the total comparator gives.
 *
 * <p>No method throws on bad input: a null ecosystem, version or range gives "cannot say".</p>
 *
 * <p>Remaining cases callers should know about:</p>
 * <ul>
 * <li>For MAVEN and GENERIC, {@link Ecosystem#canParse(String)} only requires a leading digit, so
 * strings such as "1.x" or "1.2.*" pass and are ordered by the comparator's rules rather than
 * read as wildcards, and MAVEN keeps Maven's own non-transitive ordering of unusual versions
 * such as "1.foo.2" (1.foo.2 &lt; 1-rc &lt; 1 &lt; 1.foo.2).</li>
 * <li>Building the range is not covered: {@link VersionRange#fromBounds(String, String, String, String)}
 * throws IllegalArgumentException when both an inclusive and an exclusive bound are given for the
 * same end, and {@link VersionRange#exact(String)} throws it for a blank version. Callers that
 * build ranges from external data should catch IllegalArgumentException.</li>
 * <li>RPM package URLs carry the epoch in the {@code epoch} qualifier, not in the version. Put it
 * back in front ("epoch:version") before comparing: without it "1.1.1k-9.el8_6" is compared as
 * epoch 0 and reads as inside "&lt; 1:1.1.1k-8.el8_6", although it is the fixed version.</li>
 * <li>GENERIC follows Dependency-Track and ranks a qualifier above its release, so "2.0.0-rc1"
 * is outside "&lt; 2.0.0". Package types without dedicated rules, Composer among them, use it.</li>
 * </ul>
 */
public final class EcosystemVersions {

	private EcosystemVersions() {
	}

	/**
	 * Compares two versions under an ecosystem's rules, only when both are well formed.
	 * @param ecosystem ecosystem whose ordering applies; may be null
	 * @param a first version; may be null
	 * @param b second version; may be null
	 * @return -1, 0 or 1 as a is older than, equal to or newer than b; empty when the ecosystem is
	 * null or either version is not parseable under it, meaning the order cannot be told
	 */
	public static Optional<Integer> compare(Ecosystem ecosystem, String a, String b) {
		if (ecosystem == null || !ecosystem.canParse(a) || !ecosystem.canParse(b)) return Optional.empty();
		return Optional.of(Integer.signum(ecosystem.compare(a, b)));
	}

	/**
	 * Checks whether a version lies within a range, only when the version and every bound the
	 * range has are well formed. A range without bounds, such as {@link VersionRange#all()},
	 * contains every parseable version.
	 * @param ecosystem ecosystem whose ordering applies; may be null
	 * @param version version to check; may be null
	 * @param range range to check against; may be null
	 * @return {@link RangeMembership#IN_RANGE} or {@link RangeMembership#OUT_OF_RANGE} as
	 * {@link VersionRange#contains(Ecosystem, String)} answers; {@link RangeMembership#UNKNOWN}
	 * when the ecosystem or range is null, or when the version or a bound of the range is not
	 * parseable, meaning membership cannot be told
	 */
	public static RangeMembership inRange(Ecosystem ecosystem, String version, VersionRange range) {
		if (ecosystem == null || range == null || !ecosystem.canParse(version)) return RangeMembership.UNKNOWN;
		if (range.getLower() != null && !ecosystem.canParse(range.getLower())) return RangeMembership.UNKNOWN;
		if (range.getUpper() != null && !ecosystem.canParse(range.getUpper())) return RangeMembership.UNKNOWN;
		return range.contains(ecosystem, version) ? RangeMembership.IN_RANGE : RangeMembership.OUT_OF_RANGE;
	}
}

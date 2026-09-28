/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

/**
 * Whether a version lies within a {@link VersionRange}, as answered by
 * {@link EcosystemVersions#inRange(Ecosystem, String, VersionRange)}.
 */
public enum RangeMembership {
	/**
	 * The version and every bound of the range are well-formed versions of the ecosystem, and the
	 * range contains the version.
	 */
	IN_RANGE,
	/**
	 * The version and every bound of the range are well-formed versions of the ecosystem, and the
	 * range does not contain the version.
	 */
	OUT_OF_RANGE,
	/**
	 * Membership cannot be told: the ecosystem, version or range is missing, or the version or a
	 * bound of the range is not a well-formed version of the ecosystem. The caller decides what
	 * that means for it.
	 */
	UNKNOWN
}

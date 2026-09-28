/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

/**
 * Version ordering for third-party packages, following the rules of the package ecosystem a
 * package URL (purl) type belongs to, and version ranges checked under those rules.
 *
 * <p>There are two layers:</p>
 * <ul>
 * <li>Total comparators: {@link Ecosystem#compare(String, String)},
 * {@link Ecosystem#getComparator()} and {@link VersionRange#contains(Ecosystem, String)} order
 * any string, well formed or not, so sorting never fails (MAVEN excepted, as Maven itself is not
 * transitive on some unusual versions). A string that is not a version of the ecosystem still
 * gets a place in the order, which is a guess.</li>
 * <li>Strict layer: {@link Ecosystem#canParse(String)} tells whether a string is a well-formed
 * version of the ecosystem, and {@link EcosystemVersions} answers comparisons and range checks
 * only when every version involved passes it, answering "cannot say" (an empty Optional, or
 * {@link RangeMembership#UNKNOWN}) otherwise. Use it where a wrong answer about a malformed
 * version is worse than no answer, such as deciding whether a component is affected by a
 * vulnerability.</li>
 * </ul>
 */
package io.reliza.versioning.ecosystem;

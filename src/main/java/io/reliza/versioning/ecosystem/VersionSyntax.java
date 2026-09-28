/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

import java.util.regex.Pattern;

/**
 * Strict validity tests behind {@link Ecosystem#canParse(String)}, one per ecosystem. Each test
 * takes a version that is already stripped of surrounding whitespace, non-empty and not null.
 * PYPI and ALPINE reuse their comparator's own parser, so a version passes exactly when that
 * parser reads all of it without falling back.
 */
final class VersionSyntax {

	private static final String SEMVER_NUMBER = "(?:0|[1-9][0-9]*)";

	private static final String SEMVER_PRE_RELEASE_ID = "(?:0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)";

	/**
	 * Optional pre-release and build metadata, as in the SemVer 2.0.0 grammar.
	 */
	private static final String SEMVER_TAIL = "(?:-" + SEMVER_PRE_RELEASE_ID + "(?:\\." + SEMVER_PRE_RELEASE_ID + ")*)?"
			+ "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?";

	/**
	 * SemVer 2.0.0 (https://semver.org/#backusnaur-form-grammar-for-valid-semver-versions),
	 * with an optional leading "v" and optional minor and patch.
	 */
	private static final Pattern SEMVER = Pattern.compile("[vV]?" + SEMVER_NUMBER
			+ "(?:\\." + SEMVER_NUMBER + "(?:\\." + SEMVER_NUMBER + ")?)?" + SEMVER_TAIL);

	/**
	 * As {@link #SEMVER}, with an optional fourth numeric part.
	 */
	private static final Pattern NUGET = Pattern.compile("[vV]?" + SEMVER_NUMBER
			+ "(?:\\." + SEMVER_NUMBER + "(?:\\." + SEMVER_NUMBER + "(?:\\." + SEMVER_NUMBER + ")?)?)?" + SEMVER_TAIL);

	private static final Pattern DIGIT_FIRST = Pattern.compile("[0-9].*", Pattern.DOTALL);

	private static final Pattern DEBIAN_UPSTREAM = Pattern.compile("[0-9][A-Za-z0-9.+~-]*");

	private static final Pattern DEBIAN_UPSTREAM_AFTER_EPOCH = Pattern.compile("[0-9][A-Za-z0-9.+~:-]*");

	private static final Pattern DEBIAN_REVISION = Pattern.compile("[A-Za-z0-9.+~]+");

	private static final Pattern RPM = Pattern.compile("(?:[0-9]+:)?[0-9][A-Za-z0-9._+~^]*(?:-[A-Za-z0-9._+~^]+)?");

	/**
	 * The Gem::Version pattern (https://docs.ruby-lang.org/en/master/Gem/Version.html), without
	 * the surrounding whitespace it tolerates, since callers strip it.
	 */
	private static final Pattern GEM = Pattern.compile("[0-9]+(?:\\.[0-9a-zA-Z]+)*(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?");

	private VersionSyntax() {
	}

	/**
	 * @param version stripped, non-empty version
	 * @return true for SemVer 2.0.0, allowing a leading "v" and a missing minor or patch
	 */
	static boolean isSemver(String version) {
		return SEMVER.matcher(version).matches();
	}

	/**
	 * @param version stripped, non-empty version
	 * @return true for SemVer 2.0.0, allowing a leading "v", a missing minor or patch and a fourth
	 * numeric part
	 */
	static boolean isNuget(String version) {
		return NUGET.matcher(version).matches();
	}

	/**
	 * @param version stripped, non-empty version
	 * @return true for a valid PEP 440 version
	 */
	static boolean isPep440(String version) {
		return Pep440Comparator.isValid(version);
	}

	/**
	 * A leading "v" is not skipped: the MAVEN and GENERIC comparators do not skip it either, so
	 * such a version would be ordered as text.
	 * @param version stripped, non-empty version
	 * @return true when the version starts with a digit
	 */
	static boolean isDigitFirst(String version) {
		return DIGIT_FIRST.matcher(version).matches();
	}

	/**
	 * Debian Policy 5.6.12 format [epoch:]upstream_version[-debian_revision]: the epoch is the
	 * digits before the first colon; the revision follows the last hyphen and may contain only
	 * alphanumerics and ". + ~"; the upstream version starts with a digit and may contain only
	 * alphanumerics and ". + ~ -", plus ":" when there is an epoch.
	 * @param version stripped, non-empty version
	 * @return true for a well-formed Debian version
	 */
	static boolean isDebian(String version) {
		String rest = version;
		boolean epoch = false;
		int colon = rest.indexOf(':');
		if (colon >= 0) {
			if (!NumericStrings.isDigits(rest.substring(0, colon))) return false;
			rest = rest.substring(colon + 1);
			epoch = true;
		}
		int hyphen = rest.lastIndexOf('-');
		if (hyphen >= 0) {
			if (!DEBIAN_REVISION.matcher(rest.substring(hyphen + 1)).matches()) return false;
			rest = rest.substring(0, hyphen);
		}
		return (epoch ? DEBIAN_UPSTREAM_AFTER_EPOCH : DEBIAN_UPSTREAM).matcher(rest).matches();
	}

	/**
	 * @param version stripped, non-empty version
	 * @return true for an optional digits-only epoch and colon, then a version that starts with a
	 * digit and contains only ASCII letters, digits and ". _ + ~ ^", then optionally one hyphen and
	 * a non-empty release made of the same characters
	 */
	static boolean isRpm(String version) {
		return RPM.matcher(version).matches();
	}

	/**
	 * @param version stripped, non-empty version
	 * @return true when the whole string is a well-formed apk version
	 */
	static boolean isApk(String version) {
		return ApkComparator.isValid(version);
	}

	/**
	 * @param version stripped, non-empty version
	 * @return true when the version matches the Gem::Version pattern
	 */
	static boolean isGem(String version) {
		return GEM.matcher(version).matches();
	}
}

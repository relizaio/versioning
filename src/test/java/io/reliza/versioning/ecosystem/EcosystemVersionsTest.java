package io.reliza.versioning.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EcosystemVersionsTest {

	private static final Optional<Integer> OLDER = Optional.of(-1);
	private static final Optional<Integer> SAME = Optional.of(0);
	private static final Optional<Integer> NEWER = Optional.of(1);

	@Test
	void compare_isAscendingAndNormalised() {
		assertEquals(OLDER, EcosystemVersions.compare(Ecosystem.SEMVER, "1.0.0", "2.0.0"));
		assertEquals(NEWER, EcosystemVersions.compare(Ecosystem.SEMVER, "1.0.0", "1.0.0-rc.1"));
		assertEquals(SAME, EcosystemVersions.compare(Ecosystem.SEMVER, "v1.2", "1.2.0"));
		assertEquals(OLDER, EcosystemVersions.compare(Ecosystem.PYPI, "1.8.19", "1.11.11"));
		assertEquals(NEWER, EcosystemVersions.compare(Ecosystem.DEBIAN, "1:0.1", "2.30-1"));
		assertEquals(OLDER, EcosystemVersions.compare(Ecosystem.GEM, "7.0.0.rc1", "7.0.0"));
		assertEquals(NEWER, EcosystemVersions.compare(Ecosystem.MAVEN, "1.0-sp", "1.0-beta"));
		assertEquals(OLDER, EcosystemVersions.compare(Ecosystem.MAVEN, "1.0-beta", "1.0-sp"));
	}

	@Test
	void compare_isEmptyWhenEitherSideIsUnparseable() {
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.SEMVER, "latest", "1.0.0"));
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.SEMVER, "1.0.0", "latest"));
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.SEMVER, "garbage", "garbage"));
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.PYPI, "1.0", "2.0rc1-foo"));
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.ALPINE, "1.0-r", "1.0-r1"));
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.GEM, "1.0", " "));
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.MAVEN, null, "1.0"));
		assertEquals(Optional.empty(), EcosystemVersions.compare(Ecosystem.MAVEN, "1.0", null));
		assertEquals(Optional.empty(), EcosystemVersions.compare(null, "1.0", "1.0"));
	}

	@Test
	void inRange_djangoHalfOpenRange() {
		// Dependency-Track shape: versionStartIncluding 1.11, versionEndExcluding 1.11.11
		VersionRange range = VersionRange.fromBounds("1.11", null, null, "1.11.11");
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.11", range));
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.11.5", range));
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.11.11rc1", range));
		assertEquals(RangeMembership.OUT_OF_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.11.11", range));
		assertEquals(RangeMembership.OUT_OF_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.10.8", range));
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.PYPI, "latest", range));
	}

	@Test
	void inRange_openUpperBound() {
		VersionRange range = VersionRange.fromBounds(null, "2.0", null, null);
		assertEquals(RangeMembership.OUT_OF_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "2.0", range));
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "2.0.1", range));
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "99.0", range));
		assertEquals(RangeMembership.OUT_OF_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.9", range));
	}

	@Test
	void inRange_exact() {
		VersionRange range = VersionRange.exact("1.11.1");
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.11.1", range));
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.11.1.0", range));
		assertEquals(RangeMembership.OUT_OF_RANGE, EcosystemVersions.inRange(Ecosystem.PYPI, "1.11.2", range));
	}

	@Test
	void inRange_npmHandlebars() {
		// Dependency-Track shape: only versionEndExcluding 4.7.7
		VersionRange range = VersionRange.fromBounds(null, null, null, "4.7.7");
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.SEMVER, "4.0.5", range));
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.SEMVER, "4.7.7-rc.1", range));
		assertEquals(RangeMembership.OUT_OF_RANGE, EcosystemVersions.inRange(Ecosystem.SEMVER, "4.7.7", range));
	}

	@Test
	void inRange_gemPreReleaseZeroPadding() {
		// canonical segments: 5.0.rc1 is 5.0.0.rc1, above 5.0.0.beta1 and below 5.0.0
		VersionRange range = VersionRange.fromBounds("5.0.0.beta1", null, null, "5.0.0");
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.GEM, "5.0.rc1", range));
		assertEquals(RangeMembership.OUT_OF_RANGE, EcosystemVersions.inRange(Ecosystem.GEM, "5.0", range));
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.GEM, "v5.0.rc1", range));
	}

	@Test
	void inRange_isUnknownWhenABoundIsUnparseable() {
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.SEMVER, "1.0.0", VersionRange.fromBounds("1.0.0", null, null, "latest")));
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.SEMVER, "1.0.0", VersionRange.fromBounds(null, "garbage", "2.0.0", null)));
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.PYPI, "1.0", VersionRange.exact("1.0-foo")));
		// the total order would have answered
		assertFalse(VersionRange.fromBounds("1.0.0", null, null, "latest").contains(Ecosystem.SEMVER, "1.0.0"));
	}

	@Test
	void inRange_isUnknownForNullArguments() {
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(null, "1.0", VersionRange.all()));
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.PYPI, null, VersionRange.all()));
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.PYPI, "1.0", null));
	}

	/**
	 * Dependency-Track reports some distro vulnerabilities as an identity match without any
	 * version bounds. That is VersionRange.all(): every parseable version is in range.
	 */
	@Test
	void debianIdentityOnlyMatchIsAll() {
		VersionRange identity = VersionRange.fromBounds(null, null, null, null);
		assertEquals(VersionRange.all(), identity);
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.DEBIAN, "7.68.0-1ubuntu2.7", identity));
		assertEquals(RangeMembership.IN_RANGE, EcosystemVersions.inRange(Ecosystem.DEBIAN, "1:2.30-0ubuntu1", identity));
		assertEquals(RangeMembership.UNKNOWN, EcosystemVersions.inRange(Ecosystem.DEBIAN, "latest", identity));
	}

	/**
	 * Property: over each ecosystem's own table of valid and invalid versions, the total
	 * comparator is antisymmetric, and the strict compare is either empty both ways or the
	 * negation of itself.
	 */
	@ParameterizedTest
	@EnumSource(Ecosystem.class)
	void compare_isAntisymmetricOverEachEcosystemsTable(Ecosystem e) {
		List<String> table = new ArrayList<>(EcosystemCanParseTest.VALID.get(e));
		table.addAll(EcosystemCanParseTest.INVALID.get(e));
		for (String a : table) {
			for (String b : table) {
				int ab = Integer.signum(e.compare(a, b));
				int ba = Integer.signum(e.compare(b, a));
				assertEquals(-ba, ab, e + ": antisymmetric [" + a + "] / [" + b + "]");
				Optional<Integer> strictAb = EcosystemVersions.compare(e, a, b);
				Optional<Integer> strictBa = EcosystemVersions.compare(e, b, a);
				assertEquals(strictBa.map(c -> -c), strictAb, e + ": strict antisymmetric [" + a + "] / [" + b + "]");
				if (strictAb.isPresent()) assertEquals(ab, strictAb.get(), e + ": strict agrees with total [" + a + "] / [" + b + "]");
			}
		}
	}
}

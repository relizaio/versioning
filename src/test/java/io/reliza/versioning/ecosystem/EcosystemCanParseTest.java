package io.reliza.versioning.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EcosystemCanParseTest {

	/**
	 * Well-formed versions of each ecosystem. Also used by the antisymmetry property test.
	 */
	static final Map<Ecosystem, List<String>> VALID = new EnumMap<>(Map.of(
			Ecosystem.SEMVER, List.of("1.0.0", "0.0.0", "10.20.30", "v1.2.3", "V1.2.3", "4.7", "v1", "1.0.0-alpha",
					"1.0.0-alpha.1", "1.0.0-0.3.7", "1.0.0-x.7.z.92", "1.0.0-x-y-z.--", "1.0.0+20130313144700",
					"1.0.0-beta+exp.sha.5114f85", "1.0.0-rc.1+build.1", "v0.0.0-20230101000000-abcdef123456",
					"v1.2.4-0.20230101000000-abcdef123456", "v2.0.0+incompatible", "4.0.5", "4.7.7", "1.0.0-RC1"),
			Ecosystem.NUGET, List.of("1.0.0", "1.0.0.1", "4.7", "v1.0.0", "1.0.0-beta.1", "1.0.0-RC1", "1.0.0-Preview1",
					"1.2.3.4-preview1+meta", "13.0.3"),
			Ecosystem.MAVEN, List.of("1", "1.0", "2.0-beta9", "2.15.0", "1.0-SNAPSHOT", "5.3.20.RELEASE",
					"31.1-jre", "1.0.0.RC1"),
			Ecosystem.PYPI, List.of("1.0", "1.0.0", "v1.0", "1.0a1", "1.0rc1", "1.0.post1", "1.0-1", "1.0.dev0",
					"1!2.0", "1.0+local.1", "1.0-rc.1", "1.0.0-beta1", "2012.10", "1.11", "1.11.5", "1.11.11", "1.8.19", "2.0.3"),
			Ecosystem.DEBIAN, List.of("1.0", "0:1.0", "1:1.0", "1.0-1", "7.68.0-1ubuntu2.7", "1.0~rc1-1", "1.0-1+deb9u1",
					"2.30-0ubuntu1", "1.1.1f-1ubuntu2.16", "1.0-1-2", "1:2.0:3-1", "2:3", "1.2.3+dfsg-1~bpo11+1"),
			Ecosystem.RPM, List.of("1.0", "1.0-1.el8", "1:0.9-1", "1.0~rc1", "1.0^git1", "2.0a", "1.0_1", "1.0+foo",
					"0:1.0-1", "3.2.1-10.fc39", "1.0-1~rc1^git2"),
			Ecosystem.ALPINE, List.of("1.0", "7.83.0-r0", "7.83.1_rc1-r0", "1.0_alpha", "1.0_alpha2", "1.0_alpha_pre2",
					"1.0_rc1-r1", "1.0a", "1.0_git20230101", "1.0_p1", "1.0~1a2b-r0", "3.1.4-r5", "1.01"),
			Ecosystem.GEM, List.of("0", "1.0", "1.0.0", "1.0.a", "1.0.a1", "1.0.0.pre1", "2.0.0.rc1", "1.0.0-rc1",
					"1.0.0-x-y.z", "3.2.1", "3.2.10", "6.1.7.1", "1.0.b1", "2.a", "01.0", "1.13.10-arm64-darwin"),
			Ecosystem.GENERIC, List.of("1", "1.0", "1.0.0-rc1", "2.4.1-0ubuntu1", "20230101.1", "1.3b")));

	/**
	 * Malformed versions of each ecosystem; null and blank are checked separately for all.
	 */
	static final Map<Ecosystem, List<String>> INVALID = new EnumMap<>(Map.of(
			Ecosystem.SEMVER, List.of("garbage", "latest", "1.0.0.0.0-", "1.0.0.0", "01.2.3", "1.02.3", "1.2.03",
					"1.0.0-", "1.0.0-01", "1.0.0+", "1.0.0-alpha..1", "1.0.0-alpha_beta", "1.0.0 beta", "1..0", ".1",
					"v", "vv1.0", "*", "1.2.x", "^1.2.3", ">=1.0", "dev-master"),
			Ecosystem.NUGET, List.of("garbage", "latest", "1.0.0.0.0", "1.0.0.0.0-", "1.0.0-", "1.0.0.0-", "1.0.0-beta_1"),
			Ecosystem.MAVEN, List.of("latest", "RELEASE", "v", "vx1", "v1.0", "-1.0", ".1", "alpha-1", "x1.0"),
			Ecosystem.PYPI, List.of("latest", "garbage", "1.0-foo", "2.0rc1-foo", "0.9.8-final", "1.0+", "1.0+local!",
					"1.0.x", "dev", "1.0a1b2", "1.0..1"),
			Ecosystem.DEBIAN, List.of("latest", "a1.0", "1.0-", "-1", ":1.0", "a:1.0", "1:", "1:a1.0", "1.0_1",
					"1.0-1_2", "1.0:2", "1.0-2:3", "1.0 1", "1:2.0-"),
			Ecosystem.RPM, List.of("~1.0", "^1", ":1.0", "1:", "1.0:2", "-1.0", "1.0 1", "1.0/2", "a:1.0", "1.0%{?dist}",
					"latest", "xyz.4", "1.0-", "1.0--1", "1.0-1-2", "1:0.9-"),
			Ecosystem.ALPINE, List.of("latest", "v1.0", "1.0-r", "1.0_foo", "1.0-1", "1.0.", "1.0-r1-r2", "1.0~",
					"1.0_ALPHA", ".1"),
			Ecosystem.GEM, List.of("latest", "v1.0", "1..0", ".1", "1.0-", "1.0.", "1.0-rc_1", "1.0 beta", "a.1",
					"1.0+build", "1.0.0-rc1+b"),
			Ecosystem.GENERIC, List.of("latest", "v", "v1.2.3", "abc1.0", ".1", "-1", "x")));

	@Test
	void everyEcosystemHasTables() {
		for (Ecosystem e : Ecosystem.values()) {
			assertFalse(VALID.getOrDefault(e, List.of()).isEmpty(), e + ": valid examples");
			assertFalse(INVALID.getOrDefault(e, List.of()).isEmpty(), e + ": invalid examples");
		}
	}

	@ParameterizedTest
	@EnumSource(Ecosystem.class)
	void validVersionsParse(Ecosystem e) {
		for (String v : VALID.get(e)) {
			assertTrue(e.canParse(v), e + ": expected valid [" + v + "]");
		}
	}

	@ParameterizedTest
	@EnumSource(Ecosystem.class)
	void invalidVersionsDoNotParse(Ecosystem e) {
		for (String v : INVALID.get(e)) {
			assertFalse(e.canParse(v), e + ": expected invalid [" + v + "]");
		}
	}

	@ParameterizedTest
	@EnumSource(Ecosystem.class)
	void nullAndBlankNeverParse(Ecosystem e) {
		assertFalse(e.canParse(null), e + ": null");
		for (String blank : List.of("", " ", "\t\n", "\u00a0", "\u2003")) {
			assertFalse(e.canParse(blank), e + ": blank [" + blank + "]");
		}
	}

	@ParameterizedTest
	@EnumSource(Ecosystem.class)
	void surroundingWhitespaceIsIgnored(Ecosystem e) {
		assertTrue(e.canParse(" 1.0\t"), e.name());
		assertTrue(e.canParse("\u00a01.0\u00a0"), e.name());
	}

	@ParameterizedTest
	@EnumSource(Ecosystem.class)
	void versionsLongerThanTheComparedLengthDoNotParse(Ecosystem e) {
		// a single long number is a well-formed version in every ecosystem
		String atLimit = "1" + "0".repeat(Ecosystem.MAX_COMPARED_LENGTH - 1);
		assertEquals(Ecosystem.MAX_COMPARED_LENGTH, atLimit.length());
		assertTrue(e.canParse(atLimit), e.name());
		assertFalse(e.canParse(atLimit + "0"), e.name());
		assertTrue(e.canParse(" " + atLimit + " "), e + ": whitespace does not count");
	}

	@Test
	void unparseableStringsStillCompare() {
		// the total order is unchanged: canParse is a separate, stricter test
		assertFalse(Ecosystem.SEMVER.canParse("latest"));
		assertTrue(Ecosystem.SEMVER.compare("latest", "0.0.1") < 0);
		assertFalse(Ecosystem.PYPI.canParse("2.0rc1-foo"));
		assertTrue(Ecosystem.PYPI.compare("2.0rc1-foo", "2.0") < 0);
	}
}

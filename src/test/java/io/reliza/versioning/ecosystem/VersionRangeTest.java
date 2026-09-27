package io.reliza.versioning.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionRangeTest {

	@Test
	void fromBounds_halfOpenRange() {
		// log4j-core CVE-2021-44228: 2.0-beta9 inclusive up to 2.15.0 exclusive
		VersionRange range = VersionRange.fromBounds("2.0-beta9", null, null, "2.15.0");
		assertFalse(range.contains(Ecosystem.MAVEN, "2.0-beta8"));
		assertTrue(range.contains(Ecosystem.MAVEN, "2.0-beta9"));
		assertTrue(range.contains(Ecosystem.MAVEN, "2.0"));
		assertTrue(range.contains(Ecosystem.MAVEN, "2.14.1"));
		assertTrue(range.contains(Ecosystem.MAVEN, "2.15.0-rc1"));
		assertFalse(range.contains(Ecosystem.MAVEN, "2.15.0"));
		assertFalse(range.contains(Ecosystem.MAVEN, "2.17.1"));
		assertTrue(range.contains(Ecosystem.MAVEN, " 2.14.1 "));
	}

	@Test
	void fromBounds_exclusiveStartInclusiveEnd() {
		VersionRange range = VersionRange.fromBounds(null, "1.0.0", "1.2.0", "");
		assertFalse(range.contains(Ecosystem.SEMVER, "1.0.0"));
		assertTrue(range.contains(Ecosystem.SEMVER, "1.0.1"));
		assertTrue(range.contains(Ecosystem.SEMVER, "1.2.0"));
		assertTrue(range.contains(Ecosystem.SEMVER, "1.2"));
		assertFalse(range.contains(Ecosystem.SEMVER, "1.2.1"));
		assertEquals("(1.0.0,1.2.0]", range.toString());
	}

	@Test
	void fromBounds_openEnds() {
		VersionRange below = VersionRange.fromBounds(null, null, null, "3.0");
		assertTrue(below.contains(Ecosystem.PYPI, "0.1"));
		assertTrue(below.contains(Ecosystem.PYPI, "3.0rc1"));
		assertFalse(below.contains(Ecosystem.PYPI, "3.0"));
		assertEquals("(,3.0)", below.toString());

		VersionRange everything = VersionRange.fromBounds(null, " ", "", null);
		assertEquals(VersionRange.all(), everything);
		assertTrue(everything.contains(Ecosystem.GENERIC, "anything"));
		assertEquals("(,)", everything.toString());
	}

	@Test
	void fromBounds_rejectsConflictingBounds() {
		assertThrows(IllegalArgumentException.class, () -> VersionRange.fromBounds("1.0", "1.1", null, null));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.fromBounds(null, null, "2.0", "2.1"));
	}

	@Test
	void exact_comparesUnderEcosystemRules() {
		VersionRange range = VersionRange.exact("1.0");
		assertTrue(range.isExact());
		assertTrue(range.contains(Ecosystem.SEMVER, "1.0.0"));
		assertTrue(range.contains(Ecosystem.MAVEN, "1.0.0"));
		assertFalse(range.contains(Ecosystem.SEMVER, "1.0.1"));
		assertEquals("[1.0]", range.toString());
		assertEquals(VersionRange.all(), VersionRange.exact("*"));
		assertEquals(VersionRange.all(), VersionRange.between("*", true, " * ", true));
		assertEquals(VersionRange.fromBounds("1.0", null, null, null), VersionRange.fromBounds("1.0", null, "*", null));
		assertThrows(IllegalArgumentException.class, () -> VersionRange.exact(" "));
	}

	@Test
	void between_matchesFromBounds() {
		assertEquals(VersionRange.fromBounds("1.0", null, null, "2.0"), VersionRange.between("1.0", true, "2.0", false));
		assertEquals(VersionRange.fromBounds(null, "1.0", "2.0", null), VersionRange.between(" 1.0 ", false, "2.0", true));
		assertNotEquals(VersionRange.between("1.0", true, "2.0", false), VersionRange.between("1.0", false, "2.0", false));
	}

	@Test
	void unboundedSideIsNeverInclusive() {
		VersionRange range = VersionRange.between(null, true, "", true);
		assertNull(range.getLower());
		assertNull(range.getUpper());
		assertFalse(range.isLowerInclusive());
		assertFalse(range.isUpperInclusive());
		assertEquals(VersionRange.all(), range);
	}

	@Test
	void contains_blankVersionIsNeverContained() {
		assertFalse(VersionRange.all().contains(Ecosystem.GENERIC, null));
		assertFalse(VersionRange.all().contains(Ecosystem.GENERIC, " "));
		assertFalse(VersionRange.all().contains(Ecosystem.GENERIC, "\u00a0"));
		assertEquals(VersionRange.fromBounds(null, null, null, "2.0"), VersionRange.fromBounds("\u00a0*\u00a0", null, null, "2.0"));
		assertThrows(NullPointerException.class, () -> VersionRange.all().contains(null, "1.0"));
	}

	@Test
	void contains_usesEcosystemOrdering() {
		// under dpkg the tilde pre-release sorts below the release; generic ordering disagrees
		VersionRange fixedIn = VersionRange.fromBounds(null, null, null, "1.2.3-1");
		assertTrue(fixedIn.contains(Ecosystem.DEBIAN, "1.2.3~rc1-1"));
		assertFalse(fixedIn.contains(Ecosystem.DEBIAN, "1.2.3-1"));
		assertTrue(fixedIn.contains(Ecosystem.DEBIAN, "1.2.3-0ubuntu0.1"));
		assertFalse(fixedIn.contains(Ecosystem.DEBIAN, "1:1.0-1"));

		VersionRange alpine = VersionRange.fromBounds(null, null, null, "7.83.1-r0");
		assertTrue(alpine.contains(Ecosystem.ALPINE, "7.83.0-r5"));
		assertTrue(alpine.contains(Ecosystem.ALPINE, "7.83.1_rc1-r0"));
		assertFalse(alpine.contains(Ecosystem.ALPINE, "7.83.1-r0"));
	}
}

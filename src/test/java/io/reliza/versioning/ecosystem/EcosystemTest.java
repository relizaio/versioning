package io.reliza.versioning.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class EcosystemTest {

	/**
	 * Asserts that the versions are listed in strictly ascending order under the ecosystem,
	 * checking every pair in both directions.
	 */
	private static void assertAscending(Ecosystem ecosystem, String... versions) {
		for (int i = 0; i < versions.length; i++) {
			for (int j = i + 1; j < versions.length; j++) {
				String lower = versions[i];
				String higher = versions[j];
				assertTrue(ecosystem.compare(lower, higher) < 0, ecosystem + ": expected " + lower + " < " + higher);
				assertTrue(ecosystem.compare(higher, lower) > 0, ecosystem + ": expected " + higher + " > " + lower);
			}
		}
		List<String> shuffled = new ArrayList<>(List.of(versions));
		Collections.reverse(shuffled);
		shuffled.sort(ecosystem.getComparator());
		assertEquals(List.of(versions), shuffled, ecosystem + ": sort order");
	}

	private static void assertEquivalent(Ecosystem ecosystem, String a, String b) {
		assertEquals(0, ecosystem.compare(a, b), ecosystem + ": expected " + a + " == " + b);
		assertEquals(0, ecosystem.compare(b, a), ecosystem + ": expected " + b + " == " + a);
	}

	@Test
	void fromPurlType_mapsKnownTypes() {
		assertEquals(Ecosystem.SEMVER, Ecosystem.fromPurlType("npm"));
		assertEquals(Ecosystem.SEMVER, Ecosystem.fromPurlType("golang"));
		assertEquals(Ecosystem.SEMVER, Ecosystem.fromPurlType("cargo"));
		assertEquals(Ecosystem.NUGET, Ecosystem.fromPurlType("nuget"));
		assertEquals(Ecosystem.MAVEN, Ecosystem.fromPurlType("maven"));
		assertEquals(Ecosystem.MAVEN, Ecosystem.fromPurlType(" Maven "));
		assertEquals(Ecosystem.PYPI, Ecosystem.fromPurlType("pypi"));
		assertEquals(Ecosystem.DEBIAN, Ecosystem.fromPurlType("deb"));
		assertEquals(Ecosystem.RPM, Ecosystem.fromPurlType("rpm"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurlType("alpm"));
		assertEquals(Ecosystem.ALPINE, Ecosystem.fromPurlType("apk"));
		assertEquals(Ecosystem.GEM, Ecosystem.fromPurlType("gem"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurlType("composer"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurlType("generic"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurlType(""));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurlType(null));
	}

	@Test
	void fromPurl_readsTypeSegment() {
		assertEquals(Ecosystem.MAVEN, Ecosystem.fromPurl("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1?type=jar"));
		assertEquals(Ecosystem.SEMVER, Ecosystem.fromPurl("pkg:npm/%40angular/core@12.0.0"));
		assertEquals(Ecosystem.DEBIAN, Ecosystem.fromPurl("pkg:deb/debian/curl@7.50.3-1?arch=i386&distro=jessie"));
		assertEquals(Ecosystem.PYPI, Ecosystem.fromPurl("PKG:PyPI/django@1.11.1"));
		assertEquals(Ecosystem.ALPINE, Ecosystem.fromPurl("pkg://apk/alpine/curl@7.83.0-r0"));
		assertEquals(Ecosystem.GEM, Ecosystem.fromPurl("pkg:gem/rails@7.0.4.3"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurl("pkg:composer/laravel/framework@10.48.4"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurl("pkg:github/package-url/purl-spec@244fd47"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurl("maven/org.example/lib@1.0"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurl("pkg:maven"));
		assertEquals(Ecosystem.GENERIC, Ecosystem.fromPurl(null));
	}

	@Test
	void semver_precedence() {
		// the example chain from semver.org spec item 11
		assertAscending(Ecosystem.SEMVER, "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
				"1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0", "1.0.1", "1.1.0", "2.0.0", "10.0.0");
		assertEquivalent(Ecosystem.SEMVER, "1.0.0+build.1", "1.0.0+build.2");
		assertEquivalent(Ecosystem.SEMVER, "v1.2.3", "1.2.3");
		assertEquivalent(Ecosystem.SEMVER, "1.2", "1.2.0");
		// Go pseudo-versions order by their timestamp
		assertAscending(Ecosystem.SEMVER, "v0.0.0-20190101000000-abcdefabcdef", "v0.0.0-20200101000000-012345678901", "v0.0.1");
		// NuGet four-part versions
		assertAscending(Ecosystem.SEMVER, "4.3.0", "4.3.0.1", "4.3.1");
		// numeric identifiers beyond the range of long
		assertAscending(Ecosystem.SEMVER, "1.0.0-9223372036854775807", "1.0.0-9223372036854775808", "1.0.0-alpha");
	}

	@Test
	void semver_readsInvalidVersionsLeniently() {
		// anything after the release ranks like a pre-release of it
		assertAscending(Ecosystem.SEMVER, "1.2.3", "1.2.4-beta_1", "1.2.4.rc1", "1.2.4");
		assertAscending(Ecosystem.SEMVER, "latest", "0.0.1");
		assertEquivalent(Ecosystem.SEMVER, "1.0.0-", "1.0.0");
		// build metadata is ignored and does not hide the pre-release
		assertEquivalent(Ecosystem.SEMVER, "1.0.0-rc.1+build.5", "1.0.0-rc.1");
	}

	@Test
	void nuget_preReleaseLabelsIgnoreCase() {
		assertAscending(Ecosystem.NUGET, "1.0.0-alpha", "1.0.0-Beta", "1.0.0-Preview1", "1.0.0-RC1", "1.0.0", "1.0.0.1");
		assertEquivalent(Ecosystem.NUGET, "1.0.0-BETA.2", "1.0.0-beta.2");
		// plain SEMVER compares labels in ASCII order, uppercase first
		assertAscending(Ecosystem.SEMVER, "1.0.0-Beta", "1.0.0-alpha");
	}

	@Test
	void maven_ordering() {
		assertAscending(Ecosystem.MAVEN, "1-alpha", "1-alpha-1", "1-beta", "1-milestone", "1-rc", "1-snapshot", "1",
				"1-sp", "1-abc", "1-xyz", "1-1", "1.0.1", "1.1");
		assertAscending(Ecosystem.MAVEN, "1.0a1", "1.0b1", "1.0m1", "1.0rc1", "1.0");
		assertEquivalent(Ecosystem.MAVEN, "1", "1.0.0");
		assertEquivalent(Ecosystem.MAVEN, "1.0", "1-ga");
		assertEquivalent(Ecosystem.MAVEN, "1.0", "1.0.FINAL");
		assertEquivalent(Ecosystem.MAVEN, "1.0-RELEASE", "1.0");
		assertEquivalent(Ecosystem.MAVEN, "1.0-cr1", "1.0-RC1");
		assertEquivalent(Ecosystem.MAVEN, "1.0.0.RC1", "1.0.0-rc1");
		assertAscending(Ecosystem.MAVEN, "2.12.7", "2.12.7.1", "2.13.0-rc1", "2.13.0");
		assertAscending(Ecosystem.MAVEN, "5.3.19", "5.3.20.RELEASE", "5.3.21");
		assertAscending(Ecosystem.MAVEN, "31.1-android", "31.1-jre", "32.0.0-android");
		assertAscending(Ecosystem.MAVEN, "1.0-SNAPSHOT", "1.0", "1.0-1");
	}

	@Test
	void pypi_ordering() {
		// the full ordering example from PEP 440 "Summary of permitted suffixes and relative ordering"
		assertAscending(Ecosystem.PYPI, "1.dev0", "1.0.dev456", "1.0a1", "1.0a2.dev456", "1.0a12.dev456", "1.0a12",
				"1.0b1.dev456", "1.0b2", "1.0b2.post345.dev456", "1.0b2.post345", "1.0rc1.dev456", "1.0rc1", "1.0",
				"1.0+abc.5", "1.0+abc.7", "1.0+5", "1.0.post456.dev34", "1.0.post456", "1.0.15", "1.1.dev1");
		assertEquivalent(Ecosystem.PYPI, "1.0", "1.0.0");
		assertEquivalent(Ecosystem.PYPI, "1.0alpha1", "1.0a1");
		assertEquivalent(Ecosystem.PYPI, "1.0-rc.1", "1.0rc1");
		assertEquivalent(Ecosystem.PYPI, "1.0c1", "1.0rc1");
		assertEquivalent(Ecosystem.PYPI, "1.0-1", "1.0.post1");
		assertEquivalent(Ecosystem.PYPI, "v1.0", "1.0");
		assertAscending(Ecosystem.PYPI, "2.0", "1!0.5");
		// not PEP 440: the leading release plus a local label made of the rest
		assertAscending(Ecosystem.PYPI, "0.9.8", "0.9.8-final", "0.9.8.post1", "0.9.9");
		assertEquivalent(Ecosystem.PYPI, "0.9.8-final", "0.9.8+final");
		assertAscending(Ecosystem.PYPI, "2.0rc1", "2.0rc1-foo", "2.0", "2.0-custom", "2.0.post1");
		assertEquivalent(Ecosystem.PYPI, "2.0-custom", "2.0+custom");
		assertAscending(Ecosystem.PYPI, "latest", "0.0.1");
	}

	@Test
	void debian_ordering() {
		assertAscending(Ecosystem.DEBIAN, "1.0~~", "1.0~~a", "1.0~", "1.0", "1.0a", "1.0+", "1.0.1");
		assertAscending(Ecosystem.DEBIAN, "7.68.0-1ubuntu2.7", "7.68.0-1ubuntu2.24", "7.68.0-1ubuntu3", "7.81.0-1");
		assertAscending(Ecosystem.DEBIAN, "2.30-0ubuntu1", "2.30-1", "1:0.1");
		assertAscending(Ecosystem.DEBIAN, "1.0~rc1-1", "1.0-1", "1.0-1+deb9u1", "1.0-2");
		assertEquivalent(Ecosystem.DEBIAN, "1.0", "1.0-0");
		assertEquivalent(Ecosystem.DEBIAN, "0:1.0", "1.0");
		assertEquivalent(Ecosystem.DEBIAN, "1.00", "1.0");
	}

	@Test
	void rpm_ordering() {
		assertAscending(Ecosystem.RPM, "1.0~rc1", "1.0", "1.0^git1", "1.0a", "1.0.1");
		assertAscending(Ecosystem.RPM, "2.0a", "2.0b", "2.0.1");
		assertAscending(Ecosystem.RPM, "1.0.9", "1.0.10", "1.1");
		assertAscending(Ecosystem.RPM, "1.0", "1.0-1.el8", "1.0-2.el8", "1.0-10.el8", "1:0.9-1");
		assertAscending(Ecosystem.RPM, "xyz.4", "2");
		assertEquivalent(Ecosystem.RPM, "1.0", "1_0");
		assertEquivalent(Ecosystem.RPM, "0:1.0-1", "1.0-1");
		assertEquivalent(Ecosystem.RPM, "1.01", "1.1");
	}

	@Test
	void alpine_ordering() {
		// a packaging revision of 1.0 sorts below every post-release suffix of 1.0
		assertAscending(Ecosystem.ALPINE, "1.0_alpha", "1.0_alpha2", "1.0_beta", "1.0_pre1", "1.0_rc1", "1.0",
				"1.0-r1", "1.0_cvs", "1.0_svn", "1.0_git20230101", "1.0_hg", "1.0_p1", "1.0a", "1.0.1");
		assertAscending(Ecosystem.ALPINE, "7.83.0-r0", "7.83.0-r1", "7.83.0-r10", "7.83.1-r0");
		assertAscending(Ecosystem.ALPINE, "1.01", "1.1", "1.2");
		assertAscending(Ecosystem.ALPINE, "1.0~1a2b-r0", "1.0~1a2c-r0");
		assertEquivalent(Ecosystem.ALPINE, "3.1.4-r5", "3.1.4-r5");
	}

	/**
	 * Every relation below, 116 ordered pairs in all, was checked against Ruby 3.3.12 /
	 * RubyGems 3.5.22 (Gem::Version.new(a) &lt;=&gt; Gem::Version.new(b)).
	 */
	@Test
	void gem_ordering() {
		// the example from the Gem::Version documentation, and a letter segment below a number
		assertAscending(Ecosystem.GEM, "0.9", "1.0.a.2", "1.0.a9", "1.0.a10", "1.0.b1", "1.0", "1.0.1");
		// zero padding of the release does not matter before a pre-release (canonical segments)
		assertAscending(Ecosystem.GEM, "5.0.0.alpha1", "5.0.0.beta1", "5.0.rc1", "5.0.0.rc2", "5.0.0", "5.0.0.1", "5.0.1");
		assertAscending(Ecosystem.GEM, "7.0.0.alpha2", "7.0.0.rc1", "7.0.0", "7.0.4", "7.0.4.3", "7.1.0.beta1", "7.1.0");
		// a hyphen reads as .pre., and pre sorts below rc
		assertAscending(Ecosystem.GEM, "1.0.0-rc1", "1.0.0.pre1", "1.0.0.rc1", "1.0.0");
		// a platform suffix makes a pre-release
		assertAscending(Ecosystem.GEM, "1.13.9", "1.13.10-arm64-darwin", "1.13.10-java", "1.13.10", "1.13.11");
		// only the first run of zeros before a letter is dropped
		assertAscending(Ecosystem.GEM, "1.a.b", "1.0.a.0.b", "1.a.1", "1.b");
		assertAscending(Ecosystem.GEM, "1.0.A", "1.0.a", "2.a", "2.0.0.pre", "2.0");
		assertAscending(Ecosystem.GEM, "3.2.1", "3.2.9", "3.2.10", "9223372036854775807", "9223372036854775808");
		assertEquivalent(Ecosystem.GEM, "1.0", "1.0.0");
		assertEquivalent(Ecosystem.GEM, "1.0.a", "1.a");
		assertEquivalent(Ecosystem.GEM, "1.0.0.a", "1.a");
		assertEquivalent(Ecosystem.GEM, "1.0-rc1", "1.0.0-rc1");
		assertEquivalent(Ecosystem.GEM, "1.0.0-rc1", "1.0.0.pre.rc1");
		assertEquivalent(Ecosystem.GEM, "1.a.0.b", "1.a.b");
		assertEquivalent(Ecosystem.GEM, "1.a0b", "1.0.a.0.b");
		assertEquivalent(Ecosystem.GEM, "0.0.a", "0.a");
		assertEquivalent(Ecosystem.GEM, "1.0.a.0", "1.a");
		assertEquivalent(Ecosystem.GEM, "01.0", "1");
		assertEquivalent(Ecosystem.GEM, "5.0.0.beta1", "5.beta1");
	}

	@Test
	void generic_ordering() {
		assertAscending(Ecosystem.GENERIC, "0.9", "1.0", "1.0.1", "1.2", "1.10", "2.0");
		assertEquivalent(Ecosystem.GENERIC, "1.0.0", "1.0.0.0");
		assertEquivalent(Ecosystem.GENERIC, "1.0", "1.0.0.0");
		// parts starting with digits compare by that number first
		assertAscending(Ecosystem.GENERIC, "1.3", "1.3b", "1.4", "1.10");
		assertEquivalent(Ecosystem.GENERIC, "2.4.1-0ubuntu1", "2.4.1");
		// Dependency-Track semantics: an extra qualifier part ranks above the bare release
		assertAscending(Ecosystem.GENERIC, "1.0.0", "1.0.0-rc1");
		// numeric parts compare by value at any length
		assertAscending(Ecosystem.GENERIC, "20230101.1", "2147483648.1", "99999999999999999999.1");
	}

	@Test
	void comparatorMatchesCompare() {
		for (Ecosystem e : Ecosystem.values()) {
			assertEquals(Integer.signum(e.compare("1.0", "2.0")), Integer.signum(e.getComparator().compare("1.0", "2.0")));
			assertTrue(e.compare("1.0", "2.0") < 0, e + ": 1.0 < 2.0");
		}
	}

	@Test
	void longVersionsAreComparedOnTheirLeadingPart() throws InterruptedException {
		String longDotted = "1" + ".1".repeat(20000);
		String longNested = "1" + "-1a".repeat(20000);
		List<Throwable> failures = new ArrayList<>();
		// a small stack, as on a worker thread
		Thread t = new Thread(null, () -> {
			try {
				for (Ecosystem e : Ecosystem.values()) {
					e.compare(longDotted, longDotted + ".2");
					e.compare(longNested, longNested + "x");
					assertTrue(e.compare("2", longDotted) > 0, e + ": 2 > 1.1.1...");
				}
			} catch (Throwable ex) {
				failures.add(ex);
			}
		}, "small-stack", 256 * 1024);
		t.start();
		t.join();
		assertEquals(List.of(), failures);
		String prefix = "1.0." + "0".repeat(Ecosystem.MAX_COMPARED_LENGTH);
		assertEquivalent(Ecosystem.SEMVER, prefix + ".1", prefix + ".2");
	}

	@Test
	void surroundingWhitespaceIsIgnored() {
		for (Ecosystem e : Ecosystem.values()) {
			assertEquivalent(e, " 2.14.1\t", "2.14.1");
			assertEquivalent(e, "\u00a02.14.1\u00a0", "2.14.1");
		}
		assertTrue(Ecosystem.MAVEN.compare(" 2.14.1", "2.0-beta9") > 0);
	}

	/**
	 * Checks antisymmetry and transitivity on every triple of the corpus, which is what
	 * List.sort relies on.
	 */
	private static void assertTotalOrder(Ecosystem e, List<String> corpus) {
		int n = corpus.size();
		int[][] sign = new int[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				sign[i][j] = Integer.signum(e.compare(corpus.get(i), corpus.get(j)));
			}
		}
		for (int i = 0; i < n; i++) {
			assertEquals(0, sign[i][i], e + ": reflexive " + corpus.get(i));
			for (int j = 0; j < n; j++) {
				assertEquals(-sign[j][i], sign[i][j], e + ": antisymmetric " + corpus.get(i) + " / " + corpus.get(j));
				for (int k = 0; k < n; k++) {
					if (sign[i][j] <= 0 && sign[j][k] <= 0) {
						assertTrue(sign[i][k] <= 0, e + ": transitive " + corpus.get(i) + " <= " + corpus.get(j) + " <= " + corpus.get(k));
					}
				}
			}
		}
	}

	/**
	 * Every comparator but MAVEN must be a consistent total order over arbitrary strings, valid
	 * or not, or List.sort may throw.
	 */
	@Test
	void comparatorsAreTotalOrders() {
		List<String> corpus = List.of("", "0", "1", "1.0", "1.0.0", "1.0.0.0", "01.0", "1.0.1", "1.10", "2",
				"1.0-rc1", "1.0.0-rc.1", "1.0rc1", "1.0_rc1", "1.0~rc1", "1.0-1", "1.0.post1", "1.0.dev1",
				"1.0+local", "1.0-SNAPSHOT", "1.0.Final", "1.0a", "1.0b2", "1.3b", "1:1.0", "1.0-r1", "1.0_p1",
				"1.0^git1", "v1.0.0", "V2", "latest", "abc", "-", "~", "1.0.0-Beta", "1.0.0-alpha",
				"1.0.0-beta_1", "3b", "10", "4", "2147483648", "1.0-0ubuntu1", "20230101", "1..0", "1.-0", ".1");
		for (Ecosystem e : Ecosystem.values()) {
			if (e != Ecosystem.MAVEN) assertTotalOrder(e, corpus);
		}
	}

	/**
	 * MAVEN keeps Maven's own cycles on unusual versions, but orders ordinary versions totally.
	 */
	@Test
	void maven_isATotalOrderOnOrdinaryVersions() {
		assertTotalOrder(Ecosystem.MAVEN, List.of("0", "1", "1.0", "1.0.0", "1.0.1", "1.1", "1.10", "2", "1-SNAPSHOT",
				"1.0-alpha", "1.0-alpha-1", "1.0-alpha1", "1.0a1", "1.0-beta-2", "1.0-M1", "1.0-rc1", "1.0-RC2", "1.0.CR3",
				"1.0-SNAPSHOT", "1.0-ga", "1.0.Final", "1.0.RELEASE", "1.0-sp1", "1.0-1", "1.0-2", "1.0-jre", "1.0-android",
				"31.1-jre", "2.13.4.2", "5.3.20.RELEASE", "1.0-alpha1-SNAPSHOT", "1.0-incubating", "1.0.0.RC1"));
		// Maven's own cycle, reproduced
		assertTrue(Ecosystem.MAVEN.compare("1.foo.2", "1-rc") < 0);
		assertTrue(Ecosystem.MAVEN.compare("1-rc", "1") < 0);
		assertTrue(Ecosystem.MAVEN.compare("1", "1.foo.2") < 0);
	}
}

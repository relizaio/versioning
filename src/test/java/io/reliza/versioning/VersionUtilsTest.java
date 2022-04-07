package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.reliza.versioning.Version.VersionHelper;
import io.reliza.versioning.VersionApi.ActionEnum;
import io.reliza.versioning.VersionApi.VersionApiObject;

class VersionUtilsTest {

	@Test
	void testParseVersion_Semver() {
		String version = "1.0.0";
		String schema = "semver";
		VersionHelper vh = VersionUtils.parseVersion(version, schema);
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("1");
		versionComponentsExpected.add("0");
		versionComponentsExpected.add("0");
		ArrayList<String> versionComponenetsActual = (ArrayList<String>) vh.getVersionComponents();
		assertEquals(versionComponentsExpected, versionComponenetsActual);
	}
	
	@Test
	void testParseVersion_BranchWithVersionInName() {
		String version = "dependabot/npm_and_yarn/vue/cli-plugin-babel-4.5.13.0";
		String schema = "Branch.Micro";
		VersionHelper vh = VersionUtils.parseVersion(version, schema);
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("dependabot/npm_and_yarn/vue/cli-plugin-babel-4.5.13");
		versionComponentsExpected.add("0");
		ArrayList<String> versionComponenetsActual = (ArrayList<String>) vh.getVersionComponents();
		assertEquals(versionComponentsExpected, versionComponenetsActual);
	}
	
	@Test
    public void testParseVersion_SemverPlusModifer() {
    	String version = "1.3.6-alpha.1+1234.234.5";
    	String schema = "semver";
    	VersionHelper vh = VersionUtils.parseVersion(version, schema);
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("1");
		versionComponentsExpected.add("3");
		versionComponentsExpected.add("6");
		ArrayList<String> versionComponenetsActual = (ArrayList<String>) vh.getVersionComponents();
		assertEquals(versionComponentsExpected, versionComponenetsActual);
    }
	
	@Test
    public void testParseVersion_BranchWithVersionInName2() {
    	String version = "branch-name/subbranch-name/test-name-1.2.3.3.4";
    	String schema = "Branch.Major.Micro";
    	VersionHelper vh = VersionUtils.parseVersion(version, schema);
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("branch-name/subbranch-name/test-name-1.2.3");
		versionComponentsExpected.add("3");
		versionComponentsExpected.add("4");
		ArrayList<String> versionComponenetsActual = (ArrayList<String>) vh.getVersionComponents();
		assertEquals(versionComponentsExpected, versionComponenetsActual);
    }
	
	@Test
    public void testParseVersion_BranchWithVersionInName3() {
    	String version = "branch-name/subbranch-name/test-name-1.2.3.20.3.4";
    	String schema = "Branch.YY.Major.Micro";
    	VersionHelper vh = VersionUtils.parseVersion(version, schema);
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("branch-name/subbranch-name/test-name-1.2.3");
		versionComponentsExpected.add("20");
		versionComponentsExpected.add("3");
		versionComponentsExpected.add("4");
		ArrayList<String> versionComponenetsActual = (ArrayList<String>) vh.getVersionComponents();
		assertEquals(versionComponentsExpected, versionComponenetsActual);
    }
	
	@Test
    public void testParseVersion_BranchWithVersionInName4() {
    	String version = "branch-name/subbranch-name/test-name-1.2.3.22." + AppTest.CURRENT_MONTH + ".3.4";
    	String schema = "Branch.YY.0M.Major.Micro";
    	VersionHelper vh = VersionUtils.parseVersion(version, schema);
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("branch-name/subbranch-name/test-name-1.2.3");
		String year = Integer.toString(LocalDate.now().getYear());
		year = year.substring(2, 4);
		String month = AppTest.CURRENT_MONTH_SINGLE;
		month = month.length() == 1 ? "0" + month : month;
		versionComponentsExpected.add(year);
		versionComponentsExpected.add(month);
		versionComponentsExpected.add("3");
		versionComponentsExpected.add("4");
		ArrayList<String> versionComponenetsActual = (ArrayList<String>) vh.getVersionComponents();
		assertEquals(versionComponentsExpected, versionComponenetsActual);
    }
	
	@Test
	void testIsVersionMatchingSchema_BranchWithVersionInName() {
		String version = "dependabot/npm_and_yarn/vue/cli-plugin-babel-4.5.13.0";
		//ActionEnum action = ActionEnum.BUMP;
		String schema = "Branch.Micro";
		assertTrue(VersionUtils.isVersionMatchingSchema(schema, version));
	}
	
	@Test
	void testIsVersionMatchingSchema_SemverSimple() {
		String version = "1.0.0";
		String schema = "semver";
		assertTrue(VersionUtils.isVersionMatchingSchema(schema, version));
	}
	
	@Test
	void testVersionApiObject_BranchWithVersionInName() {
		String version = "dependabot/npm_and_yarn/vue/cli-plugin-babel-4.5.13.0";
		ActionEnum action = ActionEnum.BUMP;
		String schema = "Branch.Micro";
		VersionApiObject vao = VersionApi.createVao(schema);
		vao.setVersion(version);
		Version v = VersionApi.initializeVersion(vao);
		VersionApi.applyActionOnVersion(v, action);
		String expectedVersion = "dependabot/npm_and_yarn/vue/cli-plugin-babel-4.5.13.1";
		String actualVersion = v.constructVersionString();
		assertEquals(expectedVersion, actualVersion);
	}

	@Test
	void versionsMatchingSchemas() {
		// test if two versions have the same schema
		String schema1 = "Major.Minor.Patch";
		String schema2 = "MAJOR.MINOR.MICRO";
		// schemas are functionally equal
		List<VersionElement> vel1 = VersionUtils.parseSchema(schema1);
		List<VersionElement> vel2 = VersionUtils.parseSchema(schema2);
		assertEquals(vel1, vel2);
	}
	
	@Test
	void differenceBetweenTwoVersions_MismatchVersionsAndSchema() {
		String oldV = "0.0.4";
		String newV = "0.3.4";
		String schema = "MM.Major";
		VersionElement expectedElement = null; //expecting null because versions don't match schema
		VersionElement actualElement = VersionUtils.getLargestVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void differenceBetweenTwoVersions_1() {
		// Largest differing element is minor
		String oldV = "0.0.4";
		String newV = "0.3.4";
		String schema = "Semver";
		VersionElement expectedElement = VersionElement.MINOR;
		VersionElement actualElement = VersionUtils.getLargestVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void differenceBetweenTwoVersions_2() {
		String oldV = "2019.0.4";
		String newV = "2021.3.4";
		String schema = "YYYY.Major.Minor";
		VersionElement expectedElement = VersionElement.YYYY;
		VersionElement actualElement = VersionUtils.getLargestVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void semverDifferenceBetweenTwoVersions_1() {
		// expecting MAJOR because only considering semver version elements
		String oldV = "2019.0.4";
		String newV = "2021.3.4";
		String schema = "YYYY.Major.Minor";
		VersionElement expectedElement = VersionElement.MAJOR;
		VersionElement actualElement = VersionUtils.getLargestSemverVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void semverDifferenceBetweenTwoVersions_2() {
		// expecting PATCH because only considering semver version elements
		String oldV = "2019.3.4.3";
		String newV = "2021.3.4.5";
		String schema = "YYYY.Major.Minor.Patch";
		VersionElement expectedElement = VersionElement.PATCH;
		VersionElement actualElement = VersionUtils.getLargestSemverVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void semverDifferenceBetweenTwoVersions_3() {
		String oldV = "2019.3.4.7";
		String newV = "2021.3.4.5";
		String schema = "YYYY.Major.Minor.Patch";
		VersionElement expectedElement = VersionElement.PATCH;
		VersionElement actualElement = VersionUtils.getLargestSemverVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void semverDifferenceBetweenTwoVersions_NoDifference() {
		String oldV = "3.4.7";
		String newV = "3.4.7";
		String schema = "Semver";
		VersionElement expectedElement = null;
		VersionElement actualElement = VersionUtils.getLargestSemverVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void semverDifferenceBetweenTwoVersions_largestDifference() {
		// make sure largest differnece is returned
		String oldV = "3.4.5";
		String newV = "6.7.8";
		String schema = "Semver";
		VersionElement expectedElement = VersionElement.MAJOR;
		VersionElement actualElement = VersionUtils.getLargestSemverVersionElementDifference(oldV, newV, schema);
		assertEquals(expectedElement, actualElement);
	}
	
	@Test
	void testIsSchemaSemver_isSemver() {
		String schema = "Semver";
		boolean isSemver = VersionUtils.isSchemaSemver(schema);
		assertTrue(isSemver);
	}
	
	@Test
	void testIsSchemaSemver_notSemver() {
		String schema = "YYYY.Major.Minor";
		assertFalse(VersionUtils.isSchemaSemver(schema));
	}
	
	@Test
	void testIfVersionIsSemver_isValid() {
		String version = "1.2.3";
		assertTrue(VersionUtils.isVersionSemver(version));
	}
	
	@Test
	void testIfVersionIsSemver_isNotValid() {
		String version = "0M.1.2.3";
		assertFalse(VersionUtils.isVersionSemver(version));
	}
	
	@Test
	void testIfVersionIsSemver_isValid2() {
		String version = "1.1.2+meta-valid";
		assertTrue(VersionUtils.isVersionSemver(version));
	}
	
	@Test
	void testIfVersionIsSemver_isNotValid2() {
		String version = "1.1.2+meta-valid%";
		assertFalse(VersionUtils.isVersionSemver(version));
	}
	
	@Test
	void testIfVersionIsSemver_isValid3() {
		String version = "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay";
		assertTrue(VersionUtils.isVersionSemver(version));
	}
	
	@Test
	void testIfVersionIsSemver_inValidCharacterInBuild() {
		String version = "1.0.0-rc.1+build.1%";
		assertFalse(VersionUtils.isVersionSemver(version));
	}
}

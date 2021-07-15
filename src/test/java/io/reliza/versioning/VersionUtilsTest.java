package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;

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
    	String version = "branch-name/subbranch-name/test-name-1.2.3.21." + AppTest.CURRENT_MONTH + ".3.4";
    	String schema = "Branch.YY.0M.Major.Micro";
    	VersionHelper vh = VersionUtils.parseVersion(version, schema);
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("branch-name/subbranch-name/test-name-1.2.3");
		String year = Integer.toString(LocalDate.now().getYear());
		year = year.substring(2, 4);
		String month = Integer.toString(LocalDate.now().getMonthValue());
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

}

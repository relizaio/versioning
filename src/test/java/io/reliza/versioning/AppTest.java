/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.reliza.versioning.Version.VersionHelper;
import io.reliza.versioning.Version.VersionStringComparator;
import io.reliza.versioning.VersionApi.ActionEnum;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
	static ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
	public static final String CURRENT_MONTH_SINGLE = String.valueOf(date.getMonthValue());
	public static final String CURRENT_MONTH = StringUtils.leftPad(CURRENT_MONTH_SINGLE, 2, "0");
	
	public static final String CURRENT_YEAR_LONG = String.valueOf(date.getYear());
	public static final String CURRENT_YEAR_SHORT = CURRENT_YEAR_LONG.substring(2);
	public static final String CURRENT_DAY = String.valueOf(date.getDayOfMonth());
	
	
    @Test
    public void testSchemaMatching1() {
    	String testSchema = "Major.Minor.Patch";
    	String testVersion = "1.3.6";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void testSchemaMatching2() {
    	String testSchema = "semver";
    	String testVersion = "1.3.6-alpha.1+1234.234.5";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void testSchemaMatching3() {
    	String testSchema = "semver";
    	String testVersion = "1.3.6.7-alpha.1+1234.234.5";
        assertFalse( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void testSchemaMatching4() {
    	String testSchema = "major.minor.patch-modifier";
    	String testVersion = "1.3.6-somefeofasd";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void testSchemaMatching5() {
    	String testSchema = "Year.Month";
    	String testVersion = "2019.5";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void testSchemaMatching5BuildidBuildenv() {
    	String testSchema = "Year.Month.buildenv.buildid";
    	String testVersion = "2019.5.circleci.24";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }

    @Test
    public void testSchemaMatchin6YYMMMicrohyphenBranch() {
    	String testSchema = "YY.0M.Micro-Branch";
    	String testVersion = "23.06.0-newbr";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void testSchemaMatchin7DifferentSeparatorsFail() {
    	String testSchema = "Major.Minor.Patch";
    	String testVersion = "5.7_3";
        assertFalse( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }

    @Test
    public void testSchemaMatchingPinYYMMMicrohyphenBranch() {
    	String projectSchema = "YY.0M.Micro";
    	String testSchema = "YY.0M.Micro-Branch";
    	// String testVersion = "23.06.0-newbr";
        assertFalse( VersionUtils.isPinMatchingSchema(projectSchema, testSchema) );
    }

    @Test
    public void getVersionForYYMMMicrohyphenBranch() {
    	String testSchema = "YY.0M.Micro-Branch";
    	String testVersion = CURRENT_YEAR_SHORT + "." + CURRENT_MONTH + ".0-newbr";
		Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
		v.setBranch("newbr");
        assertEquals (testVersion, v.constructVersionString());
        
    }
    
    @Test
    public void testBumpYYMMMicrohyphenBranchWithPin() {

    	String testSchema = "YY.0M.Micro-Branch";
    	
		String testVersion = "23.06.0-newbr";
    	String testPin = "23.06.micro-Branch";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testVersion, ActionEnum.BUMP);
		assertEquals("23.06.1-newbr", v.constructVersionString());
    }

    @Test
    public void testBumpYYMMMicrohyphenBranchWithPinSameAsSchema() {

    	String testSchema = "YY.0M.Micro-Branch";
    	
		String testVersion = CURRENT_YEAR_SHORT + "." + CURRENT_MONTH + ".0-newbr";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, testSchema, testVersion, ActionEnum.BUMP);
		assertEquals(CURRENT_YEAR_SHORT + "." + CURRENT_MONTH + ".1-newbr", v.constructVersionString());
    }
    
    @Test
    public void testPinMatching1Semver() {
    	String testSchema = "major.minor.patch-modifier?";
    	String testPin = "1.3.patch";
        assertTrue( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    
    @Test
    public void testPinMatching2Calver() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.1.minor.patch-modifier";
        assertTrue( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    
    @Test
    public void testPinMatching3FailSemver() {
    	String testSchema = "major.minor.patch-modifier";
    	String testPin = "1.major.patch";
        assertFalse( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    
    @Test
    public void testPinMatching4FailCalver() {
    	String testSchema = "Year.Month.minor.patch-modifier?";
    	String testPin = "2020.22.minor.patch-modifier?";
        assertFalse( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    
    @Test
    public void testPinMatching5SuccessCalver() {
    	String testSchema = "Year.Month.minor.patch-modifier?";
    	String testPin = "2020.2.minor.patch-modifier?";
        assertTrue( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }

	 @Test
    public void testPinMatchin6SuccessYY0MMicrohyphenBranch() {
    	String testSchema = "YY.0M.Micro-Branch";
    	String testPin = "23.06.micro-Branch";
        assertTrue( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema1Semver() {
    	String testSchema = "major.minor.patch-modifier?";
    	String testPin = "1.3.patch";
    	String testVersion = "1.3.5";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema2Calver() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.1.minor.patch-modifier";
    	String testVersion = "2020.1.18.5-testMod";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema3SemverOptionalModifier() {
    	String testSchema = "major.minor.patch-modifier";
    	String testPin = "1.3.patch-modifier";
    	String testVersion = "1.3.5-PROD";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema4SemverOptionalMeta() {
    	String testSchema = "major.minor.patch+metadata";
    	String testPin = "1.3.patch+metadata";
    	String testVersion = "1.3.5+PROD";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema5FailSemver() {
    	String testSchema = "major.minor.patch-modifier";
    	String testPin = "1.3.patch";
    	String testVersion = "1.2.5";
        assertFalse( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema6CalverDiffModifier() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.1.minor.patch-modifier";
    	String testVersion = "2020.1.7.8-Prod";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema6CalverOptionalModifier() {
    	String testSchema = "Year.Month.minor.patch-modifier?";
    	String testPin = "2020.1.minor.patch-modifier?";
    	String testVersion = "2020.1.7.8";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema6CalverOptionalMetadata() {
    	String testSchema = "Year.Month.minor.patch+metadata?";
    	String testPin = "2020.1.minor.patch+metadata?";
    	String testVersion = "2020.1.7.8";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema7CalverMidModifier() {
    	String testSchema = VersionType.CALVER_RELIZA_2020.getSchema();
    	String testPin = "2019.12.Calvermodifier.Minor.Micro+metadata";
    	String testVersion = "2019.12.Snapshot.0.0";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema8SemverOptionalMetadata() {
    	String testSchema = "Year.Month.minor.patch+metadata?";
    	String testPin = "2020.1.minor.patch+metadata?";
    	String testVersion = "2020.1.7.8";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema9CalverFail() {
    	String testSchema = VersionType.CALVER_RELIZA_2020.getSchema();
    	String testPin = "2019.12.Calvermodifier.Minor.Micro+metadata?";
    	String testVersion = "2019.11.Snapshot.0.0";
        assertFalse( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionStringOutput() {
    	String testSchema = "semver";
    	String testVersion = "1.3.6-alpha.1+1234.234.5";
    	Version v = Version.getVersion(testVersion, testSchema);
    	System.out.println(v);
        assertTrue( true );
    }
    
    @Test
    public void constructVersion1() {
    	String testSchema = "semver";
    	String testVersion = "1.3.6-alpha.1+1234.234.5";
    	Version v = Version.getVersion(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("1.3.6-alpha.1+1234.234.5", v.constructVersionString());
    }
    
    @Test
    public void constructVersion2() {
    	String testSchema = "YYYY.OM.Calvermodifier.patch";
    	String testVersion = "2019.05.prod.7";
    	Version v = Version.getVersion(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("2019.05.prod.7", v.constructVersionString());
    }
    
    @Test
    public void constructVersion3() {
    	String testSchema = "YYYY_OM.Calvermodifier_patch";
    	String testVersion = "2019_05.prod_7";
    	Version v = Version.getVersion(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("2019_05.prod_7", v.constructVersionString());
    }
    
    @Test
    public void constructVersion4BuildidBuildenv() {
    	String testSchema = "Year.OM.buildenv.buildid";
    	String testVersion = "2020.01.circleci.24";
    	Version v = Version.getVersion(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("2020.01.circleci.24", v.constructVersionString());
    }
    
    @Test
    public void constructVersionOtherSchemaFail() {
    	String testSchema = "YYYY_OM.Calvermodifier_patch";
    	String testVersion = "2019_05.prod_7";
    	Version v = Version.getVersion(testVersion, testSchema);
        Assertions.assertThrows(RuntimeException.class, () -> {
        	System.out.println(v.constructVersionString("semver"));
        });
    }
    
    @Test
    public void constructVersionOtherSchemaSuccess() {
    	String testSchema = "semver";
    	String testVersion = "7.4.284";
    	String useSchema = "major.minor";
    	Version v = Version.getVersion(testVersion, testSchema);
    	System.out.println(v.constructVersionString(useSchema));
        assertEquals ("7.4", v.constructVersionString(useSchema));
    }
    
    @Test
    public void initializeNewVersion1() {
    	String testSchema = "semver";
    	Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("0.1.0", v.constructVersionString());
    }
    
    @Test
    public void initializeNewVersion2() {
    	String testSchema = "yyyy.month-calvermodifier";
    	Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
        assertTrue ( true );
    }
    
    @Test
    public void initializeNewVersion3() {
    	String testSchema = "yyyy.month.minor.patch";
    	Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
        assertTrue ( true );
    }
    
    @Test
    public void initializeNewVersion4BuildidBuildenv() {
    	String testSchema = "yyyy.month.buildenv.buildid";
    	Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
        assertTrue ( true );
    }
    
	@Test
    public void initializeNewVersion5_YYOMCalver() {
    	String testSchema = "YY0M.DD";
    	Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
        assertTrue ( true );
    }

	@Test
    public void initializeNewVersion6_YYYYOMCalver() {
    	String testSchema = "YYYY0M.DD";
    	Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
        assertTrue ( true );
    }
	
    @Test
    public void initializeVersionFromPin1SemVer() {
    	String testSchema = "semver";
    	String testPin = "1.2.patch";
    	Version v = Version.getVersionFromPin(testSchema, testPin);
        assertEquals("1.2.0", v.constructVersionString());
    }
    
    @Test
    public void initializeVersionFromPin2SemVer() {
    	String testSchema = "semver";
    	String testPin = "3.minor.patch";
    	Version v = Version.getVersionFromPin(testSchema, testPin);
        assertEquals("3.0.0", v.constructVersionString());
    }
    
    @Test
    public void initializeVersionFromPin3CalVer() {
    	String testSchema = VersionType.CALVER_RELIZA_2020.getSchema();
    	String testPin = "2020.01.Calvermodifier.Minor.Micro+Mymetadata";
    	Version v = Version.getVersionFromPin(testSchema, testPin);
        assertEquals("2020.01.Snapshot.0.0+Mymetadata", v.constructVersionString());
    }
    
    @Test
    public void initializeVersionFromPin4_YYYYOM_CalVer() {
    	String testSchema = "YYYY0M.DD.Micro";
    	String testPin = "202101.1.Micro";
    	Version v = Version.getVersionFromPin(testSchema, testPin);
        assertEquals("202101.1.0", v.constructVersionString());
    }
    
    @Test
    public void initializeVersionFromPin5_YYOM_CalVer() {
    	String testSchema = "YY0M.DD.Micro";
    	String testPin = "2101.11.Micro";
    	Version v = Version.getVersionFromPin(testSchema, testPin);
        assertEquals("2101.11.0", v.constructVersionString());
    }
    
    @Test
    public void getRelizaCalver() {
    	String version = VersionApi.getRelizaCalver(null, null);
    	System.out.println("Reliza Calver = " + version);
        assertTrue ( true );
    }
    
    @Test
    public void getRelizaCalverWithModMeta() {
    	String version = VersionApi.getRelizaCalver("CustomModifier", "someMetaData");
    	System.out.println("Reliza Calver With Mod and Meta = " + version);
        assertTrue ( true );
    }
    
    @Test
    public void getRelizaCalver2020() {
    	String version = VersionApi.getRelizaCalver2020(null, null);
    	System.out.println("Reliza Calver 2020 = " + version);
        assertTrue ( true );
    }
    
    @Test
    public void getRelizaCalver2020WithModMeta() {
    	String version = VersionApi.getRelizaCalver2020("CustomModifier", "someMetaData");
    	System.out.println("Reliza Calver 2020 With Mod and Meta = " + version);
        assertTrue ( true );
    }
    
    @Test
    public void getUbuntuCalver() {
    	String version = VersionApi.getUbuntuCalver();
    	System.out.println("Ubuntu CalVer = " + version);
        assertTrue ( true );
    }
    
    @Test
    public void supportMavenSnapshotAsIs() {
    	String version = "2019.05.Stable.1-SNAPSHOT";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	Version v = Version.getVersion(version, schema);
    	assertEquals("2019.05.Stable.1-SNAPSHOT", v.constructVersionString());
    }
    
    @Test
    public void supportMavenSnapshotForceOut() {
    	String version = "2019.05.Stable.1-SNAPSHOT";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	Version v = Version.getVersion(version, schema);
    	v.setSnapshot(false);
    	assertEquals("2019.05.Stable.1", v.constructVersionString());
    }
    
    @Test
    public void supportMavenSnapshotForceIn() {
    	String version = "2019.05.Stable.1";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	Version v = Version.getVersion(version, schema);
    	v.setSnapshot(true);
    	assertEquals("2019.05.Stable.1-SNAPSHOT", v.constructVersionString());
    }
    
    @Test
    public void versionComparison1year() {
    	String version1 = "2020.05.Stable.1";
    	String version2 = "2019.10.Stable.1";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	Version v1 = Version.getVersion(version1, schema);
    	Version v2 = Version.getVersion(version2, schema);
    	List<Version> vList = new LinkedList<>();
    	vList.add(v2);
    	vList.add(v1);
    	Collections.sort(vList);
    	assertTrue(version1.equals(vList.get(0).constructVersionString()));
    }
    
    @Test
    public void versionComparison2semver() {
    	String version1 = "2.3.5";
    	String version2 = "2.7.10";
    	String schema = VersionType.SEMVER.getSchema();
    	Version v1 = Version.getVersion(version1, schema);
    	Version v2 = Version.getVersion(version2, schema);
    	List<Version> vList = new LinkedList<>();
    	vList.add(v2);
    	vList.add(v1);
    	Collections.sort(vList);
    	assertTrue(version2.equals(vList.get(0).constructVersionString()));
    }
    
    @Test
    public void versionComparison3buildid() {
    	String version1 = "2.3.5.28";
    	String version2 = "2.3.5.7";
    	String schema = "major.minor.patch.buildid";
    	Version v1 = Version.getVersion(version1, schema);
    	Version v2 = Version.getVersion(version2, schema);
    	List<Version> vList = new LinkedList<>();
    	vList.add(v2);
    	vList.add(v1);
    	Collections.sort(vList);
    	assertTrue(version1.equals(vList.get(0).constructVersionString()));
    }
    
    @Test
    public void versionStringComparator1Semver() {
    	String version1 = "2.3.25";
    	String version2 = "2.3.7";
    	String schema = VersionType.SEMVER.getSchema();
    	List<String> vList = new LinkedList<>();
    	vList.add(version2);
    	vList.add(version1);
    	Collections.sort(vList, new VersionStringComparator(schema));
    	assertTrue(version1.equals(vList.get(0)));
    }
    
    @Test
    public void versionStringComparator2Calver() {
    	String version1 = "2020.03.Stable.1";
    	String version2 = "2019.10.Stable.1";
    	String version3 = "2019.09.Stable.1";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	List<String> vList = new LinkedList<>();
    	vList.add(version2);
    	vList.add(version1);
    	vList.add(version3);
    	Collections.sort(vList, new VersionStringComparator(schema));
    	assertTrue(version1.equals(vList.get(0)));
    }

    @Test
    public void versionWithBranchGeneration() {
    	String schema = VersionType.FEATURE_BRANCH.getSchema();
    	Version v = Version.getVersion(schema);
    	v.setBranch("234-my_feature");
    	assertEquals("234-my_feature.0", v.constructVersionString());
    }
    
    @Test
    public void versionWithBranchMatching() {
    	String testSchema = VersionType.FEATURE_BRANCH.getSchema();
    	String testVersion = "234my/feature_issue-description.5";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void versionWithBranchBump() {
    	String schema = VersionType.FEATURE_BRANCH.getSchema();
    	String oldVersion = "234-my_feature.0";
    	Version v = Version.getVersionFromPinAndOldVersion(schema, schema, oldVersion, ActionEnum.BUMP);
    	assertEquals("234-my_feature.1", v.constructVersionString());
    }
    
    @Test
    public void versionWithCalverBranchGeneration() {
    	String schema = VersionType.FEATURE_BRANCH_CALVER.getSchema();
    	Version v = Version.getVersion(schema);
    	v.setBranch("234-my_feature");
    	assertEquals(CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".234-my_feature.0", v.constructVersionString());
    }
    
    @Test
    public void versionWithCalverBranchMatching() {
    	String testSchema = VersionType.FEATURE_BRANCH_CALVER.getSchema();
    	String testVersion = "2020.04.234my/feature_issue-description.5";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }
    
    @Test
    public void versionWithCalverBranchBump() {
    	String schema = VersionType.FEATURE_BRANCH_CALVER.getSchema();
    	String oldVersion = "2020.09.234-my_feature.0";
    	Version v = Version.getVersionFromPinAndOldVersion(schema, schema, oldVersion, ActionEnum.BUMP);
    	assertEquals(CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".234-my_feature.0", v.constructVersionString());
    }
    
    @Test
    public void versionStringComparator3CalverNotMatching() {
    	String version1 = "2020.03.Stable.1";
    	String version2 = "2021.10.15.Stable.1"; // this version doesn't match schema so it will be at the bottom of the list
    	String version3 = "2019.09.Stable.1";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	List<String> vList = new LinkedList<>();
    	vList.add(version2);
    	vList.add(version1);
    	vList.add(version3);
    	Collections.sort(vList, new VersionStringComparator(schema));
    	assertTrue(version1.equals(vList.get(0)));
    }
    
    @Test
    public void bumpPatchCalver() {
    	String schema = VersionType.CALVER_RELIZA_2020.getSchema();
    	Version v = Version.getVersion(schema);
    	assertEquals(CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".Snapshot.1.0", v.constructVersionString());
    	v.bumpPatch(null);
    	assertEquals(CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".Snapshot.1.1", v.constructVersionString());
    	v = Version.getVersion("2021.01.Snapshot.0.0", schema);
    	VersionApi.applyActionOnVersion(v, ActionEnum.BUMP_PATCH);
    	assertEquals("2021.01.Snapshot.0.1", v.constructVersionString());
    }
    
    @Test
    public void bumpPatchCalver_YYOM() {
    	String schema = "YYOM.Micro";
    	Version v = Version.getVersion(schema);
    	assertEquals(CURRENT_YEAR_SHORT + CURRENT_MONTH + ".0", v.constructVersionString());
    	v.bumpPatch(null);
    	assertEquals(CURRENT_YEAR_SHORT + CURRENT_MONTH + ".1", v.constructVersionString());
    	v = Version.getVersion("2112.2", schema);
    	VersionApi.applyActionOnVersion(v, ActionEnum.BUMP_PATCH);
    	assertEquals("2112.3", v.constructVersionString());
    }
    
    @Test
    public void testSemverWithMetadata() {
    	String schema = "semver";
    	Version v = Version.getVersion(schema);
    	v.setMetadata("testmetadata");
    	assertEquals("0.1.0+testmetadata", v.constructVersionString());
    }
    
    @Test
    public void TestSemverPinMatchingSemverSchema() {
    	String schema = "semver";
    	String pin = "semver";
    	assertTrue(VersionUtils.isPinMatchingSchema(schema, pin));
    }
    
    @Test
    public void TestSemverVersionMatchingSemverSchemaWithHyphenMetadata() {
    	String schema = "semver";
    	String version = "3.2.1-3";
    	assertTrue(VersionUtils.isVersionMatchingSchema(schema, version));
    }
    

    @Test
    public void TestSemverPinMatchingSemverSchemaVersionMeta() {
    	String schema = "semver";
    	String pin = "semver";
    	String version = "0.0.0+test3.relizahub.com";
    	assertTrue(VersionUtils.isVersionMatchingSchemaAndPin(schema, pin, version));
    }
    
    @Test
    public void bumpCalverVersionWithPin1() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.2.minor.patch-modifier";
    	String testOldVer = "2020.2.3.4-testmod";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	v.setModifier("newmodifier");
    	assertEquals("2020.2.3.5-newmodifier", v.constructVersionString());
    }
    
    @Test
    public void bumpCalverVersionWithPin2() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2021.month.minor.patch-modifier";
    	String testOldVer = "2021." + CURRENT_MONTH_SINGLE + ".3.4-testmod";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	v.setModifier("newmodifier");
    	assertEquals("2021." + CURRENT_MONTH_SINGLE + ".3.5-newmodifier", v.constructVersionString());
    }
    
    @Test
    public void bumpCalverVersionWithPin3() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2021.month.minor.patch-modifier";
    	String testOldVer = "2021.1.3.4-testmod";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	v.setModifier("newmodifier");
    	String assertedVersionSuffix = ("1".equals(CURRENT_MONTH_SINGLE)) ? ".3.5-newmodifier" : ".0.0-newmodifier";
    	assertEquals("2021." + CURRENT_MONTH_SINGLE + assertedVersionSuffix, v.constructVersionString());
    }
    
    @Test
    public void bumpCalverVersionWithPin4() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "Year.Month.minor.patch-modifier";
    	String testOldVer = "2021.1.3.4-testmod";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	v.setModifier("newmodifier");
    	assertEquals(CURRENT_YEAR_LONG + "." + CURRENT_MONTH_SINGLE + ".0.0-newmodifier", v.constructVersionString());
    }
    
    @Test
    public void bumpCalverVersionWithPin5() {
    	String testSchema = "YY.OM.Micro";
    	String testPin = "YY.OM.Micro";
    	String testOldVer = CURRENT_YEAR_SHORT + "." + CURRENT_MONTH + ".1";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	assertEquals(CURRENT_YEAR_SHORT + "." + CURRENT_MONTH + ".2", v.constructVersionString());
    }
    
    @Test
    public void bumpSemverVersionWithPin1() {
    	String testSchema = VersionType.SEMVER.getSchema();
    	String testPin = "5.9.patch";
    	String testOldVer = "5.9.2";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	assertEquals("5.9.3", v.constructVersionString());
    }
    
    @Test
    public void bumpSemverVersionWithPin2() {
    	String testSchema = VersionType.SEMVER.getSchema();
    	String testPin = "5.minor.patch";
    	String testOldVer = "5.6.2";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	assertEquals("5.6.3", v.constructVersionString());
    }
    
    @Test
    public void bumpSemverVersionWithPin3() {
    	String testSchema = VersionType.SEMVER.getSchema();
    	String testPin = "5.minor.patch";
    	String testOldVer = "5.6.2";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP_MINOR);
    	assertEquals("5.7.0", v.constructVersionString());
    }
	@Test
	void testParseVersion_BranchMultipleDashes() {
		String schema = "Year.Branch-modifier";
		String version = "2020.test-branch-go-mymodifier";
		VersionHelper vh = VersionUtils.parseVersion(version, schema, false).get();
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("2020");
		versionComponentsExpected.add("test-branch-go");
		ArrayList<String> versionComponenetsActual = new ArrayList<>(vh.getVersionComponents().stream().map(x -> x.representation()).toList());
		assertEquals(versionComponentsExpected, versionComponenetsActual);
		assertEquals("mymodifier", vh.getModifier());
	}
    
    @Test
    public void handleMultipleDashes1() {
    	String testSchema = "Year.Branch-modifier";
    	String testVer = "2020.test-branch-go-mymodifier";
    	boolean matches = VersionUtils.isVersionMatchingSchema(testSchema, testVer);
    	assertTrue(matches);
    }
    
    @Test
    public void handleMultipleDashes2() {
    	String testSchema = "Year.Branch.patch";
    	String testVer = "2020.test-branch-go-mymodifier.2";
    	boolean matches = VersionUtils.isVersionMatchingSchema(testSchema, testVer);
    	assertTrue(matches);
    }
    
	@Test
    public void testApplyBumpMinorOnCalver() {
    	String schema = "YYYY.0M.Micro";
    	String version = CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".1";
		ActionEnum action = ActionEnum.BUMP_MINOR;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, schema, version, action);
		String actualV = newV.constructVersionString();
		String expectedV = CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".2";
		assertEquals(expectedV, actualV);
    }
	
	@Test
    public void testApplyBumpMajorOnCalver() {
    	String schema = "YYYY.0M.Micro";
    	String version = CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".1";
		ActionEnum action = ActionEnum.BUMP_MAJOR;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, schema, version, action);
		String actualV = newV.constructVersionString();
		String expectedV = CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".2";
		assertEquals(expectedV, actualV);
    }
	
	@Test
    public void testApplyBumpPatchOnCalver() {
    	String schema = "YYYY.0M.Micro";
    	String version = "2021.02.1";
		ActionEnum action = ActionEnum.BUMP_PATCH;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, schema, version, action);
		String actualV = newV.constructVersionString();
		String expectedV = "2021.02.2";
		assertEquals(expectedV, actualV);
    }
	
	@Test
    public void testApplyBumpPatchOnFullCalver() {
    	String schema = "YYYY.0M.DD";
    	String version = "2021.08.1";
		ActionEnum action = ActionEnum.BUMP_PATCH;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, schema, version, action);
		String actualV = newV.constructVersionString();
		String expectedV = CURRENT_YEAR_LONG + "." + CURRENT_MONTH + "." + CURRENT_DAY;
		assertEquals(expectedV, actualV);
    }
	
	@Test
	public void nanoMatchSchema() {
		String testSchema = "Major.Minor.Patch.Nano";
		String testVersion = "2.3.4.5";
		assertTrue(VersionUtils.isVersionMatchingSchema(testSchema, testVersion));
	}
	
	@Test
	public void nanoCalverMatchingSchema() {
		String testSchema = "YYYY.0M.Patch.Nano";
		String testVersion = "2021.05.3.4";
		assertTrue(VersionUtils.isVersionMatchingSchema(testSchema, testVersion));
	}
	
	@Test
	public void nanoConstructVersionString() {
		String schema = "YYYY.Patch.Nano";
		Version v = Version.getVersion(schema);
		assertEquals(CURRENT_YEAR_LONG + ".0.0", v.constructVersionString());
	}
	
	@Test
	public void bumpPatchWithNano() {
		Version v = Version.getVersion("2021.3.3","YYYY.Patch.nano");
		v.bumpPatch(null);
		assertEquals("2021.4.0", v.constructVersionString());
	}
	
	@Test
	public void bumpMajorWithNano() {
		Version v = Version.getVersion("4.3.3","Major.Patch.nano");
		v.bumpMajor(null);
		assertEquals("5.0.0", v.constructVersionString());
	}
	
	@Test
	public void nanoSimpleBump() {
		Version v = Version.getVersion("1.2.3.4", "Major.minor.patch.nano");
		v.simpleBump();
		assertEquals("1.2.4.0", v.constructVersionString());
	}
	
	
	@Test
	public void semverCompareVersions() {
		Version v1 = Version.getVersion("4.4.2", "Major.Minor.Patch");
		Version v2 = Version.getVersion("4.4.3", "Major.Minor.Patch");
		// expected: v1 less than v2
		assertEquals(1, v1.compareTo(v2));
	}
	
	
	@Test
	public void nanoCompareVersions() {
		Version v1 = Version.getVersion("4.2", "Minor.Nano");
		Version v2 = Version.getVersion("4.3", "Minor.Nano");
		// expected, v1 less then v2
		assertEquals(1, v1.compareTo(v2));
	}
	
	@Test
	public void calverCompareVersions() {
		Version v1 = Version.getVersion("2021.04", "YYYY.0M");
		Version v2 = Version.getVersion("2020.02", "YYYY.0M");
		// expected: v1 greater than v2
		assertEquals(-1, v1.compareTo(v2));
	}
	
	@Test
	public void nanoGetVersionFromPinAndOldVersion() {
		String testSchema = "YYYY.0M.Major.Minor.Patch.Nano";
		String pin = "2021.07.Major.2.Patch.Nano";
		String oldVersion = "2021.07.3.2.1.0";
		ActionEnum ae = ActionEnum.BUMP_MINOR;
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, ae);
		System.out.println(v.constructVersionString());
		// YYYY.0M.Patch schema should match 2020.01.Patch pin
		assertEquals("2021.07.3.2.2.0", v.constructVersionString());
	}
	
	@Test // this is one scenario where we bump nano
	public void nanoSemverBranchPin() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "1.7.4.Nano";
		String oldVersion = "1.7.4.3";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, null);
		//System.out.println(v.constructVersionString());
		assertEquals("1.7.4.4", v.constructVersionString());
	}
	
	@Test // no old version so just initialize to starting version
	public void nanoSemverBranchPinBumpPatch() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "2.2.Patch.Nano";
		//String oldVersion = "2.2.2.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, null, null);
		//System.out.println(v.constructVersionString());
		assertEquals("2.2.0.0", v.constructVersionString());
	}
	
	// no action enum input present, default to BUMP (simple bump)
	@Test
	public void nanoSemverBranchPinOldVersion() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "2.2.Patch.Nano";
		String oldVersion = "2.2.0.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, null);
		//System.out.println(v.constructVersionString());
		assertEquals("2.2.1.0", v.constructVersionString());
	}
	
	@Test // bump nano scenario
	public void nanoSemverPinOldVersion() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "2.2.2.Nano";
		String oldVersion = "2.2.2.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, null);
		//System.out.println(v.constructVersionString());
		assertEquals("2.2.2.2", v.constructVersionString());
	}
	
	@Test // Bump Patch with old version
	public void nanoSemverBranchPinBumpPatchOldVersionBumpPatch() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "2.2.Patch.Nano";
		String oldVersion = "2.2.2.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, ActionEnum.BUMP_PATCH);
		//System.out.println(v.constructVersionString());
		assertEquals("2.2.3.0", v.constructVersionString());
	}
	
	@Test //basically same as above, if AE=Bump_Minor but Minor is pinned, will attempt to bump patch instead
	public void nanoSemverBranchPinBumpPatchOldVersionBumpPatch2() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "2.2.Patch.Nano";
		String oldVersion = "2.2.2.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, ActionEnum.BUMP_MINOR);
		//System.out.println(v.constructVersionString());
		assertEquals("2.2.3.0", v.constructVersionString());
	}
	
	@Test // bump nano scenario
	public void nanoSemverPinOldVersion2() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "5.0.5.Nano";
		String oldVersion = "5.0.5.4";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, null);
		//System.out.println(v.constructVersionString());
		assertEquals("5.0.5.5", v.constructVersionString());
	}
	
	
	@Test // bump patch because minor is not pinned
	public void nanoSemverPinOldVersion3() {
		String testSchema = "Major.Minor.Patch.Nano";
		String pin = "3.2.Patch.Nano";
		String oldVersion = "3.2.2.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, null);
		//System.out.println(v.constructVersionString());
		assertEquals("3.2.3.0", v.constructVersionString());
	}
	
	@Test // simple bump with patch and nano not pinned (so bump patch)
	public void nanoSemverPinOldVersionCalver() {
		String testSchema = "YYYY.Major.Minor.Patch.Nano";
		String pin = "2021.4.3.Patch.Nano";
		String oldVersion = "2021.4.3.2.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, ActionEnum.BUMP);
		//System.out.println(v.constructVersionString());
		assertEquals("2021.4.3.3.0", v.constructVersionString());
	}
	
	@Test // don't think we bump nano here
	public void nanoSemverPinOldVersionBumpDate1() {
		String testSchema = "YYYY.0M.Major.Minor.Patch.Nano";
		String pin = "2021.0M.3.3.3.Nano";
		String oldVersion = "2021.01.3.3.3.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, ActionEnum.BUMP_DATE);
		//System.out.println(v.constructVersionString());
		String nano = "0";
		if (CURRENT_MONTH.equals("01")) {
			nano = "1";
		}
		assertEquals("2021." + CURRENT_MONTH +".3.3.3." + nano, v.constructVersionString());
	}
	
	@Test // everything else pinned so bump nano
	public void nanoSemverPinOldVersionBumpDate2() {
		String testSchema = "YYYY.0M.Major.Minor.Patch.Nano";
		String pin = "2021.04.3.3.3.Nano";
		String oldVersion = "2021.04.3.3.3.1";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, oldVersion, null);
		//System.out.println(v.constructVersionString());
		assertEquals("2021.04.3.3.3.2", v.constructVersionString());
	}
	
	@Test // Issue #2 on GitHub - https://github.com/relizaio/versioning/issues/2
	// incorrect view of modifier
	public void modifierNotResolvedCorrectlyOnBumpWhenSchemaProvided() {
		String testSchema = "0Y.0M.0D.Micro-Modifier";
		String oldVersion = "22.03.28.2-dev";
		Version v = Version.getVersionFromPinAndOldVersion(testSchema, testSchema, oldVersion, ActionEnum.BUMP_PATCH);
		System.out.println(v.constructVersionString());
		assertEquals("22.03.28.3-dev", v.constructVersionString());
	}

	// @Test // Issue #2 on GitHub - https://github.com/relizaio/versioning/issues/2
	// // incorrect view of modifier
	// public void semverCustomPinWithNano() {
	// 	String testSchema = "semver";
	// 	String pin = "5.5.3.Nano";
	// 	//String oldVersion = "2.2.2.1";
	// 	Version v = Version.getVersionFromPinAndOldVersion(testSchema, pin, null, null);
	// 	//System.out.println(v.constructVersionString());
	// 	assertEquals("5.5.3.0", v.constructVersionString());
	// }

	@Test
    public void testPinMatchingXXXWithMicro() {
    	String testSchema = "MAJOR.MINOR.PATCH.NANO";
    	String testPin = "5.5.3.Nano";
        assertTrue( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    @Test
    public void testBumpMatchingXXXWithMicro() {

    	String testSchema = "MAJOR.MINOR.PATCH.NANO";
    	String testPin = "5.5.3.Nano";
		Version v = Version.getVersionFromPin(testSchema, testPin);
		String testVersion = "5.5.3.0";
		assertEquals(testVersion, v.constructVersionString());
		v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testVersion, ActionEnum.BUMP);
		assertEquals("5.5.3.1", v.constructVersionString());
    }
	@Test
    public void testPinMatchingBranchHyphenMicro() {
    	String testSchema = "branch-micro";
    	String testPin = "branch-1";
        assertTrue( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }

	@Test
    public void testSchemaMatchingBranchHyphenMicro() {
    	String testSchema = "branch-micro";
    	String testVersion = "foo-bar-1";
        assertTrue( VersionUtils.isVersionMatchingSchema(testSchema, testVersion) );
    }

    @Test
    public void getVersionForBranchHyphenMicro() {
    	String testSchema = "branch-micro";
    	String testVersion = "foo-0";
		Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
		v.setBranch("foo");
        assertEquals (testVersion, v.constructVersionString());
		v = Version.getVersionFromPinAndOldVersion(testSchema, testSchema, testVersion, ActionEnum.BUMP);
		assertEquals("foo-1", v.constructVersionString());
        
    }
    @Test
    public void getVersionForBranchHyphenMicroMulti() {
    	String testSchema = "branch-micro";
    	String testVersion = "foo-bar-0";
		Version v = Version.getVersion(testSchema);
    	System.out.println(v.constructVersionString());
		v.setBranch("foo-bar");
        assertEquals (testVersion, v.constructVersionString());
		v = Version.getVersionFromPinAndOldVersion(testSchema, testSchema, testVersion, ActionEnum.BUMP);
		assertEquals("foo-bar-1", v.constructVersionString());
        
    }

	@Test
	void testParseVersion_BranchHyphenMicro() {
		String version = "foo-1";
		String schema = "branch-micro";
		VersionHelper vh = VersionUtils.parseVersion(version, schema, false).get();
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("foo");
		versionComponentsExpected.add("1");
		ArrayList<String> versionComponenetsActual = new ArrayList<>(vh.getVersionComponents().stream().map(x -> x.representation()).toList());
		assertEquals(versionComponentsExpected, versionComponenetsActual);
	}

	@Test
	void testParseVersion_BranchHyphenMicroMulti() {
		String version = "foo-bar-1";
		String schema = "branch-micro";
		VersionHelper vh = VersionUtils.parseVersion(version, schema, false).get();
		ArrayList<String> versionComponentsExpected = new ArrayList<String>();
		versionComponentsExpected.add("foo-bar");
		versionComponentsExpected.add("1");
		ArrayList<String> versionComponenetsActual = new ArrayList<>(vh.getVersionComponents().stream().map(x -> x.representation()).toList());
		assertEquals(versionComponentsExpected, versionComponenetsActual);
	}

	@Test
    public void testIdenticalSemverBumpViaModifier1 () {
    	String schema = "semver";
    	String oldVersion = "1.2.0-1";
		String pin = "1.2.0-1";
		ActionEnum action = ActionEnum.BUMP;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, action);
		String actualV = newV.constructVersionString();
		String expectedV = "1.2.0-2";
		assertEquals(expectedV, actualV);
    }

	@Test
    public void testIdenticalSemverBumpViaModifier2 () {
    	String schema = "semver";
    	String oldVersion = "1.2.3";
		String pin = "1.2.3";
		ActionEnum action = ActionEnum.BUMP_MAJOR;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, action);
		String actualV = newV.constructVersionString();
		String expectedV = "1.2.3-1";
		assertEquals(expectedV, actualV);
    }

	@Test
    public void testIdenticalSemverBumpViaModifier3 () {
    	String schema = "semver";
    	String oldVersion = "1.2.0-11";
		String pin = "1.2.0-2";
		ActionEnum action = ActionEnum.BUMP;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, action);
		String actualV = newV.constructVersionString();
		String expectedV = "1.2.0-12";
		assertEquals(expectedV, actualV);
    }

	@Test
    public void testIdenticalSemverBumpViaMetadata () {
    	String schema = "semver";
    	String oldVersion = "1.2.0-testfeature.1+49";
		String pin = "1.2.0-testfeature.1+30";
		ActionEnum action = ActionEnum.BUMP;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, action);
		String actualV = newV.constructVersionString();
		String expectedV = "1.2.0-testfeature.1+50";
		assertEquals(expectedV, actualV);
    }

	@Test
    public void testIdenticalSemverBumpDashedBranchViaMetadata () {
    	String schema = "semver";
    	String oldVersion = "1.2.0-test-feature.1+49";
		String pin = "1.2.0-test-feature.1+30";
		ActionEnum action = ActionEnum.BUMP;
		Version newV = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, action);
		String actualV = newV.constructVersionString();
		String expectedV = "1.2.0-test-feature.1+50";
		assertEquals(expectedV, actualV);
    }

	// ==================== Namespace Tests ====================

	@Test
	public void testGetVersionFromPinWithSemverAlias_NoSchemaKeywordsInOutput() {
		// When pin is 'semver' alias, metadata and modifier should be empty, not 'Metadata?' or 'Modifier?'
		Version v = Version.getVersionFromPin("semver", "semver", null);
		assertEquals("0.0.0", v.constructVersionString());
		assertNull(v.getModifier());
		assertNull(v.getMetadata());
	}

	@Test
	public void testGetVersionFromPinWithNamespace_NoModifier() {
		// When no modifier exists and namespace is provided, modifier should be set to namespace
		String schema = "YYYY.0M.Calvermodifier.Micro";
		String pin = "2025.01.Calvermodifier.Micro";
		String namespace = "rc";
		Version v = Version.getVersionFromPin(schema, pin, namespace);
		assertEquals("2025.01.Snapshot.0", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_NumericModifierGetsNamespacePrefix() {
		// When old version has numeric modifier like -1, it becomes -namespace2
		String schema = "YYYY.0M.Micro-modifier";
		String pin = "2025.01.5-modifier";  // Micro is pinned to 5
		String oldVersion = "2025.01.5-1";
		String namespace = "rc";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("2025.01.5-rc2", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_ExistingNamespaceModifierBumpsNumber() {
		// When old version already has namespace prefix like -rc1, just bump to -rc2
		String schema = "YYYY.0M.Micro-modifier";
		String pin = "2025.01.5-modifier";  // Micro is pinned to 5
		String oldVersion = "2025.01.5-rc1";
		String namespace = "rc";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("2025.01.5-rc2", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_ExistingNamespaceModifierBumpsHigherNumber() {
		// When old version has -rc5, bump to -rc6
		String schema = "YYYY.0M.Micro-modifier";
		String pin = "2025.01.5-modifier";  // Micro is pinned to 5
		String oldVersion = "2025.01.5-rc5";
		String namespace = "rc";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("2025.01.5-rc6", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_CalverUpdateResetsToNamespace() {
		// When calver date changes, modifier should reset to just namespace
		String schema = "YYYY.0M.Micro-modifier";
		String pin = "YYYY.0M.Micro-modifier";
		String oldVersion = "2024.12.5-rc3";
		String namespace = "rc";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		// Since date changes to current, modifier resets to namespace
		assertEquals(CURRENT_YEAR_LONG + "." + CURRENT_MONTH + ".0-rc", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_SemverWithNamespace() {
		// Test namespace with semver schema - when patch bumps, modifier is cleared
		String schema = "semver";
		String pin = "1.2.patch";
		String oldVersion = "1.2.3-2";
		String namespace = "beta";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("1.2.4-beta", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_SemverWithNamespace_2() {
		// Test namespace with semver schema - when patch is pinned, modifier bumps with namespace
		String schema = "semver";
		String pin = "1.2.4";  // patch is pinned, so modifier should bump
		String oldVersion = "1.2.4-1";
		String namespace = "beta";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("1.2.4-beta2", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_SemverWithNamespace_3() {
		// Test namespace with semver schema - when patch is pinned, modifier bumps with namespace
		String schema = "semver";
		String pin = "1.2.4";  // patch is pinned, so modifier should bump
		String oldVersion = "1.2.4-beta1";
		String namespace = "beta";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("1.2.4-beta2", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_NullNamespaceBehavesAsOriginal() {
		// When namespace is null, behavior should be same as original method
		String schema = "YYYY.0M.Micro-modifier";
		String pin = "2025.01.5-modifier";  // Micro is pinned to 5
		String oldVersion = "2025.01.5-1";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, null);
		assertEquals("2025.01.5-2", v.constructVersionString());
	}

	@Test
	public void testGetVersionFromPinAndOldVersionWithNamespace_EmptyNamespaceBehavesAsOriginal() {
		// When namespace is empty string, behavior should be same as original method
		String schema = "YYYY.0M.Micro-modifier";
		String pin = "2025.01.5-modifier";  // Micro is pinned to 5
		String oldVersion = "2025.01.5-1";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, "");
		assertEquals("2025.01.5-2", v.constructVersionString());
	}

	// ==================== Four Part Versioning Tests ====================

	@Test
	public void testFourPartVersioning_AliasResolution() {
		// Test that 'four_part' alias resolves correctly
		String schema = "four_part";
		String pin = "four_part";
		Version v = Version.getVersionFromPin(schema, pin, null);
		assertEquals("0.0.0.0", v.constructVersionString());
		assertNull(v.getModifier());
		assertNull(v.getMetadata());
	}

	@Test
	public void testFourPartVersioning_SchemaMatching() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String version = "1.2.3.4";
		assertTrue(VersionUtils.isVersionMatchingSchema(schema, version));
	}

	@Test
	public void testFourPartVersioning_SchemaMatchingWithModifier() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String version = "1.2.3.4-beta";
		assertTrue(VersionUtils.isVersionMatchingSchema(schema, version));
	}

	@Test
	public void testFourPartVersioning_SchemaMatchingWithMetadata() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String version = "1.2.3.4+build123";
		assertTrue(VersionUtils.isVersionMatchingSchema(schema, version));
	}

	@Test
	public void testFourPartVersioning_SchemaMatchingWithModifierAndMetadata() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String version = "1.2.3.4-rc1+build123";
		assertTrue(VersionUtils.isVersionMatchingSchema(schema, version));
	}

	@Test
	public void testFourPartVersioning_PinMatching() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.patch.nano";
		assertTrue(VersionUtils.isPinMatchingSchema(schema, pin));
	}

	@Test
	public void testFourPartVersioning_PinMatchingWithRevision() {
		// Test that 'revision' synonym works for NANO
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.patch.revision";
		assertTrue(VersionUtils.isPinMatchingSchema(schema, pin));
	}

	@Test
	public void testFourPartVersioning_InitializeFromPin() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.patch.nano";
		Version v = Version.getVersionFromPin(schema, pin);
		assertEquals("1.2.0.0", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_InitializeFromPinWithRevision() {
		// Test that 'revision' synonym works in pin
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.patch.revision";
		Version v = Version.getVersionFromPin(schema, pin);
		assertEquals("1.2.0.0", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_BumpNano() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.3.nano";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP);
		assertEquals("1.2.3.6", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_BumpPatch() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.patch.nano";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP_PATCH);
		assertEquals("1.2.4.0", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_BumpMinor() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.minor.patch.nano";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP_MINOR);
		assertEquals("1.3.0.0", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_BumpMajor() {
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "major.minor.patch.nano";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP_MAJOR);
		assertEquals("2.0.0.0", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_WithNamespace_NoModifier() {
		// When no modifier exists and namespace is provided, modifier should be set to namespace+1
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.3.4";  // all pinned
		String oldVersion = "1.2.3.4";
		String namespace = "rc";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("1.2.3.4-rc1", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_WithNamespace_NumericModifier() {
		// When old version has numeric modifier, it becomes namespace + (number+1)
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.3.4";  // all pinned
		String oldVersion = "1.2.3.4-1";
		String namespace = "rc";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("1.2.3.4-rc2", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_WithNamespace_ExistingNamespaceModifier() {
		// When old version already has namespace prefix, just bump the number
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.3.4";  // all pinned
		String oldVersion = "1.2.3.4-rc1";
		String namespace = "rc";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, namespace);
		assertEquals("1.2.3.4-rc2", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_WithNamespace_BumpPatchSetsNamespace() {
		// When patch bumps with namespace, modifier should be set to namespace
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.patch.nano";
		String oldVersion = "1.2.3.5";
		String namespace = "beta";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP_PATCH, namespace);
		assertEquals("1.2.4.0-beta", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_NullNamespaceBehavesAsOriginal() {
		// When namespace is null, behavior should be same as original
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		String pin = "1.2.3.nano";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, null);
		assertEquals("1.2.3.6", v.constructVersionString());
	}

	@Test
	public void testFourPartVersioning_VersionComparison() {
		// Version compareTo sorts in descending order (newest first)
		String schema = VersionType.FOUR_PART_VERSIONING.getSchema();
		Version v1 = Version.getVersion("1.2.3.4", schema);
		Version v2 = Version.getVersion("1.2.3.5", schema);
		List<Version> vList = new LinkedList<>();
		vList.add(v1);
		vList.add(v2);
		Collections.sort(vList);
		assertEquals("1.2.3.5", vList.get(0).constructVersionString());
		assertEquals("1.2.3.4", vList.get(1).constructVersionString());
	}

	@Test
	public void testBugfixSynonymForPatch() {
		// Test that 'bugfix' works as synonym for PATCH
		String schema = "Major.Minor.Bugfix.Nano";
		String pin = "1.2.bugfix.nano";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP_PATCH);
		assertEquals("1.2.4.0", v.constructVersionString());
	}

	@Test
	public void testBuildSynonymForPatch() {
		// Test that 'build' works as synonym for PATCH
		String schema = "Major.Minor.Build.Nano";
		String pin = "1.2.build.nano";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP_PATCH);
		assertEquals("1.2.4.0", v.constructVersionString());
	}

	@Test
	public void testRevisionSynonymForNano() {
		// Test that 'revision' works as synonym for NANO
		String schema = "Major.Minor.Patch.Revision";
		String pin = "1.2.3.revision";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP);
		assertEquals("1.2.3.6", v.constructVersionString());
	}

	@Test
	public void testMajorMinorBugfixRevisionSchema() {
		// Test full major.minor.bugfix.revision schema
		String schema = "Major.Minor.Bugfix.Revision-Modifier?";
		String pin = "1.2.bugfix.revision";
		String oldVersion = "1.2.3.5";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP_PATCH);
		assertEquals("1.2.4.0", v.constructVersionString());
	}

	// ==================== isSchemaSemver and isSchemaFourPartVersioning Tests ====================

	@Test
	public void testIsSchemaSemver_ValidSchemas() {
		assertTrue(VersionUtils.isSchemaSemver("Major.Minor.Patch"));
		assertTrue(VersionUtils.isSchemaSemver("major.minor.patch"));
		assertTrue(VersionUtils.isSchemaSemver("Major.Minor.Micro"));  // micro is synonym for patch
		assertTrue(VersionUtils.isSchemaSemver("major.minor.micro"));
		assertTrue(VersionUtils.isSchemaSemver("Major.Minor.Build"));  // build is synonym for patch
		assertTrue(VersionUtils.isSchemaSemver("Major.Minor.Patch-Modifier"));
		assertTrue(VersionUtils.isSchemaSemver("Major.Minor.Patch+Metadata"));
		assertTrue(VersionUtils.isSchemaSemver("Major.Minor.Patch-Modifier+Metadata"));
		assertTrue(VersionUtils.isSchemaSemver("semver"));  // alias
	}

	@Test
	public void testIsSchemaSemver_InvalidSchemas() {
		assertFalse(VersionUtils.isSchemaSemver("Major.Minor"));
		assertFalse(VersionUtils.isSchemaSemver("Major.Minor.Patch.Nano"));
		assertFalse(VersionUtils.isSchemaSemver("YYYY.MM.Patch"));
		assertFalse(VersionUtils.isSchemaSemver(""));
		assertFalse(VersionUtils.isSchemaSemver("1.2.micro.nano"));  // version pin, not schema
	}

	@Test
	public void testIsSchemaFourPartVersioning_ValidSchemas() {
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Patch.Nano"));
		assertTrue(VersionUtils.isSchemaFourPartVersioning("major.minor.patch.nano"));
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Micro.Nano"));  // micro is synonym for patch
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Build.Nano"));  // build is synonym for patch
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Patch.Revision"));  // revision is synonym for nano
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Build.Revision"));
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Patch.Nano-Modifier"));
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Patch.Nano+Metadata"));
		assertTrue(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Patch.Nano-Modifier+Metadata"));
		assertTrue(VersionUtils.isSchemaFourPartVersioning("four_part"));  // alias
	}

	@Test
	public void testIsSchemaFourPartVersioning_InvalidSchemas() {
		assertFalse(VersionUtils.isSchemaFourPartVersioning("Major.Minor.Patch"));
		assertFalse(VersionUtils.isSchemaFourPartVersioning("Major.Minor"));
		assertFalse(VersionUtils.isSchemaFourPartVersioning("YYYY.MM.Patch.Nano"));
		assertFalse(VersionUtils.isSchemaFourPartVersioning(""));
		assertFalse(VersionUtils.isSchemaFourPartVersioning(null));
		assertFalse(VersionUtils.isSchemaFourPartVersioning("1.2.micro.nano"));  // version string, not schema
	}

	@Test
	public void testIsSchemaCalver_InvalidSchemas() {
		assertFalse(VersionUtils.isSchemaCalver(""));
		assertFalse(VersionUtils.isSchemaCalver(null));
		assertFalse(VersionUtils.isSchemaCalver("1.2.micro.nano"));  // version pin, not schema
	}

	@Test
	public void testFourPartVersioning_BumpWithNoModifierInSchema() {
		// When schema is Major.Minor.Micro.Nano (no modifier in schema) and all elements are pinned,
		// BUMP should add a modifier to the version string
		String schema = "Major.Minor.Micro.Nano";
		String pin = "0.0.5.7";
		String oldVersion = "0.0.5.7";
		Version v = Version.getVersionFromPinAndOldVersion(schema, pin, oldVersion, ActionEnum.BUMP, null);
		// Since all 4 components are pinned, modifier should be bumped
		assertEquals("0.0.5.7-1", v.constructVersionString());
	}
}

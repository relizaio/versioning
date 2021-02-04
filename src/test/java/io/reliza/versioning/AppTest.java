/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.reliza.versioning.Version.VersionStringComparator;
import io.reliza.versioning.VersionApi.ActionEnum;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
	private static final String CURRENT_MONTH_SINGLE = "2";
    private static final String CURRENT_MONTH = "02";
    
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
    	String testVersion = "1.3.6";
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
    public void testPinMatching1Semver() {
    	String testSchema = "major.minor.patch-modifier";
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
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.22.minor.patch-modifier";
        assertFalse( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    
    @Test
    public void testPinMatching5SuccessCalver() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.2.minor.patch-modifier";
        assertTrue( VersionUtils.isPinMatchingSchema(testSchema, testPin) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema1Semver() {
    	String testSchema = "major.minor.patch-modifier";
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
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.1.minor.patch-modifier";
    	String testVersion = "2020.1.7.8";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema6CalverOptionalMetadata() {
    	String testSchema = "Year.Month.minor.patch+metadata";
    	String testPin = "2020.1.minor.patch+metadata";
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
    	String testSchema = "Year.Month.minor.patch+metadata";
    	String testPin = "2020.1.minor.patch+metadata";
    	String testVersion = "2020.1.7.8";
        assertTrue( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema9CalverFail() {
    	String testSchema = VersionType.CALVER_RELIZA_2020.getSchema();
    	String testPin = "2019.12.Calvermodifier.Minor.Micro+metadata";
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
    	String testPin = "2020.01.Calvermodifier.Minor.Micro+Metadata";
    	Version v = Version.getVersionFromPin(testSchema, testPin);
        assertEquals("2020.01.Snapshot.0.0+Metadata", v.constructVersionString());
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
    	String schema = Constants.SEMVER;
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
    	String schema = Constants.SEMVER;
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
    	assertEquals("2021." + CURRENT_MONTH + ".234-my_feature.0", v.constructVersionString());
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
    	assertEquals("2021." + CURRENT_MONTH + ".234-my_feature.0", v.constructVersionString());
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
    	assertEquals("2021." + CURRENT_MONTH + ".Snapshot.1.0", v.constructVersionString());
    	v.bumpPatch(null);
    	assertEquals("2021." + CURRENT_MONTH + ".Snapshot.1.1", v.constructVersionString());
    	v = Version.getVersion("2021.01.Snapshot.0.0", schema);
    	VersionApi.applyActionOnVersion(v, ActionEnum.BUMP_PATCH);
    	assertEquals("2021.01.Snapshot.0.1", v.constructVersionString());
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
    	assertEquals("2021." + CURRENT_MONTH_SINGLE + ".0.0-newmodifier", v.constructVersionString());
    }
    
    @Test
    public void bumpCalverVersionWithPin4() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "Year.Month.minor.patch-modifier";
    	String testOldVer = "2021.1.3.4-testmod";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	v.setModifier("newmodifier");
    	assertEquals("2021." + CURRENT_MONTH_SINGLE + ".0.0-newmodifier", v.constructVersionString());
    }
    
    @Test
    public void bumpCalverVersionWithPin5() {
    	String testSchema = "YY.OM.Micro";
    	String testPin = "YY.OM.Micro";
    	String testOldVer = "21." + CURRENT_MONTH + ".1";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	assertEquals("21." + CURRENT_MONTH + ".2", v.constructVersionString());
    }
    
    @Test
    public void bumpSemverVersionWithPin1() {
    	String testSchema = VersionType.SEMVER_FULL_NOTATION.getSchema();
    	String testPin = "5.9.patch";
    	String testOldVer = "5.9.2";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	assertEquals("5.9.3", v.constructVersionString());
    }
    
    @Test
    public void bumpSemverVersionWithPin2() {
    	String testSchema = VersionType.SEMVER_FULL_NOTATION.getSchema();
    	String testPin = "5.minor.patch";
    	String testOldVer = "5.6.2";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP);
    	assertEquals("5.6.3", v.constructVersionString());
    }
    
    @Test
    public void bumpSemverVersionWithPin3() {
    	String testSchema = VersionType.SEMVER_FULL_NOTATION.getSchema();
    	String testPin = "5.minor.patch";
    	String testOldVer = "5.6.2";
    	Version v = Version.getVersionFromPinAndOldVersion(testSchema, testPin, testOldVer, ActionEnum.BUMP_MINOR);
    	assertEquals("5.7.0", v.constructVersionString());
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
}

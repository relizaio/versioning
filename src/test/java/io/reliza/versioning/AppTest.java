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

/**
 * Unit test for simple App.
 */
public class AppTest 
{
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
    public void testVersionMatchingPinAndSchema3FailSemver() {
    	String testSchema = "major.minor.patch-modifier";
    	String testPin = "1.3.patch";
    	String testVersion = "1.2.5";
        assertFalse( VersionUtils.isVersionMatchingSchemaAndPin(testSchema, testPin, testVersion) );
    }
    
    @Test
    public void testVersionMatchingPinAndSchema4FailCalver() {
    	String testSchema = "Year.Month.minor.patch-modifier";
    	String testPin = "2020.1.minor.patch-modifier";
    	String testVersion = "2020.2.7.8-mod";
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
    	String testSchema = "YYYY.OM.modifier.patch";
    	String testVersion = "2019.05.prod.7";
    	Version v = Version.getVersion(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("2019.05.prod.7", v.constructVersionString());
    }
    
    @Test
    public void constructVersion3() {
    	String testSchema = "YYYY_OM.modifier_patch";
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
    	String testSchema = "YYYY_OM.modifier_patch";
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
    	String testSchema = "yyyy.month-modifier";
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
    	String testPin = "2020.01.Modifier.Minor.Micro+Metadata";
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
    
}

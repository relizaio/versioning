/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.reliza.versioning.Version;
import io.reliza.versioning.VersionApi;
import io.reliza.versioning.VersionUtils;

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
    public void testVersionStringOutput() {
    	String testSchema = "semver";
    	String testVersion = "1.3.6-alpha.1+1234.234.5";
    	Version v = new Version(testVersion, testSchema);
    	System.out.println(v);
        assertTrue( true );
    }
    
    @Test
    public void constructVersion1() {
    	String testSchema = "semver";
    	String testVersion = "1.3.6-alpha.1+1234.234.5";
    	Version v = new Version(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("1.3.6-alpha.1+1234.234.5", v.constructVersionString());
    }
    
    @Test
    public void constructVersion2() {
    	String testSchema = "YYYY.OM.modifier.patch";
    	String testVersion = "2019.05.prod.7";
    	Version v = new Version(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("2019.05.prod.7", v.constructVersionString());
    }
    
    @Test
    public void constructVersion3() {
    	String testSchema = "YYYY_OM.modifier_patch";
    	String testVersion = "2019_05.prod_7";
    	Version v = new Version(testVersion, testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("2019_05.prod_7", v.constructVersionString());
    }
    
    @Test
    public void constructVersionOtherSchemaFail() {
    	String testSchema = "YYYY_OM.modifier_patch";
    	String testVersion = "2019_05.prod_7";
    	Version v = new Version(testVersion, testSchema);
        Assertions.assertThrows(RuntimeException.class, () -> {
        	System.out.println(v.constructVersionString("semver"));
        });
    }
    
    @Test
    public void constructVersionOtherSchemaSuccess() {
    	String testSchema = "semver";
    	String testVersion = "7.4.284";
    	String useSchema = "major.minor";
    	Version v = new Version(testVersion, testSchema);
    	System.out.println(v.constructVersionString(useSchema));
        assertEquals ("7.4", v.constructVersionString(useSchema));
    }
    
    @Test
    public void initializeNewVersion1() {
    	String testSchema = "semver";
    	Version v = new Version(testSchema);
    	System.out.println(v.constructVersionString());
        assertEquals ("0.1.0", v.constructVersionString());
    }
    
    @Test
    public void initializeNewVersion2() {
    	String testSchema = "yyyy.month-modifier";
    	Version v = new Version(testSchema);
    	System.out.println(v.constructVersionString());
        assertTrue ( true );
    }
    
    @Test
    public void initializeNewVersion3() {
    	String testSchema = "yyyy.month.minor.patch";
    	Version v = new Version(testSchema);
    	System.out.println(v.constructVersionString());
        assertTrue ( true );
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
    public void getUbuntuCalver() {
    	String version = VersionApi.getUbuntuCalver();
    	System.out.println("Ubuntu CalVer = " + version);
        assertTrue ( true );
    }
    
    @Test
    public void supportMavenSnapshotAsIs() {
    	String version = "2019.05.Stable.1-SNAPSHOT";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	Version v = new Version(version, schema);
    	assertEquals("2019.05.Stable.1-SNAPSHOT", v.constructVersionString());
    }
    
    @Test
    public void supportMavenSnapshotForceOut() {
    	String version = "2019.05.Stable.1-SNAPSHOT";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	Version v = new Version(version, schema);
    	v.setSnapshot(false);
    	assertEquals("2019.05.Stable.1", v.constructVersionString());
    }
    
    @Test
    public void supportMavenSnapshotForceIn() {
    	String version = "2019.05.Stable.1";
    	String schema = VersionType.CALVER_RELIZA.getSchema();
    	Version v = new Version(version, schema);
    	v.setSnapshot(true);
    	assertEquals("2019.05.Stable.1-SNAPSHOT", v.constructVersionString());
    }
}

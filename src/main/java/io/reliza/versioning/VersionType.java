/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

/**
 * We will use this enum to initialize some predefined recommended versions
 *
 */
public enum VersionType {
	/**
	 * VersionType CALVER_UBUNTU
	 */
	CALVER_UBUNTU("YY.0M.Micro"),
		
	/**
	 * VersionType CALVER_RELIZA
	 */
	CALVER_RELIZA("YYYY.0M.Calvermodifier.Micro+Metadata"),
		
	/**
	 * VersionType CALVER_RELIZA_2020
	 */
	CALVER_RELIZA_2020("YYYY.0M.Calvermodifier.Minor.Micro+Metadata"),
		
	/**
	 * VersionType SEMVER_FULL_NOTATION
	 */
	SEMVER_FULL_NOTATION("Major.Minor.Patch-Modifier+Metadata"),
		
	/**
	 * VersionType SEMVER_SHORT_NOTATION
	 */
	SEMVER_SHORT_NOTATION("Major.Minor.Patch"),
		
	/**
	 * VersionType FEATURE_BRANCH
	 */
	FEATURE_BRANCH("Branch.Micro"),
		
	/**
	 * VersionType FEATURE_BRANCH_CALVER
	 */
	FEATURE_BRANCH_CALVER("YYYY.0M.Branch.Micro")
	;
	
	private String schema;
	
	private VersionType(String schema) {
		this.schema = schema;
	}
	
	/**
	 * getSchema 
	 * @return schema
	 */	
	public String getSchema() {
		return schema;
	}

}

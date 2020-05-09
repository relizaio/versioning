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
	CALVER_UBUNTU("YY.0M.Micro"),
	CALVER_RELIZA("YYYY.0M.Calvermodifier.Micro+Metadata"),
	CALVER_RELIZA_2020("YYYY.0M.Calvermodifier.Minor.Micro+Metadata"),
	SEMVER_FULL_NOTATION("Major.Minor.Patch-Modifier+Metadata"),
	SEMVER_SHORT_NOTATION("Major.Minor.Patch"),
	FEATURE_BRANCH("Branch.Micro"),
	FEATURE_BRANCH_CALVER("YYYY.0M.Branch.Micro")
	;
	
	private String schema;
	
	private VersionType(String schema) {
		this.schema = schema;
	}
	
	public String getSchema() {
		return schema;
	}

}

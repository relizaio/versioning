/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * We will use this enum to initialize some predefined recommended versions
 *
 */
public enum VersionType {

	CALVER_UBUNTU("YY.0M.Micro", ""),
	CALVER_RELIZA("YYYY.0M.Calvermodifier.Micro+Metadata?", ""),
	CALVER_RELIZA_2020("YYYY.0M.Calvermodifier.Minor.Micro+Metadata?", ""),
	SEMVER("Major.Minor.Patch-Modifier?+Metadata?", "semver"),
	FEATURE_BRANCH("Branch.Micro", ""),
	FEATURE_BRANCH_CALVER("YYYY.0M.Branch.Micro", "")
	;
	
	private String schema;
	private String aliasName;
	
	private VersionType(String schema, String aliasName) {
		this.schema = schema;
		this.aliasName = aliasName;
	}
	
	/**
	 * getSchema 
	 * @return schema
	 */	
	public String getSchema() {
		return schema;
	}
	
	public String getAliasName() {
		return aliasName;
	}
	
	public static Optional<VersionType> resolveByAliasName(String aliasName) {
		Optional<VersionType> ovt = Optional.empty();
		var vTypes = VersionType.values();
		if (StringUtils.isNotEmpty(aliasName)) {
			for (int i=0; ovt.isEmpty() && i < vTypes.length; i++) {
				if (aliasName.equalsIgnoreCase(vTypes[i].aliasName)) ovt = Optional.of(vTypes[i]);
			}
		}
		return ovt;
	}

}

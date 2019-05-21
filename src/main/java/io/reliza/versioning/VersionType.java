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
	CALVER_RELIZA("YYYY.0M.Modifier.Micro+Metadata")
	;
	
	private String schema;
	
	private VersionType(String schema) {
		this.schema = schema;
	}
	
	public String getSchema() {
		return schema;
	}

}

/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import org.apache.commons.lang3.StringUtils;

public class VersionApi {
	public static String getBaseVerWithModMeta(String schema, String modifier, String metadata) {
		Version v = VersionUtils.initializeVersionWithModMeta(schema, modifier, metadata);
		return v.constructVersionString();
	}
	
	public static String getCalverType(VersionType vt, String modifier, String metadata) {
		return getBaseVerWithModMeta(vt.getSchema(), modifier, metadata);
	}
	
	public static String getUbuntuCalver() {
		return getCalverType(VersionType.CALVER_UBUNTU, null, null);
	}
	
	public static String getRelizaCalver(String modifier, String metadata) {
		if (StringUtils.isEmpty(modifier)) {
			modifier = Constants.BASE_MODIFIER;
		}
		return getCalverType(VersionType.CALVER_RELIZA, modifier, metadata);
	}
}

/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import org.apache.commons.lang3.StringUtils;

/**
 * This class contains static methods to use for higher level versioning API
 *
 */
public class VersionApi {
	/**
	 * This method returns base version based on supplied schema, modifier and metadata
	 * @param schema String
	 * @param modifier String
	 * @param metadata String
	 * @return version String
	 */
	public static String getBaseVerWithModMeta(String schema, String modifier, String metadata) {
		Version v = VersionUtils.initializeVersionWithModMeta(schema, modifier, metadata);
		return v.constructVersionString();
	}
	
	/**
	 * This method returns a CalVer version based on one of preset types
	 * @param vt preset VersionType enum
	 * @param modifier String
	 * @param metadata String
	 * @return version String
	 */
	public static String getCalverType(VersionType vt, String modifier, String metadata) {
		return getBaseVerWithModMeta(vt.getSchema(), modifier, metadata);
	}
	
	/**
	 * This method returns a Ubuntu style CalVer version
	 * @return Ubuntu style version
	 */
	public static String getUbuntuCalver() {
		return getCalverType(VersionType.CALVER_UBUNTU, null, null);
	}
	
	/**
	 * This method returns a Reliza style CalVer vesion
	 * @param modifier String
	 * @param metadata String
	 * @return Reliza style version
	 */
	public static String getRelizaCalver(String modifier, String metadata) {
		if (StringUtils.isEmpty(modifier)) {
			modifier = Constants.BASE_MODIFIER;
		}
		return getCalverType(VersionType.CALVER_RELIZA, modifier, metadata);
	}
}

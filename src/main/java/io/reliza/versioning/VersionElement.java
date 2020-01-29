/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * This enum defines known set of version schema elements
 *
 */
public enum VersionElement {
	MAJOR(new HashSet<String>(Arrays.asList(new String[] {"major"})), "^\\d+$"),
	MINOR(new HashSet<String>(Arrays.asList(new String[] {"minor"})), "^\\d+$"),
	PATCH(new HashSet<String>(Arrays.asList(new String[] {"micro", "patch"})), "^\\d+$"),
	SEMVER_MODIFIER(new HashSet<String>(Arrays.asList(new String[] {"modifier", "identifier", "mod", "ident", "id"})), "^[a-zA-Z0-9]+$"),
	CALVER_MODIFIER(new HashSet<String>(Arrays.asList(new String[] {"calvermodifier", "calvermod", "calverid"})), "^[a-zA-Z0-9]+$"),
	METADATA(new HashSet<String>(Arrays.asList(new String[] {"meta", "metadata"})), "^[a-zA-Z0-9]+$"),
	YYYY(new HashSet<String>(Arrays.asList(new String[] {"year", "yyyy"})), "^[12][0-9]{3}$"),
	YY(new HashSet<String>(Arrays.asList(new String[] {"yy"})), "^([1-9][0-9]|[1-9])?[0-9]$"),
	OY(new HashSet<String>(Arrays.asList(new String[] {"oy", "0y"})), "^([0-9])?[0-9]{2}$"),
	MM(new HashSet<String>(Arrays.asList(new String[] {"mm", "month"})), "^(1[0-2]|[1-9])$"),
	OM(new HashSet<String>(Arrays.asList(new String[] {"om", "0m"})), "^(1[0-2]|0[1-9])$"),
	DD(new HashSet<String>(Arrays.asList(new String[] {"dd", "day"})), "^(3[01]|[12][0-9]|[1-9])$"),
	OD(new HashSet<String>(Arrays.asList(new String[] {"od", "0d"})), "^(3[01]|[0-2][0-9])$"),
	BUILDID(new HashSet<String>(Arrays.asList(new String[] {"build", "buildid", "cibuildid", "cibuild"})), "^[a-zA-Z0-9]+$"),
	BUILDENV(new HashSet<String>(Arrays.asList(new String[] {"cienv", "buildenv", "cibuildenv"})), "^[a-zA-Z0-9]+$")
	;
	
	private Set<String> namingInSchema;
	private Pattern regex;
	
	private static final Map<String, VersionElement> veLookupMap;
	
	static {
		HashMap<String, VersionElement> veLookupMapBuild = new HashMap<>();
		for (VersionElement ve : VersionElement.values()) {
			ve.getNamingInSchema().forEach(name -> veLookupMapBuild.put(name, ve));
		}
		veLookupMap = Collections.unmodifiableMap(veLookupMapBuild);
	}
	
	/**
	 * Private VersionElement enum constructor
	 * @param namingInSchema
	 * @param pattern
	 */
	private VersionElement (Set<String> namingInSchema, String pattern) {
		this.namingInSchema = namingInSchema;
		this.regex = Pattern.compile(pattern);
	}
	
	/**
	 * Gets string set of names how the element may be used in schema
	 * @return namingInSchema set
	 */
	private Set<String> getNamingInSchema () {
		return this.namingInSchema;
	}
	
	/**
	 * This method retrieves VersionElement by supplied string element parameter
	 * @param elStr String
	 * @return VersionElement
	 */
	public static VersionElement getVersionElement (String elStr) {
		VersionElement retVe = null;
		if (StringUtils.isNotEmpty(elStr)) {
			retVe = veLookupMap.get(elStr.toLowerCase());
		}
		return retVe;
	}
	
	/**
	 * This method returns regex pattern of this element
	 * @return regex of this element
	 */
	public Pattern getRegexPattern () {
		return this.regex;
	}
}

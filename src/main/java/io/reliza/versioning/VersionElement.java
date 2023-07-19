/**
* Copyright 2019 - 2020 Reliza Incorporated. Licensed under MIT License.
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

	/**
	 * Version Element type Major
	 */
	MAJOR(new HashSet<String>(Arrays.asList(new String[] {"major"})), "^\\d+$"),


	/**
	 * Version Element type MINOR
	 */
	MINOR(new HashSet<String>(Arrays.asList(new String[] {"minor"})), "^\\d+$"),


	/**
	 * Version Element type PATCH
	 */
	PATCH(new HashSet<String>(Arrays.asList(new String[] {"micro", "patch"})), "^\\d+$"),


	/**
	 * Version Element type NANO
	 */
	NANO(new HashSet<String>(Arrays.asList(new String[] {"nano"})), "^\\d+$"),


	/**
	 * Version Element type SEMVER_MODIFIER
	 */
	SEMVER_MODIFIER(new HashSet<String>(Arrays.asList(new String[] {"modifier", "identifier", "mod", "ident", "id"})), "^[a-zA-Z0-9]+$"),


	/**
	 * Version Element type CALVER_MODIFIER
	 */
	CALVER_MODIFIER(new HashSet<String>(Arrays.asList(new String[] {"calvermodifier", "calvermod", "calverid", "stable"})), "^[a-zA-Z0-9]+$"),


	/**
	 * Version Element type METADATA
	 */
	METADATA(new HashSet<String>(Arrays.asList(new String[] {"meta", "metadata"})), "^[a-zA-Z0-9]+$"),


	/**
	 * Version Element type YYYY
	 */
	YYYY(new HashSet<String>(Arrays.asList(new String[] {"year", "yyyy"})), "^[12][0-9]{3}$"),


	/**
	 * Version Element type YYYYOM
	 */
	YYYYOM(new HashSet<String>(Arrays.asList(new String[] {"yyyy0m", "yyyyom"})), "^[12][0-9]{3}(?:1[0-2]|0[1-9])$"),


	/**
	 * Version Element type YYOM
	 */
	YYOM(new HashSet<String>(Arrays.asList(new String[] {"yy0m", "yyom"})), "^([1-9][0-9]|[1-9])?[0-9](1[0-2]|0[1-9])$"),


	/**
	 * Version Element type YY
	 */
	YY(new HashSet<String>(Arrays.asList(new String[] {"yy"})), "^([1-9][0-9]|[1-9])?[0-9]$"),


	/**
	 * Version Element type OY
	 */
	OY(new HashSet<String>(Arrays.asList(new String[] {"oy", "0y"})), "^([0-9])?[0-9]{2}$"),


	/**
	 * Version Element type MM
	 */
	MM(new HashSet<String>(Arrays.asList(new String[] {"mm", "month"})), "^(1[0-2]|[1-9])$"),


	/**
	 * Version Element type OM
	 */
	OM(new HashSet<String>(Arrays.asList(new String[] {"om", "0m"})), "^(1[0-2]|0[1-9])$"),


	/**
	 * Version Element type DD
	 */
	DD(new HashSet<String>(Arrays.asList(new String[] {"dd", "day"})), "^(3[01]|[12][0-9]|[1-9])$"),


	/**
	 * Version Element type OD
	 */
	OD(new HashSet<String>(Arrays.asList(new String[] {"od", "0d"})), "^(3[01]|[0-2][0-9])$"),


	/**
	 * Version Element type BUILDID
	 */
	BUILDID(new HashSet<String>(Arrays.asList(new String[] {"build", "buildid", "cibuildid", "cibuild"})), "^[a-zA-Z0-9]+$"),


	/**
	 * Version Element type BUILDENV
	 */
	BUILDENV(new HashSet<String>(Arrays.asList(new String[] {"cienv", "buildenv", "cibuildenv"})), "^[a-zA-Z0-9]+$"),


	/**
	 * Version Element type BRANCH
	 */
	BRANCH(new HashSet<String>(Arrays.asList(new String[] {"Branch", "branch", "branchName", "branchname"})), "^[-./_a-zA-Z0-9\\:]+$")
	;
	
	private Set<String> namingInSchema;
	private Pattern regex;
	private String separator;
	
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
	public Set<String> getNamingInSchema () {
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

	/**
	 * get seprator of this element
	 * @return seprator string
	 */
	public String getSeparator(){
		return this.separator;
	}

	/**
	 * set separator of this element
	 * @param separator String
	 */
	public void setSeparator(String separator){
		this.separator = separator;
	}
}

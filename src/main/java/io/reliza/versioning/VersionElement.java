/**
* Copyright 2019 - 2025 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * This enum defines known set of version schema elements
 *
 */
public enum VersionElement {
	
	MAJOR(Set.of("major"), "^\\d+$", false),
	MINOR(Set.of("minor"), "^\\d+$", false),
	PATCH(Set.of("micro", "patch", "bugfix", "build"), "^\\d+$", false),
	NANO(Set.of("nano", "revision", "hotfix"), "^\\d+$", false),
	SEMVER_MODIFIER(Set.of("modifier", "identifier", "mod", "ident", "id"), "^[a-zA-Z0-9]+$", true),
	CALVER_MODIFIER(Set.of("calvermodifier", "calvermod", "calverid", "stable"), "^[a-zA-Z0-9]+$", true),
	METADATA(Set.of("meta", "metadata"), "^[a-zA-Z0-9]+$", false),
	YYYY(Set.of("year", "yyyy"), "^[12][0-9]{3}$", false),
	YYYYOM(Set.of("yyyy0m", "yyyyom"), "^[12][0-9]{3}(?:1[0-2]|0[1-9])$", false),
    YYOM(Set.of("yy0m", "yyom"), "^([1-9][0-9]|[1-9])?[0-9](1[0-2]|0[1-9])$", false),
    YY(Set.of("yy"), "^([1-9][0-9]|[1-9])?[0-9]$", false),
    OY(Set.of("oy", "0y"), "^([0-9])?[0-9]{2}$", false),
    MM(Set.of("mm", "month"), "^(1[0-2]|[1-9])$", false),
    OM(Set.of("om", "0m"), "^(1[0-2]|0[1-9])$", false),
    DD(Set.of("dd", "day"), "^(3[01]|[12][0-9]|[1-9])$", false),
    OD(Set.of("od", "0d"), "^(3[01]|[0-2][0-9])$", false),
    BUILDID(Set.of("buildid", "cibuildid", "cibuild"), "^[a-zA-Z0-9]+$", false),
    BUILDENV(Set.of("cienv", "buildenv", "cibuildenv"), "^[a-zA-Z0-9]+$", false),
    BRANCH(Set.of("Branch", "branch", "branchName", "branchname"), "^[-./_a-zA-Z0-9\\:]+$", true)
//	DOT_SEPARATOR(Set.of("."), "^\\.$"),
//	UNDERSCORE_SEPARATOR(Set.of("_"), "^_$"),
//	DASH_SEPARATOR(Set.of("-"), "^\\-$"),
//	COLON_SEPARATOR(Set.of(":"), "^:$"),
//	PLUS_SEPARATOR(Set.of("+"), "^\\+$")
	;
	
	public static record ParsedVersionElement(VersionElement ve, String frontSeparator, Boolean isSeparatorOptional, Boolean isElementOptional) {}
	
	private Set<String> namingInSchema;
	private Pattern regex;
	private boolean mayContainSeparators;
	
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
	private VersionElement (Set<String> namingInSchema, String pattern, boolean mayContainSeparators) {
		this.namingInSchema = namingInSchema;
		this.regex = Pattern.compile(pattern);
		this.mayContainSeparators = mayContainSeparators;
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
	
	public boolean isMayContainSeparators() {
		return this.mayContainSeparators;
	}
	
}

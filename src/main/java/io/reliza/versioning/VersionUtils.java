/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.reliza.versioning.Version.VersionComponent;
import io.reliza.versioning.Version.VersionHelper;
import io.reliza.versioning.VersionElement.ParsedVersionElement;

/**
 * This class defines a set of various utils used for Versioning
 *
 */
public class VersionUtils {
	
	private final static String SPLIT_REGEX = "(?=\\+|:|-|_|\\.)";
	
	/**
	 * Private constructor for uninitializable class
	 */
	private VersionUtils () {}
	
	/**
	 * This method parses supplied schema to version elements
	 * @param schema String
	 * @return list of VersionComponent
	 */
	public static List<ParsedVersionElement> parseSchema (String schema) {
		Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
		if (ovt.isPresent()) schema = ovt.get().getSchema();
		List<ParsedVersionElement> retList = new LinkedList<>();
		String[] strElements = schema.split(SPLIT_REGEX);
		int i = 0;
		boolean nextSeparatorOptional = false;
		for (String el : strElements) {
			String separator = "";
			if (i > 0) {
				separator = el.substring(0, 1);
				el = el.substring(1);
			}
			boolean isSeparatorOptional = nextSeparatorOptional;
			nextSeparatorOptional = false;
			boolean isElementOptional = false;
			if (el.endsWith("?")) {
				isElementOptional = true;
				el = el.substring(0, el.length() - 1);
				if (i == 0) nextSeparatorOptional = true;
			}
			VersionElement ve = VersionElement.getVersionElement(el);
			
			if (null == ve) {
				throw new RuntimeException("Cannot find version element for the schema part = " + el);
			}

			ParsedVersionElement pve = new ParsedVersionElement(ve, separator, isSeparatorOptional, isElementOptional);
			retList.add(pve);
			i++;
		}
		return retList;
	}
	
	/**
	 * This method extracts separators from schema
	 * @param schema String
	 * @return list of extracted schema separator Strings
	 */
	public static List<String> extractSchemaSeparators (String schema) {
		List<String> retList = new ArrayList<>();
		for (char c : schema.toCharArray()) {
			if (c == '.' || c == '+' || c == '-' || c == '_') {
				retList.add(Character.toString(c));
			}
		}
		return retList;
	}

	private static class PlusDashElHelper {
		private String version;
		private String[] elHelper = new String[2];
		private boolean isFulfilled = false;
	}

	/**
	 * This method handles pluses in version string when parsing version
	 * @param version
	 * @param handleBranchInVersion
	 * @return
	 */
	private static PlusDashElHelper handlePlusesInVersion (String version, boolean handleBranchInVersion) {
		PlusDashElHelper pdeh = new PlusDashElHelper();
		String[] pluselHelper = null;
		if (version.contains("+") && !handleBranchInVersion) {
		    pluselHelper = version.split("\\+");
			Integer firstPlusIndexToSplit = version.indexOf("+") + 1;
			pdeh.elHelper[1] = version.substring(firstPlusIndexToSplit, version.length());
			pdeh.version = pluselHelper[0];
			pdeh.elHelper[0] = pluselHelper[0];
			pdeh.isFulfilled = true;
		}
		return pdeh;
	}

	/**
	 * This method handles dashes in version string when parsing version
	 * @param version
	 * @param handleBranchInVersion
	 * @param dashInSchemaAfterBranch
	 * @param schema
	 * @return
	 */
	private static PlusDashElHelper handleDashesInVersion (String version, boolean handleBranchInVersion,
			boolean dashInSchemaAfterBranch, String schema) {
		PlusDashElHelper pdeh = new PlusDashElHelper();
		String[] dashelHelper = null;
		if (version.contains("-") && (!handleBranchInVersion || dashInSchemaAfterBranch)) {
			dashelHelper = version.split("-");
			if (dashelHelper.length > 2) {
				if (dashInSchemaAfterBranch) {
					List<ParsedVersionElement> schemaElList = parseSchema(schema);
					if (schemaElList.stream().filter(x -> x.ve() == VersionElement.CALVER_MODIFIER).findAny().isPresent() 
							|| schemaElList.stream().filter(x -> x.ve() == VersionElement.SEMVER_MODIFIER).findAny().isPresent() ) {
						// just take the latest dash and split on that
						pdeh.elHelper[1] = dashelHelper[dashelHelper.length - 1];
						pdeh.elHelper[0] = version.replaceFirst("-" + pdeh.elHelper[1], "");
						pdeh.version = pdeh.elHelper[0];
						pdeh.isFulfilled = true;
					}
				} else {
					// split on the first dash
					pdeh.elHelper[0] = dashelHelper[0];
					Integer firstDashIndexToSplit = version.indexOf("-") + 1;
					pdeh.elHelper[1] = version.substring(firstDashIndexToSplit, version.length());
					pdeh.version = dashelHelper[0];
					pdeh.isFulfilled = true;
				}

			} else if (!dashInSchemaAfterBranch){
				// only one dash
				pdeh.elHelper = dashelHelper;
				pdeh.version = dashelHelper[0];
				pdeh.isFulfilled = true;
			}
		}
		return pdeh;
	}

	/**
	 * This method creates regex to split version string into parts based on schema.
	 * @param schema
	 * @param splitRegex
	 * @return
	 */
	private static String constructSchemaRegexFromSchema (String schema, String splitRegex) {
		String schemaRegex = "";
		List<ParsedVersionElement> pveList = parseSchema(schema);
		// Make sure splitRegex is not a capturing group
		if (splitRegex.startsWith("(")) {
			splitRegex = splitRegex.replace("(", "(?:");
		}
		String separator = splitRegex;
		for (ParsedVersionElement pve : pveList) {
			// Remove first and last characters from regex patter string (^ and $)
			String veRegex = pve.ve().getRegexPattern().pattern().substring(1, pve.ve().getRegexPattern().pattern().length()-1);
			Set<String> pinElement = pve.ve().getNamingInSchema();
			String pinRegex = pinElement.stream().reduce("", (partialString, element) -> partialString + "|" + element);
			veRegex = veRegex + pinRegex;
			// If version element regex has capturing groups -> make non capture groups so they do not interfere
			if (veRegex.startsWith("(")) {
				veRegex = veRegex.replace("(", "(?:");
			}
			// Construct total schema regex from individual version element regex's
			if (schemaRegex.equals("")) { // first
				schemaRegex += "(" + veRegex + ")";
			} else {
				separator = StringUtils.isEmpty(pve.frontSeparator()) ? splitRegex : "(?:\\" + pve.frontSeparator() + ")";
				schemaRegex += separator + "(?=" + veRegex + ")(" + veRegex + ")";
			}
		}
		return schemaRegex;
	}

	/**
	 * This method parses version string into VersionHelper based on provided schema
	 * The need for schema arises where version elements need to include special characters themselves, such as dashes, periods or underscores
	 * @param version String
	 * @param schema String
	 * @return VersionHelper
	 */
	public static Optional<VersionHelper> parseVersion (String version, String schema) {
		boolean handleBranchInVersion = (StringUtils.isNotEmpty(schema) && schema.toLowerCase().contains(VersionElement.BRANCH.name().toLowerCase())) || (StringUtils.isNotEmpty(version) && version.toLowerCase().contains(VersionElement.BRANCH.name().toLowerCase()));
		boolean dashInSchemaAfterBranch = handleBranchInVersion && StringUtils.isNotEmpty(schema) && schema.contains("-") 
			&& schema.indexOf("-") > schema.toLowerCase().indexOf(VersionElement.BRANCH.name().toLowerCase());
		// check special case for Maven-style Snapshot
		boolean isSnapshot = false;
		if (version.endsWith(Constants.MAVEN_STYLE_SNAPSHOT)) {
			isSnapshot = true;
			version = version.replaceFirst(Constants.MAVEN_STYLE_SNAPSHOT + "$", "");
		}
		// handle + and - differently as semver supports other separators after plus and dash
		String[] plusel = null;
		PlusDashElHelper plusElHelper = handlePlusesInVersion(version, handleBranchInVersion);
		if (plusElHelper.isFulfilled) {
			plusel = plusElHelper.elHelper;
			version = plusElHelper.version;
		}

		String[] dashel = null;
		PlusDashElHelper dashElHelper = handleDashesInVersion(version, handleBranchInVersion,
			dashInSchemaAfterBranch, schema);
		if (dashElHelper.isFulfilled) {
			dashel = dashElHelper.elHelper;
			version = dashElHelper.version;
		}
		
		List<ParsedVersionElement> schemaEls = parseSchema(schema);
		List<VersionComponent> versionComponents = new LinkedList<>();
		VersionComponent vc;
		int versionCharIndex = 0;
		int schemaElIndex = 0;
		
		Optional<VersionHelper> ovh = Optional.empty();
		boolean retEmpty = false;
		for (ParsedVersionElement se : schemaEls) {
			if (!se.isElementOptional() && versionCharIndex >= version.length()) {
				// version does not match schema, return empty
				retEmpty = true;
			} else if (!(se.isElementOptional() && versionCharIndex >= version.length())) {
				String versionSubstring = version.substring(versionCharIndex);
				if (schemaElIndex < schemaEls.size() - 1) {
					String separator = schemaEls.get(schemaElIndex+1).frontSeparator();
					if (".".equals(separator)) separator = "\\.";
					if ("+".equals(separator)) separator = "\\+";
					var verSplit = versionSubstring.split(separator);
					vc = new VersionComponent(se, verSplit[0]);
					versionCharIndex += verSplit[0].length() + schemaEls.get(schemaElIndex+1).frontSeparator().length();
				} else {
					vc = new VersionComponent(se, versionSubstring);
				}
				versionComponents.add(vc);
			}
			schemaElIndex++;
		}		

		if (!retEmpty) {
			String modifier = (null == dashel) ? null : dashel[1];
			String metadata = (null == plusel) ? null : plusel[1];
			ovh = Optional.of(new VersionHelper(versionComponents, modifier, metadata, isSnapshot));
		}
		return ovh;
	}
	
	/**
	 * This method returns true if supplied version string matches supplied schema string
	 * @param schema String
	 * @param version String
	 * @return true if version is matching schema, false otherwise
	 */
	public static boolean isVersionMatchingSchema (String schema, String version) {
		boolean matching = true;
		
		Optional<VersionHelper> ovh = parseVersion(version, schema);
		
		if (ovh.isEmpty()) matching = false;

		if (matching) {
			Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
			if (ovt.isPresent()) schema = ovt.get().getSchema();
			
			List<ParsedVersionElement> pveList = parseSchema(schema);
			List<VersionComponent> versionComponents = ovh.get().getVersionComponents();
	
			if (versionComponents.size() > pveList.size()) {
				matching = false;
			}
			
			var pveListIter = pveList.iterator();
			int i = 0;
			while (matching && pveListIter.hasNext() && i < versionComponents.size()) {
				ParsedVersionElement pve = pveListIter.next();
				VersionComponent vc = versionComponents.get(i);
				Pattern p = pve.ve().getRegexPattern();
				matching = p.matcher(vc.representation()).matches();
				// TODO recurse if not matching and element optional
				// if (!matching && pve.isElementOptional())
				++i;
			}
		}
		
		return matching;
	}
	
	/**
	 * This method returns true if supplied version pin matches supplied schema string
	 * Pin here means pinned version for a branch, i.e. YYYY.0M.Patch schema should match 2020.01.Patch pin
	 * @param schema String
	 * @param pin String
	 * @return true if pin is matching schema, false otherwise
	 */
	public static boolean isPinMatchingSchema (String schema, String pin) {
		boolean matching = true;

		Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
		if (ovt.isPresent()) schema = ovt.get().getSchema();
		
		Optional<VersionType> ovtpin = VersionType.resolveByAliasName(pin);
		if (ovtpin.isPresent()) pin = ovtpin.get().getSchema();
		
		Optional<VersionHelper> ovh = parseVersion(pin, schema);
		if (ovh.isEmpty()) matching = false;
		if (matching) {
			List<VersionComponent> versionComponents = ovh.get().getVersionComponents();
	
			List<ParsedVersionElement> pveList = parseSchema(schema);
	
			if (versionComponents.size() > pveList.size()) {
				matching = false;
			}
			
			var pveListIter = pveList.iterator();
			int i = 0;
			while (matching && pveListIter.hasNext() && i < versionComponents.size()) {
				ParsedVersionElement pve = pveListIter.next();
				VersionComponent vc = versionComponents.get(i);
				Pattern p = pve.ve().getRegexPattern();
				matching = p.matcher(vc.representation()).matches() 
						|| VersionElement.getVersionElement(vc.representation()) == pve.ve();
				// TODO recurse if not matching and element optional
				// if (!matching && pve.isElementOptional())
				++i;
			}
		}
		return matching;
	}
	
	/**
	 * This method returns true if supplied version matches supplied pin as well as supplied schema string
	 * Pin here means pinned version for a branch, i.e. YYYY.0M.Patch schema should match 2020.01.Patch pin
	 * @param schema String
	 * @param pin String
	 * @param version String
	 * @return true if version is matching schema and pin, false otherwise
	 */
	public static boolean isVersionMatchingSchemaAndPin (String schema, String pin, String version) {
		boolean matching = isPinMatchingSchema(schema, pin);
		if (matching) matching = isVersionMatchingSchema(schema, version); 
		if (matching) {
			Optional<VersionType> ovtpin = VersionType.resolveByAliasName(pin);
			if (ovtpin.isPresent()) pin = ovtpin.get().getSchema();
			
			Optional<VersionHelper> ovhPin = parseVersion(pin, schema);
			Optional<VersionHelper> ovhVersion = parseVersion(version, schema);
			
			if (ovhPin.isEmpty() || ovhVersion.isEmpty()) matching = false;
			
			if (matching) {
				for (int i=0; matching && i < ovhVersion.get().getVersionComponents().size(); i++) {
					if (null == VersionElement.getVersionElement(ovhPin.get().getVersionComponents().get(i).representation())) {
						matching = ovhPin.get().getVersionComponents().get(i).representation()
								.equals(ovhVersion.get().getVersionComponents().get(i).representation());
					}
				}
			}
		}
		return matching;
	}
	
	
	/**
	 * This method removes case-insensitive metadata or modifier flags from version
	 * @param schema String
	 * @return schema string without modifier or metadata elements
	 */
	public static String stripSchemaFromModMeta (String schema) {
		schema = schema.replaceAll("(?i)(\\+|-)metadata", "");
		schema = schema.replaceAll("(?i)(\\+|-)modifier", "");
		return schema;
	}
	
	/**
	 * This method returns base version based on supplied schema
	 * @param schema String
	 * @return version String
	 */
	public static Version initializeEmptyVersion(String schema) {
		return Version.getVersion(schema);
	}
	
	/**
	 * This method returns base version based on supplied schema, modifier and metadata
	 * @param schema String
	 * @param modifier String
	 * @param metadata String
	 * @return version String
	 */
	public static Version initializeVersionWithModMeta(String schema, String modifier, String metadata) {
		Version v = Version.getVersion(schema);
		v.setModifier(modifier);
		v.setMetadata(metadata);
		return v;
	}
	
	/**
	 * This method is used to test if the specified version string is a valid SemVer version.
	 * Reference: https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
	 * @param version String
	 * @return true if version is Semver valid format, false otherwise.
	 */
	public static boolean isVersionSemver(String version) {
		String regex = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
		Pattern p = Pattern.compile(regex);
		Matcher semverMatcher = p.matcher(version);
		return semverMatcher.matches();
	}
	
	/**
	 * This method is used to test if the specified schema string is Semver style schema or not.
	 * Valid Semver: Major.Minor.Patch with optional +modifer and -metadata
	 * @param schema String
	 * @return true if schema is semver format, false otherwise
	 */
	public static boolean isSchemaSemver(String schema) {
		Objects.requireNonNull(schema);
		boolean isSemver = false;
		List<ParsedVersionElement> schemaVeList = parseSchema(schema);
		if (schemaVeList.size() > 2) {
			for (int i = 0; i < schemaVeList.size(); i++) {
				VersionElement ve = schemaVeList.get(i).ve();
				switch (i) {
				case 0:
					isSemver = ve.equals(VersionElement.MAJOR);
					break;
				case 1:
					isSemver = ve.equals(VersionElement.MINOR);
					break;
				case 2:
					isSemver = ve.equals(VersionElement.PATCH);
					break;
				case 3:
					isSemver = ve.equals(VersionElement.SEMVER_MODIFIER);
					break;
				case 4:
					isSemver = ve.equals(VersionElement.METADATA);
					break;
				default:
					// any more than 5 elements and it is not semver
					isSemver = false;
				}
			}
		}
		return isSemver;
	}
	
	/**
	 * This method returns true if schema contains a year element (it is calver)
	 * @param schema String
	 * @return true if schema contains a year element
	 */
	public static boolean isSchemaCalver (String schema) {
		boolean isCalver = false;
		Set<VersionElement> veSet = parseSchema(schema).stream().map(x -> x.ve()).collect(Collectors.toSet());
		if (veSet.contains(VersionElement.OY) || 
				veSet.contains(VersionElement.YY) || 
				veSet.contains(VersionElement.YYYY) ||
				veSet.contains(VersionElement.YYYYOM) ||
				veSet.contains(VersionElement.YYOM)) {
			isCalver = true;
		}
		return isCalver;
	}
	
	/**
	 * <p>This method returns a VersionElement representing the largest version component that has changes between two versions.
	 * Largest version component is defined as first version component, reading from left to right.</p>
	 * <ul>
	 * <li>Example: oldVersion = 1.0.0, newVersion = 1.0.1, returns VersionElement.Patch</li>
	 * <li>Example: oldVersion = 1.0.0, newVersion = 1.1.1, returns VersionElement.Minor</li>
	 * <li>Example: oldVersion = 2021.1.0, newVersion = 2022.2.0 returns VersionElement.YYYY</li>
	 * <li>Example: oldVersion = 1.2021.1.0, newVersion = 0.2022.1.0 returns VersionElement.Major</li>
	 * </ul>
	 * @param oldVersion String
	 * @param newVersion String
	 * @param schema String Both versions must match the specified schema otherwise null will be returned.
	 * @return VersionElement representing the largest difference between the two versions. Returns null if versions are equal or either does not match schema.
	 */
	public static VersionElement getLargestVersionElementDifference(String oldVersion, String newVersion, String schema) {
		Objects.requireNonNull(oldVersion, "Old version must not be null");
		Objects.requireNonNull(newVersion, "New version must not be null");
		Objects.requireNonNull(schema, "Schema must not be null");
		VersionElement returnVe = null;
		Optional<VersionHelper> oldVh = parseVersion(oldVersion, schema);
		Optional<VersionHelper> newVh = parseVersion(newVersion, schema);
		if (oldVh.isPresent() && newVh.isPresent()) {
			List<ParsedVersionElement> schemaPveList = parseSchema(schema);
			int minVersionLength = Math.min(oldVh.get().getVersionComponents().size(), newVh.get().getVersionComponents().size());
			// use old for loop so we can reference both version component lists
			for (int i = 0; i < minVersionLength && returnVe == null; i++) {
				if (!oldVh.get().getVersionComponents().get(i).representation()
						.equals(newVh.get().getVersionComponents().get(i).representation())) {
					// if version components do not have same value, return the corresponding version element from schema list
					returnVe = schemaPveList.get(i).ve();
				}
			}
		}
		return returnVe;
	}
	
	/**
	 * <p>Similar to getLargestVersionElementDifference() method. Only difference is this method only
	 * considers differences between Semver version components. i.e. Major, Minor or Patch/Micro.</p>
	 * <p>This method works with versions that contain version elements other than Semver elements,
	 * however if the versions contain only non Semver elements, or are only differing in non Semver
	 * elements, then the method returns null.</p>
	 * @param oldVersion String
	 * @param newVersion String
	 * @param schema String Does not have to be strictly Semver. Must match both versions
	 * @return VersionElement representing the largest differing semver version component between the two versions, or null if either version does match schema.
	 */
	public static VersionElement getLargestSemverVersionElementDifference(String oldVersion, String newVersion, String schema) {
		Objects.requireNonNull(oldVersion, "Old version must not be null");
		Objects.requireNonNull(newVersion, "New version must not be null");
		Objects.requireNonNull(schema, "Schema must not be null");
		VersionElement returnVe = null;
		Optional<VersionHelper> oldVh = parseVersion(oldVersion, schema);
		Optional<VersionHelper> newVh = parseVersion(newVersion, schema);
		if (oldVh.isPresent() && newVh.isPresent()) {
			List<ParsedVersionElement> schemaPveList = parseSchema(schema);
			int minVersionLength = Math.min(oldVh.get().getVersionComponents().size(), newVh.get().getVersionComponents().size());
			// use old for loop so we can reference both version component lists
			for (int i = 0; i < minVersionLength && returnVe == null; i++) {
				if (!oldVh.get().getVersionComponents().get(i).representation()
						.equals(newVh.get().getVersionComponents().get(i).representation())) {
					// make sure corresponding element is a Semver element before returning
					if (schemaPveList.get(i).ve() == VersionElement.MAJOR
							|| schemaPveList.get(i).ve() == VersionElement.MINOR
							|| schemaPveList.get(i).ve() == VersionElement.PATCH) {
						returnVe = schemaPveList.get(i).ve();
					}
				}
			}
		}
		return returnVe;
	}
}

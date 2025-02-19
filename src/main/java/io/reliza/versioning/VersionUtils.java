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

import org.apache.commons.lang3.StringUtils;

import io.reliza.versioning.Version.VersionComponent;
import io.reliza.versioning.Version.VersionHelper;

/**
 * This class defines a set of various utils used for Versioning
 *
 */
public class VersionUtils {
	
	/**
	 * Private constructor for uninitializable class
	 */
	private VersionUtils () {}
	
	/**
	 * This method parses supplied schema to version elements
	 * @param schema String
	 * @return list of VersionElement
	 */
	public static List<VersionElement> parseSchema (String schema) {
		Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
		if (ovt.isPresent()) schema = ovt.get().getSchema();
		List<VersionElement> retList = new ArrayList<>();
		// split schema to elements
		String[] strElements = schema.split("(\\+|:|-|_|\\.)");
		int i = 0;
		int charIndex = 0;
		for (String el : strElements) {
			String separator = "";
			if (i < strElements.length - 1) {
				separator = schema.substring(charIndex + el.length(), charIndex + el.length() + 1);
				charIndex += el.length() + 1;
			}
			VersionElement ve = VersionElement.getVersionElement(el);
			
			if (null == ve) {
				throw new RuntimeException("Cannot find version element for the schema part = " + el);
			}

			ve.setSeparator(separator);
			retList.add(ve);
			i++;
		}
		return retList;
	}
	/**
	 * This method parses supplied schema to version elements
	 * @param pin String
	 * @return list of VersionElement
	 */
	public static List<VersionElement> parsePin (String pin) {
		Optional<VersionType> ovtpin = VersionType.resolveByAliasName(pin);
		if (ovtpin.isPresent()) pin = ovtpin.get().getSchema();

		List<VersionElement> retList = new ArrayList<>();
		// split schema to elements
		String[] strElements = pin.split("(?=\\+|-|_|\\.)");
		int i = 0;
		for (String el : strElements) {
			String separator = "";
			if(i>0){
				separator = el.substring(0, 1);
				el = el.substring(1);
			}
			VersionElement ve = VersionElement.getVersionElement(el);
			
			if (null == ve) {
				throw new RuntimeException("Cannot find version element for the schema part = " + el);
			}

			ve.setSeparator(separator);
			retList.add(ve);
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
	
	/**
	 * This method parses version string into VersionHelper
	 * @param version String
	 * @return VersionHelper
	 */
	public static VersionHelper parseVersion (String version) {
		return parseVersion(version, null);
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
					List<VersionElement> schemaElList = (ArrayList<VersionElement>) parseSchema(schema);
					if (schemaElList.contains(VersionElement.CALVER_MODIFIER) || schemaElList.contains(VersionElement.SEMVER_MODIFIER)) {
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
		ArrayList<VersionElement> veList = (ArrayList<VersionElement>) parseSchema(schema);
		// Make sure splitRegex is not a capturing group
		if (splitRegex.startsWith("(")) {
			splitRegex = splitRegex.replace("(", "(?:");
		}
		String separator = splitRegex;
		for (VersionElement ve : veList) {
			// Remove first and last characters from regex patter string (^ and $)
			String veRegex = ve.getRegexPattern().pattern().substring(1, ve.getRegexPattern().pattern().length()-1);
			Set<String> pinElement = ve.getNamingInSchema();
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
				separator = ve.getSeparator()=="" ? splitRegex : "(?:\\" + ve.getSeparator() + ")";
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
	public static VersionHelper parseVersion (String version, String schema) {
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
		
		List<VersionElement> schemaEls = parseSchema(schema);
		List<VersionComponent> versionComponents = new LinkedList<>();
		VersionComponent vc;
		int versionCharIndex = 0;
		for (VersionElement se : schemaEls) {
			String versionSubstring = version.substring(versionCharIndex);
			if (StringUtils.isNotEmpty(se.getSeparator())) {
				String separator = se.getSeparator();
				if (".".equals(separator)) separator = "\\.";
				if ("+".equals(separator)) separator = "\\+";
				var verSplit = versionSubstring.split(separator);
				vc = new VersionComponent(se, verSplit[0]);
				versionCharIndex += verSplit[0].length() + se.getSeparator().length();
			} else {
				vc = new VersionComponent(se, versionSubstring);
			}
			versionComponents.add(vc);
		}		

		String modifier = (null == dashel) ? null : dashel[1];
		String metadata = (null == plusel) ? null : plusel[1];
		return new VersionHelper(versionComponents, modifier, metadata, isSnapshot);
	}
	
	/**
	 * This method returns true if supplied version string matches supplied schema string
	 * @param schema String
	 * @param version String
	 * @return true if version is matching schema, false otherwise
	 */
	public static boolean isVersionMatchingSchema (String schema, String version) {
		boolean matching = true;
		
		VersionHelper vh = parseVersion(version, schema);

		Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
		if (ovt.isPresent()) schema = ovt.get().getSchema();
		
		// remove -modifier and +metadata from schema as it's irrelevant
		schema = stripSchemaFromModMeta(schema);
		List<VersionElement> veList = parseSchema(schema);
		List<VersionComponent> versionComponents = vh.getVersionComponents();
		String modifier = vh.getModifier();
		if(modifier != null && modifier != "" && versionComponents.contains(modifier) && versionComponents.size() > veList.size()){
			versionComponents.remove(modifier);
		}
		if (veList.size() != versionComponents.size()) {
			matching = false;
		}
		for (int i=0; matching && i<versionComponents.size(); i++) {
			Pattern p = veList
							.get(i)
							.getRegexPattern();
			matching = p
						.matcher(versionComponents.get(i).representation())
						.matches();
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
		
		VersionHelper vh = parseVersion(pin, schema);
		
		// remove -modifier and +metadata from schema as it's irrelevant
		schema = stripSchemaFromModMeta(schema);
		List<VersionElement> veList = parseSchema(schema);

		if (veList.size() != vh.getVersionComponents().size()) {
			matching = false;
		}
		for (int i=0; matching && i<vh.getVersionComponents().size(); i++) {
			Pattern p = veList
							.get(i)
							.getRegexPattern();
			matching = p
						.matcher(vh.getVersionComponents().get(i).representation())
						.matches() ||
					   vh.getVersionComponents().get(i).ve() == veList.get(i);
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
		if (matching) {
			
			Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
			if (ovt.isPresent()) schema = ovt.get().getSchema();
			
			Optional<VersionType> ovtpin = VersionType.resolveByAliasName(pin);
			if (ovtpin.isPresent()) pin = ovtpin.get().getSchema();
			
			VersionHelper vhPin = parseVersion(pin, schema);
			VersionHelper vhVersion = parseVersion(version, schema);
			
			// remove -modifier and +metadata from schema as it's irrelevant
			schema = stripSchemaFromModMeta(schema);
			List<VersionElement> veList = parseSchema(schema);
			
			if (veList.size() != vhPin.getVersionComponents().size() || veList.size() != vhVersion.getVersionComponents().size()) {
				matching = false;
			}
			for (int i=0; matching && i<vhVersion.getVersionComponents().size(); i++) {
				Pattern p = veList
								.get(i)
								.getRegexPattern();
				matching = p
							.matcher(vhVersion.getVersionComponents().get(i).representation())
							.matches();
				if (matching && p.matcher(vhPin.getVersionComponents().get(i).representation()).matches() &&
						// make sure that it's not a name of version element inside pin
						// i.e. could happen with string elements such as modifier / metadata
						null == vhPin.getVersionComponents().get(i).ve()) {
					// here we know that version is matching schema and need to verify if it's matching pin
					// means we're dealing with pin item that must match version element exactly
					matching = vhPin.getVersionComponents().get(i).representation().equals(vhVersion.getVersionComponents().get(i).representation());
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
		List<VersionElement> schemaVeList = parseSchema(schema);
		if (schemaVeList.size() > 2) {
			for (int i = 0; i < schemaVeList.size(); i++) {
				VersionElement ve = schemaVeList.get(i);
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
		Set<VersionElement> veSet = new HashSet<>(parseSchema(schema));
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
		VersionHelper oldVh = parseVersion(oldVersion);
		VersionHelper newVh = parseVersion(newVersion);
		if (isVersionMatchingSchema(schema, oldVersion)
				&& isVersionMatchingSchema(schema, newVersion)) {
			// parse schema
			List<VersionElement> schemaVeList = parseSchema(schema);
			int minVersionLength = Math.min(oldVh.getVersionComponents().size(), newVh.getVersionComponents().size());
			// use old for loop so we can reference both version component lists
			for (int i = 0; i < minVersionLength && returnVe == null; i++) {
				if (!oldVh.getVersionComponents().get(i).representation().equals(newVh.getVersionComponents().get(i).representation())) {
					// if version components do not have same value, return the corresponding version element from schema list
					returnVe = schemaVeList.get(i);
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
		VersionHelper oldVh = parseVersion(oldVersion);
		VersionHelper newVh = parseVersion(newVersion);
		if (isVersionMatchingSchema(schema, oldVersion)
				&& isVersionMatchingSchema(schema, newVersion)) {
			// parse schema
			List<VersionElement> schemaVeList = parseSchema(schema);
			int minVersionLength = Math.min(oldVh.getVersionComponents().size(), newVh.getVersionComponents().size());
			// use old for loop so we can reference both version component lists
			for (int i = 0; i < minVersionLength && returnVe == null; i++) {
				if (!oldVh.getVersionComponents().get(i).representation().equals(newVh.getVersionComponents().get(i).representation())) {
					// make sure corresponding element is a Semver element before returning
					if (schemaVeList.get(i) == VersionElement.MAJOR
							|| schemaVeList.get(i) == VersionElement.MINOR
							|| schemaVeList.get(i) == VersionElement.PATCH) {
						returnVe = schemaVeList.get(i);
					}
				}
			}
		}
		return returnVe;
	}
}

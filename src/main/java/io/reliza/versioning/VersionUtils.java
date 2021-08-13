/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

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
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = VersionType.SEMVER_FULL_NOTATION.getSchema();
		}
		List<VersionElement> retList = new ArrayList<>();
		// split schema to elements
		String[] strElements = schema.split("(\\+|-|_|\\.)");
		for (String el : strElements) {
			VersionElement ve = VersionElement.getVersionElement(el);
			if (null == ve) {
				throw new RuntimeException("Cannot find version element for the schema part = " + el);
			}
			retList.add(ve);
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
	
	/**
	 * This method parses version string into VersionHelper based on provided schema
	 * The need for schema arises where version elements need to include special characters themselves, such as dashes, periods or underscores
	 * @param version String
	 * @param schema String
	 * @return VersionHelper
	 */
	public static VersionHelper parseVersion (String version, String schema) {
		boolean handleBranchInVersion = StringUtils.isNotEmpty(schema) && schema.toLowerCase().contains(VersionElement.BRANCH.name().toLowerCase());
		// check special case for Maven-style Snapshot
		boolean isSnapshot = false;
		if (version.endsWith(Constants.MAVEN_STYLE_SNAPSHOT)) {
			isSnapshot = true;
			version = version.replaceFirst(Constants.MAVEN_STYLE_SNAPSHOT + "$", "");
		}
		// handle + and - differently as semver supports other separators after plus and dash
		String[] pluselHelper = null;
		String[] plusel = null;
		if (version.contains("+") && !handleBranchInVersion) {
		    pluselHelper = version.split("\\+");
		    // if more than one plus raise an error
		    if (pluselHelper.length > 2) {
			// if there are no dots, then only split on the last plus
			if (pluselHelper[pluselHelper.length - 1].contains(".")) {
			    // if there are dots after dashes then dashes are just part of the version - do nothing
			} else {
			    // just take the latest plus and split on that
			    plusel = new String[2];
			    plusel[1] = pluselHelper[pluselHelper.length - 1];
			    plusel[0] = version.replaceFirst("+" + plusel[1], "");
			}
		    } else {
			// only one plus
			plusel = pluselHelper;
			version = plusel[0];
		    }
		}

		String[] dashelHelper = null;
		String[] dashel = null;
		if (version.contains("-") && !handleBranchInVersion) {
		    dashelHelper = version.split("-");
		    // if more than one dash raise an error
		    if (dashelHelper.length > 2) {
			// if there are no dots, then only split on the last dash
			if (dashelHelper[dashelHelper.length - 1].contains(".")) {
			    // if there are dots after dashes then dashes are just part of the version - do nothing
			} else {
			    // just take the latest dash and split on that
			    dashel = new String[2];
			    dashel[1] = dashelHelper[dashelHelper.length - 1];
			    dashel[0] = version.replaceFirst("-" + dashel[1], "");
			}
		    } else {
			// only one dash
			dashel = dashelHelper;
			version = dashel[0];
		    }
		}
		
		String splitRegex = "\\.";
		if (StringUtils.isEmpty(schema) || !handleBranchInVersion) {
			splitRegex = "(\\.|_)";
		}
		
		List<String> versionComponents = Arrays.asList(version.split(splitRegex));
		
		// Alternative way to split version string into components. See
		// VersionUtilsTest::testParseVersion_BranchWithVersionInName() for example that would fail with just above code
		if (StringUtils.isNotEmpty(schema) && (schema.contains(".") || schema.contains("_"))) { // Only works if schema is of form VersionElement.VersoinElement...
			// Create regex to split version string into parts based on schema
			String schemaRegex = "";
			ArrayList<VersionElement> veList = (ArrayList<VersionElement>) parseSchema(schema);
			// Make sure splitRegex is not a capturing group
			if (splitRegex.startsWith("(")) {
				splitRegex = splitRegex.replace("(", "(?:");
			}
			
			for (VersionElement ve : veList) {
				// Remove first and last characters from regex patter string (^ and $)
				String veRegex = ve.getRegexPattern().pattern().substring(1, ve.getRegexPattern().pattern().length()-1);
				// If version element regex has capturing groups -> make non capture groups so they do not interfere
				if (veRegex.startsWith("(")) {
					veRegex = veRegex.replace("(", "(?:");
				}
				// Construct total schema regex from individual version element regex's
				if (schemaRegex.equals("")) { // first
					schemaRegex += "(" + veRegex + ")";
				} else {
					schemaRegex += splitRegex + "(?=" + veRegex + ")(" + veRegex + ")";
				}
			}
			// Deconstruct version string using regex
			Pattern pattern = Pattern.compile(schemaRegex);
			Matcher matcher = pattern.matcher(version);
			// Extract groups from regex result and add to new version components collection
			if (matcher.matches()) {
				versionComponents = new ArrayList<String>();
				// get elements of version from results to versionComponents.
				for (int i = 1; i <= matcher.groupCount(); i++) {
					versionComponents.add(matcher.group(i));
				}
			} else {
				// No match, do not replace versionComponents.
			}
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

		// handle semver as schema name
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = VersionType.SEMVER_SHORT_NOTATION.getSchema();
		}
		
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
						.matcher(vh.getVersionComponents().get(i))
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

		// handle semver as schema name
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = VersionType.SEMVER_SHORT_NOTATION.getSchema();
		}
		
		if (Constants.SEMVER.equalsIgnoreCase(pin)) {
			pin = VersionType.SEMVER_SHORT_NOTATION.getSchema();
		}
		
		VersionHelper vh = parseVersion(pin);
		
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
						.matcher(vh.getVersionComponents().get(i))
						.matches() ||
					   VersionElement.getVersionElement(vh.getVersionComponents().get(i)) == veList.get(i);
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
			
			// handle semver as schema name
			if (Constants.SEMVER.equalsIgnoreCase(schema)) {
				schema = VersionType.SEMVER_SHORT_NOTATION.getSchema();
			}
			
			if (Constants.SEMVER.equalsIgnoreCase(pin)) {
				pin = VersionType.SEMVER_SHORT_NOTATION.getSchema();
			}
			
			VersionHelper vhPin = parseVersion(pin);
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
							.matcher(vhVersion.getVersionComponents().get(i))
							.matches();
				if (matching && p.matcher(vhPin.getVersionComponents().get(i)).matches() &&
						// make sure that it's not a name of version element inside pin
						// i.e. could happen with string elements such as modifier / metadata
						null == VersionElement.getVersionElement(vhPin.getVersionComponents().get(i))) {
					// here we know that version is matching schema and need to verify if it's matching pin
					// means we're dealing with pin item that must match version element exactly
					matching = vhPin.getVersionComponents().get(i).equals(vhVersion.getVersionComponents().get(i));
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
	 * This method returns true if schema contains a year element (it is calver)
	 * @param schema String
	 * @return true if schema contains a year element
	 */
	public static boolean isSchemaCalver (String schema) {
		boolean isCalver = false;
		Set<VersionElement> veSet = new HashSet<>(parseSchema(schema));
		if (veSet.contains(VersionElement.OY) || 
				veSet.contains(VersionElement.YY) || 
				veSet.contains(VersionElement.YYYY)) {
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
				if (!oldVh.getVersionComponents().get(i).equals(newVh.getVersionComponents().get(i))) {
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
				if (!oldVh.getVersionComponents().get(i).equals(newVh.getVersionComponents().get(i))) {
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

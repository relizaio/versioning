/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import io.reliza.versioning.Version.VersionHelper;

/**
 * This class defines a set of various utils used for Versioning
 *
 */
public class VersionUtils {
	
	/**
	 * This method parses supplied schema to version elements
	 * @param schema String
	 * @return list of VersionElement
	 */
	public static List<VersionElement> parseSchema (String schema) {
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = "major.minor.patch-identifier+metadata";
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
		// check special case for Maven-style Snapshot
		boolean isSnapshot = false;
		if (version.endsWith(Constants.MAVEN_STYLE_SNAPSHOT)) {
			isSnapshot = true;
			version = version.replaceFirst(Constants.MAVEN_STYLE_SNAPSHOT + "$", "");
		}
		// handle + and - differently as semver supports other separators after plus and dash
		String[] plusel = null;
		if (version.contains("+")) {
			plusel = version.split("\\+");
			// if more than one plus raise an error
			if (plusel.length > 2) {
				throw new RuntimeException("Cannot handle more than one plus in version = " + version);
			}
			version = plusel[0];
		}
		String[] dashel = null;
		if (version.contains("-")) {
			dashel = version.split("-");
			// if more than one dash raise an error
			if (dashel.length > 2) {
				throw new RuntimeException("Cannot handle more than one dash in version = " + version);
			}
			version = dashel[0];
		}
		List<String> versionComponents = Arrays.asList(version.split("(\\.|_)"));
		String modifier = (null == dashel) ? null : dashel[1];
		String metadata = (null == plusel) ? null : plusel[1];
		VersionHelper vh = new VersionHelper(versionComponents, modifier, metadata, isSnapshot);
		return vh;
	}
	
	/**
	 * This method returns true if supplied version string matches supplied schema string
	 * @param schema String
	 * @param version String
	 * @return true if version is matching schema, false otherwise
	 */
	public static boolean isVersionMatchingSchema (String schema, String version) {
		boolean matching = true;
		
		VersionHelper vh = parseVersion(version);

		// handle semver as schema name
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = "Major.Minor.Patch";
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
		Version v = new Version(schema);
		return v;
	}
	
	/**
	 * This method returns base version based on supplied schema, modifier and metadata
	 * @param schema String
	 * @param modifier String
	 * @param metadata String
	 * @return version String
	 */
	public static Version initializeVersionWithModMeta(String schema, String modifier, String metadata) {
		Version v = new Version(schema);
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
}

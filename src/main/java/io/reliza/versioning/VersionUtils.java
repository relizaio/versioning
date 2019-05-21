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

public class VersionUtils {
	
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
	
	public static List<String> extractSchemaSeparators (String schema) {
		List<String> retList = new ArrayList<>();
		for (char c : schema.toCharArray()) {
			if (c == '.' || c == '+' || c == '-' || c == '_') {
				retList.add(Character.toString(c));
			}
		}
		return retList;
	}
	
	public static VersionHelper parseVersion (String version) {		
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
		VersionHelper vh = new VersionHelper(versionComponents, modifier, metadata);
		return vh;
	}
	
	public static boolean isVersionMatchingSchema (String schema, String version) {
		boolean matching = true;
		
		VersionHelper vh = parseVersion(version);

		// handle semver as schema name
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = "Major.Minor.Patch";
		}
		
		// remove -modifier and +metadata from schema as it's irrelevant
		schema = schema.replace("+metadata", "");
		schema = schema.replace("-modifier", "");
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
	
	public static Version initializeEmptyVersion(String schema) {
		Version v = new Version(schema);
		return v;
	}
	
	public static Version initializeVersionWithModMeta(String schema, String modifier, String metadata) {
		Version v = new Version(schema);
		v.setModifier(modifier);
		v.setMetadata(metadata);
		return v;
	}
	
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

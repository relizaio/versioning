/**
* Copyright 2019 - 2020 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.reliza.versioning.VersionApi.ActionEnum;

public class Version implements Comparable<Version> {
	
	/**
	 * 
	 * This class is used as a helper to parse version string
	 *
	 */
	public static class VersionHelper {
		private List<String> versionComponents;
		private String modifier;
		private String metadata;
		private boolean isSnapshot = false;
		
		/**
		 * This method constructs VersionHelper class based on parsed version components
		 * @param versionComponents Collection of Strings, parsed from version
		 * @param modifier String
		 * @param metadata String
		 * @param isSnapshot boolean, true if this is maven-style snapshot
		 */
		public VersionHelper(Collection<String> versionComponents, String modifier, 
												String metadata, boolean isSnapshot) {
			this.versionComponents = new ArrayList<>(versionComponents);
			this.modifier = modifier;
			this.metadata = metadata;
			this.isSnapshot = isSnapshot;
		}
		
		/**
		 * Returns list of version components used in the Versionelper
		 * @return list of version components
		 */
		public List<String> getVersionComponents() {
			return new ArrayList<>(versionComponents);
		}
		
		/**
		 * Returns modifier string of VersionHelper
		 * @return modifier string
		 */
		public String getModifier() {
			return modifier;
		}
		
		/**
		 * Returns metadata string of VersionHelper
		 * @return metadata strng
		 */
		public String getMetadata() {
			return metadata;
		}
		
		/**
		 * Returns Maven Style Snapshot status ("-SNAPSHOT" suffix)
		 * @return isSnapshot boolean
		 */
		public boolean isSnapshot() {
			return isSnapshot;
		}
	}
	
	private Integer major;
	private Integer minor;
	private Integer patch;
	private String modifier; // in semver this would be identifier, i.e. 1.0.0-alpha
	private Integer year;
	private Integer month;
	private Integer day;
	private String metadata; // from semver, 1.0.0+20130313144700
	private String schema;
	private String buildid; // i.e. 24 or build24
	private String buildenv; // i.e. circleci
	private boolean isSnapshot;
	
	/**
	 * Private constructor to denote uninitializable class
	 */
	private Version () {}
	
	@Override
	public String toString() {
		return String.format("Major = %d, Minor = %d, Patch = %d, Year = %d, Month = %d, Day = %d, "+ 
							 "Modifier = %s, Metadata = %s, Schema = %s, versionString = %s", this.major, 
							 this.minor, this.patch, this.year, this.month, this.day, this.modifier, 
							 this.metadata, this.schema, constructVersionString());
	}
	
	/**
	 * This method outputs string version based on supplied schema parameter
	 * If schema parameter is not supplied, uses own schema
	 * @param useSchema String, if not supplied, uses own schema
	 * @param setIsSnapshot Boolean, use current status if Null
	 * @return version String
	 */
	public String constructVersionString(String useSchema, Boolean setIsSnapshot) {
		if (StringUtils.isEmpty(useSchema)) {
			useSchema = schema;
		}
		StringBuilder versionString = new StringBuilder();
		
		if (Constants.SEMVER.equalsIgnoreCase(useSchema)) {
			useSchema = VersionType.SEMVER_FULL_NOTATION.getSchema();
		}
		List<VersionElement> schemaVeList = VersionUtils.parseSchema(useSchema);
		List<String> separators = VersionUtils.extractSchemaSeparators(useSchema);
		try {
			for (int i=0; i<schemaVeList.size(); i++) {
				boolean useEl = i < schemaVeList.size() - 1;
				switch (schemaVeList.get(i)) {
				case MAJOR:
					versionString.append(this.major.toString());
					break;
				case MINOR:
					versionString.append(this.minor.toString());
					break;
				case PATCH:
					versionString.append(this.patch.toString());
					break;
				case BUILDID:
					versionString.append(this.buildid);
					break;
				case BUILDENV:
					versionString.append(this.buildenv);
					break;
				case CALVER_MODIFIER:
					versionString.append(this.modifier);
					break;
				case SEMVER_MODIFIER:
					// special handler for semver optional modifier
					char separatorCharMod = '0';
					if (versionString.length() > 1) {
						separatorCharMod = versionString.charAt(versionString.length()-1);
					}
					if (StringUtils.isNotEmpty(this.modifier)) {
						// check if we need to add minus
						if ('-' != separatorCharMod && '+' != separatorCharMod && '.' != separatorCharMod && 
								'_' != separatorCharMod && '-' != separatorCharMod) {
							versionString.append('-');
						}
						versionString.append(this.modifier);
					} else {
						if (versionString.length() > 1 && 
							('-' == separatorCharMod || '+' == separatorCharMod)) {
							// delete trailing dash or plus
							versionString.delete(versionString.length()-1, versionString.length());
						}
						useEl = false;
					}
					break;
				case METADATA:
					char separatorChar = '0';
					if (versionString.length() > 1) {
						separatorChar = versionString.charAt(versionString.length()-1);
					}
					if (StringUtils.isNotEmpty(this.metadata)) {
						// check if we need to add plus
						if ('-' != separatorChar && '+' != separatorChar && '.' != separatorChar && 
								'_' != separatorChar && '-' != separatorChar) {
							versionString.append('+');
						}
						versionString.append(this.metadata);
					} else {
						// special handler for semver optional modifer
						if ('-' == separatorChar || '+' == separatorChar) {
							// delete trailing dash or plus
							versionString.delete(versionString.length()-1, versionString.length());
						}
						useEl = false;
					}
					break;
				case YYYY:
					String yearStr = this.year.toString();
					if (yearStr.length() == 2) {
						yearStr = "20" + yearStr;
					} else if (yearStr.length() == 3) {
						yearStr = "2" + yearStr;
					}
					versionString.append(yearStr);
					break;
				case YY:
					yearStr = this.year.toString();
					if (yearStr.length() > 2) {
						yearStr = "" + yearStr.charAt(yearStr.length() - 2) + yearStr.charAt(yearStr.length() - 1);
						yearStr = Integer.valueOf(Integer
															.parseInt(yearStr))
															.toString();
					}
					versionString.append(yearStr);
					break;
				case OY:
					if (this.year < 10) {
						versionString.append("0");
						versionString.append(this.year.toString());
					} else if (this.year < 99) {
						versionString.append(this.year.toString());
					} else {
						yearStr = this.year.toString();
						yearStr = "" + yearStr.charAt(yearStr.length() - 2) + yearStr.charAt(yearStr.length() - 1);
						yearStr = Integer.valueOf(Integer
														.parseInt(yearStr))
														.toString();
						if (yearStr.length() < 2) {
							versionString.append("0");
						}
						versionString.append(yearStr);
					}
					break;
				case MM:
					versionString.append(this.month.toString());
					break;
				case OM:
					if (this.month < 10) {
						versionString.append("0");
					}
					versionString.append(this.month.toString());
					break;
				case DD:
					versionString.append(this.day.toString());
					break;
				case OD:
					if (this.day < 10) {
						versionString.append("0");
					}
					versionString.append(this.year.toString());
					break;
				default:
					break;
				}
				if (useEl) {
					versionString.append(separators.get(i));
				}
			}
			Boolean setSnapshot = (null == setIsSnapshot) ? null : setIsSnapshot;
			if (null == setSnapshot) {
				setSnapshot = isSnapshot();
			}
			if (setSnapshot) {
				versionString.append(Constants.MAVEN_STYLE_SNAPSHOT);
			}
			
		} catch (NullPointerException npe) {
			throw new RuntimeException("The schema " + useSchema + " is not supported by this Version object");
		}
		return versionString.toString();
	}
	
	/**
	 * This method outputs string version based on supplied schema parameter
	 * If schema parameter is not supplied, uses own schema
	 * uses own Maven Style Snapshot status
	 * @param useSchema String, if not supplied, uses own schema
	 * @return version string
	 */
	public String constructVersionString(String useSchema) {
		return constructVersionString(useSchema, null); 
	}
	
	/**
	 * This method outputs string version based on own schema
	 * and own Maven Style Snapshot status
	 * @return version String
	 */
	public String constructVersionString() {
		return constructVersionString(null);
	}
	
	/**
	 * Used to set CalVer properties to specific date
	 * Pass null value to argument to set to current date
	 * @param date ZonedDateTime to set CalVer version to
	 */
	public void setDate(ZonedDateTime date) {
		if (null == date) {
			date = ZonedDateTime.now(ZoneId.of("UTC"));
		}
		this.year = date.getYear();
		this.month = date.getMonth().getValue();
		this.day = date.getDayOfMonth();
	}
	
	/**
	 * Sets version date for CalVer to today
	 */
	public void setCurrentDate() {
		setDate(null);
	}
	
	/**
	 * Increments patch element of version by step
	 * @param step, amount by which to increment version
	 */
	public void bumpPatch(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.patch = patch + step;
	}
	
	/**
	 * Sets patch element of version to patch parameter
	 * @param patch, value to set patch element to
	 */
	public void setPatch(Integer patch) {
		this.patch = patch;
	}
	
	/**
	 * Returns patch element of the version
	 * @return patch element (integer)
	 */
	public Integer getPatch() {
		return patch;
	}
	
	/**
	 * Sets minor element of version to minor parameter
	 * @param minor, value to set minor element to
	 */
	public void setMinor(Integer minor) {
		this.minor = minor;
	}
	
	/**
	 * Returns minor element of the version
	 * @return minor element (integer)
	 */
	public Integer getMinor() {
		return minor;
	}
	
	/**
	 * Sets major element of version to minor parameter
	 * @param major, value to set minor element to
	 */
	public void setMajor(Integer major) {
		this.major = major;
	}
	
	/**
	 * Returns major element of the version
	 * @return major element (integer)
	 */
	public Integer getMajor() {
		return major;
	}
	
	/**
	 * Increments minor element of version by step
	 * Resets patch element of version to zero
	 * @param step, amount by which to increment minor version
	 */
	public void bumpMinor(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.minor = minor + step;
		this.patch = 0;
	}
	
	/**
	 * Increments major element of version by step
	 * Resets minor element of version to zero
	 * Resets patch element of version to zero
	 * @param step, amount by which to increment major version
	 */
	public void bumpMajor(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.major = major + step;
		this.minor = 0;
		this.patch = 0;
	}
	
	/**
	 * Sets version modifier to modifier parameter
	 * @param modifier String
	 */
	public void setModifier(String modifier) {
		this.modifier = modifier;
	}
	
	/**
	 * Sets version metadata to metadata parameter
	 * @param metadata String
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
	
	/**
	 * This method tries to intelligently bump version
	 * by incrementing patch if present, otherwise minor, 
	 * otherwise bumps date to today's if using CalVer
	 */
	public void simpleBump () {
		Set<VersionElement> veList = new HashSet<>(VersionUtils.parseSchema(schema));
		if (veList.contains(VersionElement.PATCH)) {
			this.bumpPatch(null);
		} else if (veList.contains(VersionElement.MINOR)) {
			this.bumpMinor(null);
		}
	}
	
	/**
	 * This method returns Maven Style Snapshot status of version
	 * @return isSnapshot boolean
	 */
	public boolean isSnapshot() {
		return isSnapshot;
	}
	
	/**
	 * Sets Maven Style Snapshot to snapshot parameter
	 * @param snapshot boolean
	 */
	public void setSnapshot(boolean snapshot) {
		this.isSnapshot = snapshot;
	}

	/**
	 * Gets Buildid version field, i.e. 24
	 * @return value of Buildid version element
	 */
	public String getBuildid() {
		return buildid;
	}

	/**
	 * Sets Buildid version field
	 * @param buildid - value to set to Buildid field, i.e. 24
	 */
	public void setBuildid(String buildid) {
		this.buildid = buildid;
	}

	/**
	 * Gets Buildenv version field, i.e. circleci
	 * @return value of Buildenv version element
	 */
	public String getBuildenv() {
		return buildenv;
	}
	
	/**
	 * Sets Buildenv version field
	 * @param buildenv - value to set to Buildenv field, i.e. circleci
	 */
	public void setBuildenv(String buildenv) {
		this.buildenv = buildenv;
	}
	
	/**
	 * Factory method to initialize version based on specified schema
	 * @param schema String
	 * @return Version object corresponding to the supplied schema
	 */
	public static Version getVersion (String schema) {
		Version v = new Version();
		v.schema = schema;
		List<VersionElement> schemaVeList = VersionUtils.parseSchema(schema);
		if (schemaVeList.contains(VersionElement.MINOR)) {
			v.minor = 1;
			v.major = 0;
			v.patch = 0;
		} else if (schemaVeList.contains(VersionElement.MAJOR)) {
			v.minor = 0;
			v.major = 1;
			v.patch = 0;
		} else if (schemaVeList.contains(VersionElement.PATCH)) {
			v.minor = 0;
			v.major = 0;
			v.patch = 1;
		}
		if (schemaVeList.contains(VersionElement.CALVER_MODIFIER)) {
			v.modifier = Constants.BASE_MODIFIER;
		}
		v.setCurrentDate();
		return v;
	}
	
	/**
	 * Factory method to create a Version object based on version string (origVersion) and specified schema
	 * @param origVersion String
	 * @param schema String
	 * @return Version object corresponding to supplied schema and based on supplied original version string
	 */
	public static Version getVersion (String origVersion, String schema) {
		if (!VersionUtils.isVersionMatchingSchema(schema, origVersion)) {
			throw new RuntimeException("Cannot construct Version object, since version is not matching schema");
		}
		Version v = new Version();
		v.schema = schema;
		schema = VersionUtils.stripSchemaFromModMeta(schema);
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = "Major.Minor.Patch";
		}
		List<VersionElement> schemaVeList = VersionUtils.parseSchema(schema);
		VersionHelper vh = VersionUtils.parseVersion(origVersion);
		v.modifier = vh.getModifier();
		v.metadata = vh.getMetadata();
		v.isSnapshot = vh.isSnapshot();
		for (int i=0; i<schemaVeList.size(); i++) {
			switch (schemaVeList.get(i)) {
			case MAJOR:
				v.major = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case MINOR:
				v.minor = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case PATCH:
				v.patch = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case SEMVER_MODIFIER:
			case CALVER_MODIFIER:
				v.modifier = vh.getVersionComponents().get(i);
				if (null == v.modifier) {
					v.modifier = Constants.BASE_MODIFIER;
				}
				break;
			case METADATA:
				v.metadata = vh.getVersionComponents().get(i);
				break;
			case YYYY:
			case YY:
			case OY:
				v.year = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case MM:
			case OM:
				v.month = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case DD:
			case OD:
				v.day = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case BUILDID:
				v.buildid = vh.getVersionComponents().get(i);
				break;
			case BUILDENV:
				v.buildenv = vh.getVersionComponents().get(i);
				break;
			default:
				break;
			}
		}
		return v;
	}
	
	public static Version getVersionFromPin (String schema, String pin) {
		return getVersionFromPinAndOldVersion(schema, pin, null, null);
	}
	
	/**
	 * If oldVersionString is present and we're dealing with calver schema, this effectively does simple bump relative to old version date based on schema and pin
	 * if it's semver schema and old version present, this will effectively return old version making sure it's matching schema and pin
	 * if old version is not present, this will do simple bump relative to semver
	 * @param schema String, required
	 * @param pin String, required
	 * @param oldVersionString String, optional
	 * @return Version object which represents product of schema if pin if old version is not present, or otherwise simply bump relative to old version
	 */
	public static Version getVersionFromPinAndOldVersion (String schema, String pin, String oldVersionString, ActionEnum ae) {
		if (!VersionUtils.isPinMatchingSchema(schema, pin)) {
			throw new RuntimeException("Cannot construct Version object, since pin is not matching schema");
		}
		if (StringUtils.isNotEmpty(oldVersionString) && !VersionUtils.isVersionMatchingSchemaAndPin(schema, pin, oldVersionString)) {
			throw new RuntimeException("Cannot construct Version object, since old version is not matching either pin or schema");
		}
		Version v = new Version();
		v.schema = schema;
		schema = VersionUtils.stripSchemaFromModMeta(schema);
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = VersionType.SEMVER_SHORT_NOTATION.getSchema();
		}
		Version oldV = null;
		if (StringUtils.isNotEmpty(oldVersionString)) {
			oldV = Version.getVersion(oldVersionString, schema);
		}
		
		List<VersionElement> schemaVeList = VersionUtils.parseSchema(schema);
		if (Constants.SEMVER.equalsIgnoreCase(pin)) {
			pin = VersionType.SEMVER_SHORT_NOTATION.getSchema();
		}
		// initialize all elements at zero first or old version values if present
		if (null != oldV && null != oldV.getMinor()) {
			v.minor = oldV.getMinor();
		} else {
			v.minor = 0;
		}
		if (null != oldV && null != oldV.getMajor()) {
			v.major = oldV.getMajor();
		} else {
			v.major = 0;
		}
		if (null != oldV && null != oldV.getPatch()) {
			v.patch = oldV.getPatch();
		} else {
			v.patch = 0;
		}
		v.setCurrentDate();
		if (null != oldV && null != oldV.year) {
			v.year = oldV.year;
		}
		if (null != oldV && null != oldV.month) {
			v.month = oldV.month;
		}
		if (null != oldV && null != oldV.day) {
			v.day = oldV.day;
		}
		// skipping metadata and such - which seems reasonable in this context
		// now populate whatever we can from pin -> pin overrides old version
		VersionHelper vh = VersionUtils.parseVersion(pin);
		v.modifier = vh.getModifier();
		v.metadata = vh.getMetadata();
		v.isSnapshot = vh.isSnapshot();

		// this would be set of unmodifiable elements since they are set by pin
		Set<VersionElement> elsProtectedByPin = new HashSet<>(); // skipping dates since we are not bumping dates below
		
		for (int i=0; i<schemaVeList.size(); i++) {
			if (VersionElement.getVersionElement(vh.getVersionComponents().get(i)) != schemaVeList.get(i)) {
				switch (schemaVeList.get(i)) {
				case MAJOR:
					v.major = Integer.parseInt(vh.getVersionComponents().get(i));
					elsProtectedByPin.add(VersionElement.MAJOR);
					break;
				case MINOR:
					v.minor = Integer.parseInt(vh.getVersionComponents().get(i));
					elsProtectedByPin.add(VersionElement.MINOR);
					break;
				case PATCH:
					v.patch = Integer.parseInt(vh.getVersionComponents().get(i));
					elsProtectedByPin.add(VersionElement.PATCH);
					break;
				case SEMVER_MODIFIER:
				case CALVER_MODIFIER:
					v.modifier = vh.getVersionComponents().get(i);
					break;
				case METADATA:
					v.metadata = vh.getVersionComponents().get(i);
					break;
				case YYYY:
				case YY:
				case OY:
					v.year = Integer.parseInt(vh.getVersionComponents().get(i));
					break;
				case MM:
				case OM:
					v.month = Integer.parseInt(vh.getVersionComponents().get(i));
					break;
				case DD:
				case OD:
					v.day = Integer.parseInt(vh.getVersionComponents().get(i));
					break;
				case BUILDID:
					v.buildid = vh.getVersionComponents().get(i);
					break;
				case BUILDENV:
					v.buildenv = vh.getVersionComponents().get(i);
					break;
				default:
					break;
				}
			} else if (schemaVeList.get(i) == VersionElement.CALVER_MODIFIER) {
				v.modifier = Constants.BASE_MODIFIER;
			} else {
				// pin matches schema and we need to resolve dates as current if present
				ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
				switch (schemaVeList.get(i)) {
				case YYYY:
				case YY:
				case OY:
					v.year = date.getYear();
					break;
				case MM:
				case OM:
					v.month = date.getMonth().getValue();
					break;
				case DD:
				case OD:
					v.day = date.getDayOfMonth();
					break;
				default:
					break;
				}
			}
		}
		
		// check if we had any updated calver components - and if yes, we reset semver components to 0
		if (null != oldV && ((null != oldV.year && v.year > oldV.year) || (null != oldV.month && v.month > oldV.month) || (null != oldV.day && v.day > oldV.day))) {
			// calver update happened, reset semver
			v.minor = 0;
			v.major = 0;
			v.patch = 0;
		} else if (ae == ActionEnum.BUMP_MAJOR && !elsProtectedByPin.contains(VersionElement.MAJOR)) {
			++v.major;
			v.minor = 0;
			v.patch = 0;
		} else if (ae == ActionEnum.BUMP_MINOR && !elsProtectedByPin.contains(VersionElement.MINOR)) {
			++v.minor;
			v.patch = 0;
		} else if (ae != null && !elsProtectedByPin.contains(VersionElement.PATCH)) {
			++v.patch;
		}
		return v;
	}
	
	/**
	 * This methods compares any 2 integer values between version objects
	 * Used to generalize comparisons for compareTo method
	 * @param i1 Integer
	 * @param i2 Integer
	 * @return -1 if i1 is larger, 1 if i2 is larger or 0 if both are null or equal
	 */
	private int compareVersionIntegers (Integer i1, Integer i2) {
		if (null != i1 && null == i2) {
			return -1;
		} else if (null == i1 && null != i2) {
			return 1;
		} else if (null != i1 && i1 > i2) {
			return -1;
		} else if (null != i1 && i1 < i2) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@Override
	public int hashCode() {
		StringBuilder sb = new StringBuilder();
		if (null != major) {
			sb.append(major.toString());
		}
		if (null != minor) {
			sb.append(minor.toString());
		}
		if (null != patch) {
			sb.append(patch.toString());
		}
		if (null != modifier) {
			sb.append(modifier);
		}
		if (null != year) {
			sb.append(year.toString());
		}
		if (null != month) {
			sb.append(month.toString());
		}
		if (null != day) {
			sb.append(day.toString());
		}
		if (null != metadata) {
			sb.append(metadata);
		}
		if (null != schema) {
			sb.append(schema);
		}
		if (null != buildid) {
			sb.append(buildid);
		}
		if (null != buildenv) {
			sb.append(buildenv);
		}
		if (isSnapshot) {
			sb.append('1');
		} else {
			sb.append('0');
		}
		return sb.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Version) {
			return hashCode() == other.hashCode();
		} else {
			return false;
		}
	}
	
	@Override
	public int compareTo(Version otherV) {
		int comparison = compareVersionIntegers(year, otherV.year);
		if (0 == comparison) {
			comparison = compareVersionIntegers(month, otherV.month);
		}
		if (0 == comparison) {
			comparison = compareVersionIntegers(major, otherV.major);
		}
		if (0 == comparison) {
			comparison = compareVersionIntegers(day, otherV.day);
		}
		if (0 == comparison) {
			comparison = compareVersionIntegers(minor, otherV.minor);
		}
		if (0 == comparison) {
			comparison = compareVersionIntegers(patch, otherV.patch);
		}
		if (0 == comparison && StringUtils.isNotEmpty(buildid) && StringUtils.isNotEmpty(otherV.buildid)) {
			try {
				comparison = compareVersionIntegers(Integer.parseInt(buildid), Integer.parseInt(otherV.buildid));
			} catch (NumberFormatException nfe) {}
		}
		return comparison;
	}
	
	/**
	 * This class is used to compare any 2 version strings based on common schema
	 * @author pavel
	 *
	 */
	public static class VersionStringComparator implements Comparator<String> {

		private String schema;
		
		public VersionStringComparator(String schema) {
			this.schema = schema;
		}
		
		@Override
		public int compare(String v1Str, String v2Str) {
			boolean v1Matching = VersionUtils.isVersionMatchingSchema(schema, v1Str);
			boolean v2Matching = VersionUtils.isVersionMatchingSchema(schema, v2Str);
			if (v1Matching && !v2Matching) {
				return -1;
			} else if (!v1Matching && v2Matching) {
				return 1;
			} else if (v1Matching) {
				Version v1 = Version.getVersion(v1Str, schema);
				Version v2 = Version.getVersion(v2Str, schema);
				return v1.compareTo(v2);
			}
			return 0;
		}
		
	}
	
}

/**
* Copyright 2019 - 2020 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class Version {
	
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
	 * Initializes version based on specified schema
	 * @param schema String
	 */
	public Version (String schema) {
		this.schema = schema;
		List<VersionElement> schemaVeList = VersionUtils.parseSchema(schema);
		if (schemaVeList.contains(VersionElement.MINOR)) {
			this.minor = 1;
			this.major = 0;
			this.patch = 0;
		} else if (schemaVeList.contains(VersionElement.MAJOR)) {
			this.minor = 0;
			this.major = 1;
			this.patch = 0;
		} else if (schemaVeList.contains(VersionElement.PATCH)) {
			this.minor = 0;
			this.major = 0;
			this.patch = 1;
		}
		setCurrentDate();
	}
	
	/**
	 * Creates a Version object based on version string (origVersion) and specified schema
	 * @param origVersion String
	 * @param schema String
	 */
	public Version (String origVersion, String schema) {
		if (!VersionUtils.isVersionMatchingSchema(schema, origVersion)) {
			throw new RuntimeException("Cannot construct Version object, since version is not matching schema");
		}
		this.schema = schema;
		schema = VersionUtils.stripSchemaFromModMeta(schema);
		if (Constants.SEMVER.equalsIgnoreCase(schema)) {
			schema = "Major.Minor.Patch";
		}
		List<VersionElement> schemaVeList = VersionUtils.parseSchema(schema);
		VersionHelper vh = VersionUtils.parseVersion(origVersion);
		this.modifier = vh.getModifier();
		this.metadata = vh.getMetadata();
		this.isSnapshot = vh.isSnapshot();
		
		for (int i=0; i<schemaVeList.size(); i++) {
			switch (schemaVeList.get(i)) {
			case MAJOR:
				this.major = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case MINOR:
				this.minor = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case PATCH:
				this.patch = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case MODIFIER:
				this.modifier = vh.getVersionComponents().get(i);
				break;
			case METADATA:
				this.metadata = vh.getVersionComponents().get(i);
				break;
			case YYYY:
			case YY:
			case OY:
				this.year = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case MM:
			case OM:
				this.month = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case DD:
			case OD:
				this.day = Integer.parseInt(vh.getVersionComponents().get(i));
				break;
			case BUILDID:
				this.buildid = vh.getVersionComponents().get(i);
				break;
			case BUILDENV:
				this.buildenv = vh.getVersionComponents().get(i);
				break;
			default:
				break;
			}
		}
	}
	
	
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
			useSchema = "Major.Minor.Patch-modifier+metadata";
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
				case MODIFIER:
					// special handler for semver optional modifer
					if (StringUtils.isNotEmpty(this.modifier)) {
						versionString.append(this.modifier);
					} else {
						if (versionString.length() > 1 && 
							('-' == versionString.charAt(versionString.length()-1) ||
							'+' == versionString.charAt(versionString.length()-1))) {
							// delete trailing dash or plus
							versionString.delete(versionString.length()-1, versionString.length());
						}
						useEl = false;
					}
					break;
				case METADATA:
					if (StringUtils.isNotEmpty(this.metadata)) {
						versionString.append(this.metadata);
					} else {
						// special handler for semver optional modifer
						if (versionString.length() > 1 && 
								('-' == versionString.charAt(versionString.length()-1) ||
								'+' == versionString.charAt(versionString.length()-1))) {
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
		} else if (VersionUtils.isSchemaCalver(schema)) {
			this.setCurrentDate();
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
	
	
}

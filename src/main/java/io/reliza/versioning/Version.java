/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
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
	
	public static class VersionHelper {
		private List<String> versionComponents;
		private String modifier;
		private String metadata;
		private boolean isSnapshot = false;
		
		public VersionHelper(Collection<String> versionComponents, String modifier, String metadata) {
			this.versionComponents = new ArrayList<>(versionComponents);
			this.modifier = modifier;
			this.metadata = metadata;
		}
		
		public VersionHelper(Collection<String> versionComponents, String modifier, 
												String metadata, boolean isSnapshot) {
			this.versionComponents = new ArrayList<>(versionComponents);
			this.modifier = modifier;
			this.metadata = metadata;
			this.isSnapshot = isSnapshot;
		}
		
		public List<String> getVersionComponents() {
			return new ArrayList<>(versionComponents);
		}
		
		public String getModifier() {
			return modifier;
		}
		
		public String getMetadata() {
			return metadata;
		}
		
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
	private boolean isSnapshot;
	
	/**
	 * Initializes version based on specified schema
	 * @param schema
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
	 * @param origVersion
	 * @param schema
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
	 * If parameter is not supplied, uses own schema
	 * @param useSchema
	 * @return
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
	
	public String constructVersionString(String useSchema) {
		return constructVersionString(useSchema, null); 
	}
			
	public String constructVersionString() {
		return constructVersionString(null);
	}
	
	/**
	 * Used to set calver properties to specific date
	 * Pass null value to argument to set to current date
	 * @param date
	 */
	public void setDate(ZonedDateTime date) {
		if (null == date) {
			date = ZonedDateTime.now(ZoneId.of("UTC"));
		}
		this.year = date.getYear();
		this.month = date.getMonth().getValue();
		this.day = date.getDayOfMonth();
	}
	
	public void setCurrentDate() {
		setDate(null);
	}
	
	public void bumpPatch(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.patch = patch + step;
	}
	
	public void setPatch(Integer patch) {
		this.patch = patch;
	}
	
	public Integer getPatch() {
		return patch;
	}
	
	public void setMinor(Integer minor) {
		this.minor = minor;
	}
	
	public Integer getMinor() {
		return minor;
	}
	
	public void setMajor(Integer major) {
		this.major = major;
	}
	
	public Integer getMajor() {
		return major;
	}
	
	public void bumpMinor(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.minor = minor + step;
		this.patch = 0;
	}
	
	public void bumpMajor(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.major = major + step;
		this.minor = 0;
		this.patch = 0;
	}
	
	public void setModifier(String modifier) {
		this.modifier = modifier;
	}
	
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
	
	/**
	 * This method tries to intellegintly bump version
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
	
	public boolean isSnapshot() {
		return isSnapshot;
	}
	
	public void setSnapshot(boolean snapshot) {
		this.isSnapshot = snapshot;
	}
}

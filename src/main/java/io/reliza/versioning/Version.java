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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.reliza.versioning.VersionApi.ActionEnum;
import io.reliza.versioning.VersionElement.ParsedVersionElement;

/**
 * class Version
 */
public class Version implements Comparable<Version> {
	
	public static record VersionComponent (ParsedVersionElement pve, String representation) {}
	
	/**
	 * 
	 * This class is used as a helper to parse version string
	 *
	 */
	public static class VersionHelper {
		private List<VersionComponent> versionComponents;
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
		public VersionHelper(Collection<VersionComponent> versionComponents, String modifier, 
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
		public List<VersionComponent> getVersionComponents() {
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
	private Integer nano;
	private String modifier; // in semver this would be identifier, i.e. 1.0.0-alpha
	private Integer year;
	private Integer month;
	private Integer day;
	private String metadata; // from semver, 1.0.0+20130313144700
	private String schema;
	private String buildid; // i.e. 24 or build24
	private String buildenv; // i.e. circleci
	private String branch; // name of branch, i.e. 234-ticket_I_work_on
	private boolean isSnapshot;
	
	/**
	 * Private constructor to denote uninitializable class
	 */
	private Version () {}
	
	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return String.format("Major = %d, Minor = %d, Patch = %d, Nano = %d, Year = %d, Month = %d, " +
							 "Day = %d, "+  "Modifier = %s, Metadata = %s, Schema = %s, versionString = %s",
							 this.major, this.minor, this.patch, this.nano, this.year, this.month, this.day,
							 this.modifier, this.metadata, this.schema, constructVersionString());
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
		
		Optional<VersionType> ovt = VersionType.resolveByAliasName(useSchema);
		if (ovt.isPresent()) useSchema = ovt.get().getSchema();

		List<ParsedVersionElement> schemaPveList = VersionUtils.parseSchema(useSchema);
		List<String> separators = VersionUtils.extractSchemaSeparators(useSchema);
		try {
			for (int i=0; i<schemaPveList.size(); i++) {
				boolean useEl = i < schemaPveList.size() - 1;
				switch (schemaPveList.get(i).ve()) {
				case MAJOR:
					versionString.append(this.major.toString());
					break;
				case MINOR:
					versionString.append(this.minor.toString());
					break;
				case PATCH:
					versionString.append(this.patch.toString());
					break;
				case NANO:
					versionString.append(this.nano.toString());
					break;
				case BRANCH:
					versionString.append(this.branch);
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
				case YYOM:
					yearStr = this.year.toString();
					if (yearStr.length() > 2) {
						yearStr = "" + yearStr.charAt(yearStr.length() - 2) + yearStr.charAt(yearStr.length() - 1);
						yearStr = Integer.valueOf(Integer
															.parseInt(yearStr))
															.toString();
					}
					versionString.append(yearStr);
					if (this.month < 10) {
						versionString.append("0");
					}
					versionString.append(this.month.toString());
					break;
				case YYYYOM:
					yearStr = this.year.toString();
					if (yearStr.length() == 2) {
						yearStr = "20" + yearStr;
					} else if (yearStr.length() == 3) {
						yearStr = "2" + yearStr;
					}
					versionString.append(yearStr);
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
					versionString.append(this.day.toString());
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
	 * Sets the nano element of the version to the nano parameter
	 * @param nano, value to set nano element to
	 */
	public void setNano(Integer nano) {
		this.nano = nano;
	}
	
	/**
	 * Returns the nano element of the version
	 * @return nano element (integer)
	 */
	public Integer getNano() {
		return nano;
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
	 * Increments the nano element of version by step
	 * @param step, amount by which to increment version
	 */
	public void bumpNano(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.nano = nano + step;
	}
	
	/**
	 * Increments patch element of version by step
	 * Resets nano element of version to zero
	 * @param step, amount by which to increment version
	 */
	public void bumpPatch(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.patch = patch + step;
		this.nano = 0;
	}
	
	/**
	 * Increments minor element of version by step
	 * Resets patch element of version to zero
	 * Resets nano element of version to zero
	 * @param step, amount by which to increment minor version
	 */
	public void bumpMinor(Integer step) {
		if (null == step) {
			step = 1;
		}
	
		this.minor = minor + step;
		this.patch = 0;
		this.nano = 0;
	}
	
	/**
	 * Increments major element of version by step
	 * Resets minor element of version to zero
	 * Resets patch element of version to zero
	 * Resets nano element of version to zero
	 * @param step, amount by which to increment major version
	 */
	public void bumpMajor(Integer step) {
		if (null == step) {
			step = 1;
		}
		this.major = major + step;
		this.minor = 0;
		this.patch = 0;
		this.nano = 0;
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
		Set<VersionElement> veList = VersionUtils.parseSchema(schema).stream().map(x -> x.ve()).collect(Collectors.toSet());
		if (veList.contains(VersionElement.PATCH)) {
			this.bumpPatch(null);
		} else if (veList.contains(VersionElement.MINOR)) {
			this.bumpMinor(null);
		} else if (veList.contains(VersionElement.YY) || 
				   veList.contains(VersionElement.YYYY) ||
				   veList.contains(VersionElement.YYOM) ||
				   veList.contains(VersionElement.YYYYOM) ||
				   veList.contains(VersionElement.OY)) {
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
	
	
	/**
	 * Gets Branch version field
	 * @return value of Branch version element
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * Sets Branch version field
	 * @param branch - value to set Branch field to
	 */
	public void setBranch(String branch) {
		this.branch = branch;
	}

	/**
	 * Get version modifier field
	 * @return modifier
	 */
	public String  getModifier() {
		return modifier;
	}

	/**
	 * Get version metadata field
	 * @return metadata
	 */
	public String  getMetadata() {
		return metadata;
	}

	/**
	 * Factory method to initialize version based on specified schema
	 * @param schema String
	 * @return Version object corresponding to the supplied schema
	 */
	public static Version getVersion (String schema) {
		Version v = new Version();
		v.schema = schema;
		Set<VersionElement> schemaVeList = VersionUtils.parseSchema(schema).stream().map(x -> x.ve()).collect(Collectors.toSet());
		if (schemaVeList.contains(VersionElement.MINOR)) {
			v.minor = 1;
			v.major = 0;
			v.patch = 0;
			v.nano = 0;
		} else if (schemaVeList.contains(VersionElement.MAJOR)) {
			v.minor = 0;
			v.major = 1;
			v.patch = 0;
			v.nano = 0;
		} else if (schemaVeList.contains(VersionElement.PATCH)) {
			v.minor = 0;
			v.major = 0;
			v.patch = 0;
			v.nano = 0;
		} else if (schemaVeList.contains(VersionElement.NANO)) {
			v.minor = 0;
			v.major = 0;
			v.patch = 0;
			v.nano = 0;
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
			throw new RuntimeException("Cannot construct Version object, since version is not matching schema, schema = " + schema + " , version = " + origVersion);
		}
		Version v = new Version();
		v.schema = schema;

		Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
		if (ovt.isPresent()) schema = ovt.get().getSchema();
		Optional<VersionHelper> ovh = VersionUtils.parseVersion(origVersion, schema, false);
		if (ovh.isEmpty()) throw new RuntimeException("Version does not match schema: version = " + origVersion + " , schema = " + schema);
		v.modifier = ovh.get().getModifier();
		v.metadata = ovh.get().getMetadata();
		v.isSnapshot = ovh.get().isSnapshot();
		for (VersionComponent vc : ovh.get().getVersionComponents()) {
			switch (vc.pve().ve()) {
			case MAJOR:
				v.major = Integer.parseInt(vc.representation());
				break;
			case MINOR:
				v.minor = Integer.parseInt(vc.representation());
				break;
			case PATCH:
				v.patch = Integer.parseInt(vc.representation());
				break;
			case NANO:
				v.nano = Integer.parseInt(vc.representation());
				break;
			case SEMVER_MODIFIER:
			case CALVER_MODIFIER:
				v.modifier = vc.representation();
				if (null == v.modifier) {
					v.modifier = Constants.BASE_MODIFIER;
				}
				break;
			case METADATA:
				v.metadata = vc.representation();
				break;
			case YYYY:
			case YY:
			case OY:
				v.year = Integer.parseInt(vc.representation());
				break;
			case MM:
			case OM:
				v.month = Integer.parseInt(vc.representation());
				break;
			case YYOM:
				String compToParse = vc.representation();
				String yearPart = compToParse.substring(0, 2);
				String monthPart = compToParse.substring(2);
				v.year = Integer.parseInt(yearPart);
				v.month = Integer.parseInt(monthPart);
				break;
			case YYYYOM:
				compToParse = vc.representation();
				yearPart = compToParse.substring(0, 4);
				monthPart = compToParse.substring(4);
				v.year = Integer.parseInt(yearPart);
				v.month = Integer.parseInt(monthPart);
				break;
			case DD:
			case OD:
				v.day = Integer.parseInt(vc.representation());
				break;
			case BUILDID:
				v.buildid = vc.representation();
				break;
			case BUILDENV:
				v.buildenv = vc.representation();
				break;
			case BRANCH:
				v.branch = vc.representation();
				break;
			default:
				break;
			}
		}
		return v;
	}
	
	
	/** 
	 * @param schema String  
	 * @param pin String 
	 * @return Version
	 */
	public static Version getVersionFromPin (String schema, String pin) {
		return getVersionFromPinAndOldVersion(schema, pin, null, null);
	}
	
	/**
	 * This method validates input for get version call and throws exception if not valid
	 * @param schema
	 * @param pin
	 * @param oldVersionString
	 */
	private static void validateGetVersionFromPinAndOldVersionInput(String schema, String pin, String oldVersionString) {
		if (!VersionUtils.isPinMatchingSchema(schema, pin)) {
			throw new RuntimeException("Cannot construct Version object, since pin is not matching schema");
		}
		if (StringUtils.isNotEmpty(oldVersionString) && !VersionUtils.isVersionMatchingSchemaAndPin(schema, pin, oldVersionString)) {
			throw new RuntimeException("Cannot construct Version object, since old version is not matching either pin or schema");
		}
	}

	/**
	 * This method initializes all new version elements at zero first or old version values if present.
	 * It also skips version metadata and modifiers.
	 * @param v
	 * @param oldV
	 * @param ae
	 */
	private static void initializeVersionElements (Version v, Version oldV, ActionEnum ae) {
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
		if (null != oldV && null != oldV.getNano()) {
			v.nano = oldV.getNano();
		} else {
			v.nano = 0;
		}
		if (ae != ActionEnum.BUMP_PATCH) {
			v.setCurrentDate();
		}
		if (null != oldV && null != oldV.year) {
			v.year = oldV.year;
		}
		if (null != oldV && null != oldV.month) {
			v.month = oldV.month;
		}
		if (null != oldV && null != oldV.day) {
			v.day = oldV.day;
		}
	}

	/**
	 * This method resolves proper new version bump action based on the version structure
	 * @param schemaVeList
	 * @param ae
	 * @return
	 */
	private static ActionEnum resolveNewVersionAction (List<VersionElement> schemaVeList, ActionEnum ae, String oldVersionString) {
		Set<String> veElementCheck = schemaVeList.stream().map(sv -> sv.toString()).collect(Collectors.toSet());
		
		if (ae == ActionEnum.BUMP_MAJOR && !veElementCheck.contains("MAJOR")) {
			ae = ActionEnum.BUMP_MINOR;
		}
		
		if (ae == ActionEnum.BUMP_MINOR && !veElementCheck.contains("MINOR")) {
			ae = ActionEnum.BUMP;
		}
		
		if (ae == ActionEnum.BUMP_PATCH && !veElementCheck.contains("MICRO") && !veElementCheck.contains("PATCH")) {
			ae = ActionEnum.BUMP;
		}
		if (StringUtils.isNotEmpty(oldVersionString) && null == ae) ae = ActionEnum.BUMP;

		return ae;
	}

	/**
	 * This method populates new version from pin -> pin overrides old version
	 * @param v
	 * @param oldVersionString
	 * @param schema
	 */
	private static void populateNewVersionFromOldVersion (Version v, String oldVersionString, String schema) {
		Optional<VersionHelper> ovh = Optional.empty();
		if (StringUtils.isNotEmpty(oldVersionString)) {
		    ovh = VersionUtils.parseVersion(oldVersionString, schema, false);
			v.modifier = ovh.get().getModifier();
			v.metadata = ovh.get().getMetadata();
			v.isSnapshot = ovh.get().isSnapshot();
		}
	}

	/**
	 * This method resolves per element version changes for updated versions
	 * @param elsProtectedByPin
	 * @param v
	 * @param schemaElement
	 * @param versionHelperElement
	 */
	private static void constructVersionElementForUpdatedElement (Set<VersionElement> elsProtectedByPin, 
		Version v, VersionElement schemaElement, String versionHelperElement) {
		switch (schemaElement) {
		case MAJOR:
			v.major = Integer.parseInt(versionHelperElement);
			elsProtectedByPin.add(VersionElement.MAJOR);
			break;
		case MINOR:
			v.minor = Integer.parseInt(versionHelperElement);
			elsProtectedByPin.add(VersionElement.MINOR);
			break;
		case PATCH:
			v.patch = Integer.parseInt(versionHelperElement);
			elsProtectedByPin.add(VersionElement.PATCH);
			break;
		case NANO:
			v.nano = Integer.parseInt(versionHelperElement);
			elsProtectedByPin.add(VersionElement.NANO);
			break;
		case SEMVER_MODIFIER:
		case CALVER_MODIFIER:
			v.modifier = versionHelperElement;
			break;
		case METADATA:
			v.metadata = versionHelperElement;
			break;
		case YYYY:
		case YY:
		case OY:
			v.year = Integer.parseInt(versionHelperElement);
			elsProtectedByPin.add(schemaElement);
			break;
		case MM:
		case OM:
			v.month = Integer.parseInt(versionHelperElement);
			elsProtectedByPin.add(schemaElement);
			break;
		case YYOM:
			String yearPart = versionHelperElement.substring(0,2);
			String monthPart = versionHelperElement.substring(2);
			v.year = Integer.parseInt(yearPart);
			v.month = Integer.parseInt(monthPart);
			elsProtectedByPin.add(schemaElement);
			break;
		case YYYYOM:
			yearPart = versionHelperElement.substring(0,4);
			monthPart = versionHelperElement.substring(4);
			v.year = Integer.parseInt(yearPart);
			v.month = Integer.parseInt(monthPart);
			elsProtectedByPin.add(schemaElement);
			break;
		case DD:
		case OD:
			v.day = Integer.parseInt(versionHelperElement);
			elsProtectedByPin.add(schemaElement);
			break;
		case BRANCH:
			v.branch = versionHelperElement;
			break;
		case BUILDID:
			v.buildid = versionHelperElement;
			break;
		case BUILDENV:
			v.buildenv = versionHelperElement;
			break;
		default:
			break;
		}
	}

	/**
	 * This version handles per-element calver date updates for when the pin and schema elements are the same
	 * @param v
	 * @param schemaElement
	 * @param ae
	 * @param oldV
	 */
	private static void resolveDatesAsCurrentForNewVersion (Version v, VersionElement schemaElement,
			ActionEnum ae, Version oldV) {
		ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
		switch (schemaElement) {
		case YYYY:
		case YY:
		case OY:
			if (ae != ActionEnum.BUMP_PATCH && null != v.year) v.year = date.getYear();
			break;
		case MM:
		case OM:
			if (ae != ActionEnum.BUMP_PATCH && null != v.month) v.month = date.getMonth().getValue();
			break;
		case DD:
		case OD:
			if (ae != ActionEnum.BUMP_PATCH && null != v.day) v.day = date.getDayOfMonth();
			break;
		case BRANCH:
			v.branch = oldV.branch;
		default:
			break;
		}
	}

	/** This method checks if we had any updated calver components on the new version - and if yes, resets semver components to 0.
	 *  It also normalizes years, months and days for comparison.
	 * @param v
	 * @param elsProtectedByPin
	 * @param ae
	 * @param oldV
	 * @param schemaVeList
	 */
	private static void handleCalverOnSemverUpdates (Version v, Set<VersionElement> elsProtectedByPin,
		ActionEnum ae, Version oldV, List<VersionElement> schemaVeList) {

		if (ae == ActionEnum.BUMP_PATCH && !elsProtectedByPin.contains(VersionElement.PATCH)) {
			++v.patch;
			v.nano = 0;
		} else if (isCalverUpdated(v, oldV)) {
			// calver update happened, reset semver if not pinned
			if (!elsProtectedByPin.contains(VersionElement.MINOR)) v.minor = 0;
			if (!elsProtectedByPin.contains(VersionElement.MAJOR)) v.major = 0;
			if (!elsProtectedByPin.contains(VersionElement.PATCH)) v.patch = 0;
			if (!elsProtectedByPin.contains(VersionElement.NANO)) v.nano = 0;
		} else if (ae == ActionEnum.BUMP_MAJOR && !elsProtectedByPin.contains(VersionElement.MAJOR)) {
			++v.major;
			v.minor = 0;
			v.patch = 0;
			v.nano = 0;
		} else if (ae == ActionEnum.BUMP_MINOR && !elsProtectedByPin.contains(VersionElement.MINOR)) {
			++v.minor;
			v.patch = 0;
			v.nano = 0;
		} else if (ae != null && !elsProtectedByPin.contains(VersionElement.PATCH)) {
			++v.patch;
			v.nano = 0;
		} else if (oldV != null) {
			// if everything is pinned but nano, bump nano, else do simple bump if old version present
			Set<VersionElement> schemaSetWithoutNano = new HashSet<VersionElement>();
			schemaSetWithoutNano.addAll(schemaVeList);
			schemaSetWithoutNano.remove(VersionElement.NANO);
			if (elsProtectedByPin.containsAll(schemaSetWithoutNano) 
					 && schemaVeList.contains(VersionElement.NANO)) {
				++v.nano;
			} else {
				resolveModifierMetadataUpdate(v, oldV);
			}
		}
	}

	private static void resolveModifierMetadataUpdate (Version v, Version oldV) {
		if (StringUtils.isEmpty(v.modifier)) {
			v.setModifier("1");
		} else if (isInteger(v.modifier)) {
			Integer i = Integer.parseInt(v.modifier) + 1;
			v.setModifier(i.toString());
		} else if (isInteger(oldV.metadata)) {
			if (StringUtils.isEmpty(v.metadata) || !isInteger(v.metadata)) {
				v.setMetadata("1");
			} else {
				Integer i = Integer.parseInt(v.metadata) + 1;
				v.setMetadata(i.toString());
			}
		} else {
			v.simpleBump();
		}
	}

	private static boolean isInteger(String s) {
		return s.matches("\\d+");
	}

	/**
	 * If oldVersionString is present and we're dealing with calver schema, this effectively does simple bump relative to old version date based on schema and pin
	 * if it's semver schema and old version present, this will effectively return old version making sure it's matching schema and pin
	 * if old version is not present, this will do simple bump relative to semver
	 * @param schema String, required
	 * @param pin String, required
	 * @param oldVersionString String, optional
	 * @param ae ActionEnum
	 * @return Version object which represents product of schema if pin if old version is not present, or otherwise simply bump relative to old version
	 */
	public static Version getVersionFromPinAndOldVersion (String schema, String pin, String oldVersionString, ActionEnum ae) {
		validateGetVersionFromPinAndOldVersionInput(schema, pin, oldVersionString);
		Version v = new Version();
		v.schema = schema;
		Optional<VersionType> ovt = VersionType.resolveByAliasName(schema);
		if (ovt.isPresent()) schema = ovt.get().getSchema();
		Version oldV = null;
		if (StringUtils.isNotEmpty(oldVersionString)) oldV = Version.getVersion(oldVersionString, schema);
		
		List<ParsedVersionElement> schemaPveList = VersionUtils.parseSchema(schema);
		List<VersionElement> schemaVeList = schemaPveList.stream().map(x -> x.ve()).toList();
		ae = resolveNewVersionAction(schemaVeList, ae, oldVersionString);
		
		Optional<VersionType> ovtpin = VersionType.resolveByAliasName(pin);
		if (ovtpin.isPresent()) pin = ovtpin.get().getSchema();

		initializeVersionElements(v, oldV, ae);
		populateNewVersionFromOldVersion(v, oldVersionString, schema);

		// Parse pin to make sure we can do bump actions properly
		Optional<VersionHelper> ovh = VersionUtils.parseVersion(pin, schema, true);
		
		// this would be set of unmodifiable elements since they are set by pin
		Set<VersionElement> elsProtectedByPin = new HashSet<>(); 
		// even though dates are not bumped below, add them to set to know when to bump nano
		for (int i=0; i<ovh.get().getVersionComponents().size(); i++) {
			VersionElement parsedVe = VersionElement.getVersionElement(ovh.get().getVersionComponents().get(i).representation());
			if (parsedVe != schemaVeList.get(i)) {
				constructVersionElementForUpdatedElement(elsProtectedByPin, v, schemaVeList.get(i), 
					ovh.get().getVersionComponents().get(i).representation);
			} else if (schemaVeList.get(i) == VersionElement.CALVER_MODIFIER) {
				v.modifier = Constants.BASE_MODIFIER;
			} else {
				// pin matches schema and we need to resolve dates as current if present
				resolveDatesAsCurrentForNewVersion(v, schemaVeList.get(i), ae, oldV);
			}
		}
		
		if (StringUtils.isEmpty(v.metadata) && ovh.isPresent()) v.metadata = ovh.get().metadata;
		if (StringUtils.isEmpty(v.modifier) && ovh.isPresent()) v.modifier = ovh.get().modifier;

		handleCalverOnSemverUpdates(v, elsProtectedByPin, ae, oldV, schemaVeList);

		return v;
	}
	
	
	/** 
	 * @param v
	 * @param oldV
	 * @return boolean
	 */
	private static boolean isCalverUpdated (final Version v, final Version oldV) {
		boolean calverUpdated = false;
		if (null != oldV && null != oldV.year && null != v.year) {
			if (oldV.year.toString().length() == 2 && v.year.toString().length() == 4) {
				Integer newYearNormalized = Integer.parseInt(v.year.toString().substring(2));
				if (newYearNormalized > oldV.year) {
					calverUpdated = true;
				}
			} else {
				calverUpdated = (v.year > oldV.year);
			}
		}
		if (!calverUpdated && null != oldV && null != oldV.month && null != v.month && v.month > oldV.month) {
			calverUpdated = true;
		}
		if (!calverUpdated && null != oldV && null != oldV.day && null != v.day && v.day > oldV.day) {
			calverUpdated = true;
		}
		return calverUpdated;
	}
	
	/**
	 * This methods compares any 2 integer values between version objects
	 * Used to generalize comparisons for compareTo method
	 * @param i1 Integer
	 * @param i2 Integer
	 * @return 1 if i1 is larger or i2 is null, -1 if i2 is larger or i1 is null, or 0 if both are null or equal
	 */
	private int compareVersionIntegers (Integer i1, Integer i2) {
		if (null != i1 && null == i2) {
			return 1;
		} else if (null == i1 && null != i2) {
			return -1;
		} else if (null != i1 && i1 > i2) {
			return 1;
		} else if (null != i1 && i2 > i1) {
			return -1;
		} else {
			return 0;
		}
	}
	
	
	/** 
	 * @return int
	 */
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
		if (null != nano) {
			sb.append(nano.toString());
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
	
	
	/** 
	 * @param other Object
	 * @return boolean
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof Version) {
			return hashCode() == other.hashCode();
		} else {
			return false;
		}
	}
	
	/**
	 * Compares two version objects numerically by version component.
	 * @param otherV other version to compare to {@code this} version
	 * @return a negative integer, zero, or a positive integer as this Version is greater than, equal to, or less than the specified Version.
	 */
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
		if (0 == comparison) {
			comparison = compareVersionIntegers(nano, otherV.nano);
		}
		if (0 == comparison && StringUtils.isNotEmpty(buildid) && StringUtils.isNotEmpty(otherV.buildid)) {
			try {
				comparison = compareVersionIntegers(Integer.parseInt(buildid), Integer.parseInt(otherV.buildid));
			} catch (NumberFormatException nfe) {}
		}
		return -comparison;
	}
	
	/**
	 * This class is used to compare any 2 version strings based on common schema
	 * @author pavel
	 *
	 */
	public static class VersionStringComparator implements Comparator<String> {

		private String schema;
		
		/**
		 * Can be used to sort a collection in descending order. Latest version will
		 * be first/beginning of list. Versions that do not match the specified schema
		 * will be moved to the bottom/end of the collection.
		 * 
		 * @param schema {@code String} Versions must match this schema to be sorted
		 */
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

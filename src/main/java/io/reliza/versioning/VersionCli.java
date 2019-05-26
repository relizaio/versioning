/**
* Copyright 2019 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

public class VersionCli {

	public static void main(String[] args) {
		// create Options object
		final Options options = new Options();

		// add options
		Option action = Option.builder("a")
							    .longOpt( "action" )
							    .desc( "action for auto-increment, default is no auto-increment"  )
							    .hasArg()
							    .argName( "None|Bump|BumpMinor|BumpMajor|BumpDate" )
							    .build();
		
		Option snapshot = Option.builder("t")
			    .longOpt( "snapshot" )
			    .desc( "set maven style snapshot, none = keep as is (default), true = mark snapshot"
			    		+ ", false = mark non-snapshot"  )
			    .hasArg()
			    .argName( "None|True|False" )
			    .build();
		
		options.addOption("h", "help", false, "display this help page");
		options.addOption("s", "schema", true, "schema to use");
		options.addOption("v", "version", true, "current version, will generate baseline if empty");
		options.addOption("i", "modifier", true, "desired version modifier, also called identifier" +
												 ", must be supported by schema");
		options.addOption("m", "metadata", true, "version metadata, must be supported by schema");
		options.addOption("d", "date", true, "sets date for calver versions, use UTC timezone in YYYY-MM-DD format");
		options.addOption("r", "semver", true, "sets to specific semver version, use Major.Minor.Patch format");
		
		
		options.addOption(action);
		options.addOption(snapshot);
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse( options, args);
			
			if (cmd.hasOption("help")) {
				// automatically generate the help statement
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "versioncli", options, true);
			} else {
				// make sure schema is set
				String schema = cmd.getOptionValue("s");
				if (StringUtils.isEmpty(schema)) {
					System.out.println("Schema is required. Please specify schema using -s argument");
					System.exit(1);
				}
				String modifier = cmd.getOptionValue("i");
				String metadata = cmd.getOptionValue("m");
				String version = cmd.getOptionValue("v");
				Version v = null;
				if (StringUtils.isEmpty(version)) {
					v = VersionUtils.initializeVersionWithModMeta(schema, modifier, metadata);
				} else {
					v = new Version(version, schema);
					if (StringUtils.isNotEmpty(modifier)) {
						// if empty we might want to use the one from current version
						v.setModifier(modifier);
					}
					v.setMetadata(metadata);
				}
				String actionStr = cmd.getOptionValue("a");
				
				if ("bump".equalsIgnoreCase(actionStr)) {
					v.simpleBump();
				} else if ("bumpminor".equalsIgnoreCase(actionStr)) {
					v.bumpMinor(null);
				} else if ("bumpmajor".equalsIgnoreCase(actionStr)) {
					v.bumpMajor(null);
				} else if ("bumpdate".equalsIgnoreCase(actionStr)) {
					v.setCurrentDate();
				}
				
				String snapshotStr = cmd.getOptionValue("t");
				
				if ("true".equalsIgnoreCase(snapshotStr)) {
					v.setSnapshot(true);
				} else if ("false".equalsIgnoreCase(snapshotStr)) {
					v.setSnapshot(false);
				}

				String date = cmd.getOptionValue("d");
				if (StringUtils.isNotEmpty(date)) {
					LocalDate ld = LocalDate.parse(date);
					ZonedDateTime zdate = ZonedDateTime.of(ld, LocalTime.parse("05:00"), (ZoneId.of("UTC")));
					v.setDate(zdate);
				}
					
				String semver = cmd.getOptionValue("r");
				if (StringUtils.isNotEmpty(semver)) {
					Version semverVer = new Version(semver, Constants.SEMVER);
					v.setMajor(semverVer.getMajor());
					v.setMinor(semverVer.getMinor());
					v.setPatch(semverVer.getPatch());
				}
				System.out.println(v.constructVersionString());
			}
		} catch (Exception e) {
			System.out.println("unrecoverable error: " + e);
			System.exit(2);
		}
		System.exit(0);

	}

}

/**
* Copyright 2019 - 2020 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import io.reliza.changelog.CommitParserUtil;
import io.reliza.changelog.ConventionalCommit;
import io.reliza.versioning.VersionApi.ActionEnum;
import io.reliza.versioning.VersionApi.VersionApiObject;

/**
 * This class is used to define command-line interface of Reliza Versioning tool
 *
 */
public class VersionCli {

	/**
	 * Main method for CLI input processing
	 * @param args Command Line args
	 */
	public static void main(String[] args) {
		// create Options object
		final Options options = new Options();

		// add options
		Option action = Option.builder("a")
							    .longOpt( "action" )
							    .desc( "action for auto-increment, default is no auto-increment"  )
							    .hasArg()
							    .argName( "None|Bump|BumpMinor|BumpMajor|BumpDate|BumpPatch" )
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
		options.addOption("e", "cienv", true, "value of ci environment field of the version");
		options.addOption("b", "cibuild", true, "value of ci build field of the version");
		options.addOption("n", "branch", true, "value of branch field of the version");
		
		options.addOption("c", "commit", true, "parse commit message and bump version according to" +
											   " Conventional Commits specificaiton.");
		
		options.addOption(action);
		options.addOption(snapshot);
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse( options, args);
			
			if (cmd.hasOption("help")) {
				// automatically generate the help statement
				HelpFormatter formatter = new HelpFormatter();
				formatter.setWidth(250);
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
				String semver = cmd.getOptionValue("r");
				String cienv = cmd.getOptionValue("e");
				String cibuild = cmd.getOptionValue("b");
				String branch = cmd.getOptionValue("n");

				
				VersionApiObject vao = VersionApi.createVao(schema);
				vao.setVersion(version);
				vao.setModifier(modifier);
				vao.setMetadata(metadata);
				
				Version v = VersionApi.initializeVersion(vao);
				
				if (StringUtils.isNotEmpty(cienv)) {
					v.setBuildenv(cienv);
				}
				
				if (StringUtils.isNotEmpty(cibuild)) {
					v.setBuildenv(cibuild);
				}
				
				if (StringUtils.isNotEmpty(branch)) {
					v.setBranch(branch);
				}
				
				if (StringUtils.isNotEmpty(semver)) {
					VersionApi.setSemVerElementsOnVersion(v, semver);
				}
				
				String actionStr = cmd.getOptionValue("a");
				ActionEnum ae = null;
				if (StringUtils.isNotEmpty(actionStr)) {
					try {
						ae = ActionEnum.getActionEnum(actionStr.toLowerCase());
					} catch (Exception e) {}
				} else {
					ae = ActionEnum.BUMP;
				}
				if (null != ae && StringUtils.isNotEmpty(schema) && StringUtils.isNotEmpty(version)) {
					v = Version.getVersionFromPinAndOldVersion(schema, schema, version, ae);
				} else if (StringUtils.isNotEmpty(actionStr)) {
					VersionApi.applyActionOnVersion(v, actionStr);
				}
				
				String rawCommitStr = cmd.getOptionValue("c");
				//System.out.println("Bumped based on parsing of commit:\n" + rawCommitStr + "\n");
				// Only want to bump from commit, if have not bumped from action yet.
				if (rawCommitStr != null && actionStr == null) {
					try {
						ConventionalCommit parsedCommit = CommitParserUtil.parseRawCommit(rawCommitStr);
						//System.out.println("breaking: " + parsedCommit.isBreakingChange());
						//System.out.println("type: " + parsedCommit.getType());
						ActionEnum actionToTake = VersionApi.getActionFromConventionalCommit(parsedCommit);
						if (actionToTake == null) {
							System.out.println("No need to change version based on commit message contents.");
						} else {
							VersionApi.applyActionOnVersion(v, actionToTake);
						}
					} catch (IllegalArgumentException e) {
						System.out.println(e.getMessage());
					}
				}
				
				String snapshotStr = cmd.getOptionValue("t");
				
				if ("true".equalsIgnoreCase(snapshotStr)) {
					VersionApi.setMavenSnapshotStatus(v, true);
				} else if ("false".equalsIgnoreCase(snapshotStr)) {
					VersionApi.setMavenSnapshotStatus(v, false);
				}

				String date = cmd.getOptionValue("d");
				if (StringUtils.isNotEmpty(date)) {
					VersionApi.setVersionDateFromString(v, date);
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

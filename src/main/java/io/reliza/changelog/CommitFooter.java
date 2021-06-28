/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.changelog;

/**
 * See Conventional Commits Specfication 8-10 for more details on footer structure.
 * https://www.conventionalcommits.org/en/v1.0.0/#specification
 * 
 * Regex for Conventional Commits Footer (single footer)
 * "^(?<token>[\w\-]+|BREAKING CHANGE)(?<seperator>: | #)(?<value>.*)"(gm for multiple)
 * Regex for multiple footers (also supports multi line values)
 * "^(?<token>[\w\-]+|BREAKING CHANGE)(?<seperator>: | #)(?<value>.*?(?=^([\w\-]+|BREAKING CHANGE)(: | #)))"gms
 */
public final class CommitFooter implements BreakingChangeItem{
    public static final CommitFooter EMPTY = new CommitFooter("");
    private final boolean isBreakingChange;
    private final String breakingChangeDescription;
    private final String footer;

    public CommitFooter(String footer) {
    	// Need to check every line of footer for BREAKING-CHANGE or BREAKING CHANGE token
        String[] footerLines = footer.split(System.lineSeparator());
        boolean breakingChange = false;
        String breakingChangeLine = "";
        for (String line : footerLines) {
        	if (line.startsWith("BREAKING-CHANGE: ") || line.startsWith("BREAKING CHANGE: ")) {
        		breakingChange = true;
        		breakingChangeLine = line;
        	}
        }
    	this.isBreakingChange = breakingChange;
    	// Why trim footer?
        //this.footer = isBreakingChange ? footer.substring("BREAKING-CHANGE: ".length()).trim() : footer.trim();
    	this.footer = footer;
    	this.breakingChangeDescription = isBreakingChange ? breakingChangeLine.substring("BREAKING-CHANGE: ".length()).trim() : "";
    }

    public String getFooter() {
        return footer;
    }

    public boolean isBreakingChange() {
        return isBreakingChange;
    }

    public String getBreakingChangeDescription() {
        //return isBreakingChange ? footer : "";
    	return this.breakingChangeDescription;
    }
}

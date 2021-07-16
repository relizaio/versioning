/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.joining;



public final class CommitParserUtil {
    public static final Pattern COMMIT_MESSAGE_REGEX = createRegexPattern();
    //public static final String LINE_SEPARATOR = "\n\n";
    public static final String LINE_SEPARATOR_REGEX = "(?:\\r\\n|\\n)";

    private CommitParserUtil() {
    }

    private static Pattern createRegexPattern() {
        // i.e. ^(build|test|chore|feat|fix|docs)
        String typePrefix = Arrays.stream(CommitType.values()).map(CommitType::getPrefix).collect(joining("|", "^(", ")"));
        // Make regex case insensitive, so capitlized types can be parsed: ex: "FIX: a commit message" is a valid commit header
        return Pattern.compile(typePrefix + "[(]?([\\w\\-]+)?[)]?(!)?:\\s(.+)", Pattern.CASE_INSENSITIVE);
    }
    
    /**
     * Given a string containing a raw commit message, this function will parse the message
     * into its header, body and footer components (may not contain body or footer),
     * and return a ConventionalCommit object representing the commit message.
     * 
     * If the commit message passed does not meet the Convetional Commit specification, then
     * an IllegalArgumentException will be thrown.
     * 
     * @param rawCommitMessage String containing the raw commit message.
     * @return A ConventionalCommit object representing the raw commit.
     * @throws IllegalArgumentException thrown if the raw commit message passed does not meet the conventional commit specification.
     */
    public static ConventionalCommit parseRawCommit(String rawCommitMessage) throws IllegalArgumentException {
    	if (StringUtils.isBlank(rawCommitMessage)) {
    		// throw if input rawCommitMessage is empty, null or only whitespace
    		throw new IllegalArgumentException("Cannot parse empty/null commit message.");
    	}
    	// split to get first line as header, use negative limit to preserve trailing empty strings
    	String[] commitLines = rawCommitMessage.split(LINE_SEPARATOR_REGEX, -1);
    	String rawHeader = commitLines[0];
    	
    	// Check for body and footer
    	ArrayList<String> rawBody = new ArrayList<String>();
    	String rawFooter = "";
    	boolean inFooter = false;
    	
    	// Check if commit has more than one line, ie not just header
    	if (commitLines.length > 2) {
    		// First line after header must be blank line if body or footer follows
    		if (commitLines[1] == "") {
    			// First line after is blank: set as previousLine
    	    	String previousLine = commitLines[1];
    			// search for git trailer with following regex to see if we have footer
        		// ^(?<token>[\w\-]+|BREAKING CHANGE)(?<seperator>: | #)
        		for (int i = 2; i < commitLines.length; i++) {
        			String currentLine = commitLines[i];
        			// Check if current line is git trailer, if we have not found footer yet
        			if (inFooter == false && currentLine.matches("^(?:([\\w\\-]+)(: | #)|(BREAKING CHANGE: ))(.*)")) {
        				if (previousLine == "") {
        					// Then current line is first git trailer (ie first footer line)
            				inFooter = true;
        				} else {
        					// Must have blank line before footer
        					throw new IllegalArgumentException("Commit message does not meet conventional commit specification. " +
									   "Must have blank line before footer section.");
        				}
        			}
        			// Add line to body arraylist, unless we are in the footer section
        			if (inFooter) {
        				// Make sure line is in proper form for footer
        				if (currentLine.matches("^(?:([\\w\\-]+)(: | #)|(BREAKING CHANGE: ))(.*)")) {
            				rawFooter = rawFooter + currentLine + System.lineSeparator();
        				} else if (currentLine.equals("")) {
        					// ignore blank lines in footer, so do nothing and skip to next commitLine
        				} else {
        					throw new IllegalArgumentException("Commit message does not meet conventional commit specification. " + 
     							   							   "Lines in footer must follow git trailer convention.");
        				}
        			} else {
        				rawBody.add(currentLine);
        			}
        			
        			// Set current line (i) as previousLine before going to next iteration
        			previousLine = currentLine;
        		}
    		} else {
    			// Conventional Commit message should have a blank line before body and footer sections
    			throw new IllegalArgumentException("Commit message does not meet conventional commit specification. " + 
    							   "Conventional Commit message should have a blank line before body and footer sections");
    		}
    		
    	} else if (commitLines.length == 2) {
    		// Conventional Commit should not be just two lines. Needs to be 1 or at least 3.
			throw new IllegalArgumentException("Commit message does not meet conventional commit specification. " +
							   "Conventional Commit should not be just two lines. Needs to be 1 or at least 3.");
    	} else {
    		// body and footer are null
    	}
		// Trim trailing blank lines of body
    	for (int i = rawBody.size()-1; i >= 0; i--) {
    		if (rawBody.get(i) == "") {
    			rawBody.remove(i);
    		} else {
    			// Once we reach first non blank line, stop loop.
    			break;
    		}
    	}
    	
    	// Construct commit objects from raw strings
    	CommitMessage commitMessage;
    	// Should maybe catch this exception directly in CommitMessage->getType() method
    	// Problem is passing invalid commit messages, where to catch errors with conventional commit specs?
    	try {
    		commitMessage = new CommitMessage(rawHeader);
    	} catch (IllegalStateException e) {
    		throw new IllegalArgumentException("Commit does not meet convnetional commit specification. " + 
    						   				   "Threw error when attemping to create CommitMessage object: " + e);
    	}
    	
    	CommitBody commitBody = null;
    	if (!rawBody.isEmpty()) {
    		String[] rawBodyArray = new String[rawBody.size()];
    		rawBody.toArray(rawBodyArray);
    		commitBody = new CommitBody(rawBodyArray);
    	}
    	CommitFooter commitFooter = null;
    	if (rawFooter != "") {
    		commitFooter = new CommitFooter(rawFooter);
    	}
    	
    	// Create new convnetional commit object with raw header, body and footer
    	ConventionalCommit commit;
    	if (commitBody == null && commitFooter == null) {
    		// no body or footer
    		commit = new ConventionalCommit(commitMessage);
    	} else if (commitBody == null && commitFooter != null) {
    		// no body, yes footer
    		commit = new ConventionalCommit(commitMessage, commitFooter);
    	} else if (commitBody != null && commitFooter == null) {
    		// yes body, no footer
    		commit = new ConventionalCommit(commitMessage, commitBody);
    	} else {
    		// yes body and footer
    		commit = new ConventionalCommit(commitMessage, commitBody, commitFooter);
    	}
    	
    	return commit;
    }
    
    /**
     * WIP do not use this method. Use parseRawCommit(String rawCommitMessage) instead.
     * This is not guaranteed to parse commits correctly at the moment.
     * 
     * @param rawCommitMessage
     * @return
     * @throws IllegalArgumentException
     */
    public static ConventionalCommit parseRawCommitRegex(String rawCommitMessage) throws IllegalArgumentException {
    	if (rawCommitMessage == null || StringUtils.isBlank(rawCommitMessage)) {
    		throw new IllegalArgumentException("Please provide non-empty/non-null commit message.");
    	}
    	String LS = false ? System.lineSeparator() : LINE_SEPARATOR_REGEX;
    	String fullCommitRegex = "(?<header>^(?<type>fix|feat|perf|revert|refactor|build|test|docs|chore|ci|style)[(]?(?<scope>[\\w\\-]+)?[)]?(?<modifier>!)?: (?<desc>.+))(?<body>(?<spacer>"+LS+LS+")(?<bodyline>.+"+LS+"|"+LS+")*)?(?<footer>"+LS+"(?<trailer>^(?<token>[\\w\\-]+|BREAKING CHANGE)(?<seperator>: | #)(?<value>(?<valueline>.+"+LS+")*?(?=([\\w\\-]+|BREAKING CHANGE)(: |# ))))*(?<lasttoken>[\\w\\-]+|BREAKING CHANGE)(?<lastseperator>: | #)(?<lastvalue>(?<lastvalueline>.+|.+"+LS+")*))";
    	Pattern p = Pattern.compile(fullCommitRegex, Pattern.MULTILINE);
    	java.util.regex.Matcher m = p.matcher(rawCommitMessage);
    	
    	if (m.matches()) {
    		// Construct commit objects from raw strings
        	CommitMessage commitMessage;
        	CommitBody commitBody = null; // optional
        	CommitFooter commitFooter = null; // optional
        	ConventionalCommit commit;
        	// Should maybe catch this exception directly in CommitMessage->getType() method
        	// Problem is passing invalid commit messages, where to catch errors with conventional commit specs?
        	try {
        		commitMessage = new CommitMessage(m.group("header"));
        	} catch (IllegalStateException e) {
        		throw new IllegalArgumentException("Commit does not meet convnetional commit specification. " + 
        						   				   "Threw error when attemping to create CommitMessage object: " + e);
        	}
        	if (m.group("body") != null) {
        		String[] rawBodyArray = m.group("body").split(LINE_SEPARATOR_REGEX);
        		//rawBody.toArray(rawBodyArray);
        		commitBody = new CommitBody(rawBodyArray);
        	}
        	if (m.group("footer") != null) {
        		commitFooter = new CommitFooter(m.group("footer"));
        	}
        	
        	// Create new convnetional commit object with raw header, body and footer
        	if (commitBody == null && commitFooter == null) {
        		// no body or footer
        		commit = new ConventionalCommit(commitMessage);
        	} else if (commitBody == null && commitFooter != null) {
        		// no body, yes footer
        		commit = new ConventionalCommit(commitMessage, commitFooter);
        	} else if (commitBody != null && commitFooter == null) {
        		// yes body, no footer
        		commit = new ConventionalCommit(commitMessage, commitBody);
        	} else {
        		// yes body and footer
        		commit = new ConventionalCommit(commitMessage, commitBody, commitFooter);
        	}
        	
        	return commit;
    	}
    	// else if no match
    	throw new IllegalArgumentException("Not a valid conventional commit format.");
    }
    
}

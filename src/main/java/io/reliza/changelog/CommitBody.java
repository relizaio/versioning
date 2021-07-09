/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Body should not have breaking change indicator according to conventional commits specification.
 * Conventional Commits Specification:
 * 11. Breaking changes MUST be indicated in the type/scope prefix of a commit, or as an entry in the footer.
 * 
 */
public final class CommitBody implements BreakingChangeItem{
    private final Predicate<String> breakingChangePredicate = CommitBody::doesContainBreakingChange;
    public static final CommitBody EMPTY = new CommitBody(new String[0]);
    private final boolean isBreakingChange;
    private final List<String> body;

    public CommitBody(String[] rawBody) {
        this.body = Arrays.asList(rawBody);
        this.isBreakingChange = body.stream().anyMatch(breakingChangePredicate);
    }

    public boolean isBreakingChange() {
        return isBreakingChange;
    }

    public String getBreakingChangeDescription() {
        return getBody();
    }

    public String getBody() {
        return isBreakingChange
            ? getBreakingChangeText()
            : ""; // body is not required unless breaking change
    }
    
    public String getRawBody() {
    	String rawBody = "";
    	for (String line : body) {
    		rawBody += line + System.lineSeparator();
    	}
    	return rawBody;
    }

    private static boolean doesContainBreakingChange(String string) {
        return string.contains("BREAKING-CHANGE:") || string.contains("BREAKING CHANGE");
    }

    private String getBreakingChangeText() {
        for (String line : body) {
            if (breakingChangePredicate.test(line)) {
                return line;
            }
        }
        return "";
    }
}

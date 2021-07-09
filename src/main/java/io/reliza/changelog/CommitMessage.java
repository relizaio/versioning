/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

import java.util.regex.Matcher;
import static io.reliza.changelog.CommitParserUtil.COMMIT_MESSAGE_REGEX;
//import static io.reliza.common.Utils.getNullable;

public final class CommitMessage implements BreakingChangeItem{
    private final String rawMessage;
    private final CommitType type;
    private final String message;
    private final String scope;
    private final boolean isBreakingChange;
  
    public CommitMessage(String rawMessage) {
        this.rawMessage = rawMessage.trim();
        ConventionalCommitMatcher matcher = new ConventionalCommitMatcher(this.rawMessage);
        this.type = matcher.getType();
        this.message = matcher.getMessage();
        this.scope = matcher.getScope();
        this.isBreakingChange = matcher.isBreakingChange();
    }
  
    public String getRawMessage() {
        return rawMessage;
    }
  
    public CommitType getType() {
        return type;
    }
  
    public String getMessage() {
        return message;
    }
  
    public String getScope() {
        return scope;
    }
  
    public boolean isBreakingChange() {
        return isBreakingChange;
    }
  
    public String getBreakingChangeDescription() {
        return isBreakingChange ? message : "";
    }
  
    private static class ConventionalCommitMatcher {
        private final Matcher matcher;
    
        public ConventionalCommitMatcher(String rawString) {
            this.matcher = COMMIT_MESSAGE_REGEX.matcher(rawString);
            this.matcher.find();
        }
    
        CommitType getType() {
            return CommitType.of((matcher.group(1).toLowerCase()));
        }
    
        String getScope() {
            return getNullable(matcher.group(2));
        }
    
        boolean isBreakingChange() {
            return matcher.group(3) != null;
        }
    
        String getMessage() {
            return getNullable(matcher.group(4));
        }
        
        public static String getNullable(String nullable) {
    		if (nullable == null || nullable.trim().isEmpty()) {
    			return "";
    		}
    		return nullable;
    	}
    }
}

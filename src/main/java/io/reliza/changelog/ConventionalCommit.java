/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

import java.util.stream.Stream;

/**
 * Class ConventionalCommit
 */
public final class ConventionalCommit {
    private final CommitMessage commitMessage;
    private final CommitBody commitBody;
    private final CommitFooter commitFooter;

    /**
     * Constructs ConventionalCommit object
     * @param commitMessage CommitMessage 
     * @param commitBody CommitBody 
     * @param commitFooter CommitFooter 
     */
    public ConventionalCommit(CommitMessage commitMessage, CommitBody commitBody, CommitFooter commitFooter) {
        this.commitMessage = commitMessage;
        this.commitBody = commitBody;
        this.commitFooter = commitFooter;
    }

    /**
     * Constructs ConventionalCommit object
     * @param commitMessage CommitMessage 
     */
    public ConventionalCommit(CommitMessage commitMessage) {
        this(commitMessage, CommitBody.EMPTY, CommitFooter.EMPTY);
    }

    /**
     * Constructs ConventionalCommit object
     * @param commitMessage CommitMessage 
     * @param commitFooter CommitFooter 
     */
    public ConventionalCommit(CommitMessage commitMessage, CommitFooter commitFooter) {
        this(commitMessage, CommitBody.EMPTY, commitFooter);
    }

    /**
     * Constructs ConventionalCommit object
     * @param commitMessage CommitMessage 
     * @param commitBody CommitBody 
     */
    public ConventionalCommit(CommitMessage commitMessage, CommitBody commitBody) {
    	this(commitMessage, commitBody, CommitFooter.EMPTY);
    }

    
    /** 
     * @return String
     */
    public String getFooter() {
        return commitFooter.getFooter();
    }

    
    /** 
     * @return String
     */
    public String getBody() {
        return commitBody.getBody();
    }
    
    
    /** 
     * @return String
     */
    public String getRawBody() {
    	return commitBody.getRawBody();
    }

    
    /** 
     * @return String
     */
    public String getRawMessage() {
        return commitMessage.getRawMessage();
    }

    
    /** 
     * @return CommitType
     */
    public CommitType getType() {
        return commitMessage.getType();
    }

    
    /** 
     * @return String
     */
    public String getDecoratedScope() {
        return "**" + getScope() + "**";
    }

    
    /** 
     * @return String
     */
    public String getMessage() {
        return commitMessage.getMessage();
    }

    
    /** 
     * @return String
     */
    public String getScope() {
        return commitMessage.getScope();
    }

    
    /** 
     * @return boolean
     */
    public boolean isBreakingChange() {
        return commitMessage.isBreakingChange()
            || commitBody.isBreakingChange()
            || commitFooter.isBreakingChange();
    }

    
    /** 
     * @return String
     */
    public String getBreakingChangeDescription() {
        return Stream.of(commitFooter, commitBody, commitMessage)
            .filter(BreakingChangeItem::isBreakingChange)
            .map(BreakingChangeItem::getBreakingChangeDescription)
            .map(string -> string.replace("BREAKING-CHANGE: ", ""))
            .map(string -> string.replace("BREAKING CHANGE: ", ""))
            .findFirst()
            .orElse("");
    }
    
    
    /** 
     * @return String
     */
    public String toString() {
    	return "Header:\n" + this.getRawMessage() + 
    		   "\nBody:\n" + this.getBody() + 
    		   "\nFooter:\n" + this.getFooter();
    }
    
}


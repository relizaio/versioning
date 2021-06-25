/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

import java.util.stream.Stream;

public final class ConventionalCommit {
    private final CommitMessage commitMessage;
    private final CommitBody commitBody;
    private final CommitFooter commitFooter;

    public ConventionalCommit(CommitMessage commitMessage, CommitBody commitBody, CommitFooter commitFooter) {
        this.commitMessage = commitMessage;
        this.commitBody = commitBody;
        this.commitFooter = commitFooter;
    }

    public ConventionalCommit(CommitMessage commitMessage) {
        this(commitMessage, CommitBody.EMPTY, CommitFooter.EMPTY);
    }

    public ConventionalCommit(CommitMessage commitMessage, CommitFooter commitFooter) {
        this(commitMessage, CommitBody.EMPTY, commitFooter);
    }
    
    public ConventionalCommit(CommitMessage commitMessage, CommitBody commitBody) {
    	this(commitMessage, commitBody, CommitFooter.EMPTY);
    }

    public String getFooter() {
        return commitFooter.getFooter();
    }

    public String getBody() {
        return commitBody.getBody();
    }

    public String getRawMessage() {
        return commitMessage.getRawMessage();
    }

    public CommitType getType() {
        return commitMessage.getType();
    }

    public String getDecoratedScope() {
        return "**" + getScope() + "**";
    }

    public String getMessage() {
        return commitMessage.getMessage();
    }

    public String getScope() {
        return commitMessage.getScope();
    }

    public boolean isBreakingChange() {
        return commitMessage.isBreakingChange()
            || commitBody.isBreakingChange()
            || commitFooter.isBreakingChange();
    }

    public String getBreakingChangeDescription() {
        return Stream.of(commitFooter, commitBody, commitMessage)
            .filter(BreakingChangeItem::isBreakingChange)
            .map(BreakingChangeItem::getBreakingChangeDescription)
            .map(string -> string.replace("BREAKING-CHANGE: ", ""))
            .map(string -> string.replace("BREAKING CHANGE: ", ""))
            .findFirst()
            .orElse("");
    }
    
    public String toString() {
    	return "Header:\n" + this.getRawMessage() + 
    		   "\nBody:\n" + this.getBody() + 
    		   "\nFooter:\n" + this.getFooter();
    }
    
}


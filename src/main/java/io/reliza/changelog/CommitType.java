/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

/**
 * enum CommitType describes possible types of commits
 */
public enum CommitType {
    /**
     * a commit that fixes a bug
     */
    BUG_FIX(0, "fix", "Bug Fixes"),
    
    /**
     * a commit that adds new functionality
     */
    FEAT(1, "feat", "Features"),
    
    /**
     * a commit that improves performance
     */
    PERFORMANCE(2, "perf", "Performance Improvements"),
    
    /**
     * a commit that reverts a previous commit
     */
    REVERT(3, "revert", "Reverts"),
    
    /**
     * a commit that neither fixes a bug nor adds a feature
     */    
    REFACTOR(4, "refactor", "Code Refactoring"),
    
    /**
     * a commit that affect the build system or external dependencies
     */    
    BUILD(5, "build", "Builds"),
    
    /**
     * a commit that adds missing tests or correcting existing tests
     */
    TEST(6, "test", "Tests"),
    
    /**
     * a commit that does documentation only changes
     */
    DOCS(7, "docs", "Documentation"),
    
    /**
     * a commit with changes that don't modify src or test files
     */
    CHORE(8, "chore", "Chores"),
    
    /**
     * a commit which changes CI configuration files or scripts
     */
    CI(9, "ci", "Continuous Integration"),
    
    /**
     * a commit that does style related changes
     */
    STYLE(10, "style", "Styles");

  
    private final int displayPriority;
    private final String prefix;
    private final String fullName;
  
  
    /**
     * Constructs CommitType enum
     * @param int displayPriority
     * @param String prefix
     * @param String fullName
     */
    CommitType(int displayPriority, String prefix, String fullName) {
        this.displayPriority = displayPriority;
        this.prefix = prefix;
        this.fullName = fullName;
    }
  
    /**
     * @return int displayPriority
     */
    public int getDisplayPriority() {
        return displayPriority;
    }
  
    /**
     * @return String fullName
     */
    public String getFullName() {
        return fullName;
    }
  
    /**
     * @return String prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Create CommitType enum from string value
     * @param value String
     * @return CommitType enum
     */
    public static CommitType of(String value) {
        for (CommitType type : CommitType.values()) {
            if (type.prefix.equals(value)) {
                return type;
            }
        }
        throw new IllegalStateException(
            value + " commit type is not supported by " + CommitType.class.getSimpleName());
    }
}
  

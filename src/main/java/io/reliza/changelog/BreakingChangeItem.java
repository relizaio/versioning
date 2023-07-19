/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

/**
 * This interface makes easy to identify if a commit is a breaking change
 */
public interface BreakingChangeItem {

    /** 
     * @return boolean
     */
	boolean isBreakingChange();

    /** 
     * @return String
     */
    String getBreakingChangeDescription();
}

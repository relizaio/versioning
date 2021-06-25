/**
* Copyright 2021 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/


package io.reliza.changelog;

public interface BreakingChangeItem {
	boolean isBreakingChange();

    String getBreakingChangeDescription();
}

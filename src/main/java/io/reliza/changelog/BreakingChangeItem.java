/**
* Copyright Reliza Incorporated. 2019 - 2021. All rights reserved.
*/

package io.reliza.changelog;

public interface BreakingChangeItem {
	boolean isBreakingChange();

    String getBreakingChangeDescription();
}

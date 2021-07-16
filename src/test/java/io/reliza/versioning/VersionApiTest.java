package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.reliza.versioning.VersionApi.ActionEnum;
import io.reliza.versioning.VersionApi.VersionApiObject;

class VersionApiTest {
	
	@Test
	void testApplyActionOnVersionFromCommitVersionString_SimpleCommitPatchBump() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		VersionApi.applyActionOnVersionFromCommit(v, "fix: simple commit message");
		assert v.constructVersionString().equals("1.0.1");
	}
	
	@Test
	void testApplyActionOnVersionFromCommitVersionString_BreakingChangeFooter() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		String rawCommit = "fix: simple commit message\r\n\r\nbody\r\n\r\nFooter: 1\r\nBREAKING CHANGE: 2";
		VersionApi.applyActionOnVersionFromCommit(v, rawCommit);
		System.out.println(rawCommit);
		System.out.println(v.constructVersionString());
		assert v.constructVersionString().equals("2.0.0");
	}
	
	@Test
	void testApplyActionOnVersionFromCommitVersionString_BreakingChangeFooter2() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		String rawCommit = "feat: fix\r\n"
				+ "\r\n"
				+ "BREAKING-CHANGE: 3\r\n"
				+ "\r\n"
				+ "";
		String[] splitCommit = rawCommit.split(System.lineSeparator(), -1);
		for (String s : splitCommit) {
			System.out.println(s);
		}
		VersionApi.applyActionOnVersionFromCommit(v, rawCommit);
		System.out.println(rawCommit);
		System.out.println(v.constructVersionString());
		assert v.constructVersionString().equals("2.0.0");
	}
	
	@Test
	void testApplyActionOnVersionFromCommitVersionString_IvalidRawCommitFormat() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			VersionApi.applyActionOnVersionFromCommit(v, "fix: simple commit message" + System.lineSeparator() + "");
		});
		String expectedMessage = "Commit message does not";
		String actualMessage = e.getMessage();
		assertTrue(actualMessage.contains(expectedMessage));
	}
	
	@Test
	void testApplyActionOnVersion_BumpBranchMicro() {
		VersionApiObject vao = VersionApi.createVao("Branch.Major.Micro");
		vao.setVersion("branch-name.1.1");
		Version v = VersionApi.initializeVersion(vao);
		ActionEnum action = ActionEnum.BUMP;
		VersionApi.applyActionOnVersion(v, action);
		String expectedVersion = "branch-name.1.2";
		String actualVersion = v.constructVersionString();
		System.out.println(actualVersion);
		assertEquals(expectedVersion, actualVersion);
	}
	
	@Test
    public void testApplyBumpMinorOnCalver() {
    	String schema = "YYYY.0M.Micro";
    	String version = "2021." + AppTest.CURRENT_MONTH + ".1";
		ActionEnum action = ActionEnum.BUMP_MINOR;
		VersionApiObject vao = VersionApi.createVao(schema);
		vao.setVersion(version);
		Version v = VersionApi.initializeVersion(vao);
		VersionApi.applyActionOnVersion(v, action);
		String actualV = v.constructVersionString();
		String expectedV = "2021." + AppTest.CURRENT_MONTH + ".2";
		assertEquals(expectedV, actualV);
    }
	
//	@Test
//    public void testApplyBumpMinorOnCalverLastMonth() {
//    	String schema = "YYYY.0M.Micro";
//    	String version = "2021.05.1";
//		ActionEnum action = ActionEnum.BUMP;
//		VersionApiObject vao = VersionApi.createVao(schema);
//		vao.setVersion(version);
//		Version v = VersionApi.initializeVersion(vao);
//		VersionApi.applyActionOnVersion(v, action);
//		String actualV = v.constructVersionString();
//		String expectedV = "2021." + AppTest.CURRENT_MONTH + ".0";
//		assertEquals(expectedV, actualV);
//    }
}

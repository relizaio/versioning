package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.reliza.versioning.VersionApi.ActionEnum;
import io.reliza.versioning.VersionApi.VersionApiObject;

class VersionApiTest {
	private static final String LS = System.lineSeparator();

	@Test
	void testApplyActionOnVersionFromCommitVersionString_SimpleCommitPatchBump() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		VersionApi.applyActionOnVersionFromCommit(v, "fix: simple commit message");
		assert v.constructVersionString().equals("1.0.1");
	}
	
	@Test
	@Disabled
	void testApplyActionOnVersionFromCommitVersionString_BreakingChangeFooter() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		String rawCommit = "fix: simple commit message"
				+LS + LS
				+"body"
				+LS + LS
				+"Footer: 1"
				+LS + LS
				+"BREAKING CHANGE: 2";

		VersionApi.applyActionOnVersionFromCommit(v, rawCommit);
		//System.out.println(rawCommit);
		//System.out.println(v.constructVersionString());
		assert v.constructVersionString().equals("2.0.0");
	}
	
	@Test
	@Disabled
	void testApplyActionOnVersionFromCommitVersionString_BreakingChangeFooter2() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		String rawCommit = "feat: fix" + LS
				+ LS
				+ "BREAKING-CHANGE: 3"+LS
				+ LS
				+ "";
		//String[] splitCommit = rawCommit.split(System.lineSeparator(), -1);
		//for (String s : splitCommit) {
		//	System.out.println(s);
		//}
		VersionApi.applyActionOnVersionFromCommit(v, rawCommit);
		//System.out.println(rawCommit);
		//System.out.println(v.constructVersionString());
		assert v.constructVersionString().equals("2.0.0");
	}
	
	@Test
	void testApplyActionOnVersionFromCommitVersionString_IvalidRawCommitFormat() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			VersionApi.applyActionOnVersionFromCommit(v, "fix: simple commit message" + LS + "");
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
		//System.out.println(actualVersion);
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
	
	@Test
	public void testGetBumpActionBetweenVersions_MinorBump() {
		String oldV = "1.0.1";
		String newV = "1.1.2";
		String schema = "Major.Minor.Patch";
		ActionEnum expectedAction = ActionEnum.BUMP_MINOR;
		ActionEnum actualAction = VersionApi.getBumpActionBetweenVersions(oldV, newV, schema);
		// even though patch also changes, Minor is a bigger change
		assertEquals(expectedAction, actualAction);
	}
	
	@Test
	public void testGetBumpActionBetweenVersions_MismatchingSchemas() {
		String oldV = "2021.1.0.0";
		String newV = "3.2.1";
		String schema = "YYYY.Major.Minor.micro";
		ActionEnum expectedAction = null;
		ActionEnum actualAction = VersionApi.getBumpActionBetweenVersions(oldV, newV, schema);
		// Should return null because versions have mismatching schemas.
		assertEquals(expectedAction, actualAction);
	}

	@Test
	public void testGetBumpActionBetweenVersions_MajorBump() {
		String oldV = "1.0.1";
		String newV = "3.1.2";
		String schema = "Major.Minor.Patch";
		ActionEnum expectedAction = ActionEnum.BUMP_MAJOR;
		ActionEnum actualAction = VersionApi.getBumpActionBetweenVersions(oldV, newV, schema);
		// even though patch and minor also change, major is a bigger change
		assertEquals(expectedAction, actualAction);
	}
	
	@Test
	public void testGetBumpActionBetweenVersions_MajorBumpWithCalver() {
		String oldV = "2020.04.3.2";
		String newV = "2021.05.4.5";
		String schema = "YYYY.0M.Major.Minor";
		ActionEnum expectedAction = ActionEnum.BUMP_MAJOR;
		ActionEnum actualAction = VersionApi.getBumpActionBetweenVersions(oldV, newV, schema);
		// although all elements change, largest applicable element change is Major
		assertEquals(expectedAction, actualAction);
	}
	
	@Test
	public void testGetBumpActionBetweenVersions_NoApplicableChange() {
		String oldV = "2020.04.3.2.1";
		String newV = "2021.05.3.2.1";
		String schema = "YYYY.0M.Major.Minor.Patch";
		ActionEnum expectedAction = null;
		ActionEnum actualAction = VersionApi.getBumpActionBetweenVersions(oldV, newV, schema);
		// although date elements change, they are not relevant for this method
		assertEquals(expectedAction, actualAction);
	}
	
	@Test
	public void testGetBumpActionBetweenVersions_NullOldVersion() {
		String oldV = null;
		String newV = "2021.05.3.2.1";
		String schema = "YYYY.0M.Major.Minor.Patch";
		Throwable exception = assertThrows(NullPointerException.class, () -> VersionApi.getBumpActionBetweenVersions(oldV, newV, schema));
		assertEquals("Old version must not be null", exception.getMessage());
	}
	
	@Test
	public void testGetBumpActionBetweenVersions_NullSchema() {
		String oldV = "2020.04.3.2.1";
		String newV = "2021.05.3.2.1";
		String schema = null;
		Throwable exception = assertThrows(NullPointerException.class, () -> VersionApi.getBumpActionBetweenVersions(oldV, newV, schema));
		assertEquals("Schema must not be null", exception.getMessage());
	}
	
	@Test
	public void testNullInput() {
		assertThrows(NullPointerException.class, () -> VersionApi.applyActionOnVersion(null, ActionEnum.BUMP_MAJOR));
	}
}

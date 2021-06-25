package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.reliza.versioning.VersionApi.VersionApiObject;

class VersionApiTest {

	@Test
	void testCreateVao() {
		fail("Not yet implemented");
	}

	@Test
	void testInitializeVersion() {
		fail("Not yet implemented");
	}

	@Test
	void testInitializeSemVerVersion() {
		fail("Not yet implemented");
	}

	@Test
	void testApplyActionOnVersionVersionActionEnum() {
		fail("Not yet implemented");
	}

	@Test
	void testSetSemVerElementsOnVersion() {
		fail("Not yet implemented");
	}

	@Test
	void testApplyActionOnVersionVersionString() {
		fail("Not yet implemented");
	}

	@Test
	void testGetActionFromConventionalCommit() {
		fail("Not yet implemented");
	}

	@Test
	void testGetActionFromRawCommit() {
		VersionApi.getActionFromRawCommit("fix: simple commit message");
		fail("Not yet implemented");
	}

	@Test
	void testApplyActionOnVersionFromCommitVersionConventionalCommit() {
		fail("Not yet implemented");
	}

	@Test
	void testApplyActionOnVersionFromCommitVersionString_SimpleCommitPatchBump() {
		VersionApiObject vao = VersionApi.createVao("semver");
		vao.setVersion("1.0.0");
		Version v = VersionApi.initializeVersion(vao);
		VersionApi.applyActionOnVersionFromCommit(v, "fix: simple commit message");
		assert v.constructVersionString().equals("1.0.1");
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
	void testSetMavenSnapshotStatus() {
		fail("Not yet implemented");
	}

	@Test
	void testSetVersionDateFromString() {
		fail("Not yet implemented");
	}

	@Test
	void testGetBaseVerWithModMeta() {
		fail("Not yet implemented");
	}

	@Test
	void testGetCalverType() {
		fail("Not yet implemented");
	}

	@Test
	void testGetUbuntuCalver() {
		fail("Not yet implemented");
	}

	@Test
	void testGetRelizaCalver() {
		fail("Not yet implemented");
	}

	@Test
	void testGetRelizaCalver2020() {
		fail("Not yet implemented");
	}

}

package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

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

}

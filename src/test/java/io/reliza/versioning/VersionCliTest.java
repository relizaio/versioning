package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionCliTest {
	/* Attempt to capture system out content to check error messages from cli tool.
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private final PrintStream originalOut;
	private final PrintStream originalErr;
	private String message;
	
	VersionCliTest() {
		originalOut = System.out;
		originalErr = System.err;
	}
	
	@BeforeEach
	public void setUpStreams() {
	    System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	public void restoreStreams() {
	    System.setOut(originalOut);
	    System.setErr(originalErr);
	    System.out.println(message);
	}
	*/
	
	@Test
	void testMainInvalidCommitTwoLines() {
		String commitMessage = "fix: commit message" + System.lineSeparator() + "";
		VersionCli.main(new String[] {"-s", "semver", "-c", commitMessage});
		String outContent = "";
		assertTrue(outContent.toString().contains("Commit message does not meet conventional commit specification."));
	}

}

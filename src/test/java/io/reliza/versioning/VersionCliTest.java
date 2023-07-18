package io.reliza.versioning;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JUnit Test Suite for Command-Line operation of Versioning Library.
 * 
 * This class can be used to test the command line operation of the versioning library.
 * The ByteArrayOutputStreams outContent and errContent help to capture System.out.println
 * calls and the ExitException and security manager class help to capture System.exit calls.
 *
 */
class VersionCliTest {
	// Attempt to capture system out content to check error messages from cli tool.
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private static final PrintStream originalOut = System.out;
	private static final PrintStream originalErr = System.err;
	private static String message; // Assign value to this to print to original System.out after running test case
	
	// Test Cases //
	
	@Test
	void sampleCliTestcase() {
		// Catch ExitException that occurs when VersionCli.main method calls System.exit
		// Check exit code using e.status
		try {
			VersionCli.main(new String[] {"-s", "semver"});
		} catch (ExitException e) {
			assertEquals(0, e.status, "Exit status not 0");
		}
		// Capture System.out contents, and print by assigning to message variable
		message = outContent.toString();
	}
	
	@Test
	void testMainInvalidCommitTwoLines() {
		String commitMessage = "fix: commit message" + System.lineSeparator() + "";
		try {
			VersionCli.main(new String[] {"-s", "semver", "-c", commitMessage});
		} catch (ExitException e) {
			
		}
		message = outContent.toString();
		assertTrue(outContent.toString().contains("Commit message does not meet conventional commit specification."));
	}
	
	// Setup //

	@BeforeEach
	void setUpStreams() {
	    System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	void restoreStreams() {
	    System.setOut(originalOut);
	    System.setErr(originalErr);
	    System.out.println(message);
	    message = "";
	}
	
	// Classes used to test main method without System.exit calls stopping JVM, so that JUnit can keep running
	@SuppressWarnings("serial")
	protected static class ExitException extends SecurityException {
        public final int status;
        public ExitException(int status) 
        {
            super("There is no escape!");
            this.status = status;
        }
    }
	
}

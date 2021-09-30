package io.reliza.changelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CommitParserUtilTest {
	private static final String LS = System.lineSeparator();//"\n";

	@Test
	@Disabled
	void testParseRawCommit_BreakingChangeFooter() {
		String rawCommit = "fix: simple commit message"+LS+LS+"body"+LS+LS+"Footer: 1"+LS+"BREAKING CHANGE: 2";
		ConventionalCommit commit = CommitParserUtil.parseRawCommit(rawCommit);
		//System.out.println("raw" + rawCommit);
		//System.out.println("message: "+ commit.getMessage());
		//System.out.println("body: "+commit.getRawBody());
		//System.out.println("footer: "+commit.getFooter());
		assertTrue(commit.isBreakingChange());
	}
	
	@Test
	void testParseRawCommitRegex_1() {
		String rawCommit = "fix: simple commit message"+LS+LS+"body"+LS+LS+"Footer: 1"+LS+"BREAKING CHANGE: 2";
		ConventionalCommit commit = CommitParserUtil.parseRawCommitRegex(rawCommit);
		System.out.println(rawCommit);
		System.out.println("message: "+ commit.getMessage());
		System.out.println("body: "+commit.getRawBody());
		System.out.println("footer: "+commit.getFooter());
		assertTrue(commit.isBreakingChange());
	}
	
	@Test
	@Disabled
	void testIsCommitValid() {
		String rawcommit = "feat: fff" + LS
				+ LS
				+ "BREAKING-CHANGE: 3";
		boolean expected_valid = true;
		boolean actually_valid;
		ConventionalCommit c = null;
		try {
			c = CommitParserUtil.parseRawCommit(rawcommit);
			actually_valid = true;
		} catch (Exception e) {
			actually_valid = false;
		}
		assertEquals(expected_valid, actually_valid);
		assertEquals(true, c.isBreakingChange());
	}
	
	@Test
	@Disabled
	void testAllCommitMessages() {
		boolean testSuccess = true;
		ArrayList<CommitTestCase> commits = new ArrayList<CommitTestCase>();
		loadCommitMessages(".\\src\\test\\java\\io\\reliza\\changelog\\test-commit-messages.txt", commits);
		// try to parse commit and see if it fails or not and match with expectation
		for (CommitTestCase c : commits) {
			ConventionalCommit commit = null;
			try {
				commit = CommitParserUtil.parseRawCommit(c.commit);
			} catch (IllegalArgumentException e) {
				
			}
			if (commit != null && c.isValid()) {
				// commit was valid and parsed succesfully, test success
				//System.out.println("SUCCESS - " + c.desc);
			} else if (commit == null && !c.isValid()) {
				// commit was not valid and not parsed, test success
				//System.out.println("SUCCESS - " + c.desc);
			} else {
				String s1 = ((c.isValid()==false) ? "Invalid" : "Valid");
				String s2 = ((commit==null) ? "Invalid" : "Valid");
				System.out.println("FAILED - expected commit to be " + s1 + ", but was actually " + s2 + "\ntest desc:" + c.desc);
				System.out.println("\"" + c.commit + "\"\n");
				testSuccess = false;
			}
		}
		assertTrue(testSuccess);
	}
	
	// Helper method to load conventional commit test cases from test file
	// Fills List of CommitTestCase objects which store the commit string to be tested,
	// as well as if the commit is valid or not and a description of the test case.
	void loadCommitMessages(String pathname, List<CommitTestCase> commits) {
		try {
			FileReader fr = new FileReader(new File(pathname));
			String text = "";
			int c;
			while ((c = fr.read()) != -1) {
				text += (char) c;
			}
			// change line endings to just \n
			text = text.replaceAll("\r\n", "\n");
			// extract commits and expectation with regex
			String regex = "^\\t//[ ]{1,2}(?<expectation>VALID|INVALID)([ ]{1,2}/(?<desc>.*?(?=$)))?\\n(\\t//[ ]{1,2}(?<expectation2>VALID|INVALID)[ ]{1,2}/(?<desc2>.*?(?=$))\\n)?(?<startquote>[`\\\"])(?<commit>.*?(?=\\k<startquote>))(?<endquote>[`\\\"])";
			Pattern p = Pattern.compile(regex, Pattern.MULTILINE|Pattern.DOTALL);
			Matcher m = p.matcher(text);
			while(m.find() == true) {
				//String committestcase = m.group("expectation") + m.group("desc") + m.group("expectation2") + m.group("desc2") + "|||" + m.group("commit");
				CommitTestCase committestcase = new CommitTestCase(m.group("expectation"),m.group("desc"),m.group("expectation2"),m.group("desc2"),m.group("commit"));
				commits.add(committestcase);
			}
		} catch (IOException e) {
			System.out.println("An error occured reading file: " + pathname);
			e.printStackTrace();
			return;
		}
	}
	
	private class CommitTestCase {
		public final String expectation;
		public final String desc;
		public final String expectation2;
		public final String desc2;
		public final String commit;
		public final boolean has2desc;
		
		public CommitTestCase (String e, String d, String e2, String d2, String c) {
			expectation = e;
			desc = d;
			expectation2 = e2;
			desc2 = d2;
			commit = c;
			has2desc = e2 == null ? false : true; // if e2 does not exist, only one desc
		}
		
		public boolean isValid() {
			if (expectation.equals("VALID")) {
				return true;
			} else if (expectation.equals("INVALID")) {
				return false;
			} else {
				throw new IllegalStateException("expectation is not VALID or INVALID");
			}
		}
		
		@Override
		public String toString() {
			if (has2desc) {
				return expectation + desc + "\n" + expectation2 + desc2 + "\n" + commit;
			} else {
				return expectation + desc + "\n" + commit;
			}
		}
	}
}

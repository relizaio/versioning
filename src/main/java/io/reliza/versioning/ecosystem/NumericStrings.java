/**
* Copyright 2026 Reliza Incorporated. Licensed under MIT License.
* https://reliza.io
*/

package io.reliza.versioning.ecosystem;

/**
 * Helpers shared by the ecosystem comparators.
 */
final class NumericStrings {

	private NumericStrings() {
	}

	/**
	 * Compares two non-empty strings of ASCII digits by numeric value, without overflow.
	 * @param a digits
	 * @param b digits
	 * @return negative, zero or positive as a is numerically lower, equal or higher than b
	 */
	static int compare(String a, String b) {
		String x = stripLeadingZeros(a);
		String y = stripLeadingZeros(b);
		if (x.length() != y.length()) return Integer.compare(x.length(), y.length());
		return Integer.signum(x.compareTo(y));
	}

	/**
	 * Compares two dotted numeric releases part by part, treating missing trailing parts as
	 * zero, so [1, 2] equals [1, 2, 0].
	 * @param a parts made of ASCII digits
	 * @param b parts made of ASCII digits
	 * @return negative, zero or positive as a is lower, equal or higher than b
	 */
	static int compareParts(String[] a, String[] b) {
		int n = Math.max(a.length, b.length);
		for (int i = 0; i < n; i++) {
			int c = compare(i < a.length ? a[i] : "0", i < b.length ? b[i] : "0");
			if (c != 0) return c;
		}
		return 0;
	}

	/**
	 * Checks for a non-empty string made of ASCII digits only.
	 * @param s string
	 * @return true when s is non-empty and made of ASCII digits only
	 */
	static boolean isDigits(String s) {
		if (s.isEmpty()) return false;
		for (int i = 0; i < s.length(); i++) {
			if (!isDigit(s.charAt(i))) return false;
		}
		return true;
	}

	/**
	 * @param c character
	 * @return true when c is an ASCII digit
	 */
	static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * @param c character
	 * @return true when c is an ASCII letter
	 */
	static boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	/**
	 * @param digits string of ASCII digits
	 * @return the digits without leading zeros, "0" when all digits are zero
	 */
	static String stripLeadingZeros(String digits) {
		int i = 0;
		while (i < digits.length() - 1 && digits.charAt(i) == '0') i++;
		return digits.substring(i);
	}
}

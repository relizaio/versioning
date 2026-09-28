/**
* This file is a derivative of Dependency-Track's org.dependencytrack.util.ComponentVersion
* (Dependency-Track 4.13.2), itself ported from DependencyVersion in OWASP Dependency-Check
* 5.2.1 (author Jeremy Long), and is distributed under the Apache License, Version 2.0, unlike
* the rest of this project, which is MIT licensed (see NOTICE).
*
*   Copyright (c) OWASP Foundation. All Rights Reserved.
*
*   Modifications Copyright 2026 Reliza Incorporated (https://reliza.io).
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*   Unless required by applicable law or agreed to in writing, software distributed under the
*   License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
*   either express or implied. See the License for the specific language governing permissions
*   and limitations under the License.
*
* Changes from the original, made so that the ordering is a consistent total order: reduced to
* parsing and comparison over plain strings; numeric parts compare by value at any length (the
* original compares parts that overflow an int as strings); any number of trailing zero parts is
* ignored (the original ignores a single one, so it has 1.0 = 1.0.0 = 1.0.0.0 but 1.0 &lt; 1.0.0.0);
* parts starting with digits compare by that number first (the original compares "3b" and "10"
* as strings, placing 3b above 10 while 3b &lt; 4 &lt; 10).
*/

package io.reliza.versioning.ecosystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic version ordering, following Dependency-Track's ComponentVersion so that results agree
 * with the ranges Dependency-Track itself matched. A version is split into parts: runs of digits,
 * short letter runs attached to digits (such as "rc1" or a trailing "3b") and trailing
 * rc/release/snapshot/beta/alpha words; everything else is dropped. An Ubuntu package revision
 * such as "-0ubuntu1" is ignored, and so are trailing zero parts (1.0 equals 1.0.0.0). Parts
 * compare pairwise: parts starting with digits by that number and then by the rest, all other
 * combinations lexically. When one version runs out of parts it is lower, so an extra qualifier
 * part ranks above the bare release (1.0.0 &lt; 1.0.0-rc1).
 */
final class GenericComparator implements Comparator<String> {

	private static final Pattern UBUNTU_REVISION = Pattern.compile("^([0-9]+:)?(.*)(-[^-]+ubuntu[^-]+)$");

	private static final Pattern PART = Pattern.compile(
			"(\\d+[a-z]{1,3}$|[a-z]{1,3}[_-]?\\d+|\\d+|(rc|release|snapshot|beta|alpha)$)",
			Pattern.CASE_INSENSITIVE);

	@Override
	public int compare(String a, String b) {
		List<String> left = parts(a);
		List<String> right = parts(b);
		int common = Math.min(left.size(), right.size());
		for (int i = 0; i < common; i++) {
			int c = compareParts(left.get(i), right.get(i));
			if (c != 0) return c;
		}
		return Integer.compare(left.size(), right.size());
	}

	/**
	 * Parts starting with a digit compare by their leading number, then by the remainder; any
	 * other pair compares lexically, which a part starting with a digit and one that does not
	 * settle on their first characters.
	 */
	private static int compareParts(String l, String r) {
		if (l.isEmpty() || r.isEmpty() || !NumericStrings.isDigit(l.charAt(0)) || !NumericStrings.isDigit(r.charAt(0))) {
			return Integer.signum(l.compareTo(r));
		}
		int ld = leadingDigits(l);
		int rd = leadingDigits(r);
		int c = NumericStrings.compare(l.substring(0, ld), r.substring(0, rd));
		if (c != 0) return c;
		return Integer.signum(l.substring(ld).compareTo(r.substring(rd)));
	}

	private static int leadingDigits(String s) {
		int i = 0;
		while (i < s.length() && NumericStrings.isDigit(s.charAt(i))) i++;
		return i;
	}

	private static List<String> parts(String version) {
		List<String> parts = new ArrayList<>();
		String lower = version.toLowerCase(Locale.ROOT);
		Matcher ubuntu = UBUNTU_REVISION.matcher(lower);
		if (ubuntu.matches()) lower = ubuntu.group(2);
		Matcher m = PART.matcher(lower);
		while (m.find()) {
			parts.add(m.group());
		}
		if (parts.isEmpty()) parts.add(version);
		while (parts.size() > 1 && isZero(parts.get(parts.size() - 1))) {
			parts.remove(parts.size() - 1);
		}
		return parts;
	}

	private static boolean isZero(String part) {
		return NumericStrings.isDigits(part) && NumericStrings.stripLeadingZeros(part).equals("0");
	}
}

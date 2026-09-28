/**
* This file is a derivative of Apache Maven's
* org.apache.maven.artifact.versioning.ComparableVersion (maven-artifact 3.9.16) and is
* distributed under the Apache License, Version 2.0, unlike the rest of this project, which is
* MIT licensed (see NOTICE).
*
*   Apache Maven. Copyright The Apache Software Foundation.
*   This product includes software developed at The Apache Software Foundation
*   (http://www.apache.org/).
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
* Changes from the original: reduced to comparison over plain strings; the int, long and
* BigInteger item types are merged into one numeric item compared by value; only ASCII digits
* count as digits.
*/

package io.reliza.versioning.ecosystem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Maven version ordering, ported from Apache Maven's ComparableVersion as of maven-artifact
 * 3.9.x (https://maven.apache.org/pom.html#version-order-specification):
 * <ul>
 * <li>"." and "-" separate items, as does every transition between digits and letters;</li>
 * <li>a "-" (or a qualifier followed by a number) opens a nested list, which sorts below a
 * number at the same position;</li>
 * <li>known qualifiers sort alpha &lt; beta &lt; milestone &lt; rc = cr &lt; snapshot &lt;
 * "" = ga = final = release &lt; sp, with unknown qualifiers after them in lexical order;</li>
 * <li>a, b and m directly followed by a number mean alpha, beta and milestone;</li>
 * <li>trailing zeros and empty qualifiers are dropped, so "1.0.0" equals "1".</li>
 * </ul>
 * Comparison is case-insensitive. Digits are ASCII digits only.
 */
final class MavenComparator implements Comparator<String> {

	private static final List<String> QUALIFIERS = List.of("alpha", "beta", "milestone", "rc", "snapshot", "", "sp");

	private static final String RELEASE_RANK = String.valueOf(QUALIFIERS.indexOf(""));

	private sealed interface Item permits NumberItem, QualifierItem, ListItem {
	}

	private record NumberItem(String digits) implements Item {
		static final NumberItem ZERO = new NumberItem("0");

		static NumberItem of(String digits) {
			return new NumberItem(NumericStrings.stripLeadingZeros(digits));
		}
	}

	private record QualifierItem(String value) implements Item {
		static QualifierItem of(String value, boolean followedByDigit) {
			String v = value;
			if (followedByDigit && v.length() == 1) {
				v = switch (v.charAt(0)) {
					case 'a' -> "alpha";
					case 'b' -> "beta";
					case 'm' -> "milestone";
					default -> v;
				};
			}
			v = switch (v) {
				case "ga", "final", "release" -> "";
				case "cr" -> "rc";
				default -> v;
			};
			return new QualifierItem(v);
		}

		/**
		 * Rank string for lexical comparison: known qualifiers by position, unknown ones after
		 * them, ordered by name.
		 */
		String rank() {
			int i = QUALIFIERS.indexOf(value);
			return i == -1 ? QUALIFIERS.size() + "-" + value : String.valueOf(i);
		}
	}

	@SuppressWarnings("serial")
	private static final class ListItem extends ArrayList<Item> implements Item {
	}

	@Override
	public int compare(String a, String b) {
		return compareItems(parse(a), parse(b));
	}

	private static ListItem parse(String version) {
		String v = version.toLowerCase(Locale.ROOT);
		ListItem root = new ListItem();
		ListItem list = root;
		Deque<ListItem> lists = new ArrayDeque<>();
		lists.push(root);
		boolean inDigits = false;
		int start = 0;
		for (int i = 0; i < v.length(); i++) {
			char c = v.charAt(i);
			if (c == '.' || c == '-') {
				list.add(i == start ? NumberItem.ZERO : toItem(inDigits, v.substring(start, i)));
				start = i + 1;
				if (c == '-') list = openList(list, lists);
			} else if (NumericStrings.isDigit(c)) {
				if (!inDigits && i > start) {
					// a qualifier followed by a number: "1.0.0.X1" sorts like "1.0.0-X1"
					if (!list.isEmpty()) list = openList(list, lists);
					list.add(QualifierItem.of(v.substring(start, i), true));
					start = i;
					list = openList(list, lists);
				}
				inDigits = true;
			} else {
				if (inDigits && i > start) {
					list.add(toItem(true, v.substring(start, i)));
					start = i;
					list = openList(list, lists);
				}
				inDigits = false;
			}
		}
		if (v.length() > start) {
			// a trailing qualifier after a dot sorts like one after a hyphen
			if (!inDigits && !list.isEmpty()) list = openList(list, lists);
			list.add(toItem(inDigits, v.substring(start)));
		}
		while (!lists.isEmpty()) {
			normalize(lists.pop());
		}
		return root;
	}

	private static Item toItem(boolean digits, String token) {
		return digits ? NumberItem.of(token) : QualifierItem.of(token, false);
	}

	private static ListItem openList(ListItem parent, Deque<ListItem> lists) {
		ListItem child = new ListItem();
		parent.add(child);
		lists.push(child);
		return child;
	}

	/**
	 * Drops trailing null items (zero, release qualifier, empty list), looking through
	 * trailing nested lists.
	 */
	private static void normalize(ListItem list) {
		for (int i = list.size() - 1; i >= 0; i--) {
			Item last = list.get(i);
			if (isNull(last)) {
				list.remove(i);
			} else if (!(last instanceof ListItem)) {
				break;
			}
		}
	}

	private static boolean isNull(Item item) {
		return switch (item) {
			case NumberItem n -> n.digits().equals("0");
			case QualifierItem q -> q.rank().equals(RELEASE_RANK);
			case ListItem l -> l.isEmpty();
		};
	}

	/**
	 * Compares two items; a null right-hand side stands for a missing item at that position.
	 */
	private static int compareItems(Item left, Item right) {
		return switch (left) {
			case NumberItem n -> switch (right) {
				case null -> isNull(n) ? 0 : 1;
				case NumberItem o -> NumericStrings.compare(n.digits(), o.digits());
				case QualifierItem o -> 1;
				case ListItem o -> 1;
			};
			case QualifierItem q -> switch (right) {
				case null -> Integer.signum(q.rank().compareTo(RELEASE_RANK));
				case NumberItem o -> -1;
				case QualifierItem o -> Integer.signum(q.rank().compareTo(o.rank()));
				case ListItem o -> -1;
			};
			case ListItem l -> switch (right) {
				case null -> compareListToMissing(l);
				case NumberItem o -> -1;
				case QualifierItem o -> 1;
				case ListItem o -> compareLists(l, o);
			};
		};
	}

	private static int compareListToMissing(ListItem list) {
		for (Item item : list) {
			int c = compareItems(item, null);
			if (c != 0) return c;
		}
		return 0;
	}

	private static int compareLists(ListItem left, ListItem right) {
		Iterator<Item> l = left.iterator();
		Iterator<Item> r = right.iterator();
		while (l.hasNext() || r.hasNext()) {
			Item li = l.hasNext() ? l.next() : null;
			Item ri = r.hasNext() ? r.next() : null;
			int c;
			if (li == null) {
				c = ri == null ? 0 : -compareItems(ri, null);
			} else {
				c = compareItems(li, ri);
			}
			if (c != 0) return c;
		}
		return 0;
	}
}

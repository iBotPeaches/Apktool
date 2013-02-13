/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package brut.androlib.res.xml;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public final class ResXmlEncoders {

	public static String escapeXmlChars(String str) {
		return str.replace("&", "&amp;").replace("<", "&lt;");
	}

	public static String encodeAsResXmlAttr(String str) {
		if (str.isEmpty()) {
			return str;
		}

		char[] chars = str.toCharArray();
		StringBuilder out = new StringBuilder(str.length() + 10);

		switch (chars[0]) {
		case '#':
		case '@':
		case '?':
			out.append('\\');
		}

		for (char c : chars) {
			switch (c) {
			case '\\':
				out.append('\\');
				break;
			case '"':
				out.append("&quot;");
				continue;
			case '\n':
				out.append("\\n");
				continue;
			default:
				if (!isPrintableChar(c)) {
					out.append(String.format("\\u%04x", (int) c));
					continue;
				}
			}
			out.append(c);
		}

		return out.toString();
	}

	public static String encodeAsXmlValue(String str) {
		if (str.isEmpty()) {
			return str;
		}

		char[] chars = str.toCharArray();
		StringBuilder out = new StringBuilder(str.length() + 10);

		switch (chars[0]) {
		case '#':
		case '@':
		case '?':
			out.append('\\');
		}

		boolean isInStyleTag = false;
		int startPos = 0;
		boolean enclose = false;
		boolean wasSpace = true;
		for (char c : chars) {
			if (isInStyleTag) {
				if (c == '>') {
					isInStyleTag = false;
					startPos = out.length() + 1;
					enclose = false;
				}
			} else if (c == ' ') {
				if (wasSpace) {
					enclose = true;
				}
				wasSpace = true;
			} else {
				wasSpace = false;
				switch (c) {
				case '\\':
					out.append('\\');
					break;
				case '\'':
				case '\n':
					enclose = true;
					break;
				case '"':
					out.append('\\');
					break;
				case '<':
					isInStyleTag = true;
					if (enclose) {
						out.insert(startPos, '"').append('"');
					}
					break;
				default:
					if (!isPrintableChar(c)) {
						out.append(String.format("\\u%04x", (int) c));
						continue;
					}
				}
			}
			out.append(c);
		}

		if (enclose || wasSpace) {
			out.insert(startPos, '"').append('"');
		}

		return out.toString();
	}

	public static boolean hasMultipleNonPositionalSubstitutions(String str) {
		return findNonPositionalSubstitutions(str, 2).size() > 1;
	}

	public static String enumerateNonPositionalSubstitutions(String str) {
		List<Integer> subs = findNonPositionalSubstitutions(str, -1);
		if (subs.size() < 2) {
			return str;
		}

		StringBuilder out = new StringBuilder();
		int pos = 0;
		int count = 0;
		for (Integer sub : subs) {
			out.append(str.substring(pos, ++sub)).append(++count).append('$');
			pos = sub;
		}
		out.append(str.substring(pos));

		return out.toString();
	}

	/**
	 * It searches for "%", but not "%%" nor "%(\d)+\$"
	 */
	private static List<Integer> findNonPositionalSubstitutions(String str,
			int max) {
		int pos = 0;
		int pos2 = 0;
		int count = 0;
		int length = str.length();
		List<Integer> ret = new ArrayList<Integer>();
		while ((pos2 = (pos = str.indexOf('%', pos2)) + 1) != 0) {
			if (pos2 == length) {
				break;
			}
			char c = str.charAt(pos2++);
			if (c == '%') {
				continue;
			}
			if (c >= '0' && c <= '9' && pos2 < length) {
				do {
					c = str.charAt(pos2++);
				} while (c >= '0' && c <= '9' && pos2 < length);
				if (c == '$') {
					continue;
				}
			}

			ret.add(pos);
			if (max != -1 && ++count >= max) {
				break;
			}
		}

		return ret;
	}

	private static boolean isPrintableChar(char c) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
		return !Character.isISOControl(c) && c != KeyEvent.CHAR_UNDEFINED
				&& block != null && block != Character.UnicodeBlock.SPECIALS;
	}
}

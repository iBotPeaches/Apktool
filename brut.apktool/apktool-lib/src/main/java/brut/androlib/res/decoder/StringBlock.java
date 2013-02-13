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

package brut.androlib.res.decoder;

import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.ExtDataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 * @author Dmitry Skiba
 * 
 *         Block of strings, used in binary xml and arsc.
 * 
 *         TODO: - implement get()
 * 
 */
public class StringBlock {

	/**
	 * Reads whole (including chunk type) string block from stream. Stream must
	 * be at the chunk type.
	 */
	public static StringBlock read(ExtDataInput reader) throws IOException {
		reader.skipCheckInt(CHUNK_TYPE);
		int chunkSize = reader.readInt();
		int stringCount = reader.readInt();
		int styleOffsetCount = reader.readInt();
		int flags = reader.readInt();
		int stringsOffset = reader.readInt();
		int stylesOffset = reader.readInt();

		StringBlock block = new StringBlock();
		block.m_isUTF8 = (flags & UTF8_FLAG) != 0;
		block.m_stringOffsets = reader.readIntArray(stringCount);
		block.m_stringOwns = new int[stringCount];
		for (int i = 0; i < stringCount; i++) {
			block.m_stringOwns[i] = -1;
		}
		if (styleOffsetCount != 0) {
			block.m_styleOffsets = reader.readIntArray(styleOffsetCount);
		}
		{
			int size = ((stylesOffset == 0) ? chunkSize : stylesOffset)
					- stringsOffset;
			if ((size % 4) != 0) {
				throw new IOException("String data size is not multiple of 4 ("
						+ size + ").");
			}
			block.m_strings = new byte[size];
			reader.readFully(block.m_strings);
		}
		if (stylesOffset != 0) {
			int size = (chunkSize - stylesOffset);
			if ((size % 4) != 0) {
				throw new IOException("Style data size is not multiple of 4 ("
						+ size + ").");
			}
			block.m_styles = reader.readIntArray(size / 4);
		}

		return block;
	}

	/**
	 * Returns number of strings in block.
	 */
	public int getCount() {
		return m_stringOffsets != null ? m_stringOffsets.length : 0;
	}

	/**
	 * Returns raw string (without any styling information) at specified index.
	 */
	public String getString(int index) {
		if (index < 0 || m_stringOffsets == null
				|| index >= m_stringOffsets.length) {
			return null;
		}
		int offset = m_stringOffsets[index];
		int length;

		if (!m_isUTF8) {
			length = getShort(m_strings, offset) * 2;
			offset += 2;
		} else {
			offset += getVarint(m_strings, offset)[1];
			int[] varint = getVarint(m_strings, offset);
			offset += varint[1];
			length = varint[0];
		}
		return decodeString(offset, length);
	}

	/**
	 * Not yet implemented.
	 * 
	 * Returns string with style information (if any).
	 */
	public CharSequence get(int index) {
		return getString(index);
	}

	/**
	 * Returns string with style tags (html-like).
	 */
	public String getHTML(int index) {
		String raw = getString(index);
		if (raw == null) {
			return raw;
		}
		int[] style = getStyle(index);
		if (style == null) {
			return ResXmlEncoders.escapeXmlChars(raw);
		}
		StringBuilder html = new StringBuilder(raw.length() + 32);
		int[] opened = new int[style.length / 3];
		int offset = 0, depth = 0;
		while (true) {
			int i = -1, j;
			for (j = 0; j != style.length; j += 3) {
				if (style[j + 1] == -1) {
					continue;
				}
				if (i == -1 || style[i + 1] > style[j + 1]) {
					i = j;
				}
			}
			int start = ((i != -1) ? style[i + 1] : raw.length());
			for (j = depth - 1; j >= 0; j--) {
				int last = opened[j];
				int end = style[last + 2];
				if (end >= start) {
					break;
				}
				if (offset <= end) {
					html.append(ResXmlEncoders.escapeXmlChars(raw.substring(
							offset, end + 1)));
					offset = end + 1;
				}
				outputStyleTag(getString(style[last]), html, true);
			}
			depth = j + 1;
			if (offset < start) {
				html.append(ResXmlEncoders.escapeXmlChars(raw.substring(offset,
						start)));
				offset = start;
			}
			if (i == -1) {
				break;
			}
			outputStyleTag(getString(style[i]), html, false);
			style[i + 1] = -1;
			opened[depth++] = i;
		}
		return html.toString();
	}

	private void outputStyleTag(String tag, StringBuilder builder, boolean close) {
		builder.append('<');
		if (close) {
			builder.append('/');
		}

		int pos = tag.indexOf(';');
		if (pos == -1) {
			builder.append(tag);
		} else {
			builder.append(tag.substring(0, pos));
			if (!close) {
				boolean loop = true;
				while (loop) {
					int pos2 = tag.indexOf('=', pos + 1);
					builder.append(' ').append(tag.substring(pos + 1, pos2))
							.append("=\"");
					pos = tag.indexOf(';', pos2 + 1);

					String val;
					if (pos != -1) {
						val = tag.substring(pos2 + 1, pos);
					} else {
						loop = false;
						val = tag.substring(pos2 + 1);
					}

					builder.append(ResXmlEncoders.escapeXmlChars(val)).append(
							'"');
				}
			}
		}
		builder.append('>');
	}

	/**
	 * Finds index of the string. Returns -1 if the string was not found.
	 */
	public int find(String string) {
		if (string == null) {
			return -1;
		}
		for (int i = 0; i != m_stringOffsets.length; ++i) {
			int offset = m_stringOffsets[i];
			int length = getShort(m_strings, offset);
			if (length != string.length()) {
				continue;
			}
			int j = 0;
			for (; j != length; ++j) {
				offset += 2;
				if (string.charAt(j) != getShort(m_strings, offset)) {
					break;
				}
			}
			if (j == length) {
				return i;
			}
		}
		return -1;
	}

	// /////////////////////////////////////////// implementation
	private StringBlock() {
	}

	/**
	 * Returns style information - array of int triplets, where in each triplet:
	 * * first int is index of tag name ('b','i', etc.) * second int is tag
	 * start index in string * third int is tag end index in string
	 */
	private int[] getStyle(int index) {
		if (m_styleOffsets == null || m_styles == null
				|| index >= m_styleOffsets.length) {
			return null;
		}
		int offset = m_styleOffsets[index] / 4;
		int style[];
		{
			int count = 0;
			for (int i = offset; i < m_styles.length; ++i) {
				if (m_styles[i] == -1) {
					break;
				}
				count += 1;
			}
			if (count == 0 || (count % 3) != 0) {
				return null;
			}
			style = new int[count];
		}
		for (int i = offset, j = 0; i < m_styles.length;) {
			if (m_styles[i] == -1) {
				break;
			}
			style[j++] = m_styles[i++];
		}
		return style;
	}

	private String decodeString(int offset, int length) {
		try {
			return (m_isUTF8 ? UTF8_DECODER : UTF16LE_DECODER).decode(
					ByteBuffer.wrap(m_strings, offset, length)).toString();
		} catch (CharacterCodingException ex) {
			LOGGER.log(Level.WARNING, null, ex);
			return null;
		}
	}

	private static final int getShort(byte[] array, int offset) {
		return (array[offset + 1] & 0xff) << 8 | array[offset] & 0xff;
	}

	private static final int getShort(int[] array, int offset) {
		int value = array[offset / 4];
		if ((offset % 4) / 2 == 0) {
			return (value & 0xFFFF);
		} else {
			return (value >>> 16);
		}
	}

	private static final int[] getVarint(byte[] array, int offset) {
		int val = array[offset];
		boolean more = (val & 0x80) != 0;
		val &= 0x7f;

		if (!more) {
			return new int[] { val, 1 };
		} else {
			return new int[] { val << 8 | array[offset + 1] & 0xff, 2 };
		}
	}

	public boolean touch(int index, int own) {
		if (index < 0 || m_stringOwns == null || index >= m_stringOwns.length) {
			return false;
		}
		if (m_stringOwns[index] == -1) {
			m_stringOwns[index] = own;
			return true;
		} else if (m_stringOwns[index] == own) {
			return true;
		} else {
			return false;
		}
	}

	private int[] m_stringOffsets;
	private byte[] m_strings;
	private int[] m_styleOffsets;
	private int[] m_styles;
	private boolean m_isUTF8;
	private int[] m_stringOwns;
	private static final CharsetDecoder UTF16LE_DECODER = Charset.forName(
			"UTF-16LE").newDecoder();
	private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8")
			.newDecoder();
	private static final Logger LOGGER = Logger.getLogger(StringBlock.class
			.getName());
	private static final int CHUNK_TYPE = 0x001C0001;
	private static final int UTF8_FLAG = 0x00000100;
}

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.util;

/**
 * Container for a dynamically typed data value. Primarily used with
 * {@link android.content.res.Resources} for holding resource values.
 */
public class TypedValue {
	/** The value contains no data. */
	public static final int TYPE_NULL = 0x00;

	/** The <var>data</var> field holds a resource identifier. */
	public static final int TYPE_REFERENCE = 0x01;
	/**
	 * The <var>data</var> field holds an attribute resource identifier
	 * (referencing an attribute in the current theme style, not a resource
	 * entry).
	 */
	public static final int TYPE_ATTRIBUTE = 0x02;
	/**
	 * The <var>string</var> field holds string data. In addition, if
	 * <var>data</var> is non-zero then it is the string block index of the
	 * string and <var>assetCookie</var> is the set of assets the string came
	 * from.
	 */
	public static final int TYPE_STRING = 0x03;
	/** The <var>data</var> field holds an IEEE 754 floating point number. */
	public static final int TYPE_FLOAT = 0x04;
	/**
	 * The <var>data</var> field holds a complex number encoding a dimension
	 * value.
	 */
	public static final int TYPE_DIMENSION = 0x05;
	/**
	 * The <var>data</var> field holds a complex number encoding a fraction of a
	 * container.
	 */
	public static final int TYPE_FRACTION = 0x06;

	/**
	 * Identifies the start of plain integer values. Any type value from this to
	 * {@link #TYPE_LAST_INT} means the <var>data</var> field holds a generic
	 * integer value.
	 */
	public static final int TYPE_FIRST_INT = 0x10;

	/**
	 * The <var>data</var> field holds a number that was originally specified in
	 * decimal.
	 */
	public static final int TYPE_INT_DEC = 0x10;
	/**
	 * The <var>data</var> field holds a number that was originally specified in
	 * hexadecimal (0xn).
	 */
	public static final int TYPE_INT_HEX = 0x11;
	/**
	 * The <var>data</var> field holds 0 or 1 that was originally specified as
	 * "false" or "true".
	 */
	public static final int TYPE_INT_BOOLEAN = 0x12;

	/**
	 * Identifies the start of integer values that were specified as color
	 * constants (starting with '#').
	 */
	public static final int TYPE_FIRST_COLOR_INT = 0x1c;

	/**
	 * The <var>data</var> field holds a color that was originally specified as
	 * #aarrggbb.
	 */
	public static final int TYPE_INT_COLOR_ARGB8 = 0x1c;
	/**
	 * The <var>data</var> field holds a color that was originally specified as
	 * #rrggbb.
	 */
	public static final int TYPE_INT_COLOR_RGB8 = 0x1d;
	/**
	 * The <var>data</var> field holds a color that was originally specified as
	 * #argb.
	 */
	public static final int TYPE_INT_COLOR_ARGB4 = 0x1e;
	/**
	 * The <var>data</var> field holds a color that was originally specified as
	 * #rgb.
	 */
	public static final int TYPE_INT_COLOR_RGB4 = 0x1f;

	/**
	 * Identifies the end of integer values that were specified as color
	 * constants.
	 */
	public static final int TYPE_LAST_COLOR_INT = 0x1f;

	/** Identifies the end of plain integer values. */
	public static final int TYPE_LAST_INT = 0x1f;

	/* ------------------------------------------------------------ */

	/** Complex data: bit location of unit information. */
	public static final int COMPLEX_UNIT_SHIFT = 0;
	/**
	 * Complex data: mask to extract unit information (after shifting by
	 * {@link #COMPLEX_UNIT_SHIFT}). This gives us 16 possible types, as defined
	 * below.
	 */
	public static final int COMPLEX_UNIT_MASK = 0xf;

	/** {@link #TYPE_DIMENSION} complex unit: Value is raw pixels. */
	public static final int COMPLEX_UNIT_PX = 0;
	/**
	 * {@link #TYPE_DIMENSION} complex unit: Value is Device Independent Pixels.
	 */
	public static final int COMPLEX_UNIT_DIP = 1;
	/** {@link #TYPE_DIMENSION} complex unit: Value is a scaled pixel. */
	public static final int COMPLEX_UNIT_SP = 2;
	/** {@link #TYPE_DIMENSION} complex unit: Value is in points. */
	public static final int COMPLEX_UNIT_PT = 3;
	/** {@link #TYPE_DIMENSION} complex unit: Value is in inches. */
	public static final int COMPLEX_UNIT_IN = 4;
	/** {@link #TYPE_DIMENSION} complex unit: Value is in millimeters. */
	public static final int COMPLEX_UNIT_MM = 5;

	/**
	 * {@link #TYPE_FRACTION} complex unit: A basic fraction of the overall size.
	 */
	public static final int COMPLEX_UNIT_FRACTION = 0;
	/** {@link #TYPE_FRACTION} complex unit: A fraction of the parent size. */
	public static final int COMPLEX_UNIT_FRACTION_PARENT = 1;

	/**
	 * Complex data: where the radix information is, telling where the decimal
	 * place appears in the mantissa.
	 */
	public static final int COMPLEX_RADIX_SHIFT = 4;
	/**
	 * Complex data: mask to extract radix information (after shifting by
	 * {@link #COMPLEX_RADIX_SHIFT}). This give us 4 possible fixed point
	 * representations as defined below.
	 */
	public static final int COMPLEX_RADIX_MASK = 0x3;

	/** Complex data: the mantissa is an integral number -- i.e., 0xnnnnnn.0 */
	public static final int COMPLEX_RADIX_23p0 = 0;
	/** Complex data: the mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn */
	public static final int COMPLEX_RADIX_16p7 = 1;
	/** Complex data: the mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn */
	public static final int COMPLEX_RADIX_8p15 = 2;
	/** Complex data: the mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn */
	public static final int COMPLEX_RADIX_0p23 = 3;

	/** Complex data: bit location of mantissa information. */
	public static final int COMPLEX_MANTISSA_SHIFT = 8;
	/**
	 * Complex data: mask to extract mantissa information (after shifting by
	 * {@link #COMPLEX_MANTISSA_SHIFT}). This gives us 23 bits of precision; the
	 * top bit is the sign.
	 */
	public static final int COMPLEX_MANTISSA_MASK = 0xffffff;

	/* ------------------------------------------------------------ */

	/**
	 * If {@link #density} is equal to this value, then the density should be
	 * treated as the system's default density value:
	 * {@link DisplayMetrics#DENSITY_DEFAULT}.
	 */
	public static final int DENSITY_DEFAULT = 0;

	/**
	 * If {@link #density} is equal to this value, then there is no density
	 * associated with the resource and it should not be scaled.
	 */
	public static final int DENSITY_NONE = 0xffff;

	/* ------------------------------------------------------------ */

	/**
	 * The type held by this value, as defined by the constants here. This tells
	 * you how to interpret the other fields in the object.
	 */
	public int type;

	private static final float MANTISSA_MULT = 1.0f / (1 << TypedValue.COMPLEX_MANTISSA_SHIFT);
	private static final float[] RADIX_MULTS = new float[] {
			1.0f * MANTISSA_MULT, 1.0f / (1 << 7) * MANTISSA_MULT,
			1.0f / (1 << 15) * MANTISSA_MULT, 1.0f / (1 << 23) * MANTISSA_MULT };

	/**
	 * Retrieve the base value from a complex data integer. This uses the
	 * {@link #COMPLEX_MANTISSA_MASK} and {@link #COMPLEX_RADIX_MASK} fields of
	 * the data to compute a floating point representation of the number they
	 * describe. The units are ignored.
	 * 
	 * @param complex
	 *            A complex data value.
	 * 
	 * @return A floating point value corresponding to the complex data.
	 */
	public static float complexToFloat(int complex) {
		return (complex & (TypedValue.COMPLEX_MANTISSA_MASK << TypedValue.COMPLEX_MANTISSA_SHIFT))
				* RADIX_MULTS[(complex >> TypedValue.COMPLEX_RADIX_SHIFT)
						& TypedValue.COMPLEX_RADIX_MASK];
	}

	private static final String[] DIMENSION_UNIT_STRS = new String[] { "px",
			"dip", "sp", "pt", "in", "mm" };
	private static final String[] FRACTION_UNIT_STRS = new String[] { "%", "%p" };

	/**
	 * Perform type conversion as per {@link #coerceToString()} on an explicitly
	 * supplied type and data.
	 * 
	 * @param type
	 *            The data type identifier.
	 * @param data
	 *            The data value.
	 * 
	 * @return String The coerced string value. If the value is null or the type
	 *         is not known, null is returned.
	 */
	public static final String coerceToString(int type, int data) {
		switch (type) {
		case TYPE_NULL:
			return null;
		case TYPE_REFERENCE:
			return "@" + data;
		case TYPE_ATTRIBUTE:
			return "?" + data;
		case TYPE_FLOAT:
			return Float.toString(Float.intBitsToFloat(data));
		case TYPE_DIMENSION:
			return Float.toString(complexToFloat(data))
					+ DIMENSION_UNIT_STRS[(data >> COMPLEX_UNIT_SHIFT)
							& COMPLEX_UNIT_MASK];
		case TYPE_FRACTION:
			return Float.toString(complexToFloat(data) * 100)
					+ FRACTION_UNIT_STRS[(data >> COMPLEX_UNIT_SHIFT)
							& COMPLEX_UNIT_MASK];
		case TYPE_INT_HEX:
			return "0x" + Integer.toHexString(data);
		case TYPE_INT_BOOLEAN:
			return data != 0 ? "true" : "false";
		}

		if (type >= TYPE_FIRST_COLOR_INT && type <= TYPE_LAST_COLOR_INT) {
			String res = String.format("%08x", data);
			char[] vals = res.toCharArray();
			switch (type) {
			default:
			case TYPE_INT_COLOR_ARGB8:// #AaRrGgBb
				break;
			case TYPE_INT_COLOR_RGB8:// #FFRrGgBb->#RrGgBb
				res = res.substring(2);
				break;
			case TYPE_INT_COLOR_ARGB4:// #AARRGGBB->#ARGB
				res = new StringBuffer().append(vals[0]).append(vals[2])
						.append(vals[4]).append(vals[6]).toString();
				break;
			case TYPE_INT_COLOR_RGB4:// #FFRRGGBB->#RGB
				res = new StringBuffer().append(vals[2]).append(vals[4])
						.append(vals[6]).toString();
				break;
			}
			return "#" + res;
		} else if (type >= TYPE_FIRST_INT && type <= TYPE_LAST_INT) {
			String res;
			switch (type) {
			default:
			case TYPE_INT_DEC:
				res = Integer.toString(data);
				break;
			// defined before
			/*
			 * case TYPE_INT_HEX: res = "0x" + Integer.toHexString(data); break;
			 * case TYPE_INT_BOOLEAN: res = (data != 0) ? "true":"false"; break;
			 */
			}
			return res;
		}

		return null;
	}

};

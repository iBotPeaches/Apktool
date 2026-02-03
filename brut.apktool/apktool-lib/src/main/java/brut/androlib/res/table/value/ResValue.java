/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.table.value;

public abstract class ResValue {
    // The value contains no data.
    public static final int TYPE_NULL = 0x00;
    // The data field holds a resource identifier.
    public static final int TYPE_REFERENCE = 0x01;
    // The data field holds an attribute resource identifier (referencing an attribute in the current theme style, not
    // a resource entry).
    public static final int TYPE_ATTRIBUTE = 0x02;
    // The string field holds string data. In addition, if data is non-zero then it is the string block index of the
    // string and assetCookie is the set of assets the string came from.
    public static final int TYPE_STRING = 0x03;
    // The data field holds an IEEE 754 floating point number.
    public static final int TYPE_FLOAT = 0x04;
    // The data field holds a complex number encoding a dimension value.
    public static final int TYPE_DIMENSION = 0x05;
    // The data field holds a complex number encoding a fraction of a container.
    public static final int TYPE_FRACTION = 0x06;
    // The data holds a dynamic res table reference, which needs to be resolved before it can be used like
    // TYPE_REFERENCE.
    public static final int TYPE_DYNAMIC_REFERENCE = 0x07;
    // The data an attribute resource identifier, which needs to be resolved before it can be used like a
    // TYPE_ATTRIBUTE.
    public static final int TYPE_DYNAMIC_ATTRIBUTE = 0x08;
    // Identifies the start of plain integer values. Any type value from this to TYPE_LAST_INT means the data field
    // holds a generic integer value.
    public static final int TYPE_FIRST_INT = 0x10;
    // The data field holds a number that was originally specified in decimal.
    public static final int TYPE_INT_DEC = 0x10;
    // The data field holds a number that was originally specified in hexadecimal (0xn).
    public static final int TYPE_INT_HEX = 0x11;
    // The data field holds 0 or 1 that was originally specified as "false" or "true".
    public static final int TYPE_INT_BOOLEAN = 0x12;
    // Identifies the start of integer values that were specified as color constants (starting with '#').
    public static final int TYPE_FIRST_COLOR_INT = 0x1C;
    // The data field holds a color that was originally specified as #AARRGGBB.
    public static final int TYPE_INT_COLOR_ARGB8 = 0x1C;
    // The data field holds a color that was originally specified as #RRGGBB.
    public static final int TYPE_INT_COLOR_RGB8 = 0x1D;
    // The data field holds a color that was originally specified as #ARGB.
    public static final int TYPE_INT_COLOR_ARGB4 = 0x1E;
    // The data field holds a color that was originally specified as #RGB.
    public static final int TYPE_INT_COLOR_RGB4 = 0x1F;
    // Identifies the end of integer values that were specified as color constants.
    public static final int TYPE_LAST_COLOR_INT = 0x1F;
    // Identifies the end of plain integer values.
    public static final int TYPE_LAST_INT = 0x1F;

    // TYPE_NULL data indicating the value was not specified.
    public static final int DATA_NULL_UNDEFINED = 0;
    // TYPE_NULL data indicating the value was explicitly set to null.
    public static final int DATA_NULL_EMPTY = 1;
}

/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StringWrapper {
    /**
     * Splits the given string into lines using on any embedded newlines, and wrapping the text as needed to conform to
     * the given maximum line width.
     *
     * This uses and assumes unix-style newlines
     *
     * @param str The string to split
     * @param maxWidth The maximum length of any line
     * @param output If given, try to use this array as the return value. If there are more values than will fit
     *               into the array, a new array will be allocated and returned, while the given array will be filled
     *               with as many lines as would fit.
     * @return The split lines from the original, as an array of Strings. The returned array may be larger than the
     *         number of lines. If this is the case, the end of the split lines will be denoted by a null entry in the
     *         array. If there is no null entry, then the size of the array exactly matches the number of lines.
     *         The returned lines will not contain an ending newline
     */
    public static String[] wrapString(@Nonnull String str, int maxWidth, @Nullable String[] output) {
        if (output == null) {
            output = new String[(int)((str.length() / maxWidth) * 1.5d + 1)];
        }

        int lineStart = 0;
        int arrayIndex = 0;
        int i;
        for (i=0; i<str.length(); i++) {
            char c = str.charAt(i);

            if (c == '\n') {
                output = addString(output, str.substring(lineStart, i), arrayIndex++);
                lineStart = i+1;
            } else if (i - lineStart == maxWidth) {
                output = addString(output, str.substring(lineStart, i), arrayIndex++);
                lineStart = i;
            }
        }
        if (lineStart != i || i == 0) {
            output = addString(output, str.substring(lineStart), arrayIndex++, output.length+1);
        }

        if (arrayIndex < output.length) {
            output[arrayIndex] = null;
        }
        return output;
    }

    private static String[] addString(@Nonnull String[] arr, String str, int index) {
        if (index >= arr.length) {
            arr = enlargeArray(arr, (int)(Math.ceil((arr.length + 1) * 1.5)));
        }

        arr[index] = str;
        return arr;
    }

    private static String[] addString(@Nonnull String[] arr, String str, int index, int newLength) {
        if (index >= arr.length) {
            arr = enlargeArray(arr, newLength);
        }

        arr[index] = str;
        return arr;
    }

    private static String[] enlargeArray(String[] arr, int newLength) {
        String[] newArr = new String[newLength];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        return newArr;
    }
}

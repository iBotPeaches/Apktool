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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Class that takes a combined output destination and provides two
 * output writers, one of which ends up writing to the left column and
 * one which goes on the right.
 */
public final class TwoColumnOutput {
    /** non-null; underlying writer for final output */
    private final Writer out;

    /** &gt; 0; the left column width */
    private final int leftWidth;

    private final int rightWidth;

    private final String spacer;

    /**
     * Constructs an instance.
     *
     * @param out non-null; writer to send final output to
     * @param leftWidth &gt; 0; width of the left column, in characters
     * @param rightWidth &gt; 0; width of the right column, in characters
     * @param spacer non-null; spacer string to sit between the two columns
     */
    public TwoColumnOutput(@Nonnull Writer out, int leftWidth, int rightWidth,
                           @Nonnull String spacer) {

        if (leftWidth < 1) {
            throw new IllegalArgumentException("leftWidth < 1");
        }

        if (rightWidth < 1) {
            throw new IllegalArgumentException("rightWidth < 1");
        }

        this.out = out;
        this.leftWidth = leftWidth;
        this.rightWidth = rightWidth;
        this.spacer = spacer;
    }

    /**
     * Constructs an instance.
     *
     * @param out non-null; stream to send final output to
     * @param leftWidth &gt;= 1; width of the left column, in characters
     * @param rightWidth &gt;= 1; width of the right column, in characters
     * @param spacer non-null; spacer string to sit between the two columns
     */
    public TwoColumnOutput(OutputStream out, int leftWidth, int rightWidth,
                           String spacer) {
        this(new OutputStreamWriter(out), leftWidth, rightWidth, spacer);
    }

    private String[] leftLines = null;
    private String[] rightLines = null;
    public void write(String left, String right) throws IOException {
        leftLines = StringWrapper.wrapString(left, leftWidth, leftLines);
        rightLines = StringWrapper.wrapString(right, rightWidth, rightLines);
        int leftCount = leftLines.length;
        int rightCount = rightLines.length;

        for (int i=0; i<leftCount || i <rightCount; i++) {
            String leftLine = null;
            String rightLine = null;

            if (i < leftCount) {
                leftLine = leftLines[i];
                if (leftLine == null) {
                    leftCount = i;
                }
            }

            if (i < rightCount) {
                rightLine = rightLines[i];
                if (rightLine == null) {
                    rightCount = i;
                }
            }

            if (leftLine != null || rightLine != null) {
                int written = 0;
                if (leftLine != null) {
                    out.write(leftLine);
                    written = leftLine.length();
                }

                int remaining = leftWidth - written;
                if (remaining > 0) {
                    writeSpaces(out, remaining);
                }

                out.write(spacer);

                if (rightLine != null) {
                    out.write(rightLine);
                }

                out.write('\n');
            }
        }
    }

    /**
     * Writes the given number of spaces to the given writer.
     *
     * @param out non-null; where to write
     * @param amt &gt;= 0; the number of spaces to write
     */
    private static void writeSpaces(Writer out, int amt) throws IOException {
        while (amt > 0) {
            out.write(' ');
            amt--;
        }
    }
}

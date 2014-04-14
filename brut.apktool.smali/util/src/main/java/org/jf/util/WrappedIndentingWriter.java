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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Writer that wraps another writer and passes width-limited and
 * optionally-prefixed output to its subordinate. When lines are
 * wrapped they are automatically indented based on the start of the
 * line.
 */
public final class WrappedIndentingWriter extends FilterWriter {
    /** null-ok; optional prefix for every line */
    private final String prefix;

    /** &gt; 0; the maximum output width */
    private final int width;

    /** &gt; 0; the maximum indent */
    private final int maxIndent;

    /** &gt;= 0; current output column (zero-based) */
    private int column;

    /** whether indent spaces are currently being collected */
    private boolean collectingIndent;

    /** &gt;= 0; current indent amount */
    private int indent;

    /**
     * Constructs an instance.
     *
     * @param out non-null; writer to send final output to
     * @param width &gt;= 0; the maximum output width (not including
     * <code>prefix</code>), or <code>0</code> for no maximum
     * @param prefix non-null; the prefix for each line
     */
    public WrappedIndentingWriter(Writer out, int width, String prefix) {
        super(out);

        if (out == null) {
            throw new NullPointerException("out == null");
        }

        if (width < 0) {
            throw new IllegalArgumentException("width < 0");
        }

        if (prefix == null) {
            throw new NullPointerException("prefix == null");
        }

        this.width = (width != 0) ? width : Integer.MAX_VALUE;
        this.maxIndent = width >> 1;
        this.prefix = (prefix.length() == 0) ? null : prefix;

        bol();
    }

    /**
     * Constructs a no-prefix instance.
     *
     * @param out non-null; writer to send final output to
     * @param width &gt;= 0; the maximum output width (not including
     * <code>prefix</code>), or <code>0</code> for no maximum
     */
    public WrappedIndentingWriter(Writer out, int width) {
        this(out, width, "");
    }

    /** {@inheritDoc} */
    @Override
    public void write(int c) throws IOException {
        synchronized (lock) {
            if (collectingIndent) {
                if (c == ' ') {
                    indent++;
                    if (indent >= maxIndent) {
                        indent = maxIndent;
                        collectingIndent = false;
                    }
                } else {
                    collectingIndent = false;
                }
            }

            if ((column == width) && (c != '\n')) {
                out.write('\n');
                column = 0;
                /*
                 * Note: No else, so this should fall through to the next
                 * if statement.
                 */
            }

            if (column == 0) {
                if (prefix != null) {
                    out.write(prefix);
                }

                if (!collectingIndent) {
                    for (int i = 0; i < indent; i++) {
                        out.write(' ');
                    }
                    column = indent;
                }
            }

            out.write(c);

            if (c == '\n') {
                bol();
            } else {
                column++;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            while (len > 0) {
                write(cbuf[off]);
                off++;
                len--;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            while (len > 0) {
                write(str.charAt(off));
                off++;
                len--;
            }
        }
    }

    /**
     * Indicates that output is at the beginning of a line.
     */
    private void bol() {
        column = 0;
        collectingIndent = (maxIndent != 0);
        indent = 0;
    }
}

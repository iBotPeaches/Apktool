/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

import com.google.common.collect.Lists;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class WrappedIndentingWriter extends FilterWriter {

    private final int maxIndent;
    private final int maxWidth;

    private int currentIndent = 0;
    private final StringBuilder line = new StringBuilder();

    public WrappedIndentingWriter(Writer out, int maxIndent, int maxWidth) {
        super(out);
        this.maxIndent = maxIndent;
        this.maxWidth = maxWidth;
    }

    private void writeIndent() throws IOException {
        for (int i=0; i<getIndent(); i++) {
            write(' ');
        }
    }

    private int getIndent() {
        if (currentIndent < 0) {
            return 0;
        }
        if (currentIndent > maxIndent) {
            return maxIndent;
        }
        return currentIndent;
    }

    public void indent(int indent) {
        currentIndent += indent;
    }

    public void deindent(int indent) {
        currentIndent -= indent;
    }

    private void wrapLine() throws IOException {
        List<String> wrapped = Lists.newArrayList(StringWrapper.wrapStringOnBreaks(line.toString(), maxWidth));
        out.write(wrapped.get(0), 0, wrapped.get(0).length());
        out.write('\n');

        line.replace(0, line.length(), "");
        writeIndent();
        for (int i=1; i<wrapped.size(); i++) {
            if (i > 1) {
                write('\n');
            }
            write(wrapped.get(i));
        }
    }

    @Override public void write(int c) throws IOException {
        if (c == '\n') {
            out.write(line.toString());
            out.write(c);
            line.replace(0, line.length(), "");
            writeIndent();
        } else {
            line.append((char)c);
            if (line.length() > maxWidth) {
                wrapLine();
            }
        }
    }

    @Override public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i=0; i<len; i++) {
            write(cbuf[i+off]);
        }
    }

    @Override public void write(String str, int off, int len) throws IOException {
        for (int i=0; i<len; i++) {
            write(str.charAt(i+off));
        }
    }

    @Override public void flush() throws IOException {
        out.write(line.toString());
        line.replace(0, line.length(), "");
    }
}

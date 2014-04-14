/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

import java.io.IOException;
import java.io.Writer;

public class IndentingWriter extends Writer {
    protected final Writer writer;
    protected final char[] buffer = new char[24];
    protected int indentLevel = 0;
    private boolean beginningOfLine = true;
    private static final String newLine = System.getProperty("line.separator");

    public IndentingWriter(Writer writer) {
        this.writer = writer;
    }

    protected void writeIndent() throws IOException {
        for (int i=0; i<indentLevel; i++) {
            writer.write(' ');
        }
    }

    @Override
    public void write(int chr) throws IOException {
        if (chr == '\n') {
            writer.write(newLine);
            beginningOfLine = true;
        } else {
            if (beginningOfLine) {
                writeIndent();
            }
            beginningOfLine = false;
            writer.write(chr);
        }
    }

    /**
     * Writes out a block of text that contains no newlines
     */
    private void writeLine(char[] chars, int start, int len) throws IOException {
        if (beginningOfLine && len > 0) {
            writeIndent();
            beginningOfLine = false;
        }
        writer.write(chars, start, len);
    }


    /**
     * Writes out a block of text that contains no newlines
     */
    private void writeLine(String str, int start, int len) throws IOException {
        if (beginningOfLine && len > 0) {
            writeIndent();
            beginningOfLine = false;
        }
        writer.write(str, start, len);
    }

    @Override
    public void write(char[] chars) throws IOException {
        write(chars, 0, chars.length);
    }

    @Override
    public void write(char[] chars, int start, int len) throws IOException {
        final int end = start+len;
        int pos = start;
        while (pos < end) {
            if (chars[pos] == '\n') {
                writeLine(chars, start, pos-start);

                writer.write(newLine);
                beginningOfLine = true;
                pos++;
                start = pos;
            } else {
                pos++;
            }
        }
        writeLine(chars, start, pos-start);
    }

    @Override
    public void write(String s) throws IOException {
        write(s, 0, s.length());
    }

    @Override
    public void write(String str, int start, int len) throws IOException {
        final int end = start+len;
        int pos = start;
        while (pos < end) {
            pos = str.indexOf('\n', start);
            if (pos == -1) {
                writeLine(str, start, end-start);
                return;
            } else {
                writeLine(str, start, pos-start);
                writer.write(newLine);
                beginningOfLine = true;
                start = pos+1;
            }
        }
    }

    @Override
    public Writer append(CharSequence charSequence) throws IOException {
        write(charSequence.toString());
        return this;
    }

    @Override
    public Writer append(CharSequence charSequence, int start, int len) throws IOException {
        write(charSequence.subSequence(start, len).toString());
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        write(c);
        return this;
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    public void indent(int indentAmount) {
        this.indentLevel += indentAmount;
        if (indentLevel < 0) {
            indentLevel = 0;
        }
    }

    public void deindent(int indentAmount) {
        this.indentLevel -= indentAmount;
        if (indentLevel < 0) {
            indentLevel = 0;
        }
    }

    public void printUnsignedLongAsHex(long value) throws IOException {
        int bufferIndex = 23;
        do {
            int digit = (int)(value & 15);
            if (digit < 10) {
                buffer[bufferIndex--] = (char)(digit + '0');
            } else {
                buffer[bufferIndex--] = (char)((digit - 10) + 'a');
            }

            value >>>= 4;
        } while (value != 0);

        bufferIndex++;

        writeLine(buffer, bufferIndex, 24-bufferIndex);
    }

    public void printSignedLongAsDec(long value) throws IOException {
        int bufferIndex = 23;

        if (value < 0) {
            value *= -1;
            write('-');
        }

        do {
            long digit = value % 10;
            buffer[bufferIndex--] = (char)(digit + '0');

            value = value / 10;
        } while (value != 0);

        bufferIndex++;

        writeLine(buffer, bufferIndex, 24-bufferIndex);
    }

    public void printSignedIntAsDec(int value) throws IOException {
        int bufferIndex = 15;

        if (value < 0) {
            value *= -1;
            write('-');
        }

        do {
            int digit = value % 10;
            buffer[bufferIndex--] = (char)(digit + '0');

            value = value / 10;
        } while (value != 0);

        bufferIndex++;

        writeLine(buffer, bufferIndex, 16-bufferIndex);
    }

    public void printUnsignedIntAsDec(int value) throws IOException {
        int bufferIndex = 15;

        if (value < 0) {
            printSignedLongAsDec(value & 0xFFFFFFFFL);
        } else {
            printSignedIntAsDec(value);
        }
    }
}

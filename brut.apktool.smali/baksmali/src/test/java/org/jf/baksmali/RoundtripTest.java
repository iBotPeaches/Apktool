/*
 * Copyright 2015, Google Inc.
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

package org.jf.baksmali;

import com.google.common.io.ByteStreams;
import org.antlr.runtime.RecognitionException;
import org.junit.Assert;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A base test class for performing a roundtrip assembly/disassembly
 *
 * The test accepts a smali file as input, performs a smali -> dex -> smali roundtrip, and
 * verifies that the result equals a known-good output smali file.
 *
 * By default, the input and output files should be resources at [testDir]/[testName]Input.smali
 * and [testDir]/[testName]Output.smali respectively
 */
public abstract class RoundtripTest {
    protected final String testDir;

    protected RoundtripTest(@Nonnull String testDir) {
        this.testDir = testDir;
    }

    protected RoundtripTest() {
        this.testDir = this.getClass().getSimpleName();
    }

    @Nonnull
    protected String getInputFilename(@Nonnull String testName) {
        return String.format("%s%s%sInput.smali", testDir, File.separatorChar, testName);
    }

    @Nonnull
    protected String getOutputFilename(@Nonnull String testName) {
        return String.format("%s%s%sOutput.smali", testDir, File.separatorChar, testName);
    }

    protected void runTest(@Nonnull String testName) {
        runTest(testName, new baksmaliOptions());
    }

    protected void runTest(@Nonnull String testName, @Nonnull baksmaliOptions options) {
        try {
            // Load file from resources as a stream
            String inputFilename = getInputFilename(testName);
            String input = readResourceFully(getInputFilename(testName));
            String output;
            if (getOutputFilename(testName).equals(inputFilename)) {
                output = input;
            } else {
                output = readResourceFully(getOutputFilename(testName));
            }

            // Run smali, baksmali, and then compare strings are equal (minus comments/whitespace)
            BaksmaliTestUtils.assertSmaliCompiledEquals(input, output, options, true);
        } catch (IOException ex) {
            Assert.fail();
        } catch (RecognitionException ex) {
            Assert.fail();
        }
    }

    @Nonnull
    public static String readResourceFully(@Nonnull String fileName) throws IOException {
        return readResourceFully(fileName, "UTF-8");
    }

    @Nonnull
    public static String readResourceFully(@Nonnull String fileName, @Nonnull String encoding)
            throws IOException {
        InputStream smaliStream = RoundtripTest.class.getClassLoader().
                getResourceAsStream(fileName);
        if (smaliStream == null) {
            Assert.fail("Could not load " + fileName);
        }

        return new String(ByteStreams.toByteArray(smaliStream), encoding);
    }
}

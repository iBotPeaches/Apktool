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

package org.jf.dexlib2.util;

import com.google.common.io.ByteStreams;
import org.jf.dexlib2.dexbacked.DexBackedDexFile.NotADexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile.NotAnOdexFile;
import org.jf.dexlib2.dexbacked.raw.CdexHeaderItem;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;
import org.jf.dexlib2.dexbacked.raw.OdexHeaderItem;

import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class DexUtil {

    /**
     * Reads in the dex header from the given input stream and verifies that it is valid and a supported version
     *
     * The inputStream must support mark(), and will be reset to initial position upon exiting the method
     *
     * @param inputStream An input stream that is positioned at a dex header
     * @return The dex version
     * @throws NotADexFile If the file is not a dex file
     * @throws InvalidFile If the header appears to be a dex file, but is not valid for some reason
     * @throws UnsupportedFile If the dex header is valid, but uses unsupported functionality
     */
    public static int verifyDexHeader(@Nonnull InputStream inputStream) throws IOException {
        if (!inputStream.markSupported()) {
            throw new IllegalArgumentException("InputStream must support mark");
        }
        inputStream.mark(44);
        byte[] partialHeader = new byte[44];
        try {
            ByteStreams.readFully(inputStream, partialHeader);
        } catch (EOFException ex) {
            throw new NotADexFile("File is too short");
        } finally {
            inputStream.reset();
        }

        return verifyDexHeader(partialHeader, 0);
    }

    /**
     * Verifies that the dex header is valid and a supported version
     *
     * @param buf A byte array containing at least the first 44 bytes of a dex file
     * @param offset The offset within the array to the dex header
     * @return The dex version
     * @throws NotADexFile If the file is not a dex file
     * @throws InvalidFile If the header appears to be a dex file, but is not valid for some reason
     * @throws UnsupportedFile If the dex header is valid, but uses unsupported functionality
     */
    public static int verifyDexHeader(@Nonnull byte[] buf, int offset) {
        int dexVersion = HeaderItem.getVersion(buf, offset);
        if (dexVersion == -1) {
            StringBuilder sb = new StringBuilder("Not a valid dex magic value:");
            for (int i=0; i<8; i++) {
                sb.append(String.format(" %02x", buf[i]));
            }
            throw new NotADexFile(sb.toString());
        }

        if (!HeaderItem.isSupportedDexVersion(dexVersion)) {
            throw new UnsupportedFile(String.format("Dex version %03d is not supported", dexVersion));
        }

        int endian = HeaderItem.getEndian(buf, offset);
        if (endian == HeaderItem.BIG_ENDIAN_TAG) {
            throw new UnsupportedFile("Big endian dex files are not supported");
        }

        if (endian != HeaderItem.LITTLE_ENDIAN_TAG) {
            throw new InvalidFile(String.format("Invalid endian tag: 0x%x", endian));
        }

        return dexVersion;
    }

    /**
     * Verifies that the cdex header is valid and a supported version
     *
     * @param buf A byte array containing at least the first 44 bytes of a cdex file
     * @param offset The offset within the array to the dex header
     * @return The dex version
     * @throws NotADexFile If the file is not a cdex file
     * @throws InvalidFile If the header appears to be a cdex file, but is not valid for some reason
     * @throws UnsupportedFile If the cdex header is valid, but uses unsupported functionality
     */
    public static int verifyCdexHeader(@Nonnull byte[] buf, int offset) {
        int cdexVersion = CdexHeaderItem.getVersion(buf, offset);
        if (cdexVersion == -1) {
            StringBuilder sb = new StringBuilder("Not a valid cdex magic value:");
            for (int i=0; i<8; i++) {
                sb.append(String.format(" %02x", buf[offset + i]));
            }
            throw new NotADexFile(sb.toString());
        }

        if (!CdexHeaderItem.isSupportedCdexVersion(cdexVersion)) {
            throw new UnsupportedFile(String.format("Dex version %03d is not supported", cdexVersion));
        }

        int endian = HeaderItem.getEndian(buf, offset);
        if (endian == HeaderItem.BIG_ENDIAN_TAG) {
            throw new UnsupportedFile("Big endian dex files are not supported");
        }

        if (endian != HeaderItem.LITTLE_ENDIAN_TAG) {
            throw new InvalidFile(String.format("Invalid endian tag: 0x%x", endian));
        }

        return cdexVersion;
    }

    /**
     * Reads in the odex header from the given input stream and verifies that it is valid and a supported version
     *
     * The inputStream must support mark(), and will be reset to initial position upon exiting the method
     *
     * @param inputStream An input stream that is positioned at an odex header
     * @throws NotAnOdexFile If the file is not an odex file
     * @throws UnsupportedFile If the odex header is valid, but is an unsupported version
     */
    public static void verifyOdexHeader(@Nonnull InputStream inputStream) throws IOException {
        if (!inputStream.markSupported()) {
            throw new IllegalArgumentException("InputStream must support mark");
        }
        inputStream.mark(8);
        byte[] partialHeader = new byte[8];
        try {
            ByteStreams.readFully(inputStream, partialHeader);
        } catch (EOFException ex) {
            throw new NotAnOdexFile("File is too short");
        } finally {
            inputStream.reset();
        }

        verifyOdexHeader(partialHeader, 0);
    }

    /**
     * Verifies that the odex header is valid and a supported version
     *
     * @param buf A byte array containing at least the first 8 bytes of an odex file
     * @param offset The offset within the array to the odex header
     * @throws NotAnOdexFile If the file is not an odex file
     * @throws UnsupportedFile If the odex header is valid, but uses unsupported functionality
     */
    public static void verifyOdexHeader(@Nonnull byte[] buf, int offset) {
        int odexVersion = OdexHeaderItem.getVersion(buf, offset);
        if (odexVersion == -1) {
            StringBuilder sb = new StringBuilder("Not a valid odex magic value:");
            for (int i=0; i<8; i++) {
                sb.append(String.format(" %02x", buf[i]));
            }
            throw new NotAnOdexFile(sb.toString());
        }

        if (!OdexHeaderItem.isSupportedOdexVersion(odexVersion)) {
            throw new UnsupportedFile(String.format("Odex version %03d is not supported", odexVersion));
        }
    }

    public static class InvalidFile extends RuntimeException {
        public InvalidFile() {
        }

        public InvalidFile(String message) {
            super(message);
        }

        public InvalidFile(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidFile(Throwable cause) {
            super(cause);
        }
    }

    public static class UnsupportedFile extends RuntimeException {
        public UnsupportedFile() {
        }

        public UnsupportedFile(String message) {
            super(message);
        }

        public UnsupportedFile(String message, Throwable cause) {
            super(message, cause);
        }

        public UnsupportedFile(Throwable cause) {
            super(cause);
        }
    }
}

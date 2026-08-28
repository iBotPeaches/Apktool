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
package brut.zip;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class RepairingZipFileTest {
    private static final String PAYLOAD = "stored entry payload";

    @Test
    public void cleanZipOpensInPlace() throws IOException {
        File zip = buildZip(false);
        try (RepairingZipFile file = new RepairingZipFile(zip)) {
            assertEquals(PAYLOAD, readEntry(file, "entry.txt"));
        }
        assertTrue(zip.isFile());
    }

    @Test
    public void spuriousEncryptedBitIsTolerated() throws IOException {
        File zip = buildZip(true);
        try {
            new java.util.zip.ZipFile(zip);
            fail("base ZipFile unexpectedly opens the corrupted archive");
        } catch (ZipException expected) {
        }
        try (RepairingZipFile file = new RepairingZipFile(zip)) {
            assertEquals(PAYLOAD, readEntry(file, "entry.txt"));
        }
        // The source archive keeps its bytes; only the removed temp copy was repaired.
        assertTrue(zip.isFile());
        assertEquals(PAYLOAD.length(), storedEntrySize(zip));
    }

    @Test
    public void garbageArchiveStillFails() throws IOException {
        File garbage = File.createTempFile("garbage-", ".zip");
        garbage.deleteOnExit();
        Files.write(garbage.toPath(), "not a zip archive".getBytes(StandardCharsets.US_ASCII));
        try {
            new RepairingZipFile(garbage);
            fail("non-archive input must fail, not be repaired");
        } catch (ZipException expected) {
        }
    }

    private static File buildZip(boolean setEncryptedBit) throws IOException {
        File zip = File.createTempFile("repairing-", ".zip");
        zip.deleteOnExit();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            out.putNextEntry(new ZipEntry("entry.txt"));
            out.write(PAYLOAD.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        if (setEncryptedBit) {
            setEncryptedBit(zip);
        }
        return zip;
    }

    private static void setEncryptedBit(File zip) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(zip, "rw")) {
            long cdfhOff = findFirstCdfh(f);
            f.seek(cdfhOff + 8);
            writeFlagWithEncryptedBit(f);
            // Re-read the LFH offset from the CDFH, then set the flag there too.
            f.seek(cdfhOff + 42);
            long rawLfhOff = readUInt(f);
            f.seek(rawLfhOff + 6);
            writeFlagWithEncryptedBit(f);
        }
    }

    private static void writeFlagWithEncryptedBit(RandomAccessFile f) throws IOException {
        int flags = f.read() | (f.read() << 8);
        f.seek(f.getFilePointer() - 2);
        f.write((flags | 0x1) & 0xff);
        f.write(((flags | 0x1) >> 8) & 0xff);
    }

    private static long findFirstCdfh(RandomAccessFile f) throws IOException {
        long size = f.length();
        int maxSearch = (int) Math.min(size, 22 + 0xFFFF);
        f.seek(size - maxSearch);
        byte[] data = new byte[maxSearch];
        f.readFully(data);
        for (int i = 0; i <= data.length - 4; i++) {
            if (data[i] == 0x50 && data[i + 1] == 0x4b && data[i + 2] == 0x01 && data[i + 3] == 0x02) {
                return (size - maxSearch) + i;
            }
        }
        throw new IOException("central directory not found");
    }

    private static long readUInt(RandomAccessFile f) throws IOException {
        long b0 = f.read();
        long b1 = f.read();
        long b2 = f.read();
        long b3 = f.read();
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private static String readEntry(RepairingZipFile file, String name) throws IOException {
        ZipEntry entry = file.getEntry(name);
        assertNotNull(entry);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = file.getInputStream(entry)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static long storedEntrySize(File zip) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(zip, "rw")) {
            long cdfhOff = findFirstCdfh(f);
            f.seek(cdfhOff + 24);
            return readUInt(f);
        }
    }
}

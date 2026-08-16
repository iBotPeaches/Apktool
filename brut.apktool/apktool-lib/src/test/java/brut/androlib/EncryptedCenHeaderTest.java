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
package brut.androlib;

import brut.directory.ExtFile;
import brut.util.OS;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.*;
import static org.junit.Assert.*;

public class EncryptedCenHeaderTest extends BaseTest {
    private static ExtFile sCorruptedApk;

    @BeforeClass
    public static void beforeClass() throws Exception {
        File buildDir = new File(sTmpDir, "encrypted_cen-build");
        copyResourceDir(EncryptedCenHeaderTest.class, "testapp", buildDir);

        log("Building base testapp.apk...");
        ExtFile baseApk = new ExtFile(sTmpDir, "encrypted_cen-base.apk");
        new ApkBuilder(buildDir, sConfig).build(baseApk);

        sCorruptedApk = new ExtFile(sTmpDir, "encrypted_cen.apk");
        OS.cpfile(baseApk, sCorruptedApk);
        baseApk.close();

        log("Setting spurious encrypted bit on zip headers...");
        setSpuriousEncryptedBit(sCorruptedApk);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (sCorruptedApk != null) {
            sCorruptedApk.close();
        }
    }

    @Test
    public void decodeSpuriousEncryptedBitApkTest() throws Exception {
        File outDir = new File(sTmpDir, "encrypted_cen.out");
        new ApkDecoder(sCorruptedApk, sConfig).decode(outDir);
        assertTrue(new File(outDir, "AndroidManifest.xml").isFile());
        assertTrue(readTextFile(new File(outDir, "apktool.yml")).contains("apkFileName: encrypted_cen.apk"));

        ExtFile decodeDir = new ExtFile(outDir);
        new ApkBuilder(decodeDir, sConfig).build(null);
        assertTrue(new File(outDir, "dist/encrypted_cen.apk").isFile());
    }

    private static void setSpuriousEncryptedBit(File apk) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(apk, "rw")) {
            byte[] eocdData = findEocd(f);
            int cdfhCnt = readUShort(eocdData, 10);
            int cdOff = readUInt(eocdData, 16);
            f.seek(cdOff);
            int cdfhIdx = 0;
            while (cdfhIdx < cdfhCnt) {
                long cdfhOff = f.getFilePointer();
                f.seek(cdfhOff + 8);
                setEncryptedFlag(f);
                f.seek(cdfhOff + 28);
                int nameLen = readUShort(f);
                int extraLen = readUShort(f);
                int commentLen = readUShort(f);
                f.seek(cdfhOff + 42);
                long lfhOff = readUInt(f);
                f.seek(lfhOff + 6);
                setEncryptedFlag(f);
                f.seek(cdfhOff + 46 + nameLen + extraLen + commentLen);
                cdfhIdx++;
            }
        }
    }

    private static void setEncryptedFlag(RandomAccessFile f) throws IOException {
        long flagsPos = f.getFilePointer();
        int flags = readUShort(f);
        f.seek(flagsPos);
        writeUShort(f, flags | 0x1);
    }

    private static byte[] findEocd(RandomAccessFile f) throws IOException {
        int EOCD_SIG = 0x06054b50;
        int EOCD_MIN_LEN = 22;
        int COMMENT_MAX_LEN = 0xFFFF;
        long size = f.length();
        int maxSearch = (int) Math.min(size, EOCD_MIN_LEN + COMMENT_MAX_LEN);
        f.seek(size - maxSearch);
        byte[] data = new byte[maxSearch];
        f.readFully(data);
        int idx = lastIndexOf(data, EOCD_SIG);
        if (idx < 0) {
            throw new IOException("end of central directory record not found");
        }
        byte[] eocd = new byte[EOCD_MIN_LEN];
        System.arraycopy(data, idx, eocd, 0, EOCD_MIN_LEN);
        return eocd;
    }

    private static int lastIndexOf(byte[] data, int value) {
        int b0 = value & 0xff;
        int b1 = (value >> 8) & 0xff;
        int b2 = (value >> 16) & 0xff;
        int b3 = (value >> 24) & 0xff;
        for (int i = data.length - 4; i >= 0; i--) {
            if (data[i] == b0 && data[i + 1] == b1 && data[i + 2] == b2 && data[i + 3] == b3) {
                return i;
            }
        }
        return -1;
    }

    private static int readUShort(RandomAccessFile f) throws IOException {
        int b0 = f.read();
        int b1 = f.read();
        if (b0 < 0 || b1 < 0) {
            throw new IOException("unexpected end of file");
        }
        return (b1 << 8) | b0;
    }

    private static long readUInt(RandomAccessFile f) throws IOException {
        long b0 = f.read();
        long b1 = f.read();
        long b2 = f.read();
        long b3 = f.read();
        if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
            throw new IOException("unexpected end of file");
        }
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private static void writeUShort(RandomAccessFile f, int value) throws IOException {
        f.write(value & 0xff);
        f.write((value >> 8) & 0xff);
    }

    private static int readUShort(byte[] data, int offset) {
        return (data[offset + 1] << 8) | (data[offset] & 0xff);
    }

    private static int readUInt(byte[] data, int offset) {
        return (data[offset] & 0xff)
            | ((data[offset + 1] & 0xff) << 8)
            | ((data[offset + 2] & 0xff) << 16)
            | ((data[offset + 3] & 0xff) << 24);
    }
}

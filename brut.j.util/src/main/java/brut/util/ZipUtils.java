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
package brut.util;

import brut.common.Log;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {
    private static final String TAG = "";

    private static final int EOCD_SIG = 0x06054b50;
    private static final int CDFH_SIG = 0x02014b50;
    private static final int LFH_SIG = 0x04034b50;
    private static final int CDFH_LEN = 46;
    private static final int EOCD_MIN_LEN = 22;
    private static final int COMMENT_MAX_LEN = 0xFFFF;
    private static final int EOCD_MAX_SEARCH = EOCD_MIN_LEN + COMMENT_MAX_LEN;

    private ZipUtils() {
        // Private constructor for utility class.
    }

    /**
     * Copies {@code source} and repairs central-directory / local-header fields that AOSP tolerates
     * but {@code java.util.zip.ZipFile} rejects (spurious encrypted bit, non-DEFLATE/STORE methods).
     */
    public static File openReadableZip(File source) throws IOException {
        try (ZipFile zip = new ZipFile(source)) {
            return source;
        } catch (ZipException ex) {
            if (!isRepairableCenHeader(ex)) {
                throw ex;
            }
            return repairInvalidCenHeadersCopy(source);
        }
    }

    public static boolean isRepairableCenHeader(IOException ex) {
        if (ex instanceof ZipException) {
            String message = ex.getMessage();
            return message != null && message.contains("invalid CEN header");
        }
        return false;
    }

    public static File repairInvalidCenHeadersCopy(File source) throws IOException {
        File dest = Files.createTempFile("apktool-zip-repair-", ".zip").toFile();
        dest.deleteOnExit();
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        repairInvalidCenHeaders(dest);
        return dest;
    }

    public static void repairInvalidCenHeaders(File file) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(file, "rw")) {
            byte[] eocdData = findEocd(f);
            int cdfhCnt = readUShort(eocdData, 10);
            int cdOff = readUInt(eocdData, 16);
            f.seek(cdOff);
            int cdfhIdx = 0;
            while (cdfhIdx < cdfhCnt) {
                long cdfhOff = f.getFilePointer();
                long cdfhSig = readUInt(f);
                if (cdfhSig != CDFH_SIG) {
                    throw new IOException("invalid central directory file header signature");
                }
                f.seek(cdfhOff + 8);
                fixHeaderFields(f);
                f.seek(cdfhOff + 28);
                int nameLen = readUShort(f);
                int extraLen = readUShort(f);
                int commentLen = readUShort(f);
                f.seek(cdfhOff + 42);
                long lfhOff = readUInt(f);
                f.seek(lfhOff);
                long lfhSig = readUInt(f);
                if (lfhSig != LFH_SIG) {
                    throw new IOException("invalid local file header signature");
                }
                f.seek(lfhOff + 6);
                fixHeaderFields(f);
                f.seek(cdfhOff + CDFH_LEN + nameLen + extraLen + commentLen);
                cdfhIdx++;
            }
        }
    }

    private static byte[] findEocd(RandomAccessFile f) throws IOException {
        long size = f.length();
        int maxSearch = (int) Math.min(size, EOCD_MAX_SEARCH);
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

    private static void fixHeaderFields(RandomAccessFile f) throws IOException {
        long flagsPos = f.getFilePointer();
        int flags = readUShort(f);
        if ((flags & 0x1) != 0) {
            f.seek(flagsPos);
            writeUShort(f, flags & ~0x1);
        }
        long comprPos = flagsPos + 2;
        f.seek(comprPos);
        int compr = readUShort(f);
        if (compr != 0 && compr != 8) {
            f.seek(comprPos);
            writeUShort(f, 0);
        }
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

    public static void zipDir(File dir, ZipOutputStream out, Collection<String> doNotCompress) throws IOException {
        zipDir(dir, null, out, doNotCompress);
    }

    public static void zipDir(File baseDir, String dirName, ZipOutputStream out, Collection<String> doNotCompress)
            throws IOException {
        File dir;
        if (dirName != null && !dirName.isEmpty()) {
            dir = new File(baseDir, dirName);
        } else {
            dir = baseDir;
        }
        if (!dir.isDirectory()) {
            return;
        }

        for (File file : dir.listFiles()) {
            String fileName = baseDir.toPath().relativize(file.toPath()).toString();

            if (file.isDirectory()) {
                zipDir(baseDir, fileName, out, doNotCompress);
            } else if (file.isFile()) {
                zipFile(baseDir, fileName, out, (doNotCompress != null && !doNotCompress.isEmpty())
                    ? entryName -> doNotCompress.contains(entryName)
                                || doNotCompress.contains(FilenameUtils.getExtension(entryName))
                    : entryName -> false);
            }
        }
    }

    public static void zipFile(File baseDir, String fileName, ZipOutputStream out, boolean doNotCompress)
            throws IOException {
        zipFile(baseDir, fileName, out, entryName -> doNotCompress);
    }

    private static void zipFile(File baseDir, String fileName, ZipOutputStream out, Predicate<String> doNotCompress)
            throws IOException {
        try {
            fileName = BrutIO.sanitizePath(baseDir, fileName);
            if (fileName.isEmpty()) {
                return;
            }

            File file = new File(baseDir, fileName);
            if (!file.isFile()) {
                return;
            }

            String entryName = FilenameUtils.separatorsToUnix(fileName);
            ZipEntry zipEntry = new ZipEntry(entryName);

            if (doNotCompress.test(entryName)) {
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setSize(file.length());
                try (InputStream in = Files.newInputStream(file.toPath())) {
                    CRC32 crc = BrutIO.calculateCrc(in);
                    zipEntry.setCrc(crc.getValue());
                }
            } else {
                zipEntry.setMethod(ZipEntry.DEFLATED);
            }

            out.putNextEntry(zipEntry);
            try (InputStream in = Files.newInputStream(file.toPath())) {
                IOUtils.copy(in, out);
            }
            out.closeEntry();
        } catch (InvalidPathException ex) {
            Log.w(TAG, "Skipping file %s (%s)", fileName, ex.getMessage());
        }
    }
}

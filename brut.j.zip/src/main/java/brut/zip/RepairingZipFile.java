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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A {@link ZipFile} that tolerates the spurious encrypted-entry bit some producers
 * leave on central-directory and local file headers of classic single-disk ZIPs.
 * AOSP reads such archives unencrypted while {@code java.util.zip.ZipFile} rejects them.
 *
 * <p>An archive that opens cleanly is read in place. A repairable one is copied to a temporary
 * file, the flag is cleared there, and the copy is opened; the copy is removed on {@link #close()}.
 */
public class RepairingZipFile extends ZipFile {
    private static final int EOCD_SIG = 0x06054b50;
    private static final int CDFH_SIG = 0x02014b50;
    private static final int LFH_SIG = 0x04034b50;
    private static final int CDFH_LEN = 46;
    private static final int EOCD_MIN_LEN = 22;
    private static final int COMMENT_MAX_LEN = 0xFFFF;
    private static final int EOCD_MAX_SEARCH = EOCD_MIN_LEN + COMMENT_MAX_LEN;

    private final File repairCopy;

    public RepairingZipFile(File file) throws IOException {
        this(openTarget(file));
    }

    private RepairingZipFile(OpenTarget target) throws IOException {
        super(target.file);
        this.repairCopy = target.repairCopy;
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            super.close();
        } catch (IOException ex) {
            failure = ex;
        }
        if (repairCopy != null) {
            try {
                Files.deleteIfExists(repairCopy.toPath());
            } catch (IOException ex) {
                if (failure == null) {
                    failure = ex;
                } else {
                    failure.addSuppressed(ex);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static OpenTarget openTarget(File source) throws IOException {
        try (ZipFile ignored = new ZipFile(source)) {
            return new OpenTarget(source, null);
        } catch (ZipException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains("invalid CEN header (encrypted entry)")) {
                throw ex;
            }
        }
        File dest = Files.createTempFile("apktool-zip-repair-", ".zip").toFile();
        dest.deleteOnExit();
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            repairInvalidCenHeaders(dest);
            try (ZipFile ignored = new ZipFile(dest)) {
                return new OpenTarget(dest, dest);
            }
        } catch (IOException | RuntimeException ex) {
            try {
                Files.deleteIfExists(dest.toPath());
            } catch (IOException cleanup) {
                ex.addSuppressed(cleanup);
            }
            throw ex;
        }
    }

    private static void repairInvalidCenHeaders(File file) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(file, "rw")) {
            EndRecord eocd = findEocd(f);
            byte[] eocdData = eocd.data;
            int disk = readUShort(eocdData, 4);
            int centralDirectoryDisk = readUShort(eocdData, 6);
            int diskEntryCount = readUShort(eocdData, 8);
            int cdfhCnt = readUShort(eocdData, 10);
            long cdSize = readUInt(eocdData, 12);
            long storedCdOff = readUInt(eocdData, 16);
            if (disk != 0 || centralDirectoryDisk != 0 || diskEntryCount != cdfhCnt) {
                throw new ZipException("multi-disk ZIP repair is not supported");
            }
            if (cdfhCnt == 0xffff || cdSize == 0xffffffffL || storedCdOff == 0xffffffffL) {
                throw new ZipException("ZIP64 repair is not supported");
            }
            long cdOff = eocd.offset - cdSize;
            long prefixLength = cdOff - storedCdOff;
            if (cdOff < 0 || prefixLength < 0) {
                throw new ZipException("invalid central directory offset");
            }
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
                f.seek(prefixLength + lfhOff);
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

    private static EndRecord findEocd(RandomAccessFile f) throws IOException {
        long size = f.length();
        int maxSearch = (int) Math.min(size, EOCD_MAX_SEARCH);
        long searchStart = size - maxSearch;
        f.seek(searchStart);
        byte[] data = new byte[maxSearch];
        f.readFully(data);
        for (int idx = data.length - EOCD_MIN_LEN; idx >= 0; idx--) {
            if (hasSignature(data, idx, EOCD_SIG)
                && readUShort(data, idx + 20) == data.length - idx - EOCD_MIN_LEN) {
                byte[] eocd = new byte[EOCD_MIN_LEN];
                System.arraycopy(data, idx, eocd, 0, EOCD_MIN_LEN);
                return new EndRecord(eocd, searchStart + idx);
            }
        }
        throw new IOException("end of central directory record not found");
    }

    private static void fixHeaderFields(RandomAccessFile f) throws IOException {
        // Only the spurious encrypted bit is cleared (issue #4028): AOSP reads
        // these entries unencrypted, and the payload bytes need no re-interpretation.
        // Unknown compression methods are left alone — relabeling them as stored
        // would corrupt entries whose bytes are actually deflated.
        long flagsPos = f.getFilePointer();
        int flags = readUShort(f);
        if ((flags & 0x1) != 0) {
            f.seek(flagsPos);
            writeUShort(f, flags & ~0x1);
        }
    }

    private static boolean hasSignature(byte[] data, int offset, int value) {
        int b0 = value & 0xff;
        int b1 = (value >> 8) & 0xff;
        int b2 = (value >> 16) & 0xff;
        int b3 = (value >> 24) & 0xff;
        return data[offset] == b0 && data[offset + 1] == b1
            && data[offset + 2] == b2 && data[offset + 3] == b3;
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
        return ((data[offset + 1] & 0xff) << 8) | (data[offset] & 0xff);
    }

    private static long readUInt(byte[] data, int offset) {
        return (data[offset] & 0xffL)
            | ((data[offset + 1] & 0xffL) << 8)
            | ((data[offset + 2] & 0xffL) << 16)
            | ((data[offset + 3] & 0xffL) << 24);
    }

    private static final class OpenTarget {
        final File file;
        final File repairCopy;

        OpenTarget(File file, File repairCopy) {
            this.file = file;
            this.repairCopy = repairCopy;
        }
    }

    private static final class EndRecord {
        final byte[] data;
        final long offset;

        EndRecord(byte[] data, long offset) {
            this.data = data;
            this.offset = offset;
        }
    }
}

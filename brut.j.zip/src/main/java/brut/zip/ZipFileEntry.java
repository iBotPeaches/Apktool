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

import brut.util.IOUtils;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;

public final class ZipFileEntry implements Comparable<ZipFileEntry> {
    public static final int STORED = ZipEntry.STORED;
    public static final int DEFLATED = ZipEntry.DEFLATED;

    private final ZipArchive mArchive;
    private final ZipDirEntry mParent;
    private final String mName;
    private final ZipEntry mEntry;

    ZipFileEntry(ZipArchive archive, ZipDirEntry parent, String name, ZipEntry entry) {
        mArchive = archive;
        mParent = parent;
        mName = name;
        mEntry = entry;
    }

    public ZipArchive getArchive() {
        return mArchive;
    }

    public ZipDirEntry getParent() {
        return mParent;
    }

    public String getName() {
        return mName;
    }

    public long getTime() {
        return mEntry.getTime();
    }

    public FileTime getLastModifiedTime() {
        return mEntry.getLastModifiedTime();
    }

    public FileTime getLastAccessTime() {
        return mEntry.getLastAccessTime();
    }

    public FileTime getCreationTime() {
        return mEntry.getCreationTime();
    }

    public long getSize() {
        return mEntry.getSize();
    }

    public long getCompressedSize() {
        return mEntry.getCompressedSize();
    }

    public boolean isEmpty() {
        return getSize() == 0;
    }

    public long getCrc() {
        return mEntry.getCrc();
    }

    public int getMethod() {
        return mEntry.getMethod();
    }

    public boolean isStored() {
        return getMethod() == STORED;
    }

    public boolean isDeflated() {
        return getMethod() == DEFLATED;
    }

    public byte[] getExtra() {
        return mEntry.getExtra();
    }

    public String getComment() {
        return mEntry.getComment();
    }

    public InputStream getInputStream() throws IOException {
        return mArchive.getInputStream(mEntry);
    }

    public void extract(Path dest) throws IOException {
        IOUtils.createParentDirectories(dest);
        try (InputStream in = getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public String toString() {
        return String.format("ZipFileEntry{archive=%s, name=%s}", mParent, mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZipFileEntry other = (ZipFileEntry) obj;
        return mParent.equals(other.mParent)
            && mName.equals(other.mName);
    }

    @Override
    public int hashCode() {
        return 31 * mParent.hashCode() + mName.hashCode();
    }

    @Override
    public int compareTo(ZipFileEntry other) {
        int result = mArchive.getPath().compareTo(other.mArchive.getPath());
        if (result != 0) {
            return result;
        }
        return mName.compareTo(other.mName);
    }
}

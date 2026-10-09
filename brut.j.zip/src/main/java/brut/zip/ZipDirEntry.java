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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class ZipDirEntry implements Comparable<ZipDirEntry> {
    private final ZipArchive mArchive;
    private final ZipDirEntry mParent;
    private final String mName;
    private Map<String, ZipDirEntry> mDirs;
    private Map<String, ZipFileEntry> mFiles;

    ZipDirEntry(ZipArchive archive, ZipDirEntry parent, String name) {
        mArchive = archive;
        mParent = parent;
        mName = name;
    }

    ZipDirEntry getOrCreateDir(String key, String name) {
        if (mDirs == null) {
            mDirs = new HashMap<>();
        }
        return mDirs.computeIfAbsent(key, n -> new ZipDirEntry(mArchive, this, name));
    }

    void putFile(String key, String name, ZipEntry entry) {
        if (mFiles == null) {
            mFiles = new HashMap<>();
        }
        mFiles.put(key, new ZipFileEntry(mArchive, this, name, entry));
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

    public boolean isEmpty() {
        return (mDirs == null || mDirs.isEmpty()) && (mFiles == null || mFiles.isEmpty());
    }

    public Stream<ZipDirEntry> listDirs() {
        return mDirs != null ? mDirs.values().stream() : Stream.empty();
    }

    public Stream<ZipDirEntry> walkDirs() {
        return mDirs != null
            ? mDirs.values().stream().flatMap(dir -> Stream.concat(Stream.of(dir), dir.walkDirs()))
            : Stream.empty();
    }

    public boolean containsDir(String name) {
        return resolveDir(ZipUtils.normalize(name)) != null;
    }

    public ZipDirEntry getDir(String name) throws ZipNoSuchEntryException {
        name = ZipUtils.normalize(name);
        ZipDirEntry dir = resolveDir(name);
        if (dir == null) {
            throw new ZipNoSuchEntryException(mName.isEmpty() ? name : mName + ZipUtils.SEPARATOR + name);
        }
        return dir;
    }

    private ZipDirEntry resolveDir(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is empty.");
        }
        ZipDirEntry dir = this;
        int len = name.length();
        for (int i = 0, segEnd; dir != null && i < len; i = segEnd + 1) {
            segEnd = name.indexOf(ZipUtils.SEPARATOR_CHAR, i);
            if (segEnd == -1) {
                segEnd = len;
            }
            dir = dir.mDirs != null ? dir.mDirs.get(name.substring(i, segEnd)) : null;
        }
        return dir;
    }

    public Stream<ZipFileEntry> listFiles() {
        return mFiles != null ? mFiles.values().stream() : Stream.empty();
    }

    public Stream<ZipFileEntry> walkFiles() {
        Stream<ZipFileEntry> files = listFiles();
        return mDirs != null
            ? Stream.concat(files, mDirs.values().stream().flatMap(dir -> dir.walkFiles()))
            : files;
    }

    public boolean containsFile(String name) {
        return resolveFile(ZipUtils.normalize(name)) != null;
    }

    public ZipFileEntry getFile(String name) throws ZipNoSuchEntryException {
        name = ZipUtils.normalize(name);
        ZipFileEntry file = resolveFile(name);
        if (file == null) {
            throw new ZipNoSuchEntryException(mName.isEmpty() ? name : mName + ZipUtils.SEPARATOR + name);
        }
        return file;
    }

    private ZipFileEntry resolveFile(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is empty.");
        }
        ZipDirEntry dir = this;
        for (int i = 0, segEnd; dir != null; i = segEnd + 1) {
            segEnd = name.indexOf(ZipUtils.SEPARATOR_CHAR, i);
            if (segEnd == -1) {
                return dir.mFiles != null ? dir.mFiles.get(name.substring(i)) : null;
            }
            dir = dir.mDirs != null ? dir.mDirs.get(name.substring(i, segEnd)) : null;
        }
        return null;
    }

    public void extract(Path dest) throws IOException {
        Files.createDirectories(dest);
        if (mFiles != null) {
            for (Map.Entry<String, ZipFileEntry> entry : mFiles.entrySet()) {
                entry.getValue().extract(dest.resolve(entry.getKey()));
            }
        }
        if (mDirs != null) {
            for (Map.Entry<String, ZipDirEntry> entry : mDirs.entrySet()) {
                entry.getValue().extract(dest.resolve(entry.getKey()));
            }
        }
    }

    public void extract(Path dest, String name) throws IOException {
        name = ZipUtils.normalize(name);
        ZipDirEntry dir = resolveDir(name);
        if (dir != null) {
            dir.extract(dest.resolve(name));
            return;
        }
        ZipFileEntry file = resolveFile(name);
        if (file != null) {
            file.extract(dest.resolve(name));
        }
    }

    public void extract(Path dest, String... names) throws IOException {
        for (String name : names) {
            extract(dest, name);
        }
    }

    @Override
    public String toString() {
        return String.format("ZipDirEntry{archive=%s, name=%s}", mArchive, mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZipDirEntry other = (ZipDirEntry) obj;
        return mArchive.equals(other.mArchive)
            && mName.equals(other.mName);
    }

    @Override
    public int hashCode() {
        return 31 * mArchive.hashCode() + mName.hashCode();
    }

    @Override
    public int compareTo(ZipDirEntry other) {
        int result = mArchive.getPath().compareTo(other.mArchive.getPath());
        if (result != 0) {
            return result;
        }
        return mName.compareTo(other.mName);
    }
}

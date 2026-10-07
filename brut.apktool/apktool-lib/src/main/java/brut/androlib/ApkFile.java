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

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.meta.ApkInfo;
import brut.zip.ZipArchive;
import brut.zip.ZipDirEntry;
import brut.zip.ZipFileEntry;
import brut.zip.ZipNoSuchEntryException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

public class ApkFile implements Closeable {
    // Fixed entry time for reproducible archives. Use with ZipEntry.setTimeLocal(LocalDateTime) on Java 9+.
    public static final LocalDateTime TIME_LOCAL = LocalDateTime.of(2009, 1, 1, 0, 0);
    // Same time as TIME_LOCAL, as epoch millis in the default zone. Use with ZipEntry.setTime(long) on Java 8.
    public static final long TIME_MILLIS = TIME_LOCAL.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    private final Path mPath;
    private ZipArchive mArchive;
    private ApkInfo mApkInfo;

    public ApkFile(String path) {
        this(Paths.get(path));
    }

    public ApkFile(Path path) {
        mPath = path;
    }

    public Path getPath() {
        return mPath;
    }

    public ApkInfo getApkInfo() {
        if (mApkInfo == null) {
            mApkInfo = new ApkInfo();
            mApkInfo.setApkFileName(mPath.getFileName().toString());
        }
        return mApkInfo;
    }

    @Override
    public void close() throws IOException {
        if (mArchive != null) {
            mArchive.close();
            mArchive = null;
        }
    }

    private void ensureOpen() throws AndrolibException {
        if (mArchive == null) {
            try {
                mArchive = new ZipArchive(mPath);
            } catch (IOException ex) {
                throw new AndrolibException("Could not open apk file: " + mPath, ex);
            }
        }
    }

    public Stream<ZipDirEntry> listDirs() throws AndrolibException {
        ensureOpen();
        return mArchive.listDirs();
    }

    public Stream<ZipDirEntry> walkDirs() throws AndrolibException {
        ensureOpen();
        return mArchive.walkDirs();
    }

    public boolean containsDir(String name) throws AndrolibException {
        ensureOpen();
        return mArchive.containsDir(name);
    }

    public ZipDirEntry getDir(String name) throws AndrolibException, ZipNoSuchEntryException {
        ensureOpen();
        return mArchive.getDir(name);
    }

    public Stream<ZipFileEntry> listFiles() throws AndrolibException {
        ensureOpen();
        return mArchive.listFiles();
    }

    public Stream<ZipFileEntry> walkFiles() throws AndrolibException {
        ensureOpen();
        return mArchive.walkFiles();
    }

    public boolean containsFile(String name) throws AndrolibException {
        ensureOpen();
        return mArchive.containsFile(name);
    }

    public ZipFileEntry getFile(String name) throws AndrolibException, ZipNoSuchEntryException {
        ensureOpen();
        return mArchive.getFile(name);
    }

    public void extract(Path dest) throws AndrolibException, IOException {
        ensureOpen();
        mArchive.extract(dest);
    }

    public void extract(Path dest, String name) throws AndrolibException, IOException {
        ensureOpen();
        mArchive.extract(dest, name);
    }

    public void extract(Path dest, String... names) throws AndrolibException, IOException {
        ensureOpen();
        mArchive.extract(dest, names);
    }

    @Override
    public String toString() {
        return mPath.toString();
    }
}

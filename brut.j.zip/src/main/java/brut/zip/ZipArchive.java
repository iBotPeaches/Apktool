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

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipArchive implements Closeable {
    private final Path mPath;
    private final ZipFile mZip;
    private ZipDirEntry mRoot;
    private boolean mClosed;

    public ZipArchive(Path path) throws IOException {
        mPath = path;
        mZip = new ZipFile(path.toFile());
    }

    public Path getPath() {
        return mPath;
    }

    public String getComment() {
        return mZip.getComment();
    }

    InputStream getInputStream(ZipEntry entry) throws IOException {
        return mZip.getInputStream(entry);
    }

    @Override
    public void close() throws IOException {
        if (!mClosed) {
            mZip.close();
            mRoot = null;
            mClosed = true;
        }
    }

    private void ensureLoaded() {
        if (mClosed) {
            throw new IllegalStateException();
        }
        if (mRoot != null) {
            return;
        }
        ZipDirEntry root = new ZipDirEntry(this, null, "");
        Enumeration<? extends ZipEntry> entries = mZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name;
            try {
                name = ZipUtils.normalize(entry.getName());
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (name.isEmpty()) {
                continue;
            }
            ZipDirEntry dir = root;
            for (int i = 0, segEnd;; i = segEnd + 1) {
                segEnd = name.indexOf(ZipUtils.SEPARATOR_CHAR, i);
                boolean lastSeg = segEnd == -1;
                if (lastSeg) {
                    segEnd = name.length();
                }
                String key = name.substring(i, segEnd);
                if (lastSeg) {
                    if (entry.isDirectory()) {
                        dir.getOrCreateDir(key, name);
                    } else {
                        dir.putFile(key, name, entry);
                    }
                    break;
                }
                dir = dir.getOrCreateDir(key, name.substring(0, segEnd));
            }
        }
        mRoot = root;
    }

    public Stream<ZipDirEntry> listDirs() {
        ensureLoaded();
        return mRoot.listDirs();
    }

    public Stream<ZipDirEntry> walkDirs() {
        ensureLoaded();
        return mRoot.walkDirs();
    }

    public boolean containsDir(String name) {
        ensureLoaded();
        return mRoot.containsDir(name);
    }

    public ZipDirEntry getDir(String name) throws ZipNoSuchEntryException {
        ensureLoaded();
        return mRoot.getDir(name);
    }

    public Stream<ZipFileEntry> listFiles() {
        ensureLoaded();
        return mRoot.listFiles();
    }

    public Stream<ZipFileEntry> walkFiles() {
        ensureLoaded();
        return mRoot.walkFiles();
    }

    public boolean containsFile(String name) {
        ensureLoaded();
        return mRoot.containsFile(name);
    }

    public ZipFileEntry getFile(String name) throws ZipNoSuchEntryException {
        ensureLoaded();
        return mRoot.getFile(name);
    }

    public void extract(Path dest) throws IOException {
        ensureLoaded();
        mRoot.extract(dest);
    }

    public void extract(Path dest, String name) throws IOException {
        ensureLoaded();
        mRoot.extract(dest, name);
    }

    public void extract(Path dest, String... names) throws IOException {
        ensureLoaded();
        mRoot.extract(dest, names);
    }

    @Override
    public String toString() {
        return String.format("ZipArchive{name=%s}", mZip.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ZipArchive other = (ZipArchive) obj;
        return mZip.equals(other.mZip);
    }

    @Override
    public int hashCode() {
        return mZip.hashCode();
    }
}

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
package brut.directory;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipRODirectory extends Directory {
    private final ZipFile mZipFile;
    private final String mPath;

    public ZipRODirectory(String fileName) throws DirectoryException {
        this(fileName, "");
    }

    public ZipRODirectory(File file) throws DirectoryException {
        this(file, "");
    }

    public ZipRODirectory(String fileName, String path) throws DirectoryException {
        this(new File(fileName), path);
    }

    public ZipRODirectory(File file, String path) throws DirectoryException {
        try {
            mZipFile = new ZipFile(file);
        } catch (IOException ex) {
            throw new DirectoryException(ex);
        }
        mPath = path;
    }

    private ZipRODirectory(ZipFile zipFile, String path) {
        mZipFile = zipFile;
        mPath = path;
    }

    @Override
    protected void load() {
        mFiles = new LinkedHashSet<>();
        mDirs = new LinkedHashMap<>();

        int prefixLen = mPath.length();
        Enumeration<? extends ZipEntry> entries = mZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.equals(mPath) || !name.startsWith(mPath) || name.contains(".." + separator)) {
                continue;
            }

            String subname = name.substring(prefixLen);

            int pos = subname.indexOf(separatorChar);
            if (pos == -1) {
                if (!entry.isDirectory()) {
                    mFiles.add(subname);
                    continue;
                }
            } else {
                subname = subname.substring(0, pos);
            }

            if (!mDirs.containsKey(subname)) {
                mDirs.put(subname, new ZipRODirectory(mZipFile, mPath + subname + separator));
            }
        }
    }

    private ZipEntry getZipFileEntry(String name) throws DirectoryException {
        ZipEntry entry = mZipFile.getEntry(name);
        if (entry == null) {
            throw new PathNotExist("Entry not found: " + name);
        }
        return entry;
    }

    @Override
    protected InputStream getFileInputImpl(String name) throws DirectoryException {
        try {
            return mZipFile.getInputStream(new ZipEntry(mPath + name));
        } catch (IOException ex) {
            throw new PathNotExist(name, ex);
        }
    }

    @Override
    public long getSize(String name) throws DirectoryException {
        ZipEntry entry = getZipFileEntry(name);
        return entry.getSize();
    }

    @Override
    public long getCompressedSize(String name) throws DirectoryException {
        ZipEntry entry = getZipFileEntry(name);
        return entry.getCompressedSize();
    }

    @Override
    public int getCompressionLevel(String name) throws DirectoryException {
        ZipEntry entry = getZipFileEntry(name);
        return entry.getMethod();
    }

    @Override
    public void close() throws DirectoryException {
        try {
            mZipFile.close();
        } catch (IOException ex) {
            throw new DirectoryException(ex);
        }
    }
}

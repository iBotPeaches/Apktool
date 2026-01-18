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

import brut.util.OS;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class FileDirectory extends Directory {
    private final File mDir;

    public FileDirectory(String dirName) throws DirectoryException {
        this(new File(dirName));
    }

    public FileDirectory(File dir) throws DirectoryException {
        if (!dir.isDirectory()) {
            throw new DirectoryException("file must be a directory: " + dir);
        }
        mDir = dir;
    }

    @Override
    protected void load() {
        mFiles = new LinkedHashSet<>();
        mDirs = new LinkedHashMap<>();

        File[] files = mDir.listFiles();
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (file.isFile()) {
                mFiles.add(file.getName());
            } else {
                try {
                    mDirs.put(file.getName(), new FileDirectory(file));
                } catch (DirectoryException ignored) {
                }
            }
        }
    }

    private String generatePath(String name) {
        return mDir.getPath() + separator + name;
    }

    @Override
    protected InputStream getFileInputImpl(String name) throws DirectoryException {
        try {
            File file = new File(generatePath(name));
            return Files.newInputStream(file.toPath());
        } catch (IOException ex) {
            throw new DirectoryException(ex);
        }
    }

    @Override
    protected OutputStream getFileOutputImpl(String name) throws DirectoryException {
        try {
            File file = new File(generatePath(name));
            return Files.newOutputStream(file.toPath());
        } catch (IOException ex) {
            throw new DirectoryException(ex);
        }
    }

    @Override
    protected void removeFileImpl(String name) {
        File file = new File(generatePath(name));
        OS.rmfile(file);
    }

    @Override
    protected Directory createDirImpl(String name) throws DirectoryException {
        File dir = new File(generatePath(name));
        OS.mkdir(dir);
        return new FileDirectory(dir);
    }

    @Override
    public long getSize(String name) throws DirectoryException {
        File file = new File(generatePath(name));
        if (!file.isFile()) {
            throw new DirectoryException("file must be a file: " + file);
        }
        return file.length();
    }

    @Override
    public long getCompressedSize(String name) throws DirectoryException {
        return getSize(name);
    }

    @Override
    public int getCompressionLevel(String name) {
        return 0;
    }
}

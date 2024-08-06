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

import java.io.*;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class FileDirectory extends AbstractDirectory {
    private final File mDir;

    public FileDirectory(ExtFile dir, String folder) throws DirectoryException {
        this(new File(dir.toString().replaceAll("%20", " "), folder));
    }

    public FileDirectory(String dir) throws DirectoryException, UnsupportedEncodingException {
        this(new File(URLDecoder.decode(dir, "UTF-8")));
    }

    public FileDirectory(File dir) throws DirectoryException {
        super();
        if (! dir.isDirectory()) {
            throw new DirectoryException("file must be a directory: " + dir);
        }
        mDir = dir;
    }

    @Override
    public long getSize(String fileName)
            throws DirectoryException {
        File file = new File(generatePath(fileName));
        if (! file.isFile()) {
            throw new DirectoryException("file must be a file: " + file);
        }
        return file.length();
    }

    @Override
    public long getCompressedSize(String fileName)
            throws DirectoryException {
        return getSize(fileName);
    }

    @Override
    protected AbstractDirectory createDirLocal(String name) throws DirectoryException {
        File dir = new File(generatePath(name));
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();
        return new FileDirectory(dir);
    }

    @Override
    protected InputStream getFileInputLocal(String name) throws DirectoryException {
        try {
            return new FileInputStream(generatePath(name));
        } catch (FileNotFoundException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    protected OutputStream getFileOutputLocal(String name) throws DirectoryException {
        try {
            return new FileOutputStream(generatePath(name));
        } catch (FileNotFoundException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    protected void loadDirs() {
        loadAll();
    }

    @Override
    protected void loadFiles() {
        loadAll();
    }

    @Override
    protected void removeFileLocal(String name) {
        //noinspection ResultOfMethodCallIgnored
        new File(generatePath(name)).delete();
    }

    private String generatePath(String name) {
        return getDir().getPath() + separator + name;
    }

    private void loadAll() {
        mFiles = new LinkedHashSet<>();
        mDirs = new LinkedHashMap<>();

        File[] files = getDir().listFiles();
        Arrays.sort(files, Comparator.comparing(f -> f.getName()));

        for (File file : files) {
            if (file.isFile()) {
                mFiles.add(file.getName());
            } else {
                // IMPOSSIBLE_EXCEPTION
                try {
                    mDirs.put(file.getName(), new FileDirectory(file));
                } catch (DirectoryException ignored) {}
            }
        }
    }

    public File getDir() {
        return mDir;
    }
}

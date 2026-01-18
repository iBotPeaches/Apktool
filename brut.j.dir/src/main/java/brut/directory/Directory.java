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

import brut.util.BrutIO;
import brut.util.OS;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public abstract class Directory implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger("");

    public final String separator = "/";
    public final char separatorChar = '/';

    protected Set<String> mFiles;
    protected Set<String> mFilesRecursive;
    protected Map<String, Directory> mDirs;

    protected abstract void load();

    protected InputStream getFileInputImpl(String name) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    protected OutputStream getFileOutputImpl(String name) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    protected void removeFileImpl(String name) {
        throw new UnsupportedOperationException();
    }

    protected Directory createDirImpl(String name) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    public long getSize(String name) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    public long getCompressedSize(String name) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    public int getCompressionLevel(String name) throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws DirectoryException {
        // Stub.
    }

    public Set<String> getFiles() {
        return getFiles(false);
    }

    public Set<String> getFiles(boolean recursive) {
        if (mFiles == null) {
            load();
        }
        if (!recursive) {
            return mFiles;
        }

        if (mFilesRecursive == null) {
            mFilesRecursive = new LinkedHashSet<>(mFiles);
            for (Map.Entry<String, ? extends Directory> dir : getDirs().entrySet()) {
                for (String path : dir.getValue().getFiles(true)) {
                    mFilesRecursive.add(dir.getKey() + separator + path);
                }
            }
        }
        return mFilesRecursive;
    }

    public Map<String, Directory> getDirs() {
        return getDirs(false);
    }

    public Map<String, Directory> getDirs(boolean recursive) {
        if (mDirs == null) {
            load();
        }
        if (!recursive) {
            return mDirs;
        }

        Map<String, Directory> dirs = new LinkedHashMap<>(mDirs);
        for (Map.Entry<String, Directory> dir : mDirs.entrySet()) {
            for (Map.Entry<String, Directory> subdir : dir.getValue().getDirs(true).entrySet()) {
                dirs.put(dir.getKey() + separator + subdir.getKey(), subdir.getValue());
            }
        }
        return dirs;
    }

    public boolean containsFile(String path) {
        SubPath subpath;
        try {
            subpath = getSubPath(path);
        } catch (PathNotExist ignored) {
            return false;
        }

        if (subpath.dir != null) {
            return subpath.dir.containsFile(subpath.path);
        }
        return getFiles().contains(subpath.path);
    }

    public boolean containsDir(String path) {
        SubPath subpath;
        try {
            subpath = getSubPath(path);
        } catch (PathNotExist ignored) {
            return false;
        }

        if (subpath.dir != null) {
            return subpath.dir.containsDir(subpath.path);
        }
        return getDirs().containsKey(subpath.path);
    }

    public InputStream getFileInput(String path) throws DirectoryException {
        SubPath subpath = getSubPath(path);
        if (subpath.dir != null) {
            return subpath.dir.getFileInput(subpath.path);
        }

        if (!getFiles().contains(subpath.path)) {
            throw new PathNotExist(path);
        }
        return getFileInputImpl(subpath.path);
    }

    public OutputStream getFileOutput(String path) throws DirectoryException {
        ParsedPath parsed = parsePath(path);
        if (parsed.dir == null) {
            getFiles().add(parsed.subpath);
            return getFileOutputImpl(parsed.subpath);
        }

        Directory dir;
        try {
            dir = createDir(parsed.dir);
        } catch (PathAlreadyExists ignored) {
            dir = getDirs().get(parsed.dir);
        }
        return dir.getFileOutput(parsed.subpath);
    }

    public boolean removeFile(String path) {
        SubPath subpath;
        try {
            subpath = getSubPath(path);
        } catch (PathNotExist ignored) {
            return false;
        }

        if (subpath.dir != null) {
            return subpath.dir.removeFile(subpath.path);
        }
        if (!getFiles().contains(subpath.path)) {
            return false;
        }
        removeFileImpl(subpath.path);
        getFiles().remove(subpath.path);
        return true;
    }

    public Directory getDir(String path) throws PathNotExist {
        SubPath subpath = getSubPath(path);
        if (subpath.dir != null) {
            return subpath.dir.getDir(subpath.path);
        }
        if (!getDirs().containsKey(subpath.path)) {
            throw new PathNotExist(path);
        }
        return getDirs().get(subpath.path);
    }

    public Directory createDir(String path) throws DirectoryException {
        ParsedPath parsed = parsePath(path);
        Directory dir;
        if (parsed.dir == null) {
            if (getDirs().containsKey(parsed.subpath)) {
                throw new PathAlreadyExists(path);
            }
            dir = createDirImpl(parsed.subpath);
            getDirs().put(parsed.subpath, dir);
            return dir;
        }

        if (getDirs().containsKey(parsed.dir)) {
            dir = getDirs().get(parsed.dir);
        } else {
            dir = createDirImpl(parsed.dir);
            getDirs().put(parsed.dir, dir);
        }
        return dir.createDir(parsed.subpath);
    }

    public void copyToDir(Directory out) throws DirectoryException {
        for (String fileName : getFiles(true)) {
            copyToDir(out, fileName);
        }
    }

    public void copyToDir(Directory out, String fileName) throws DirectoryException {
        copyToDir(fileName, out, fileName);
    }

    public void copyToDir(Directory out, String... fileNames) throws DirectoryException {
        for (String fileName : fileNames) {
            copyToDir(out, fileName);
        }
    }

    public void copyToDir(String inFileName, Directory out, String outFileName) throws DirectoryException {
        try {
            if (containsDir(inFileName)) {
                getDir(inFileName).copyToDir(out.createDir(outFileName));
            } else {
                BrutIO.copyAndClose(getFileInput(inFileName), out.getFileOutput(outFileName));
            }
        } catch (IOException ex) {
            throw new DirectoryException("Error copying file: " + inFileName, ex);
        }
    }

    public void copyToDir(File out) throws DirectoryException {
        for (String fileName : getFiles(true)) {
            copyToDir(out, fileName);
        }
    }

    public void copyToDir(File out, String fileName) throws DirectoryException {
        copyToDir(fileName, out, fileName);
    }

    public void copyToDir(File out, String... fileNames) throws DirectoryException {
        for (String fileName : fileNames) {
            copyToDir(out, fileName);
        }
    }

    public void copyToDir(String inFileName, File out, String outFileName) throws DirectoryException {
        try {
            if (containsDir(inFileName)) {
                File outDir = new File(out, outFileName);
                getDir(inFileName).copyToDir(outDir);
            } else if (containsFile(inFileName)) {
                outFileName = BrutIO.sanitizePath(out, outFileName);
                if (outFileName.isEmpty()) {
                    return;
                }
                File outFile = new File(out, outFileName);
                if (outFile.exists()) {
                    OS.rmfile(outFile);
                } else {
                    File parentDir = outFile.getParentFile();
                    if (parentDir != null) {
                        OS.mkdir(parentDir);
                    }
                }
                BrutIO.copyAndClose(getFileInput(inFileName), Files.newOutputStream(outFile.toPath()));
            } else {
                // Do nothing if directory/file not found.
                return;
            }
        } catch (InvalidPathException ex) {
            LOGGER.warning(String.format("Skipping file %s (%s)", inFileName, ex.getMessage()));
        } catch (IOException ex) {
            throw new DirectoryException("Error copying file: " + inFileName, ex);
        }
    }

    private SubPath getSubPath(String path) throws PathNotExist {
        ParsedPath parsed = parsePath(path);
        if (parsed.dir == null) {
            return new SubPath(null, parsed.subpath);
        }
        if (!getDirs().containsKey(parsed.dir)) {
            throw new PathNotExist(path);
        }
        return new SubPath(getDirs().get(parsed.dir), parsed.subpath);
    }

    private ParsedPath parsePath(String path) {
        int pos = path.indexOf(separatorChar);
        if (pos == -1) {
            return new ParsedPath(null, path);
        }
        return new ParsedPath(path.substring(0, pos), path.substring(pos + 1));
    }

    private static class ParsedPath {
        public final String dir;
        public final String subpath;

        public ParsedPath(String dir, String subpath) {
            this.dir = dir;
            this.subpath = subpath;
        }
    }

    private static class SubPath {
        public final Directory dir;
        public final String path;

        public SubPath(Directory dir, String path) {
            this.dir = dir;
            this.path = path;
        }
    }
}

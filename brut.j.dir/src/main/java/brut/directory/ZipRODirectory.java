/**
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package brut.directory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipRODirectory extends AbstractDirectory {
    private ZipFile mZipFile;
    private String mPath;

    public ZipRODirectory(String zipFileName) throws DirectoryException {
        this(zipFileName, "");
    }

    public ZipRODirectory(File zipFile) throws DirectoryException {
        this(zipFile, "");
    }

    public ZipRODirectory(ZipFile zipFile) {
        this(zipFile, "");
    }

    public ZipRODirectory(String zipFileName, String path)
            throws DirectoryException {
        this(new File(zipFileName), path);
    }

    public ZipRODirectory(File zipFile, String path) throws DirectoryException {
        super();
        try {
            mZipFile = new ZipFile(zipFile);
        } catch (IOException e) {
            throw new DirectoryException(e);
        }
        mPath = path;
    }

    public ZipRODirectory(ZipFile zipFile, String path) {
        super();
        mZipFile = zipFile;
        mPath = path;
    }

    @Override
    protected AbstractDirectory createDirLocal(String name)
            throws DirectoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream getFileInputLocal(String name)
            throws DirectoryException {
        try {
            return getZipFile().getInputStream(new ZipEntry(getPath() + name));
        } catch (IOException e) {
            throw new PathNotExist(name, e);
        }
    }

    @Override
    protected OutputStream getFileOutputLocal(String name)
            throws DirectoryException {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    private void loadAll() {
        mFiles = new LinkedHashSet<String>();
        mDirs = new LinkedHashMap<String, AbstractDirectory>();
        
        int prefixLen = getPath().length();
        Enumeration<? extends ZipEntry> entries = getZipFile().entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            
            if (name.equals(getPath()) || ! name.startsWith(getPath())) {
                continue;
            }
            
            String subname = name.substring(prefixLen);
            
            int pos = subname.indexOf(separator);
            if (pos == -1) {
                if (! entry.isDirectory()) {
                    mFiles.add(subname);
                    continue;
                }
            } else {
                subname = subname.substring(0, pos);
            }
            
            if (! mDirs.containsKey(subname)) {
                AbstractDirectory dir = new ZipRODirectory(getZipFile(), getPath() + subname + separator);
                mDirs.put(subname, dir);                
            }
        }
    }

    private String getPath() {
        return mPath;
    }

    private ZipFile getZipFile() {
        return mZipFile;
    }

}

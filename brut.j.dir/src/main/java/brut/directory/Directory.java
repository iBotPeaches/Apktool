/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import java.io.*;
import java.util.Map;
import java.util.Set;

public interface Directory {
    public Set<String> getFiles();
    public Set<String> getFiles(boolean recursive);
    public Map<String, Directory> getDirs();
    public Map<String, Directory> getDirs(boolean recursive);
    
    public boolean containsFile(String path);
    public boolean containsDir(String path);
    
    public InputStream getFileInput(String path) throws DirectoryException;
    public OutputStream getFileOutput(String path) throws DirectoryException;
    public Directory getDir(String path) throws PathNotExist;
    public Directory createDir(String path) throws DirectoryException;
    
    public boolean removeFile(String path);

    public void copyToDir(Directory out) throws DirectoryException;
    public void copyToDir(Directory out, String[] fileNames)
        throws DirectoryException;
    public void copyToDir(Directory out, String fileName)
        throws DirectoryException;
    public void copyToDir(File out) throws DirectoryException;
    public void copyToDir(File out, String[] fileNames)
        throws DirectoryException;
    public void copyToDir(File out, String fileName)
        throws DirectoryException;

    public long getSize(String fileName)
            throws DirectoryException;
    public long getCompressedSize(String fileName)
            throws DirectoryException;
    public int getCompressionLevel(String fileName)
            throws DirectoryException;


    public void close() throws IOException;
    
    public final char separator = '/';
}

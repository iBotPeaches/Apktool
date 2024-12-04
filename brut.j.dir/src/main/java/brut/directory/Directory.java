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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

public interface Directory {
    char separator = '/';

    Set<String> getFiles();

    Set<String> getFiles(boolean recursive);

    Map<String, Directory> getDirs();

    Map<String, Directory> getDirs(boolean recursive);

    boolean containsFile(String path);

    boolean containsDir(String path);

    InputStream getFileInput(String path) throws DirectoryException;

    OutputStream getFileOutput(String path) throws DirectoryException;

    Directory getDir(String path) throws PathNotExist;

    Directory createDir(String path) throws DirectoryException;

    boolean removeFile(String path);

    void copyToDir(Directory out) throws DirectoryException;

    void copyToDir(Directory out, String[] fileNames) throws DirectoryException;

    void copyToDir(Directory out, String fileName) throws DirectoryException;

    void copyToDir(File out) throws DirectoryException;

    void copyToDir(File out, String[] fileNames) throws DirectoryException;

    void copyToDir(File out, String fileName) throws DirectoryException;

    long getSize(String fileName) throws DirectoryException;

    long getCompressedSize(String fileName) throws DirectoryException;

    int getCompressionLevel(String fileName) throws DirectoryException;

    void close() throws IOException;
}

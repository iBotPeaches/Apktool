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

import brut.common.BrutException;
import brut.common.InvalidUnknownFileException;
import brut.common.RootUnknownFileException;
import brut.common.TraversalUnknownFileException;
import brut.util.BrutIO;
import brut.util.OS;

import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.util.logging.Logger;

public class DirUtil {
    private static final Logger LOGGER = Logger.getLogger("");

    private DirUtil() {
        // Private constructor for utility class
    }

    public static void copyToDir(Directory in, Directory out) throws DirectoryException {
        for (String fileName : in.getFiles(true)) {
            copyToDir(in, out, fileName);
        }
    }

    public static void copyToDir(Directory in, Directory out, String[] fileNames) throws DirectoryException {
        for (String fileName : fileNames) {
            copyToDir(in, out, fileName);
        }
    }

    public static void copyToDir(Directory in, Directory out, String fileName)
            throws DirectoryException {
        copyToDir(in, out, fileName, fileName);
    }

    public static void copyToDir(Directory in, Directory out, String inFile, String outFile)
            throws DirectoryException {
        try {
            if (in.containsDir(inFile)) {
                in.getDir(inFile).copyToDir(out.createDir(outFile));
            } else {
                BrutIO.copyAndClose(in.getFileInput(inFile), out.getFileOutput(outFile));
            }
        } catch (IOException ex) {
            throw new DirectoryException("Error copying file: " + inFile, ex);
        }
    }

    public static void copyToDir(Directory in, File out)
            throws DirectoryException {
        for (String fileName : in.getFiles(true)) {
            copyToDir(in, out, fileName);
        }
    }

    public static void copyToDir(Directory in, File out, String[] fileNames)
            throws DirectoryException {
        for (String fileName : fileNames) {
            copyToDir(in, out, fileName);
        }
    }

    public static void copyToDir(Directory in, File out, String fileName)
            throws DirectoryException {
        try {
            if (in.containsDir(fileName)) {
                File outDir = new File(out, fileName);
                OS.rmdir(outDir);
                in.getDir(fileName).copyToDir(outDir);
            } else if (in.containsFile(fileName)) {
                String validFileName = BrutIO.sanitizePath(out, fileName);
                if (!validFileName.isEmpty()) {
                    File outFile = new File(out, validFileName);
                    //noinspection ResultOfMethodCallIgnored
                    outFile.getParentFile().mkdirs();
                    BrutIO.copyAndClose(in.getFileInput(fileName), Files.newOutputStream(outFile.toPath()));
                }
            } else {
                // Skip if directory/file not found
            }
        } catch (FileSystemException ex) {
            LOGGER.warning(String.format("Skipping file %s (%s)", fileName, ex.getReason()));
        } catch (RootUnknownFileException | InvalidUnknownFileException | TraversalUnknownFileException | IOException ex) {
            LOGGER.warning(String.format("Skipping file %s (%s)", fileName, ex.getMessage()));
        } catch (BrutException ex) {
            throw new DirectoryException("Error copying file: " + fileName, ex);
        }
    }
}

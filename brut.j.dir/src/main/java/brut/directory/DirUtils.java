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

public final class DirUtils {
    private static final Logger LOGGER = Logger.getLogger("");

    private DirUtils() {
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

    public static void copyToDir(Directory in, Directory out, String inFileName, String outFileName)
            throws DirectoryException {
        try {
            if (in.containsDir(inFileName)) {
                in.getDir(inFileName).copyToDir(out.createDir(outFileName));
            } else {
                BrutIO.copyAndClose(in.getFileInput(inFileName), out.getFileOutput(outFileName));
            }
        } catch (IOException ex) {
            throw new DirectoryException("Error copying file: " + inFileName, ex);
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
        copyToDir(in, out, fileName, fileName);
    }

    public static void copyToDir(Directory in, File out, String inFileName, String outFileName)
            throws DirectoryException {
        try {
            if (in.containsDir(inFileName)) {
                File outDir = new File(out, outFileName);
                OS.rmdir(outDir);
                in.getDir(inFileName).copyToDir(outDir);
            } else if (in.containsFile(inFileName)) {
                outFileName = BrutIO.sanitizePath(out, outFileName);
                if (!outFileName.isEmpty()) {
                    File outFile = new File(out, outFileName);
                    OS.mkdir(outFile.getParentFile());
                    BrutIO.copyAndClose(in.getFileInput(inFileName), Files.newOutputStream(outFile.toPath()));
                }
            } else {
                // Skip if directory/file not found
            }
        } catch (FileSystemException ex) {
            LOGGER.warning(String.format("Skipping file %s (%s)", inFileName, ex.getReason()));
        } catch (RootUnknownFileException | InvalidUnknownFileException | TraversalUnknownFileException | IOException ex) {
            LOGGER.warning(String.format("Skipping file %s (%s)", inFileName, ex.getMessage()));
        } catch (BrutException ex) {
            throw new DirectoryException("Error copying file: " + inFileName, ex);
        }
    }
}

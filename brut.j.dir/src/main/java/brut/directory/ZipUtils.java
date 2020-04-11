/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import brut.common.BrutException;
import brut.util.BrutIO;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Collection;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static Collection<String> mDoNotCompress;

    public static void zipFolders(final File folder, final File zip, final File assets, final Collection<String> doNotCompress)
            throws BrutException, IOException {

        mDoNotCompress = doNotCompress;
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zip));
        zipFolders(folder, zipOutputStream);

        // We manually set the assets because we need to retain the folder structure
        if (assets != null) {
            processFolder(assets, zipOutputStream, assets.getPath().length() - 6);
        }
        zipOutputStream.close();
    }

    private static void zipFolders(final File folder, final ZipOutputStream outputStream)
            throws BrutException, IOException {
        processFolder(folder, outputStream, folder.getPath().length() + 1);
    }

    private static void processFolder(final File folder, final ZipOutputStream zipOutputStream, final int prefixLength)
            throws BrutException, IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final String cleanedPath = BrutIO.sanitizeUnknownFile(folder, file.getPath().substring(prefixLength));
                final ZipEntry zipEntry = new ZipEntry(BrutIO.normalizePath(cleanedPath));

                // aapt binary by default takes in parameters via -0 arsc to list extensions that shouldn't be
                // compressed. We will replicate that behavior
                final String extension = FilenameUtils.getExtension(file.getAbsolutePath());
                if (mDoNotCompress != null && (mDoNotCompress.contains(extension) || mDoNotCompress.contains(zipEntry.getName()))) {
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setSize(file.length());
                    BufferedInputStream unknownFile = new BufferedInputStream(new FileInputStream(file));
                    CRC32 crc = BrutIO.calculateCrc(unknownFile);
                    zipEntry.setCrc(crc.getValue());
                    unknownFile.close();
                } else {
                    zipEntry.setMethod(ZipEntry.DEFLATED);
                }

                zipOutputStream.putNextEntry(zipEntry);
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
            } else if (file.isDirectory()) {
                processFolder(file, zipOutputStream, prefixLength);
            }
        }
    }
}

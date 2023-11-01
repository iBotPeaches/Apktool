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
import brut.util.BrutIO;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.zip.CRC32;

public class ZipUtils {
    public interface AdditionalZipOperation {
        void run(ParallelScatterZipCreator zipCreator) throws BrutException;
    }

    private static Collection<String> mDoNotCompress;

    private ZipUtils() {
        // Private constructor for utility class
    }

    public static void zipFolders(final File folder, final File zip, final File assets, final Collection<String> doNotCompress, final AdditionalZipOperation additionalZipOperation)
            throws BrutException, IOException {

        mDoNotCompress = doNotCompress;
        ParallelScatterZipCreator zipCreator = new ParallelScatterZipCreator();
        zipFolders(folder, zipCreator);

        // We manually set the assets because we need to retain the folder structure
        if (assets != null) {
            processFolder(assets, zipCreator, assets.getPath().length() - 6);
        }
        if (additionalZipOperation != null) {
            additionalZipOperation.run(zipCreator);
        }

        ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(Files.newOutputStream(zip.toPath()));
        zipOutputStream.setUseZip64(Zip64Mode.AsNeeded);
        try {
            zipCreator.writeTo(zipOutputStream);
        } catch (InterruptedException | ExecutionException e) {
            throw new BrutException(e);
        }
        zipOutputStream.close();
    }

    private static void zipFolders(final File folder, final ParallelScatterZipCreator zipCreator)
            throws BrutException, IOException {
        processFolder(folder, zipCreator, folder.getPath().length() + 1);
    }

    private static void processFolder(final File folder, final ParallelScatterZipCreator zipCreator, final int prefixLength)
            throws BrutException, IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final String cleanedPath = BrutIO.sanitizeUnknownFile(folder, file.getPath().substring(prefixLength));
                final ZipArchiveEntry zipEntry = new ZipArchiveEntry(BrutIO.normalizePath(cleanedPath));

                // aapt binary by default takes in parameters via -0 arsc to list extensions that shouldn't be
                // compressed. We will replicate that behavior
                final String extension = FilenameUtils.getExtension(file.getAbsolutePath());
                if (mDoNotCompress != null && (mDoNotCompress.contains(extension) || mDoNotCompress.contains(zipEntry.getName()))) {
                    zipEntry.setMethod(ZipArchiveEntry.STORED);
                    zipEntry.setSize(file.length());
                    BufferedInputStream unknownFile = new BufferedInputStream(Files.newInputStream(file.toPath()));
                    CRC32 crc = BrutIO.calculateCrc(unknownFile);
                    zipEntry.setCrc(crc.getValue());
                    unknownFile.close();
                } else {
                    zipEntry.setMethod(ZipArchiveEntry.DEFLATED);
                }

                InputStreamSupplier streamSupplier = () -> {
                    try {
                        return new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                };

                zipCreator.addArchiveEntry(zipEntry, streamSupplier);
            } else if (file.isDirectory()) {
                processFolder(file, zipCreator, prefixLength);
            }
        }
    }
}

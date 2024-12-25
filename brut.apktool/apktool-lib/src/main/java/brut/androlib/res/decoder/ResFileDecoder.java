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
package brut.androlib.res.decoder;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.CantFind9PatchChunkException;
import brut.androlib.exceptions.RawXmlEncounteredException;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.value.ResBoolValue;
import brut.androlib.res.data.value.ResFileValue;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.DirUtils;
import brut.util.BrutIO;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResFileDecoder {
    private static final Logger LOGGER = Logger.getLogger(ResFileDecoder.class.getName());

    private static final String[] RAW_IMAGE_EXTENSIONS = {
            "m4a", // apple
            "qmg", // samsung
    };
    private static final String[] RAW_9PATCH_IMAGE_EXTENSIONS = {
            "qmg", // samsung
            "spi", // samsung
    };

    private final ResStreamDecoderContainer mDecoders;

    public ResFileDecoder(ResStreamDecoderContainer decoders) {
        mDecoders = decoders;
    }

    public void decode(ResResource res, Directory inDir, Directory outDir, Map<String, String> resFileMapping)
            throws AndrolibException {

        ResFileValue fileValue = (ResFileValue) res.getValue();
        String inFilePath = fileValue.toString();
        String inFileName = fileValue.getStrippedPath();
        String typeName = res.getResSpec().getType().getName();
        String outResName = res.getFilePath();

        if (BrutIO.detectPossibleDirectoryTraversal(outResName)) {
            outResName = inFileName;
            LOGGER.warning(String.format(
                "Potentially malicious file path: %s, using instead %s", res.getFilePath(), outResName
            ));
        }

        String ext = null;
        String outFileName;
        int extPos = inFileName.lastIndexOf(".");
        if (extPos == -1) {
            outFileName = outResName;
        } else {
            ext = inFileName.substring(extPos).toLowerCase();
            outFileName = outResName + ext;
        }

        String outFilePath = "res/" + outFileName;
        if (!inFilePath.equals(outFilePath)) {
            resFileMapping.put(inFilePath, outFilePath);
        }

        LOGGER.fine("Decoding file " + inFilePath + " to " + outFilePath);

        try {
            if (typeName.equals("raw")) {
                decode(inDir, inFilePath, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("font") && !".xml".equals(ext)) {
                decode(inDir, inFilePath, outDir, outFileName, "raw");
                return;
            }
            if (typeName.equals("drawable") || typeName.equals("mipmap")) {
                if (inFileName.toLowerCase().endsWith(".9" + ext)) {
                    outFileName = outResName + ".9" + ext;

                    // check for htc .r.9.png
                    if (inFileName.toLowerCase().endsWith(".r.9" + ext)) {
                        outFileName = outResName + ".r.9" + ext;
                    }

                    // check for raw 9patch images
                    for (String extension : RAW_9PATCH_IMAGE_EXTENSIONS) {
                        if (inFileName.toLowerCase().endsWith("." + extension)) {
                            copyRaw(inDir, outDir, inFilePath, outFileName);
                            return;
                        }
                    }

                    // check for xml 9 patches which are just xml files
                    if (inFileName.toLowerCase().endsWith(".xml")) {
                        decode(inDir, inFilePath, outDir, outFileName, "xml");
                        return;
                    }

                    try {
                        decode(inDir, inFilePath, outDir, outFileName, "9patch");
                        return;
                    } catch (CantFind9PatchChunkException ex) {
                        LOGGER.log(Level.WARNING, String.format(
                            "Could not find 9patch chunk in file: \"%s\". Renaming it to *.png.", inFileName
                        ), ex);
                        outDir.removeFile(outFileName);
                        outFileName = outResName + ext;
                    }
                }

                // check for raw image
                for (String extension : RAW_IMAGE_EXTENSIONS) {
                    if (inFileName.toLowerCase().endsWith("." + extension)) {
                        copyRaw(inDir, outDir, inFilePath, outFileName);
                        return;
                    }
                }

                if (!".xml".equals(ext)) {
                    decode(inDir, inFilePath, outDir, outFileName, "raw");
                    return;
                }
            }

            decode(inDir, inFilePath, outDir, outFileName, "xml");
        } catch (RawXmlEncounteredException ex) {
            // If we got an error to decode XML, lets assume the file is in raw format.
            // This is a large assumption, that might increase runtime, but will save us for situations where
            // XSD files are AXML`d on aapt1, but left in plaintext in aapt2.
            decode(inDir, inFilePath, outDir, outFileName, "raw");
        } catch (AndrolibException ex) {
            LOGGER.log(Level.SEVERE, String.format(
                "Could not decode file, replacing by FALSE value: %s",
            inFileName), ex);
            res.replace(new ResBoolValue(false, 0, null));
        }
    }

    public void decode(Directory inDir, String inFileName, Directory outDir,
                       String outFileName, String decoder) throws AndrolibException {
        try (
            InputStream in = inDir.getFileInput(inFileName);
            OutputStream out = outDir.getFileOutput(outFileName)
        ) {
            mDecoders.decode(in, out, decoder);
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void copyRaw(Directory inDir, Directory outDir, String inFileName,
                        String outFileName) throws AndrolibException {
        try {
            DirUtils.copyToDir(inDir, outDir, inFileName, outFileName);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }
}

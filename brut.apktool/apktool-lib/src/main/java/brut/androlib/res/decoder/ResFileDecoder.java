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
import brut.androlib.exceptions.NinePatchNotFoundException;
import brut.androlib.exceptions.RawXmlEncounteredException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.table.value.*;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.util.BrutIO;
import org.apache.commons.io.FilenameUtils;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Logger;

public class ResFileDecoder {
    private static final Logger LOGGER = Logger.getLogger(ResFileDecoder.class.getName());

    public enum Type { UNKNOWN, BINARY_XML, PNG_9PATCH }

    private final Map<Type, ResStreamDecoder> mDecoders;

    public ResFileDecoder(Map<Type, ResStreamDecoder> decoders) {
        mDecoders = decoders;
    }

    public void decode(ResEntry entry, Directory inDir, Directory outDir, Map<String, String> resFileMapping)
            throws AndrolibException {
        String inFileName = ((ResFileReference) entry.getValue()).getPath();

        // Some apps have string values where they shouldn't be.
        // We assumed that they are file references, but if no such file then
        // fall back to a string value.
        if (!inDir.containsFile(inFileName)) {
            entry.setValue(new ResString(inFileName));
            return;
        }

        // Strip resources dir to get inner path.
        String inResPath = inFileName;
        for (String dirName : ApkInfo.RESOURCES_DIRNAMES) {
            String prefix = dirName + "/";
            if (inResPath.startsWith(prefix)) {
                inResPath = inResPath.substring(prefix.length());
                break;
            }
        }

        // Get resource file name.
        String inResFileName = FilenameUtils.getName(inResPath);

        // Some apps were somehow built with Thumbs.db in drawables.
        // Replace with a null reference so we can rebuild the app.
        if (inResFileName.equals("Thumbs.db")) {
            entry.setValue(ResReference.NULL);
            return;
        }

        // Get the file extension.
        String ext;
        if (inResFileName.endsWith(".9.png")) {
            ext = "9.png";
        } else {
            ext = FilenameUtils.getExtension(inResFileName).toLowerCase();
        }

        // Use aapt2-like logic to determine which decoder to use.
        // TODO: Determine by magic bytes and fill in stripped extensions?
        Type type = Type.UNKNOWN;
        if (!ext.isEmpty() && !entry.getTypeName().equals("raw")) {
            switch (ext) {
                case "xml":
                case "xsd":
                    type = Type.BINARY_XML;
                    break;
                case "9.png":
                    type = Type.PNG_9PATCH;
                    break;
            }
        }

        // Generate output file path from entry.
        String outResPath = entry.getTypeName() + entry.getConfig().getQualifiers() + "/" + entry.getName()
                + (ext.isEmpty() ? "" : "." + ext);

        // Map output path to original path if it's different.
        String outFileName = "res/" + outResPath;
        if (!inFileName.equals(outFileName)) {
            resFileMapping.put(inFileName, outFileName);
        }

        LOGGER.fine("Decoding file " + inFileName + " to " + outFileName);

        try {
            if (type != Type.UNKNOWN) {
                try {
                    decode(type, inDir, inFileName, outDir, outFileName);
                    return;
                } catch (RawXmlEncounteredException ignored) {
                    // Assume the file is a raw XML.
                    LOGGER.fine("Could not decode binary XML file: " + inFileName);
                } catch (NinePatchNotFoundException ignored) {
                    // Assume the file is a raw PNG.
                    // Some apps contain unprocessed dummy 3x3 9-patch PNGs.
                    // Extract them as-is, let aapt2 process them properly later.
                    LOGGER.fine("Could not find 9-patch chunk in file: " + inFileName);
                }
            }

            decode(Type.UNKNOWN, inDir, inFileName, outDir, outFileName);
        } catch (AndrolibException ignored) {
            LOGGER.warning("Could not decode file, replacing by FALSE value: " + inFileName);
            entry.setValue(ResPrimitive.FALSE);
        }
    }

    private void decode(Type type, Directory inDir, String inFileName, Directory outDir, String outFileName)
            throws AndrolibException {
        ResStreamDecoder decoder = mDecoders.get(type);
        if (decoder == null) {
            throw new AndrolibException("Undefined decoder for type: " + type);
        }

        boolean success = false;
        try (
            InputStream in = inDir.getFileInput(inFileName);
            OutputStream out = outDir.getFileOutput(outFileName)
        ) {
            decoder.decode(in, out);
            success = true;
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        } finally {
            if (!success) {
                outDir.removeFile(outFileName);
            }
        }
    }
}

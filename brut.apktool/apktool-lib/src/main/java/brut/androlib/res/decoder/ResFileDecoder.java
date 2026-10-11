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

import brut.androlib.ApkFile;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.NinePatchNotFoundException;
import brut.androlib.exceptions.RawXmlEncounteredException;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.table.value.ResFileReference;
import brut.androlib.res.table.value.ResPrimitive;
import brut.androlib.res.table.value.ResString;
import brut.common.Log;
import brut.util.IOUtils;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class ResFileDecoder {
    private static final String TAG = ResFileDecoder.class.getName();

    public enum Type { UNKNOWN, BINARY_XML, PNG_9PATCH }

    private final ApkFile mApkFile;
    private final Map<Type, ResStreamDecoder> mDecoders;

    public ResFileDecoder(ApkFile apkFile, Map<Type, ResStreamDecoder> decoders) {
        mApkFile = apkFile;
        mDecoders = decoders;
    }

    public void decode(ResEntry entry, Path outDir, Map<String, String> resFileMap) throws AndrolibException {
        String fileName = ((ResFileReference) entry.getValue()).getPath();

        // Some apps have string values where they shouldn't be.
        // We assumed that they are file references, but if no such file then fall back to a string value.
        if (!mApkFile.containsFile(fileName)) {
            entry.setValue(new ResString(fileName));
            return;
        }

        // Get input file extension.
        String ext = fileName.endsWith(".9.png") ? "9.png"
            : IOUtils.getFileExtension(fileName).toLowerCase(Locale.ROOT);

        // Use aapt2-like logic to determine which decoder to use.
        // TODO: Determine by magic bytes and fill in stripped extensions?
        Type type = Type.UNKNOWN;
        if (!ext.isEmpty() && !entry.getType().getName().equals("raw")) {
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

        // Generate output file name from entry.
        String outFileName = "res/" + entry.getType().getName() + entry.getType().getConfig().toQualifiers()
                           + "/" + entry.getName() + (ext.isEmpty() ? "" : "." + ext);

        // Map input file name to output file name.
        resFileMap.put(fileName, outFileName);

        Log.d(TAG, "Decoding file " + fileName + " to " + outFileName);
        try {
            Path outFile = outDir.resolve(outFileName);

            if (type != Type.UNKNOWN) {
                try {
                    decode(type, fileName, outFile);
                    return;
                } catch (RawXmlEncounteredException ignored) {
                    // Assume the file is a raw XML.
                    Log.d(TAG, "Could not decode binary XML file: " + fileName);
                } catch (NinePatchNotFoundException ignored) {
                    // Assume the file is a raw PNG.
                    // Some apps contain unprocessed dummy 3x3 9-patch PNGs.
                    // Extract them as-is, let aapt2 process them properly later.
                    Log.d(TAG, "Could not find 9-patch chunk in file: " + fileName);
                }
            }

            decode(Type.UNKNOWN, fileName, outFile);
        } catch (AndrolibException | IOException ignored) {
            Log.w(TAG, "Could not decode file, replacing with NULL value: " + fileName);
            entry.setValue(ResPrimitive.NULL);
        }
    }

    private void decode(Type type, String fileName, Path outFile) throws AndrolibException, IOException {
        ResStreamDecoder decoder = mDecoders.get(type);
        if (decoder == null) {
            throw new IllegalStateException("Undefined decoder for file type: " + type);
        }

        if (!Files.deleteIfExists(outFile)) {
            IOUtils.createParentDirectories(outFile);
        }
        try (
            InputStream in = mApkFile.getFile(fileName).getInputStream();
            OutputStream out = Files.newOutputStream(outFile)
        ) {
            decoder.decode(in, out);
        } catch (AndrolibException | IOException ex) {
            try {
                Files.deleteIfExists(outFile);
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }
    }
}

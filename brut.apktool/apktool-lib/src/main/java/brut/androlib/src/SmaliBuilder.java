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
package brut.androlib.src;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.mod.SmaliMod;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import org.antlr.runtime.RecognitionException;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Logger;

public class SmaliBuilder {

    public static void build(ExtFile smaliDir, File dexFile, int apiLevel) throws AndrolibException {
        new SmaliBuilder(smaliDir, dexFile, apiLevel).build();
    }

    private SmaliBuilder(ExtFile smaliDir, File dexFile, int apiLevel) {
        mSmaliDir = smaliDir;
        mDexFile = dexFile;
        mApiLevel = apiLevel;
    }

    private void build() throws AndrolibException {
        try {
            DexBuilder dexBuilder;
            if (mApiLevel > 0) {
                dexBuilder = new DexBuilder(Opcodes.forApi(mApiLevel));
            } else {
                dexBuilder = new DexBuilder(Opcodes.getDefault());
            }

            for (String fileName : mSmaliDir.getDirectory().getFiles(true)) {
                buildFile(fileName, dexBuilder);
            }
            dexBuilder.writeTo(new FileDataStore( new File(mDexFile.getAbsolutePath())));
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildFile(String fileName, DexBuilder dexBuilder)
            throws AndrolibException, IOException {
        File inFile = new File(mSmaliDir, fileName);
        InputStream inStream = Files.newInputStream(inFile.toPath());

        if (fileName.endsWith(".smali")) {
            try {
                if (!SmaliMod.assembleSmaliFile(inFile, dexBuilder, mApiLevel, false, false)) {
                    throw new AndrolibException("Could not smali file: " + fileName);
                }
            } catch (IOException | RecognitionException ex) {
                throw new AndrolibException(ex);
            }
        } else {
            LOGGER.warning("Unknown file type, ignoring: " + inFile);
        }
        inStream.close();
    }

    private final ExtFile mSmaliDir;
    private final File mDexFile;
    private final int mApiLevel;

    private final static Logger LOGGER = Logger.getLogger(SmaliBuilder.class.getName());
}

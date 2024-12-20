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
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Logger;

public class SmaliBuilder {
    private static final Logger LOGGER = Logger.getLogger(SmaliBuilder.class.getName());

    private final ExtFile mSmaliDir;
    private final int mApiLevel;

    public SmaliBuilder(File smaliDir, int apiLevel) {
        mSmaliDir = new ExtFile(smaliDir);
        mApiLevel = apiLevel;
    }

    public void build(File dexFile) throws AndrolibException {
        try {
            DexBuilder dexBuilder = new DexBuilder(
                mApiLevel > 0 ? Opcodes.forApi(mApiLevel) : Opcodes.getDefault());

            for (String fileName : mSmaliDir.getDirectory().getFiles(true)) {
                buildFile(fileName, dexBuilder);
            }

            dexBuilder.writeTo(new FileDataStore(dexFile));
        } catch (DirectoryException | IOException | RuntimeException ex) {
            throw new AndrolibException("Could not smali folder: " + mSmaliDir.getName(), ex);
        }
    }

    private void buildFile(String fileName, DexBuilder dexBuilder) throws AndrolibException {
        if (!fileName.endsWith(".smali")) {
            LOGGER.warning("Unknown file type, ignoring: " + fileName);
            return;
        }

        boolean success;
        Exception cause;
        try {
            File inFile = new File(mSmaliDir, fileName);
            success = SmaliMod.assembleSmaliFile(inFile, dexBuilder, mApiLevel, false, false);
            cause = null;
        } catch (Exception ex) {
            success = false;
            cause = ex;
        }
        if (!success) {
            AndrolibException ex = new AndrolibException("Could not smali file: " + fileName);
            if (cause != null) {
                ex.initCause(cause);
            }
            throw ex;
        }
    }
}

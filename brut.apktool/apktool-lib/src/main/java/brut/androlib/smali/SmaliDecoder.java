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
package brut.androlib.smali;

import brut.androlib.exceptions.AndrolibException;
import com.android.tools.smali.baksmali.Baksmali;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.analysis.InlineMethodResolver;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;

import java.io.*;

public class SmaliDecoder {
    private final File mApkFile;
    private final String mDexName;
    private final boolean mBakDeb;
    private int mInferredApiLevel;

    public SmaliDecoder(File apkFile, String dexName, boolean bakDeb) {
        mApkFile = apkFile;
        mDexName = dexName;
        mBakDeb = bakDeb;
    }

    public int getInferredApiLevel() {
        return mInferredApiLevel;
    }

    public void decode(File outDir) throws AndrolibException {
        try {
            BaksmaliOptions options = new BaksmaliOptions();
            options.deodex = false;
            options.implicitReferences = false;
            options.parameterRegisters = true;
            options.localsDirective = true;
            options.sequentialLabels = true;
            options.debugInfo = mBakDeb;
            options.codeOffsets = false;
            options.accessorComments = false;
            options.registerInfo = 0;
            options.inlineResolver = null;

            // Set jobs automatically.
            int jobs = Runtime.getRuntime().availableProcessors();
            if (jobs > 6) {
                jobs = 6;
            }

            // Create the container.
            MultiDexContainer<? extends DexBackedDexFile> container =
                DexFileFactory.loadDexContainer(mApkFile, null);
            MultiDexContainer.DexEntry<? extends DexBackedDexFile> dexEntry;
            DexBackedDexFile dexFile;

            // If we have 1 item, ignore the passed file. Pull the DexFile we need.
            if (container.getDexEntryNames().size() == 1) {
                dexEntry = container.getEntry(container.getDexEntryNames().get(0));
            } else {
                dexEntry = container.getEntry(mDexName);
            }

            // Double-check the passed param exists.
            if (dexEntry == null) {
                dexEntry = container.getEntry(container.getDexEntryNames().get(0));
            }

            assert dexEntry != null;
            dexFile = dexEntry.getDexFile();

            if (dexFile.supportsOptimizedOpcodes()) {
                throw new AndrolibException("Could not disassemble an odex file without deodexing it.");
            }

            if (dexFile instanceof DexBackedOdexFile) {
                options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(
                    ((DexBackedOdexFile) dexFile).getOdexVersion());
            }

            Baksmali.disassembleDexFile(dexFile, outDir, jobs, options);

            mInferredApiLevel = dexFile.getOpcodes().api;
        } catch (IOException ex) {
            throw new AndrolibException("Could not baksmali file: " + mDexName, ex);
        }
    }
}

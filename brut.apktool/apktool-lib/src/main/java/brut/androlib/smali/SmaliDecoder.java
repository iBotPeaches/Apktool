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
import brut.util.OS;
import com.android.tools.smali.baksmali.Baksmali;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.analysis.InlineMethodResolver;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SmaliDecoder {
    private final File mApkFile;
    private final boolean mDebugMode;
    private final Set<String> mDexFiles;
    private int mInferredApiLevel;

    public SmaliDecoder(File apkFile, boolean debugMode) {
        mApkFile = apkFile;
        mDebugMode = debugMode;
        mDexFiles = new HashSet<>();
    }

    public Set<String> getDexFiles() {
        return mDexFiles;
    }

    public int getInferredApiLevel() {
        return mInferredApiLevel;
    }

    public void decode(String dexName, File smaliDir) throws AndrolibException {
        try {
            BaksmaliOptions options = new BaksmaliOptions();
            options.deodex = false;
            options.implicitReferences = false;
            options.parameterRegisters = true;
            options.localsDirective = true;
            options.sequentialLabels = true;
            options.debugInfo = mDebugMode;
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

            // If we have 1 item, ignore the passed file. Pull the DexFile we need.
            MultiDexContainer.DexEntry<? extends DexBackedDexFile> dexEntry =
                container.getDexEntryNames().size() == 1
                    ? container.getEntry(container.getDexEntryNames().get(0))
                    : container.getEntry(dexName);

            // Double-check the passed param exists.
            if (dexEntry == null) {
                dexEntry = container.getEntry(container.getDexEntryNames().get(0));
                assert dexEntry != null;
            }

            DexBackedDexFile dexFile = dexEntry.getDexFile();

            if (dexFile.supportsOptimizedOpcodes()) {
                throw new AndrolibException("Could not disassemble an odex file without deodexing it.");
            }

            if (dexFile instanceof DexBackedOdexFile) {
                options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(
                    ((DexBackedOdexFile) dexFile).getOdexVersion());
            }

            OS.mkdir(smaliDir);
            Baksmali.disassembleDexFile(dexFile, smaliDir, jobs, options);

            synchronized (mDexFiles) {
                int apiLevel = dexFile.getOpcodes().api;
                if (mInferredApiLevel == 0 || mInferredApiLevel > apiLevel) {
                    mInferredApiLevel = apiLevel;
                }

                mDexFiles.add(dexName);
            }
        } catch (IOException ex) {
            throw new AndrolibException("Could not baksmali file: " + dexName, ex);
        }
    }
}

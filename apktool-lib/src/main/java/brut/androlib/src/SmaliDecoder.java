/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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

package brut.androlib.src;

import brut.androlib.AndrolibException;
import java.io.File;
import java.io.IOException;
import org.jf.baksmali.baksmali;
import org.jf.baksmali.main;
import org.jf.dexlib.DexFile;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class SmaliDecoder {

    public static void decode(File apkFile, File outDir, boolean debug)
            throws AndrolibException {
        new SmaliDecoder(apkFile, outDir, debug).decode();
    }

    private SmaliDecoder(File apkFile, File outDir, boolean debug) {
        mApkFile = apkFile;
        mOutDir = outDir;
        mDebug = debug;
    }

    private void decode() throws AndrolibException {
        try {
            baksmali.disassembleDexFile(mApkFile.getAbsolutePath(),
                new DexFile(mApkFile), false, mOutDir.getAbsolutePath(), null,
                null, null, false, true, true, true, false, false, 
                mDebug ? main.DIFFPRE : 0, false, false, null);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private final File mApkFile;
    private final File mOutDir;
    private final boolean mDebug;
}

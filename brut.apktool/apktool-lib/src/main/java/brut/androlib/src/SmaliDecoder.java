/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
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
import org.jf.baksmali.baksmali;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.analysis.InlineMethodResolver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class SmaliDecoder {

    public static void decode(File apkFile, File outDir, String dexName, boolean debug, String debugLinePrefix,
                              boolean bakdeb, int api) throws AndrolibException {
        new SmaliDecoder(apkFile, outDir, dexName, debug, debugLinePrefix, bakdeb, api).decode();
    }

    private SmaliDecoder(File apkFile, File outDir, String dexName, boolean debug, String debugLinePrefix,
                         boolean bakdeb, int api) {
        mApkFile = apkFile;
        mOutDir = outDir.toPath();
        mDexFile = dexName;
        mDebug = debug;
        mDebugLinePrefix = debugLinePrefix;
        mBakDeb = bakdeb;
        mApi    = api;
    }

    private void decode() throws AndrolibException {
        try {
            ClassPath.dontLoadClassPath = mDebug;

            baksmaliOptions options = new baksmaliOptions();

            // options
            options.deodex = false;
            options.outputDirectory = mOutDir.toAbsolutePath().toString();
            options.noParameterRegisters = false;
            options.useLocalsDirective = true;
            options.useSequentialLabels = true;
            options.outputDebugInfo = mBakDeb;
            options.addCodeOffsets = false;
            options.jobs = -1;
            options.noAccessorComments = false;
            options.registerInfo = (mDebug ? baksmaliOptions.DIFFPRE : 0);
            options.ignoreErrors = false;
            options.inlineResolver = null;
            options.checkPackagePrivateAccess = false;

            // set jobs automatically
            if (options.jobs <= 0) {
                if (mDebug) {
                    options.jobs = 1;
                } else {
                    options.jobs = Runtime.getRuntime().availableProcessors();
                    if (options.jobs > 6) {
                        options.jobs = 6;
                    }
                }
            }

            // create the dex
            DexBackedDexFile dexFile = DexFileFactory.loadDexFile(mApkFile, mDexFile, mApi, false);

            if (dexFile.isOdexFile()) {
                throw new AndrolibException("Warning: You are disassembling an odex file without deodexing it.");
            }

            if (dexFile instanceof DexBackedOdexFile) {
                options.inlineResolver =
                        InlineMethodResolver.createInlineMethodResolver(((DexBackedOdexFile)dexFile).getOdexVersion());
            }

            baksmali.disassembleDexFile(dexFile,options);

            if (mDebug) {
                Files.walkFileTree(mOutDir, new SmaliFileVisitor());
            }
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private final File mApkFile;
    private final Path mOutDir;
    private final boolean mDebug;
    private final String mDebugLinePrefix;
    private final String mDexFile;
    private final boolean mBakDeb;
    private final int mApi;

    private class SmaliFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = file.getFileName().toString();
            if (! fileName.endsWith(".smali")) {
                return FileVisitResult.CONTINUE;
            }
            fileName = fileName.substring(0, fileName.length() - 6);
            try (
                    BufferedReader in = Files.newBufferedReader(file, Charset.defaultCharset());
                    BufferedWriter out = Files.newBufferedWriter(
                            file.resolveSibling(fileName + ".java"), Charset.defaultCharset())
            ) {
                TypeName type = TypeName.fromPath(mOutDir.relativize(file.resolveSibling(fileName)));
                out.write("package " + type.package_ + "; class " + type.getName(true, true) + " { void a() { int a;");
                out.newLine();

                String line;
                final String debugLinePrefix = mDebugLinePrefix;
                while ((line = in.readLine()) != null) {
                    out.write(debugLinePrefix);
                    out.write(line);
                    out.newLine();
                }

                out.write("}}");
                out.newLine();
            }
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }
    }
}

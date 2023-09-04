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
import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;
import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedOdexFile;
import com.android.tools.smali.dexlib2.analysis.InlineMethodResolver;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.MultiDexContainer;
import com.android.tools.smali.util.ClassFileNameHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import brut.androlib.Config;

public class SmaliDecoder {

    Config config;
    private final static Logger LOGGER = Logger.getLogger(SmaliDecoder.class.getName());

    public static DexFile decode(File apkFile, File outDir, String dexName, Config mConfig)
            throws AndrolibException {
        return new SmaliDecoder(apkFile, outDir, dexName, mConfig).decode();
    }

    private SmaliDecoder(File mApkFile, File mOutDir, String mDexName, Config mConfig) {
        config = mConfig;
        config.dexFile = mDexName;
        config.apkFile = mApkFile;
        config.outDir = mOutDir;
    }

    private DexFile decode() throws AndrolibException {
        try {
            // options
            config.deodex = false;
            config.implicitReferences = false;
            config.parameterRegisters = true;
            config.localsDirective = true;
            config.sequentialLabels = true;
            config.codeOffsets = false;
            config.accessorComments = false;
            config.registerInfo = 0;
            config.inlineResolver = null;

            // set jobs automatically
            int jobs = Runtime.getRuntime().availableProcessors();
            if (jobs > 6) {
                jobs = 6;
            }

            // create the container
            MultiDexContainer<? extends DexBackedDexFile> container =
                    DexFileFactory.loadDexContainer(config.apkFile, config.apiLevel > 0 ? Opcodes.forApi(config.apiLevel) : null);
            MultiDexContainer.DexEntry<? extends DexBackedDexFile> dexEntry;
            DexBackedDexFile dexFile;

            // If we have 1 item, ignore the passed file. Pull the DexFile we need.
            if (container.getDexEntryNames().size() == 1) {
                dexEntry = container.getEntry(container.getDexEntryNames().get(0));
            } else {
                dexEntry = container.getEntry(config.dexFile);
            }

            // Double-check the passed param exists
            if (dexEntry == null) {
                dexEntry = container.getEntry(container.getDexEntryNames().get(0));
            }

            assert dexEntry != null;
            dexFile = dexEntry.getDexFile();

            if (dexFile.supportsOptimizedOpcodes()) {
                throw new AndrolibException("Warning: You are disassembling an odex file without deodexing it.");
            }

            if (dexFile instanceof DexBackedOdexFile) {
                config.inlineResolver =
                        InlineMethodResolver.createInlineMethodResolver(((DexBackedOdexFile)dexFile).getOdexVersion());
            }

            if (config.resolveResources)
                LOGGER.info("Parsing resource ids in " + config.dexFile + "...");
            disassembleDexFile(dexFile, config.outDir, jobs, config);

            return dexFile;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public static boolean disassembleDexFile(DexFile dexFile, File outputDir, int jobs, final Config config) {
        return disassembleDexFile(dexFile, outputDir, jobs, config, null);
    }

    public static boolean disassembleDexFile(DexFile dexFile, File outputDir, int jobs, final Config config,
                                             @Nullable List<String> classes) {

        List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(dexFile.getClasses());

        final ClassFileNameHandler fileNameHandler = new ClassFileNameHandler(outputDir, ".smali");

        ExecutorService executor = Executors.newFixedThreadPool(jobs);
        List<Future<Boolean>> tasks = Lists.newArrayList();

        Set<String> classSet = null;
        if (classes != null) {
            classSet = new HashSet<String>(classes);
        }

        for (final ClassDef classDef: classDefs) {
            if (classSet != null && !classSet.contains(classDef.getType())) {
                continue;
            }
            tasks.add(executor.submit(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    return disassembleClass(classDef, fileNameHandler, config);
                }
            }));
        }

        boolean errorOccurred = false;
        try {
            for (Future<Boolean> task: tasks) {
                while(true) {
                    try {
                        if (!task.get()) {
                            errorOccurred = true;
                        }
                    } catch (InterruptedException ex) {
                        continue;
                    } catch (ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                    break;
                }
            }
        } finally {
            executor.shutdown();
        }
        return !errorOccurred;
    }

    private static boolean disassembleClass(ClassDef classDef, ClassFileNameHandler fileNameHandler,
                                            Config config) {
        String classDescriptor = classDef.getType();

        if (classDescriptor.charAt(0) != 'L' ||
                classDescriptor.charAt(classDescriptor.length()-1) != ';') {
            System.err.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
            return false;
        }

        File smaliFile = null;
        try {
            smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor);
        } catch (IOException ex) {
            System.err.println("\n\nError occurred while creating file for class " + classDescriptor);
            ex.printStackTrace();
            return false;
        }

        ClassDefinition classDefinition = new ClassDefinition(config, classDef);

        BaksmaliWriter writer = null;
        try {
            File smaliParent = smaliFile.getParentFile();
            if (!smaliParent.exists()) {
                if (!smaliParent.mkdirs()) {
                    if (!smaliParent.exists()) {
                        System.err.println("Unable to create directory " + smaliParent.toString() + " - skipping class");
                        return false;
                    }
                }
            }

            if (!smaliFile.exists()){
                if (!smaliFile.createNewFile()) {
                    System.err.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                    return false;
                }
            }

            BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(smaliFile), StandardCharsets.UTF_8));

            writer = new BaksmaliWriter(
                    bufWriter,
                    config.implicitReferences ? classDef.getType() : null);
            classDefinition.writeTo(writer);
        } catch (Exception ex) {
            System.err.println("\n\nError occurred while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
            ex.printStackTrace();
            smaliFile.delete();
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ex) {
                    System.err.println("\n\nError occurred while closing file " + smaliFile.toString());
                    ex.printStackTrace();
                }
            }
        }
        return true;
    }
}

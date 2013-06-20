/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Analysis;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.cli.*;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.util.ConsoleUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DumpVtables {
    private static final Options options;

    static {
        options = new Options();
        buildOptions();
    }

    public static void main(String[] args) {
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            usage();
            return;
        }

        String[] remainingArgs = commandLine.getArgs();

        Option[] parsedOptions = commandLine.getOptions();
        ArrayList<String> bootClassPathDirs = Lists.newArrayList();
        String outFile = "vtables.txt";

        for (int i=0; i<parsedOptions.length; i++) {
            Option option = parsedOptions[i];
            String opt = option.getOpt();

            switch (opt.charAt(0)) {
                case 'd':
                    bootClassPathDirs.add(option.getValue());
                    break;
                case 'o':
                    outFile = option.getValue();
                    break;
                default:
                    assert false;
            }
        }

        if (remainingArgs.length != 1) {
            usage();
            return;
        }

        String inputDexFileName = remainingArgs[0];

        File dexFileFile = new File(inputDexFileName);
        if (!dexFileFile.exists()) {
            System.err.println("Can't find the file " + inputDexFileName);
            System.exit(1);
        }

        try {
            DexFile dexFile = new DexFile(dexFileFile);
            String[] bootClassPaths = new String[bootClassPathDirs.size()];

            int j = 0;
            for (String bootClassPathDir: bootClassPathDirs) {
                bootClassPaths[j++] = bootClassPathDir;
            }

            ClassPath.InitializeClassPathFromOdex(bootClassPaths, null, inputDexFileName, dexFile, false);
            FileOutputStream outStream = new FileOutputStream(outFile);

            for (ClassDefItem classDefItem: dexFile.ClassDefsSection.getItems()) {
                ClassPath.ClassDef classDef = ClassPath.getClassDef(classDefItem.getClassType());
                ClassPath.VirtualMethod[] methods = classDef.getVtable();
                String className = "Class "  + classDef.getClassType() + " extends " + classDef.getSuperclass().getClassType() + " : " + methods.length + " methods\n";
                outStream.write(className.getBytes());
                for (int i=0;i<methods.length;i++) {
                    String method = i + ":" + methods[i].containingClass + "->" + methods[i].method + "\n";
                    outStream.write(method.getBytes());
                }
                outStream.write("\n".getBytes());
            }
            outStream.close();
        } catch (IOException ex) {
            System.out.println("IOException thrown when trying to open a dex file or write out vtables: " + ex);
        }

    }

    /**
     * Prints the usage message.
     */
    private static void usage() {
        int consoleWidth = ConsoleUtil.getConsoleWidth();
        if (consoleWidth <= 0) {
            consoleWidth = 80;
        }

        System.out.println("java -cp baksmali.jar org.jf.dexlib.Code.Analysis.DumpVtables -d path/to/jar/files <dex-file>");
    }

    private static void buildOptions() {
        Option classPathDirOption = OptionBuilder.withLongOpt("bootclasspath-dir")
        .withDescription("the base folder to look for the bootclasspath files in. Defaults to the current " +
                "directory")
        .hasArg()
        .withArgName("DIR")
        .create("d");

        Option outputFileOption = OptionBuilder.withLongOpt("out-file")
        .withDescription("output file")
        .hasArg()
        .withArgName("FILE")
        .create("o");

        options.addOption(classPathDirOption);
        options.addOption(outputFileOption);
    }
}

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

package org.jf.dexlib2.analysis;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.cli.*;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.util.ConsoleUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        int apiLevel = 15;
        boolean experimental = false;

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
                case 'a':
                    apiLevel = Integer.parseInt(commandLine.getOptionValue("a"));
                    break;
                case 'X':
                    experimental = true;
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
            DexBackedDexFile dexFile = DexFileFactory.loadDexFile(dexFileFile, apiLevel, experimental);
            Iterable<String> bootClassPaths = Splitter.on(":").split("core.jar:ext.jar:framework.jar:android.policy.jar:services.jar");
            ClassPath classPath = ClassPath.fromClassPath(bootClassPathDirs, bootClassPaths, dexFile, apiLevel, experimental);
            FileOutputStream outStream = new FileOutputStream(outFile);

            for (ClassDef classDef: dexFile.getClasses()) {
                ClassProto classProto = (ClassProto) classPath.getClass(classDef);
                List<Method> methods = classProto.getVtable();
                String className = "Class "  + classDef.getType() + " extends " + classDef.getSuperclass() + " : " + methods.size() + " methods\n";
                outStream.write(className.getBytes());
                for (int i=0;i<methods.size();i++) {
                    Method method = methods.get(i);

                    String methodString = i + ":" + method.getDefiningClass() + "->" + method.getName() + "(";
                    for (CharSequence parameter: method.getParameterTypes()) {
                        methodString += parameter;
                    }
                    methodString += ")" + method.getReturnType() + "\n";
                    outStream.write(methodString.getBytes());
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

        System.out.println("java -cp baksmali.jar org.jf.dexlib2.analysis.DumpVtables -d path/to/framework/jar/files <dex-file>");
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

        Option apiLevelOption = OptionBuilder.withLongOpt("api-level")
                .withDescription("The numeric api-level of the file being disassembled. If not " +
                        "specified, it defaults to 15 (ICS).")
                .hasArg()
                .withArgName("API_LEVEL")
                .create("a");

        Option experimentalOption = OptionBuilder.withLongOpt("experimental")
                .withDescription("Enable dumping experimental opcodes, that aren't necessarily " +
                                "supported by the android runtime yet.")
                .create("X");

        options.addOption(classPathDirOption);
        options.addOption(outputFileOption);
        options.addOption(apiLevelOption);
        options.addOption(experimentalOption);
    }
}

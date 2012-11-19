/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali;

import org.apache.commons.cli.*;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.DexFile;
import org.jf.util.ConsoleUtil;
import org.jf.util.SmaliHelpFormatter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class main {

    public static final String VERSION;

    private static final Options basicOptions;
    private static final Options debugOptions;
    private static final Options options;

    public static final int ALL = 1;
    public static final int ALLPRE = 2;
    public static final int ALLPOST = 4;
    public static final int ARGS = 8;
    public static final int DEST = 16;
    public static final int MERGE = 32;
    public static final int FULLMERGE = 64;
    
    public static final int DIFFPRE = 128;

    static {
        options = new Options();
        basicOptions = new Options();
        debugOptions = new Options();
        buildOptions();

        InputStream templateStream = baksmali.class.getClassLoader().getResourceAsStream("baksmali.properties");
        Properties properties = new Properties();
        String version = "(unknown)";
        try {
            properties.load(templateStream);
            version = properties.getProperty("application.version");
        } catch (IOException ex) {
        }
        VERSION = version;
    }

    /**
     * This class is uninstantiable.
     */
    private main() {
    }

    /**
     * Run!
     */
    public static void main(String[] args) {
        Locale locale = new Locale("en", "US");
        Locale.setDefault(locale);

        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            usage();
            return;
        }

        boolean disassemble = true;
        boolean doDump = false;
        boolean write = false;
        boolean sort = false;
        boolean fixRegisters = false;
        boolean noParameterRegisters = false;
        boolean useLocalsDirective = false;
        boolean useSequentialLabels = false;
        boolean outputDebugInfo = true;
        boolean addCodeOffsets = false;
        boolean noAccessorComments = false;
        boolean deodex = false;
        boolean verify = false;
        boolean ignoreErrors = false;
        boolean checkPackagePrivateAccess = false;

        int apiLevel = 14;

        int registerInfo = 0;

        String outputDirectory = "out";
        String dumpFileName = null;
        String outputDexFileName = null;
        String inputDexFileName = null;
        String bootClassPath = null;
        StringBuffer extraBootClassPathEntries = new StringBuffer();
        List<String> bootClassPathDirs = new ArrayList<String>();
        bootClassPathDirs.add(".");
        String inlineTable = null;
        boolean jumboInstructions = false;

        String[] remainingArgs = commandLine.getArgs();

        Option[] options = commandLine.getOptions();

        for (int i=0; i<options.length; i++) {
            Option option = options[i];
            String opt = option.getOpt();

            switch (opt.charAt(0)) {
                case 'v':
                    version();
                    return;
                case '?':
                    while (++i < options.length) {
                        if (options[i].getOpt().charAt(0) == '?') {
                            usage(true);
                            return;
                        }
                    }
                    usage(false);
                    return;
                case 'o':
                    outputDirectory = commandLine.getOptionValue("o");
                    break;
                case 'p':
                    noParameterRegisters = true;
                    break;
                case 'l':
                    useLocalsDirective = true;
                    break;
                case 's':
                    useSequentialLabels = true;
                    break;
                case 'b':
                    outputDebugInfo = false;
                    break;
                case 'd':
                    bootClassPathDirs.add(option.getValue());
                    break;
                case 'f':
                    addCodeOffsets = true;
                    break;
                case 'r':
                    String[] values = commandLine.getOptionValues('r');

                    if (values == null || values.length == 0) {
                        registerInfo = ARGS | DEST;
                    } else {
                        for (String value: values) {
                            if (value.equalsIgnoreCase("ALL")) {
                                registerInfo |= ALL;
                            } else if (value.equalsIgnoreCase("ALLPRE")) {
                                registerInfo |= ALLPRE;
                            } else if (value.equalsIgnoreCase("ALLPOST")) {
                                registerInfo |= ALLPOST;
                            } else if (value.equalsIgnoreCase("ARGS")) {
                                registerInfo |= ARGS;
                            } else if (value.equalsIgnoreCase("DEST")) {
                                registerInfo |= DEST;
                            } else if (value.equalsIgnoreCase("MERGE")) {
                                registerInfo |= MERGE;
                            } else if (value.equalsIgnoreCase("FULLMERGE")) {
                                registerInfo |= FULLMERGE;
                            } else {
                                usage();
                                return;
                            }
                        }

                        if ((registerInfo & FULLMERGE) != 0) {
                            registerInfo &= ~MERGE;
                        }
                    }
                    break;
                case 'c':
                    String bcp = commandLine.getOptionValue("c");
                    if (bcp != null && bcp.charAt(0) == ':') {
                        extraBootClassPathEntries.append(bcp);
                    } else {
                        bootClassPath = bcp;
                    }
                    break;
                case 'x':
                    deodex = true;
                    break;
                case 'm':
                    noAccessorComments = true;
                    break;
                case 'a':
                    apiLevel = Integer.parseInt(commandLine.getOptionValue("a"));
                    if (apiLevel >= 17) {
                        checkPackagePrivateAccess = true;
                    }
                    break;
                case 'N':
                    disassemble = false;
                    break;
                case 'D':
                    doDump = true;
                    dumpFileName = commandLine.getOptionValue("D", inputDexFileName + ".dump");
                    break;
                case 'I':
                    ignoreErrors = true;
                    break;
                case 'J':
                    jumboInstructions = true;
                    break;
                case 'W':
                    write = true;
                    outputDexFileName = commandLine.getOptionValue("W");
                    break;
                case 'S':
                    sort = true;
                    break;
                case 'F':
                    fixRegisters = true;
                    break;
                case 'V':
                    verify = true;
                    break;
                case 'T':
                    inlineTable = commandLine.getOptionValue("T");
                    break;
                case 'K':
                    checkPackagePrivateAccess = true;
                    break;
                default:
                    assert false;
            }
        }

        if (remainingArgs.length != 1) {
            usage();
            return;
        }

        inputDexFileName = remainingArgs[0];

        try {
            File dexFileFile = new File(inputDexFileName);
            if (!dexFileFile.exists()) {
                System.err.println("Can't find the file " + inputDexFileName);
                System.exit(1);
            }

            Opcode.updateMapsForApiLevel(apiLevel, jumboInstructions);

            //Read in and parse the dex file
            DexFile dexFile = new DexFile(dexFileFile, !fixRegisters, false);

            if (dexFile.isOdex()) {
                if (doDump) {
                    System.err.println("-D cannot be used with on odex file. Ignoring -D");
                }
                if (write) {
                    System.err.println("-W cannot be used with an odex file. Ignoring -W");
                }
                if (!deodex) {
                    System.err.println("Warning: You are disassembling an odex file without deodexing it. You");
                    System.err.println("won't be able to re-assemble the results unless you deodex it with the -x");
                    System.err.println("option");
                }
            } else {
                deodex = false;

                if (bootClassPath == null) {
                    bootClassPath = "core.jar:ext.jar:framework.jar:android.policy.jar:services.jar";
                }
            }

            if (disassemble) {
                String[] bootClassPathDirsArray = new String[bootClassPathDirs.size()];
                for (int i=0; i<bootClassPathDirsArray.length; i++) {
                    bootClassPathDirsArray[i] = bootClassPathDirs.get(i);
                }

                baksmali.disassembleDexFile(dexFileFile.getPath(), dexFile, deodex, outputDirectory,
                        bootClassPathDirsArray, bootClassPath, extraBootClassPathEntries.toString(),
                        noParameterRegisters, useLocalsDirective, useSequentialLabels, outputDebugInfo, addCodeOffsets,
                        noAccessorComments, registerInfo, verify, ignoreErrors, inlineTable, checkPackagePrivateAccess);
            }

            if ((doDump || write) && !dexFile.isOdex()) {
                try
                {
                    dump.dump(dexFile, dumpFileName, outputDexFileName, sort);
                }catch (IOException ex) {
                    System.err.println("Error occured while writing dump file");
                    ex.printStackTrace();
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("\n\nUNEXPECTED TOP-LEVEL EXCEPTION:");
            ex.printStackTrace();
            System.exit(1);
        } catch (Throwable ex) {
            System.err.println("\n\nUNEXPECTED TOP-LEVEL ERROR:");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Prints the usage message.
     */
    private static void usage(boolean printDebugOptions) {
        SmaliHelpFormatter formatter = new SmaliHelpFormatter();
        int consoleWidth = ConsoleUtil.getConsoleWidth();
        if (consoleWidth <= 0) {
            consoleWidth = 80;
        }

        formatter.setWidth(consoleWidth);

        formatter.printHelp("java -jar baksmali.jar [options] <dex-file>",
                "disassembles and/or dumps a dex file", basicOptions, printDebugOptions?debugOptions:null);
    }

    private static void usage() {
        usage(false);
    }

    /**
     * Prints the version message.
     */
    protected static void version() {
        System.out.println("baksmali " + VERSION + " (http://smali.googlecode.com)");
        System.out.println("Copyright (C) 2010 Ben Gruver (JesusFreke@JesusFreke.com)");
        System.out.println("BSD license (http://www.opensource.org/licenses/bsd-license.php)");
        System.exit(0);
    }

    private static void buildOptions() {
        Option versionOption = OptionBuilder.withLongOpt("version")
                .withDescription("prints the version then exits")
                .create("v");

        Option helpOption = OptionBuilder.withLongOpt("help")
                .withDescription("prints the help message then exits. Specify twice for debug options")
                .create("?");

        Option outputDirOption = OptionBuilder.withLongOpt("output")
                .withDescription("the directory where the disassembled files will be placed. The default is out")
                .hasArg()
                .withArgName("DIR")
                .create("o");

        Option noParameterRegistersOption = OptionBuilder.withLongOpt("no-parameter-registers")
                .withDescription("use the v<n> syntax instead of the p<n> syntax for registers mapped to method " +
                        "parameters")
                .create("p");

        Option deodexerantOption = OptionBuilder.withLongOpt("deodex")
                .withDescription("deodex the given odex file. This option is ignored if the input file is not an " +
                        "odex file")
                .create("x");

        Option useLocalsOption = OptionBuilder.withLongOpt("use-locals")
                .withDescription("output the .locals directive with the number of non-parameter registers, rather" +
                        " than the .register directive with the total number of register")
                .create("l");

        Option sequentialLabelsOption = OptionBuilder.withLongOpt("sequential-labels")
                .withDescription("create label names using a sequential numbering scheme per label type, rather than " +
                        "using the bytecode address")
                .create("s");

        Option noDebugInfoOption = OptionBuilder.withLongOpt("no-debug-info")
                .withDescription("don't write out debug info (.local, .param, .line, etc.)")
                .create("b");

        Option registerInfoOption = OptionBuilder.withLongOpt("register-info")
                .hasOptionalArgs()
                .withArgName("REGISTER_INFO_TYPES")
                .withValueSeparator(',')
                .withDescription("print the specificed type(s) of register information for each instruction. " +
                        "\"ARGS,DEST\" is the default if no types are specified.\nValid values are:\nALL: all " +
                        "pre- and post-instruction registers.\nALLPRE: all pre-instruction registers\nALLPOST: all " +
                        "post-instruction registers\nARGS: any pre-instruction registers used as arguments to the " +
                        "instruction\nDEST: the post-instruction destination register, if any\nMERGE: Any " +
                        "pre-instruction register has been merged from more than 1 different post-instruction " +
                        "register from its predecessors\nFULLMERGE: For each register that would be printed by " +
                        "MERGE, also show the incoming register types that were merged")
                .create("r");

        Option classPathOption = OptionBuilder.withLongOpt("bootclasspath")
                .withDescription("the bootclasspath jars to use, for analysis. Defaults to " +
                        "core.jar:ext.jar:framework.jar:android.policy.jar:services.jar. If the value begins with a " +
                        ":, it will be appended to the default bootclasspath instead of replacing it")
                .hasOptionalArg()
                .withArgName("BOOTCLASSPATH")
                .create("c");

        Option classPathDirOption = OptionBuilder.withLongOpt("bootclasspath-dir")
                .withDescription("the base folder to look for the bootclasspath files in. Defaults to the current " +
                        "directory")
                .hasArg()
                .withArgName("DIR")
                .create("d");

        Option codeOffsetOption = OptionBuilder.withLongOpt("code-offsets")
                .withDescription("add comments to the disassembly containing the code offset for each address")
                .create("f");

        Option noAccessorCommentsOption = OptionBuilder.withLongOpt("no-accessor-comments")
                .withDescription("don't output helper comments for synthetic accessors")
                .create("m");

        Option apiLevelOption = OptionBuilder.withLongOpt("api-level")
                .withDescription("The numeric api-level of the file being disassembled. If not " +
                        "specified, it defaults to 14 (ICS).")
                .hasArg()
                .withArgName("API_LEVEL")
                .create("a");

        Option dumpOption = OptionBuilder.withLongOpt("dump-to")
                .withDescription("dumps the given dex file into a single annotated dump file named FILE" +
                        " (<dexfile>.dump by default), along with the normal disassembly")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("D");

        Option ignoreErrorsOption = OptionBuilder.withLongOpt("ignore-errors")
                .withDescription("ignores any non-fatal errors that occur while disassembling/deodexing," +
                        " ignoring the class if needed, and continuing with the next class. The default" +
                        " behavior is to stop disassembling and exit once an error is encountered")
                .create("I");

        Option jumboInstructionsOption = OptionBuilder.withLongOpt("jumbo-instructions")
                .withDescription("adds support for the jumbo opcodes that were temporarily available around the" +
                        " ics timeframe. Note that support for these opcodes was removed from newer version of" +
                        " dalvik. You shouldn't use this option unless you know the dex file contains these jumbo" +
                        " opcodes.")
                .create("J");

        Option noDisassemblyOption = OptionBuilder.withLongOpt("no-disassembly")
                .withDescription("suppresses the output of the disassembly")
                .create("N");

        Option writeDexOption = OptionBuilder.withLongOpt("write-dex")
                .withDescription("additionally rewrites the input dex file to FILE")
                .hasArg()
                .withArgName("FILE")
                .create("W");

        Option sortOption = OptionBuilder.withLongOpt("sort")
                .withDescription("sort the items in the dex file into a canonical order before dumping/writing")
                .create("S");

        Option fixSignedRegisterOption = OptionBuilder.withLongOpt("fix-signed-registers")
                .withDescription("when dumping or rewriting, fix any registers in the debug info that are encoded as" +
                        " a signed value")
                .create("F");

        Option verifyDexOption = OptionBuilder.withLongOpt("verify")
                .withDescription("perform bytecode verification")
                .create("V");

        Option inlineTableOption = OptionBuilder.withLongOpt("inline-table")
                .withDescription("specify a file containing a custom inline method table to use for deodexing")
                .hasArg()
                .withArgName("FILE")
                .create("T");

        basicOptions.addOption(versionOption);
        basicOptions.addOption(helpOption);
        basicOptions.addOption(outputDirOption);
        basicOptions.addOption(noParameterRegistersOption);
        basicOptions.addOption(deodexerantOption);
        basicOptions.addOption(useLocalsOption);
        basicOptions.addOption(sequentialLabelsOption);
        basicOptions.addOption(noDebugInfoOption);
        basicOptions.addOption(registerInfoOption);
        basicOptions.addOption(classPathOption);
        basicOptions.addOption(classPathDirOption);
        basicOptions.addOption(codeOffsetOption);
        basicOptions.addOption(noAccessorCommentsOption);
        basicOptions.addOption(apiLevelOption);

        debugOptions.addOption(dumpOption);
        debugOptions.addOption(ignoreErrorsOption);
        debugOptions.addOption(jumboInstructionsOption);
        debugOptions.addOption(noDisassemblyOption);
        debugOptions.addOption(writeDexOption);
        debugOptions.addOption(sortOption);
        debugOptions.addOption(fixSignedRegisterOption);
        debugOptions.addOption(verifyDexOption);
        debugOptions.addOption(inlineTableOption);

        for (Object option: basicOptions.getOptions()) {
            options.addOption((Option)option);
        }
        for (Object option: debugOptions.getOptions()) {
            options.addOption((Option)option);
        }
    }
}
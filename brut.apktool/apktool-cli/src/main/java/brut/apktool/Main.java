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
package brut.apktool;

import brut.androlib.*;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.CantFindFrameworkResException;
import brut.androlib.exceptions.InFileNotFoundException;
import brut.androlib.exceptions.OutDirExistsException;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.AaptManager;
import brut.util.OSDetection;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class Main {
    public static void main(String[] args) throws BrutException {

        // headless
        System.setProperty("java.awt.headless", "true");

        // set verbosity default
        Verbosity verbosity = Verbosity.NORMAL;

        // cli parser
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;

        // load options
        _Options();

        try {
            commandLine = parser.parse(allOptions, args, false);

            if (! OSDetection.is64Bit()) {
                System.err.println("32 bit support is deprecated. Apktool will not support 32bit on v3.0.0.");
            }
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            usage();
            System.exit(1);
            return;
        }

        // check for verbose / quiet
        if (commandLine.hasOption("-v") || commandLine.hasOption("--verbose")) {
            verbosity = Verbosity.VERBOSE;
        } else if (commandLine.hasOption("-q") || commandLine.hasOption("--quiet")) {
            verbosity = Verbosity.QUIET;
        }
        setupLogging(verbosity);

        // check for advance mode
        if (commandLine.hasOption("advance") || commandLine.hasOption("advanced")) {
            setAdvanceMode();
        }

        Config config = Config.getDefaultConfig();
        initConfig(commandLine, config);

        boolean cmdFound = false;
        for (String opt : commandLine.getArgs()) {
            if (opt.equalsIgnoreCase("d") || opt.equalsIgnoreCase("decode")) {
                cmdDecode(commandLine, config);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("b") || opt.equalsIgnoreCase("build")) {
                cmdBuild(commandLine, config);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("if") || opt.equalsIgnoreCase("install-framework")) {
                cmdInstallFramework(commandLine, config);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("empty-framework-dir")) {
                cmdEmptyFrameworkDirectory(commandLine, config);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("list-frameworks")) {
                cmdListFrameworks(commandLine, config);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("publicize-resources")) {
                cmdPublicizeResources(commandLine, config);
                cmdFound = true;
            }
        }

        // if no commands ran, run the version / usage check.
        if (!cmdFound) {
            if (commandLine.hasOption("version")) {
                _version();
                System.exit(0);
            } else {
                usage();
            }
        }
    }

    private static void initConfig(CommandLine cli, Config config) throws AndrolibException {
        // init common config options from command line flags

        if (cli.hasOption("p") || cli.hasOption("frame-path")) {
            config.frameworkDirectory = cli.getOptionValue("p");
        }
        if (cli.hasOption("t") || cli.hasOption("tag")) {
            config.frameworkTag = cli.getOptionValue("t");
        }
        if (cli.hasOption("api") || cli.hasOption("api-level")) {
            config.apiLevel = Integer.parseInt(cli.getOptionValue("api"));
        }
    }

    private static void cmdDecode(CommandLine cli, Config config) throws AndrolibException {
        String apkName = getLastArg(cli);

        // check decode options
        if (cli.hasOption("s") || cli.hasOption("no-src")) {
            config.setDecodeSources(Config.DECODE_SOURCES_NONE);
        }
        if (cli.hasOption("only-main-classes")) {
            config.setDecodeSources(Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES);
        }
        if (cli.hasOption("d") || cli.hasOption("debug")) {
            System.err.println("SmaliDebugging has been removed in 2.1.0 onward. Please see: https://github.com/iBotPeaches/Apktool/issues/1061");
            System.exit(1);
        }
        if (cli.hasOption("b") || cli.hasOption("no-debug-info")) {
            config.baksmaliDebugMode = false;
        }
        if (cli.hasOption("f") || cli.hasOption("force")) {
            config.forceDelete = true;
        }
        if (cli.hasOption("r") || cli.hasOption("no-res")) {
            config.setDecodeResources(Config.DECODE_RESOURCES_NONE);
        }
        if (cli.hasOption("force-manifest")) {
            config.setForceDecodeManifest(Config.FORCE_DECODE_MANIFEST_FULL);
        }
        if (cli.hasOption("no-assets")) {
            config.setDecodeAssets(Config.DECODE_ASSETS_NONE);
        }
        if (cli.hasOption("k") || cli.hasOption("keep-broken-res")) {
            config.keepBrokenResources = true;
        }
        if (cli.hasOption("m") || cli.hasOption("match-original")) {
            config.analysisMode = true;
        }

        File outDir;
        if (cli.hasOption("o") || cli.hasOption("output")) {
            outDir = new File(cli.getOptionValue("o"));
        } else {
            // make out folder manually using name of apk
            String outName = apkName;
            outName = outName.endsWith(".apk") ? outName.substring(0,
                    outName.length() - 4).trim() : outName + ".out";

            // make file from path
            outName = new File(outName).getName();
            outDir = new File(outName);
        }

        ApkDecoder decoder = new ApkDecoder(config, new ExtFile(apkName));
        decoder.setOutDir(outDir);

        try {
            decoder.decode();
        } catch (OutDirExistsException ex) {
            System.err
                    .println("Destination directory ("
                            + outDir.getAbsolutePath()
                            + ") "
                            + "already exists. Use -f switch if you want to overwrite it.");
            System.exit(1);
        } catch (InFileNotFoundException ex) {
            System.err.println("Input file (" + apkName + ") " + "was not found or was not readable.");
            System.exit(1);
        } catch (CantFindFrameworkResException ex) {
            System.err
                    .println("Can't find framework resources for package of id: "
                            + ex.getPkgId()
                            + ". You must install proper "
                            + "framework files, see project website for more info.");
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Could not modify file. Please ensure you have permission.");
            System.exit(1);
        } catch (DirectoryException ex) {
            System.err.println("Could not modify internal dex files. Please ensure you have permission.");
            System.exit(1);
        } finally {
            try {
                decoder.close();
            } catch (IOException ignored) {}
        }
    }

    private static void cmdBuild(CommandLine cli, Config config) {
        String[] args = cli.getArgs();
        String appDirName = args.length < 2 ? "." : args[1];

        // check for build options
        if (cli.hasOption("f") || cli.hasOption("force-all")) {
            config.forceBuildAll = true;
        }
        if (cli.hasOption("d") || cli.hasOption("debug")) {
            config.debugMode = true;
        }
        if (cli.hasOption("n") || cli.hasOption("net-sec-conf")) {
            config.netSecConf = true;
        }
        if (cli.hasOption("v") || cli.hasOption("verbose")) {
            config.verbose = true;
        }
        if (cli.hasOption("a") || cli.hasOption("aapt")) {
            config.aaptPath = cli.getOptionValue("a");
        }
        if (cli.hasOption("c") || cli.hasOption("copy-original")) {
            System.err.println("-c/--copy-original has been deprecated. Removal planned for v3.0.0 (#2129)");
            config.copyOriginalFiles = true;
        }
        if (cli.hasOption("nc") || cli.hasOption("no-crunch")) {
            config.noCrunch = true;
        }

        // Temporary flag to enable the use of aapt2. This will transform in time to a use-aapt1 flag, which will be
        // legacy and eventually removed.
        if (cli.hasOption("use-aapt2")) {
            config.useAapt2 = true;
        }

        File outFile;
        if (cli.hasOption("o") || cli.hasOption("output")) {
            outFile = new File(cli.getOptionValue("o"));
        } else {
            outFile = null;
        }

        if (config.netSecConf && !config.useAapt2) {
            System.err.println("-n / --net-sec-conf is only supported with --use-aapt2.");
            System.exit(1);
        }

        // try and build apk
        try {
            if (cli.hasOption("a") || cli.hasOption("aapt")) {
                config.aaptVersion = AaptManager.getAaptVersion(cli.getOptionValue("a"));
            }
            new Androlib(config).build(new File(appDirName), outFile);
        } catch (BrutException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static void cmdInstallFramework(CommandLine cli, Config config) throws AndrolibException {
        String apkName = getLastArg(cli);
        new Androlib(config).installFramework(new File(apkName));
    }

    private static void cmdListFrameworks(CommandLine cli, Config config) throws AndrolibException {
        new Androlib(config).listFrameworks();
    }

    private static void cmdPublicizeResources(CommandLine cli, Config config) throws AndrolibException {
        String apkName = getLastArg(cli);
        new Androlib(config).publicizeResources(new File(apkName));
    }

    private static void cmdEmptyFrameworkDirectory(CommandLine cli, Config config) throws AndrolibException {
        if (cli.hasOption("f") || cli.hasOption("force")) {
            config.forceDeleteFramework = true;
        }
        new Androlib(config).emptyFrameworkDirectory();
    }

    private static String getLastArg(CommandLine cli) {
        int paraCount = cli.getArgList().size();
        return cli.getArgList().get(paraCount - 1);
    }

    private static void _version() {
        System.out.println(Androlib.getVersion());
    }

    private static void _Options() {

        // create options
        Option versionOption = Option.builder("version")
                .longOpt("version")
                .desc("prints the version then exits")
                .build();

        Option advanceOption = Option.builder("advance")
                .longOpt("advanced")
                .desc("prints advance information.")
                .build();

        Option noSrcOption = Option.builder("s")
                .longOpt("no-src")
                .desc("Do not decode sources.")
                .build();

        Option onlyMainClassesOption = Option.builder()
                .longOpt("only-main-classes")
                .desc("Only disassemble the main dex classes (classes[0-9]*.dex) in the root.")
                .build();

        Option noResOption = Option.builder("r")
                .longOpt("no-res")
                .desc("Do not decode resources.")
                .build();

        Option forceManOption = Option.builder()
                .longOpt("force-manifest")
                .desc("Decode the APK's compiled manifest, even if decoding of resources is set to \"false\".")
                .build();

        Option noAssetOption = Option.builder()
                .longOpt("no-assets")
                .desc("Do not decode assets.")
                .build();

        Option debugDecOption = Option.builder("d")
                .longOpt("debug")
                .desc("REMOVED (DOES NOT WORK): Decode in debug mode.")
                .build();

        Option analysisOption = Option.builder("m")
                .longOpt("match-original")
                .desc("Keeps files to closest to original as possible. Prevents rebuild.")
                .build();

        Option apiLevelOption = Option.builder("api")
                .longOpt("api-level")
                .desc("The numeric api-level of the file to generate, e.g. 14 for ICS.")
                .hasArg(true)
                .argName("API")
                .build();

        Option debugBuiOption = Option.builder("d")
                .longOpt("debug")
                .desc("Sets android:debuggable to \"true\" in the APK's compiled manifest")
                .build();

        Option netSecConfOption = Option.builder("n")
            .longOpt("net-sec-conf")
            .desc("Adds a generic Network Security Configuration file in the output APK")
            .build();

        Option noDbgOption = Option.builder("b")
                .longOpt("no-debug-info")
                .desc("don't write out debug info (.local, .param, .line, etc.)")
                .build();

        Option forceDecOption = Option.builder("f")
                .longOpt("force")
                .desc("Force delete destination directory.")
                .build();

        Option frameTagOption = Option.builder("t")
                .longOpt("frame-tag")
                .desc("Uses framework files tagged by <tag>.")
                .hasArg(true)
                .argName("tag")
                .build();

        Option frameDirOption = Option.builder("p")
                .longOpt("frame-path")
                .desc("Uses framework files located in <dir>.")
                .hasArg(true)
                .argName("dir")
                .build();

        Option frameIfDirOption = Option.builder("p")
                .longOpt("frame-path")
                .desc("Stores framework files into <dir>.")
                .hasArg(true)
                .argName("dir")
                .build();

        Option keepResOption = Option.builder("k")
                .longOpt("keep-broken-res")
                .desc("Use if there was an error and some resources were dropped, e.g.\n"
                        + "            \"Invalid config flags detected. Dropping resources\", but you\n"
                        + "            want to decode them anyway, even with errors. You will have to\n"
                        + "            fix them manually before building.")
                .build();

        Option forceBuiOption = Option.builder("f")
                .longOpt("force-all")
                .desc("Skip changes detection and build all files.")
                .build();

        Option aaptOption = Option.builder("a")
                .longOpt("aapt")
                .hasArg(true)
                .argName("loc")
                .desc("Loads aapt from specified location.")
                .build();

        Option aapt2Option = Option.builder()
                .longOpt("use-aapt2")
                .desc("Upgrades apktool to use experimental aapt2 binary.")
                .build();

        Option originalOption = Option.builder("c")
                .longOpt("copy-original")
                .desc("Copies original AndroidManifest.xml and META-INF. See project page for more info.")
                .build();

        Option noCrunchOption = Option.builder("nc")
                .longOpt("no-crunch")
                .desc("Disable crunching of resource files during the build step.")
                .build();

        Option tagOption = Option.builder("t")
                .longOpt("tag")
                .desc("Tag frameworks using <tag>.")
                .hasArg(true)
                .argName("tag")
                .build();

        Option outputBuiOption = Option.builder("o")
                .longOpt("output")
                .desc("The name of apk that gets written. Default is dist/name.apk")
                .hasArg(true)
                .argName("dir")
                .build();

        Option outputDecOption = Option.builder("o")
                .longOpt("output")
                .desc("The name of folder that gets written. Default is apk.out")
                .hasArg(true)
                .argName("dir")
                .build();

        Option quietOption = Option.builder("q")
                .longOpt("quiet")
                .build();

        Option verboseOption = Option.builder("v")
                .longOpt("verbose")
                .build();

        // check for advance mode
        if (isAdvanceMode()) {
            decodeOptions.addOption(noDbgOption);
            decodeOptions.addOption(keepResOption);
            decodeOptions.addOption(analysisOption);
            decodeOptions.addOption(onlyMainClassesOption);
            decodeOptions.addOption(apiLevelOption);
            decodeOptions.addOption(noAssetOption);
            decodeOptions.addOption(forceManOption);

            buildOptions.addOption(apiLevelOption);
            buildOptions.addOption(debugBuiOption);
            buildOptions.addOption(netSecConfOption);
            buildOptions.addOption(aaptOption);
            buildOptions.addOption(originalOption);
            buildOptions.addOption(aapt2Option);
            buildOptions.addOption(noCrunchOption);
        }

        // add global options
        normalOptions.addOption(versionOption);
        normalOptions.addOption(advanceOption);

        // add basic decode options
        decodeOptions.addOption(frameTagOption);
        decodeOptions.addOption(outputDecOption);
        decodeOptions.addOption(frameDirOption);
        decodeOptions.addOption(forceDecOption);
        decodeOptions.addOption(noSrcOption);
        decodeOptions.addOption(noResOption);

        // add basic build options
        buildOptions.addOption(outputBuiOption);
        buildOptions.addOption(frameDirOption);
        buildOptions.addOption(forceBuiOption);

        // add basic framework options
        frameOptions.addOption(tagOption);
        frameOptions.addOption(frameIfDirOption);

        // add empty framework options
        emptyFrameworkOptions.addOption(forceDecOption);
        emptyFrameworkOptions.addOption(frameIfDirOption);

        // add list framework options
        listFrameworkOptions.addOption(frameIfDirOption);

        // add all, loop existing cats then manually add advance
        for (Option op : normalOptions.getOptions()) {
            allOptions.addOption(op);
        }
        for (Option op : decodeOptions.getOptions()) {
            allOptions.addOption(op);
        }
        for (Option op : buildOptions.getOptions()) {
            allOptions.addOption(op);
        }
        for (Option op : frameOptions.getOptions()) {
            allOptions.addOption(op);
        }
        allOptions.addOption(apiLevelOption);
        allOptions.addOption(analysisOption);
        allOptions.addOption(debugDecOption);
        allOptions.addOption(noDbgOption);
        allOptions.addOption(forceManOption);
        allOptions.addOption(noAssetOption);
        allOptions.addOption(keepResOption);
        allOptions.addOption(debugBuiOption);
        allOptions.addOption(netSecConfOption);
        allOptions.addOption(aaptOption);
        allOptions.addOption(originalOption);
        allOptions.addOption(verboseOption);
        allOptions.addOption(quietOption);
        allOptions.addOption(aapt2Option);
        allOptions.addOption(noCrunchOption);
        allOptions.addOption(onlyMainClassesOption);
    }

    private static String verbosityHelp() {
        if (isAdvanceMode()) {
            return "[-q|--quiet OR -v|--verbose] ";
        } else {
            return "";
        }
    }

    private static void usage() {
        _Options();
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);

        // print out license info prior to formatter.
        System.out.println(
                "Apktool v" + Androlib.getVersion() + " - a tool for reengineering Android apk files\n" +
                        "with smali v" + ApktoolProperties.get("smaliVersion") +
                        " and baksmali v" + ApktoolProperties.get("baksmaliVersion") + "\n" +
                        "Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>\n" +
                        "Copyright 2010 Connor Tumbleson <connor.tumbleson@gmail.com>" );
        if (isAdvanceMode()) {
            System.out.println("Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0)\n");
        }else {
            System.out.println();
        }

        // 4 usage outputs (general, frameworks, decode, build)
        formatter.printHelp("apktool " + verbosityHelp(), normalOptions);
        formatter.printHelp("apktool " + verbosityHelp() + "if|install-framework [options] <framework.apk>", frameOptions);
        formatter.printHelp("apktool " + verbosityHelp() + "d[ecode] [options] <file_apk>", decodeOptions);
        formatter.printHelp("apktool " + verbosityHelp() + "b[uild] [options] <app_path>", buildOptions);
        if (isAdvanceMode()) {
            formatter.printHelp("apktool " + verbosityHelp() + "publicize-resources <file_path>", emptyOptions);
            formatter.printHelp("apktool " + verbosityHelp() + "empty-framework-dir [options]", emptyFrameworkOptions);
            formatter.printHelp("apktool " + verbosityHelp() + "list-frameworks [options]", listFrameworkOptions);
        }
        System.out.println();

        // print out more information
        System.out.println(
                "For additional info, see: https://ibotpeaches.github.io/Apktool/ \n"
                        + "For smali/baksmali info, see: https://github.com/JesusFreke/smali");
    }

    private static void setupLogging(final Verbosity verbosity) {
        Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        LogManager.getLogManager().reset();

        if (verbosity == Verbosity.QUIET) {
            return;
        }

        Handler handler = new Handler(){
            @Override
            public void publish(LogRecord record) {
                if (getFormatter() == null) {
                    setFormatter(new SimpleFormatter());
                }

                try {
                    String message = getFormatter().format(record);
                    if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                        System.err.write(message.getBytes());
                    } else {
                        if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                            System.out.write(message.getBytes());
                        } else {
                            if (verbosity == Verbosity.VERBOSE) {
                                System.out.write(message.getBytes());
                            }
                        }
                    }
                } catch (Exception exception) {
                    reportError(null, exception, ErrorManager.FORMAT_FAILURE);
                }
            }
            @Override
            public void close() throws SecurityException {}
            @Override
            public void flush(){}
        };

        logger.addHandler(handler);

        if (verbosity == Verbosity.VERBOSE) {
            handler.setLevel(Level.ALL);
            logger.setLevel(Level.ALL);
        } else {
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getLevel().toString().charAt(0) + ": "
                            + record.getMessage()
                            + System.getProperty("line.separator");
                }
            });
        }
    }

    private static boolean isAdvanceMode() {
        return advanceMode;
    }

    private static void setAdvanceMode() {
        Main.advanceMode = true;
    }

    private enum Verbosity {
        NORMAL, VERBOSE, QUIET
    }

    private static boolean advanceMode = false;

    private final static Options normalOptions;
    private final static Options decodeOptions;
    private final static Options buildOptions;
    private final static Options frameOptions;
    private final static Options allOptions;
    private final static Options emptyOptions;
    private final static Options emptyFrameworkOptions;
    private final static Options listFrameworkOptions;

    static {
        //normal and advance usage output
        normalOptions = new Options();
        buildOptions = new Options();
        decodeOptions = new Options();
        frameOptions = new Options();
        allOptions = new Options();
        emptyOptions = new Options();
        emptyFrameworkOptions = new Options();
        listFrameworkOptions = new Options();
    }
}

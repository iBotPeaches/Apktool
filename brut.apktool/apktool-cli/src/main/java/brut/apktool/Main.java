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
import brut.androlib.res.Framework;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OSDetection;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.logging.*;

/**
 * Main entry point of the apktool.
 */
public class Main {
    private enum Verbosity { NORMAL, VERBOSE, QUIET }

    private static final Options normalOptions = new Options();
    private static final Options decodeOptions = new Options();
    private static final Options buildOptions = new Options();
    private static final Options frameOptions = new Options();
    private static final Options allOptions = new Options();
    private static final Options emptyOptions = new Options();
    private static final Options emptyFrameworkOptions = new Options();
    private static final Options listFrameworkOptions = new Options();

    private static boolean advanceMode = false;

    public static void main(String[] args) throws BrutException {

        // headless
        System.setProperty("java.awt.headless", "true");

        // Ignore stricter validation on zip files from java 11 onwards as this is a protection technique
        // that applications use to thwart disassembly tools. We have protections in place for directory traversal
        // and handling of bogus data in the zip header, so we can ignore this.
        System.setProperty("jdk.nio.zipfs.allowDotZipEntry", "true");
        System.setProperty("jdk.util.zip.disableZip64ExtraFieldValidation", "true");

        // set verbosity default
        Verbosity verbosity = Verbosity.NORMAL;

        // cli parser
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;

        // load options
        _options();

        try {
            commandLine = parser.parse(allOptions, args, false);

            if (!OSDetection.is64Bit()) {
                System.err.println("32-bit support is deprecated and will be removed in 3.0.0.");
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

        Config config = new Config();
        initConfig(commandLine, config);

        boolean cmdFound = false;
        for (String opt : commandLine.getArgs()) {
            switch (opt) {
                case "d":
                case "decode":
                    cmdDecode(commandLine, config);
                    cmdFound = true;
                    break;
                case "b":
                case "build":
                    cmdBuild(commandLine, config);
                    cmdFound = true;
                    break;
                case "if":
                case "install-framework":
                    cmdInstallFramework(commandLine, config);
                    cmdFound = true;
                    break;
                case "empty-framework-dir":
                    cmdEmptyFrameworkDirectory(commandLine, config);
                    cmdFound = true;
                    break;
                case "list-frameworks":
                    cmdListFrameworks(commandLine, config);
                    cmdFound = true;
                    break;
                case "publicize-resources":
                    cmdPublicizeResources(commandLine, config);
                    cmdFound = true;
                    break;
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

    private static void initConfig(CommandLine cli, Config config) {
        if (cli.hasOption("p") || cli.hasOption("frame-path")) {
            config.setFrameworkDirectory(cli.getOptionValue("p"));
        }
        if (cli.hasOption("t") || cli.hasOption("tag")) {
            config.setFrameworkTag(cli.getOptionValue("t"));
        }
        if (cli.hasOption("api") || cli.hasOption("api-level")) {
            config.setApiLevel(Integer.parseInt(cli.getOptionValue("api")));
        }
        if (cli.hasOption("j") || cli.hasOption("jobs")) {
            config.setJobs(Integer.parseInt(cli.getOptionValue("j")));
        }
    }

    private static void cmdDecode(CommandLine cli, Config config) throws AndrolibException {
        String apkName = getLastArg(cli);

        // check decode options
        if (cli.hasOption("s") || cli.hasOption("no-src")) {
            config.setDecodeSources(Config.DECODE_SOURCES_NONE);
        }
        if (cli.hasOption("only-main-classes")) {
            if (cli.hasOption("s") || cli.hasOption("no-src")) {
                System.err.println("--only-main-classes cannot be paired with -s/--no-src. Ignoring.");
            } else {
                config.setDecodeSources(Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES);
            }
        }
        if (cli.hasOption("d") || cli.hasOption("debug")) {
            System.err.println("SmaliDebugging has been removed in 2.1.0 onward. Please see: https://github.com/iBotPeaches/Apktool/issues/1061");
            System.exit(1);
        }
        if (cli.hasOption("b") || cli.hasOption("no-debug-info")) {
            config.setBaksmaliDebugMode(false);
        }
        if (cli.hasOption("f") || cli.hasOption("force")) {
            config.setForceDelete(true);
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
            config.setKeepBrokenResources(true);
        }
        if (cli.hasOption("m") || cli.hasOption("match-original")) {
            config.setAnalysisMode(true);
        }
        if (cli.hasOption("resm") || cli.hasOption("res-mode") || cli.hasOption("resolve-resources-mode")) {
            String mode = cli.getOptionValue("resm");
            if (mode == null) {
                mode = cli.getOptionValue("res-mode");
            }
            if (mode == null) {
                mode = cli.getOptionValue("resolve-resources-mode");
            }

            switch (mode) {
                case "remove":
                case "delete":
                    config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_REMOVE);
                    break;
                case "dummy":
                case "dummies":
                    config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_DUMMY);
                    break;
                case "keep":
                case "preserve":
                    config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_RETAIN);
                    break;
                default:
                    System.err.println("Unknown resolve resources mode: " + mode);
                    System.err.println("Expect: 'remove', 'dummy' or 'keep'.");
                    System.exit(1);
            }
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

        ExtFile apkFile = new ExtFile(apkName);
        ApkDecoder decoder = new ApkDecoder(apkFile, config);
        try {
            decoder.decode(outDir);
        } catch (OutDirExistsException ex) {
            System.err
                    .println("Destination directory ("
                            + outDir.getAbsolutePath()
                            + ") "
                            + "already exists. Use -f switch if you want to overwrite it.");
            System.exit(1);
        } catch (InFileNotFoundException ex) {
            System.err.println("Input file (" + apkFile.getAbsolutePath() + ") " + "was not found or was not readable.");
            System.exit(1);
        } catch (CantFindFrameworkResException ex) {
            System.err
                    .println("Could not find framework resources for package of id: "
                            + ex.getPkgId()
                            + ". You must install proper "
                            + "framework files, see project website for more info.");
            System.exit(1);
        }
    }

    private static void cmdBuild(CommandLine cli, Config config) throws AndrolibException {
        String[] args = cli.getArgs();
        String apkDirName = args.length < 2 ? "." : args[1];

        // check for build options
        if (cli.hasOption("f") || cli.hasOption("force-all")) {
            config.setForceBuildAll(true);
        }
        if (cli.hasOption("d") || cli.hasOption("debug")) {
            config.setDebugMode(true);
        }
        if (cli.hasOption("n") || cli.hasOption("net-sec-conf")) {
            config.setNetSecConf(true);
        }
        if (cli.hasOption("v") || cli.hasOption("verbose")) {
            config.setVerbose(true);
        }
        if (cli.hasOption("a") || cli.hasOption("aapt")) {
            if (cli.hasOption("use-aapt1") || cli.hasOption("use-aapt2")) {
                System.err.println("You can only use one of -a/--aapt or --use-aapt1 or --use-aapt2.");
                System.exit(1);
            }

            try {
                config.setAaptBinary(new File(cli.getOptionValue("a")));
            } catch (BrutException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        } else if (cli.hasOption("use-aapt1")) {
            if (cli.hasOption("use-aapt2")) {
                System.err.println("You can only use one of --use-aapt1 or --use-aapt2.");
                System.exit(1);
            }

            config.setAaptVersion(1);
        }
        if (cli.hasOption("c") || cli.hasOption("copy-original")) {
            config.setCopyOriginalFiles(true);
        }
        if (cli.hasOption("nc") || cli.hasOption("no-crunch")) {
            config.setNoCrunch(true);
        }
        if (cli.hasOption("na") || cli.hasOption("no-apk")) {
            config.setNoApk(true);
        }

        if (config.isNetSecConf() && config.getAaptVersion() == 1) {
            System.err.println("-n / --net-sec-conf is not supported with legacy aapt.");
            System.exit(1);
        }

        File outFile = cli.hasOption("o") || cli.hasOption("output")
            ? new File(cli.getOptionValue("o")) : null;

        ExtFile apkDir = new ExtFile(apkDirName);
        ApkBuilder builder = new ApkBuilder(apkDir, config);
        builder.build(outFile);
    }

    private static void cmdInstallFramework(CommandLine cli, Config config) throws AndrolibException {
        String apkName = getLastArg(cli);
        new Framework(config).install(new File(apkName));
    }

    private static void cmdListFrameworks(CommandLine cli, Config config) throws AndrolibException {
        new Framework(config).listDirectory();
    }

    private static void cmdPublicizeResources(CommandLine cli, Config config) throws AndrolibException {
        String apkName = getLastArg(cli);
        new Framework(config).publicizeResources(new File(apkName));
    }

    private static void cmdEmptyFrameworkDirectory(CommandLine cli, Config config) throws AndrolibException {
        if (cli.hasOption("f") || cli.hasOption("force")) {
            config.setForceDeleteFramework(true);
        }
        new Framework(config).emptyDirectory();
    }

    private static String getLastArg(CommandLine cli) {
        int paraCount = cli.getArgList().size();
        return cli.getArgList().get(paraCount - 1);
    }

    private static void _version() {
        System.out.println(ApktoolProperties.getVersion());
    }

    private static void _options() {
        Option versionOption = Option.builder("version")
                .longOpt("version")
                .desc("Print the version.")
                .build();

        Option advanceOption = Option.builder("advance")
                .longOpt("advanced")
                .desc("Print advanced information.")
                .build();

        Option jobsOption = Option.builder("j")
                .longOpt("jobs")
                .hasArg()
                .type(Integer.class)
                .desc("Sets the number of threads to use.")
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
                .desc("Keep files closest to original as possible (prevents rebuild).")
                .build();

        Option apiLevelOption = Option.builder("api")
                .longOpt("api-level")
                .desc("The numeric api-level of the file to generate, e.g. 14 for ICS.")
                .hasArg(true)
                .argName("API")
                .build();

        Option debugBuiOption = Option.builder("d")
                .longOpt("debug")
                .desc("Set android:debuggable to \"true\" in the APK's compiled manifest.")
                .build();

        Option netSecConfOption = Option.builder("n")
                .longOpt("net-sec-conf")
                .desc("Add a generic Network Security Configuration file in the output APK")
                .build();

        Option noDbgOption = Option.builder("b")
                .longOpt("no-debug-info")
                .desc("Do not write out debug info (.local, .param, .line, etc.)")
                .build();

        Option forceDecOption = Option.builder("f")
                .longOpt("force")
                .desc("Force delete destination directory.")
                .build();

        Option frameTagOption = Option.builder("t")
                .longOpt("frame-tag")
                .desc("Use framework files tagged by <tag>.")
                .hasArg(true)
                .argName("tag")
                .build();

        Option frameDirOption = Option.builder("p")
                .longOpt("frame-path")
                .desc("Use framework files located in <dir>.")
                .hasArg(true)
                .argName("dir")
                .build();

        Option frameIfDirOption = Option.builder("p")
                .longOpt("frame-path")
                .desc("Store framework files into <dir>.")
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

        Option resolveResModeOption = Option.builder("resm")
                .longOpt("resource-mode")
                .desc("Sets the resolve resources mode. Possible values are: 'remove' (default), 'dummy' or 'keep'.")
                .hasArg(true)
                .argName("mode")
                .build();

        Option aaptOption = Option.builder("a")
                .longOpt("aapt")
                .hasArg(true)
                .argName("loc")
                .desc("Load aapt from specified location.")
                .build();

        Option aapt1Option = Option.builder()
                .longOpt("use-aapt1")
                .desc("Use aapt binary instead of aapt2 during the build step.")
                .build();

        Option aapt2Option = Option.builder()
                .longOpt("use-aapt2")
                .desc("Use aapt2 binary instead of aapt during the build step. (default)")
                .build();

        Option originalOption = Option.builder("c")
                .longOpt("copy-original")
                .desc("Copy original AndroidManifest.xml and META-INF. See project page for more info.")
                .build();

        Option noCrunchOption = Option.builder("nc")
                .longOpt("no-crunch")
                .desc("Disable crunching of resource files during the build step.")
                .build();

        Option noApkOption = Option.builder("na")
                .longOpt("no-apk")
                .desc("Disable repacking of the built files into a new apk.")
                .build();

        Option tagOption = Option.builder("t")
                .longOpt("tag")
                .desc("Tag frameworks using <tag>.")
                .hasArg(true)
                .argName("tag")
                .build();

        Option outputBuiOption = Option.builder("o")
                .longOpt("output")
                .desc("The name of apk that gets written. (default: dist/name.apk)")
                .hasArg(true)
                .argName("file")
                .build();

        Option outputDecOption = Option.builder("o")
                .longOpt("output")
                .desc("The name of folder that gets written. (default: apk.out)")
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
            decodeOptions.addOption(jobsOption);
            decodeOptions.addOption(noDbgOption);
            decodeOptions.addOption(keepResOption);
            decodeOptions.addOption(analysisOption);
            decodeOptions.addOption(onlyMainClassesOption);
            decodeOptions.addOption(apiLevelOption);
            decodeOptions.addOption(noAssetOption);
            decodeOptions.addOption(forceManOption);
            decodeOptions.addOption(resolveResModeOption);

            buildOptions.addOption(jobsOption);
            buildOptions.addOption(apiLevelOption);
            buildOptions.addOption(debugBuiOption);
            buildOptions.addOption(netSecConfOption);
            buildOptions.addOption(aaptOption);
            buildOptions.addOption(originalOption);
            buildOptions.addOption(aapt1Option);
            buildOptions.addOption(noCrunchOption);
            buildOptions.addOption(noApkOption);
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
        allOptions.addOption(jobsOption);
        allOptions.addOption(apiLevelOption);
        allOptions.addOption(analysisOption);
        allOptions.addOption(debugDecOption);
        allOptions.addOption(noDbgOption);
        allOptions.addOption(forceManOption);
        allOptions.addOption(resolveResModeOption);
        allOptions.addOption(noAssetOption);
        allOptions.addOption(keepResOption);
        allOptions.addOption(debugBuiOption);
        allOptions.addOption(netSecConfOption);
        allOptions.addOption(aaptOption);
        allOptions.addOption(originalOption);
        allOptions.addOption(verboseOption);
        allOptions.addOption(quietOption);
        allOptions.addOption(aapt1Option);
        allOptions.addOption(aapt2Option);
        allOptions.addOption(noCrunchOption);
        allOptions.addOption(noApkOption);
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
        _options();
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);

        // print out license info prior to formatter.
        System.out.println(
            "Apktool " + ApktoolProperties.getVersion() + " - a tool for reengineering Android apk files\n" +
                    "with smali " + ApktoolProperties.getSmaliVersion() +
                    " and baksmali " + ApktoolProperties.getBaksmaliVersion() + "\n" +
                    "Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>\n" +
                    "Copyright 2010 Connor Tumbleson <connor.tumbleson@gmail.com>");
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
        System.out.println("For additional info, see: https://apktool.org \n"
            + "For smali/baksmali info, see: https://github.com/google/smali");
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

        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (getFormatter() == null) {
                    setFormatter(new Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            return record.getLevel().toString().charAt(0) + ": "
                                + record.getMessage()
                                + System.getProperty("line.separator");
                        }
                    });
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
                } catch (Exception ex) {
                    reportError(null, ex, ErrorManager.FORMAT_FAILURE);
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
        }
    }

    private static boolean isAdvanceMode() {
        return advanceMode;
    }

    private static void setAdvanceMode() {
        Main.advanceMode = true;
    }
}

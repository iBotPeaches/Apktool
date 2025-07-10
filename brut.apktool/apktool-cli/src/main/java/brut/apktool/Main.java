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
import java.util.Arrays;
import java.util.List;
import java.util.logging.*;

/**
 * Main entry point of apktool.
 */
public class Main {
    private enum Verbosity { NORMAL, VERBOSE, QUIET }

    private static final Option verboseOption = Option.builder("v")
            .longOpt("verbose")
            .desc("Increase output verbosity.")
            .build();

    private static final Option quietOption = Option.builder("q")
            .longOpt("quiet")
            .desc("Suppress normal output.")
            .build();

    private static final Option jobsOption = Option.builder("j")
            .longOpt("jobs")
            .desc("Set the number of jobs to execute in parallel to <num>.")
            .hasArg()
            .argName("num")
            .type(Integer.class)
            .build();

    private static final Option frameDirOption = Option.builder("p")
            .longOpt("frame-path")
            .desc("Use framework files located in <dir>.")
            .hasArg()
            .argName("dir")
            .build();

    private static final Option frameTagOption = Option.builder("t")
            .longOpt("frame-tag")
            .desc("Use framework files tagged with <tag>.")
            .hasArg()
            .argName("tag")
            .build();

    private static final Option libOption = Option.builder("l")
            .longOpt("lib")
            .desc("Use shared library <package> located in <file>.\n"
                    + "            Can be specified multiple times.")
            .hasArg()
            .argName("package:file")
            .build();

    private static final Option decodeForceOption = Option.builder("f")
            .longOpt("force")
            .desc("Force delete destination directory.")
            .build();

    private static final Option decodeNoSrcOption = Option.builder("s")
            .longOpt("no-src")
            .desc("Do not decode sources.")
            .build();

    private static final Option decodeOnlyMainClassesOption = Option.builder()
            .longOpt("only-main-classes")
            .desc("Only disassemble the main dex classes (classes[0-9]*.dex) in the root.")
            .build();

    private static final Option decodeNoDebugInfoOption = Option.builder("b")
            .longOpt("no-debug-info")
            .desc("Do not write out debug info (.local, .param, .line, etc.)")
            .build();

    private static final Option decodeApiLevelOption = Option.builder("api")
            .longOpt("api-level")
            .desc("Force the API level to use for baksmali to <api>.")
            .hasArg()
            .argName("api")
            .build();

    private static final Option decodeNoResOption = Option.builder("r")
            .longOpt("no-res")
            .desc("Do not decode resources.")
            .build();

    private static final Option decodeOnlyManifestOption = Option.builder()
            .longOpt("only-manifest")
            .desc("Only decode AndroidManifest.xml without resources.")
            .build();

    private static final Option decodeResResolveModeOption = Option.builder("resm")
            .longOpt("res-resolve-mode")
            .desc("Set the resolve mode for resources to <mode>.\n"
                    + "            Possible values are: 'remove' (default), 'dummy' or 'keep'.")
            .hasArg()
            .argName("mode")
            .build();

    private static final Option decodeKeepBrokenResOption = Option.builder("k")
            .longOpt("keep-broken-res")
            .desc("Use if there was an error and some resources were dropped, e.g.\n"
                    + "            \"Invalid config flags detected. Dropping resources\", but you\n"
                    + "            want to decode them anyway, even with errors. You will have to\n"
                    + "            fix them manually before building.")
            .build();

    private static final Option decodeMatchOriginalOption = Option.builder("m")
            .longOpt("match-original")
            .desc("Keep files closest to original as possible (prevents rebuild).")
            .build();

    private static final Option decodeNoAssetOption = Option.builder()
            .longOpt("no-assets")
            .desc("Do not decode assets.")
            .build();

    private static final Option decodeOutputOption = Option.builder("o")
            .longOpt("output")
            .desc("Output decoded files to <dir>. (default: apk.out)")
            .hasArg()
            .argName("dir")
            .build();

    private static final Option buildForceOption = Option.builder("f")
            .longOpt("force")
            .desc("Skip changes detection and build all files.")
            .build();

    private static final Option buildDebugOption = Option.builder("d")
            .longOpt("debug")
            .desc("Set android:debuggable to \"true\" in AndroidManifest.xml for the built apk.")
            .build();

    private static final Option buildNetSecConfOption = Option.builder("n")
            .longOpt("net-sec-conf")
            .desc("Add a generic network security configuration file to the built apk.")
            .build();

    private static final Option buildCopyOriginalOption = Option.builder("c")
            .longOpt("copy-original")
            .desc("Copy original AndroidManifest.xml and META-INF. See project page for more info.")
            .build();

    private static final Option buildNoCrunchOption = Option.builder("nc")
            .longOpt("no-crunch")
            .desc("Disable crunching of resource files during the build step.")
            .build();

    private static final Option buildNoApkOption = Option.builder("na")
            .longOpt("no-apk")
            .desc("Disable repacking of the built files into a new apk.")
            .build();

    private static final Option buildAaptOption = Option.builder("a")
            .longOpt("aapt")
            .desc("Load aapt located in <file>.")
            .hasArg()
            .argName("file")
            .build();

    private static final Option buildUseAapt1Option = Option.builder()
            .longOpt("use-aapt1")
            .desc("Use aapt1 binary instead of aapt2 during the build step.")
            .build();

    private static final Option buildApiLevelOption = Option.builder("api")
            .longOpt("api-level")
            .desc("Force the API level to use for smali to <api>.")
            .hasArg()
            .argName("api")
            .build();

    private static final Option buildOutputOption = Option.builder("o")
            .longOpt("output")
            .desc("Output the built apk to <file>. (default: dist/name.apk)")
            .hasArg()
            .argName("file")
            .build();

    private static final Option frameFrameTagOption = Option.builder("t")
            .longOpt("frame-tag")
            .desc("Tag frameworks with <tag>.")
            .hasArg()
            .argName("tag")
            .build();

    private static final Option frameFrameDirOption = Option.builder("p")
            .longOpt("frame-path")
            .desc("Store framework files into <dir>.")
            .hasArg()
            .argName("dir")
            .build();

    private static final Option frameForceOption = Option.builder("f")
            .longOpt("force")
            .desc("Force delete all framework files.")
            .build();

    private static final Options decodeOptions = new Options();
    private static final Options buildOptions = new Options();
    private static final Options installFrameworkOptions = new Options();
    private static final Options emptyFrameworkDirOptions = new Options();
    private static final Options listFrameworksOptions = new Options();
    private static final Options publicizeResourcesOptions = new Options();

    private static final Config config = new Config();
    private static Options loadedOptions = null;
    private static boolean advancedMode = false;

    private static void loadOptions(Options options, boolean advanced) {
        loadedOptions = options;
        advancedMode = advanced;

        if (options == null || options == decodeOptions) {
            decodeOptions.addOption(decodeForceOption);
            decodeOptions.addOption(decodeNoResOption);
            decodeOptions.addOption(decodeNoSrcOption);
            decodeOptions.addOption(decodeOutputOption);
            decodeOptions.addOption(frameDirOption);
            decodeOptions.addOption(frameTagOption);
            decodeOptions.addOption(libOption);
            if (advanced) {
                decodeOptions.addOption(decodeApiLevelOption);
                decodeOptions.addOption(decodeKeepBrokenResOption);
                decodeOptions.addOption(decodeMatchOriginalOption);
                decodeOptions.addOption(decodeNoAssetOption);
                decodeOptions.addOption(decodeNoDebugInfoOption);
                decodeOptions.addOption(decodeOnlyMainClassesOption);
                decodeOptions.addOption(decodeOnlyManifestOption);
                decodeOptions.addOption(decodeResResolveModeOption);
                decodeOptions.addOption(jobsOption);
                decodeOptions.addOption(quietOption);
                decodeOptions.addOption(verboseOption);
            }
        }

        if (options == null || options == buildOptions) {
            buildOptions.addOption(buildForceOption);
            buildOptions.addOption(buildOutputOption);
            buildOptions.addOption(frameDirOption);
            buildOptions.addOption(libOption);
            if (advanced) {
                buildOptions.addOption(buildAaptOption);
                buildOptions.addOption(buildApiLevelOption);
                buildOptions.addOption(buildCopyOriginalOption);
                buildOptions.addOption(buildDebugOption);
                buildOptions.addOption(buildNetSecConfOption);
                buildOptions.addOption(buildNoApkOption);
                buildOptions.addOption(buildNoCrunchOption);
                buildOptions.addOption(buildUseAapt1Option);
                buildOptions.addOption(jobsOption);
                buildOptions.addOption(quietOption);
                buildOptions.addOption(verboseOption);
            }
        }

        if (options == null || options == installFrameworkOptions) {
            installFrameworkOptions.addOption(frameFrameDirOption);
            installFrameworkOptions.addOption(frameFrameTagOption);
            if (advanced) {
                installFrameworkOptions.addOption(quietOption);
                installFrameworkOptions.addOption(verboseOption);
            }
        }

        if (options == null || options == emptyFrameworkDirOptions) {
            emptyFrameworkDirOptions.addOption(frameForceOption);
            emptyFrameworkDirOptions.addOption(frameFrameDirOption);
            if (advanced) {
                emptyFrameworkDirOptions.addOption(quietOption);
                emptyFrameworkDirOptions.addOption(verboseOption);
            }
        }

        if (options == null || options == listFrameworksOptions) {
            listFrameworksOptions.addOption(frameFrameDirOption);
        }
    }

    public static void main(String[] args) throws BrutException {
        // headless
        System.setProperty("java.awt.headless", "true");

        // Ignore stricter validation on zip files from java 11 onwards as this is a protection technique
        // that applications use to thwart disassembly tools. We have protections in place for directory traversal
        // and handling of bogus data in the zip header, so we can ignore this.
        System.setProperty("jdk.nio.zipfs.allowDotZipEntry", "true");
        System.setProperty("jdk.util.zip.disableZip64ExtraFieldValidation", "true");

        if (!OSDetection.is64Bit()) {
            System.err.println("32-bit support is deprecated and will be removed in 3.0.0.");
        }

        if (args.length == 0) {
            loadOptions(null, false);
            printUsage();
            return;
        }

        // Keep support for older (-version/--version) command line arguments
        for (String arg : args) {
            if (arg.equals("-version") || arg.equals("--version")) {
                printVersion();
                return;
            }
        }

        String cmdName = args[0];
        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (cmdName) {
            case "d":
            case "decode":
                cmdDecode(cmdArgs);
                break;
            case "b":
            case "build":
                cmdBuild(cmdArgs);
                break;
            case "if":
            case "install-framework":
                cmdInstallFramework(cmdArgs);
                break;
            case "efd":
            case "empty-framework-dir":
                cmdEmptyFrameworkDir(cmdArgs);
                break;
            case "lf":
            case "list-frameworks":
                cmdListFrameworks(cmdArgs);
                break;
            case "pr":
            case "publicize-resources":
                cmdPublicizeResources(cmdArgs);
                break;
            case "h":
            case "help":
                loadOptions(null, true);
                printUsage();
                break;
            case "v":
            case "version":
                printVersion();
                break;
            default:
                System.err.println("Unrecognized command: " + cmdName);
                loadOptions(null, false);
                printUsage();
                System.exit(1);
        }
    }

    private static CommandLine parseOptions(Options options, String[] args) {
        loadOptions(options, true);

        CommandLine cli;
        try {
            cli = new DefaultParser().parse(options, args, false);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            printUsage();
            System.exit(1);
            return null;
        }

        // check for verbose/quiet
        Verbosity verbosity = Verbosity.NORMAL;
        if (cli.hasOption(verboseOption)) {
            config.setVerbose(true);
            verbosity = Verbosity.VERBOSE;
        }
        if (cli.hasOption(quietOption)) {
            if (cli.hasOption(verboseOption)) {
                printOptionConflict(quietOption, verboseOption);
            } else {
                verbosity = Verbosity.QUIET;
            }
        }
        setupLogging(verbosity);

        return cli;
    }

    private static void cmdDecode(String[] args) throws AndrolibException {
        CommandLine cli = parseOptions(decodeOptions, args);
        List<String> argList = cli.getArgList();
        String apkName;
        switch (argList.size()) {
            case 0:
                System.err.println("Input apk file was not specified.");
                System.exit(1);
                return;
            case 1:
                apkName = argList.get(0);
                break;
            default:
                System.err.println("Invalid arguments.");
                printUsage();
                System.exit(1);
                return;
        }

        if (cli.hasOption(jobsOption)) {
            config.setJobs(Integer.parseInt(cli.getOptionValue(jobsOption)));
        }
        if (cli.hasOption(frameDirOption)) {
            config.setFrameworkDirectory(cli.getOptionValue(frameDirOption));
        }
        if (cli.hasOption(frameTagOption)) {
            config.setFrameworkTag(cli.getOptionValue(frameTagOption));
        }
        if (cli.hasOption(libOption)) {
            config.setLibraryFiles(cli.getOptionValues(libOption));
        }
        if (cli.hasOption(decodeForceOption)) {
            config.setForced(true);
        }
        if (cli.hasOption(decodeNoSrcOption)) {
            config.setDecodeSources(Config.DecodeSources.NONE);
        }
        if (cli.hasOption(decodeOnlyMainClassesOption)) {
            if (cli.hasOption(decodeNoSrcOption)) {
                printOptionConflict(decodeOnlyMainClassesOption, decodeNoSrcOption);
            } else {
                config.setDecodeSources(Config.DecodeSources.ONLY_MAIN_CLASSES);
            }
        }
        if (cli.hasOption(decodeNoDebugInfoOption)) {
            if (cli.hasOption(decodeNoSrcOption)) {
                printOptionConflict(decodeNoDebugInfoOption, decodeNoSrcOption);
            } else {
                config.setBaksmaliDebugMode(false);
            }
        }
        if (cli.hasOption(decodeApiLevelOption)) {
            if (cli.hasOption(decodeNoSrcOption)) {
                printOptionConflict(decodeApiLevelOption, decodeNoSrcOption);
            } else {
                config.setBaksmaliApiLevel(Integer.parseInt(cli.getOptionValue(decodeApiLevelOption)));
            }
        }
        if (cli.hasOption(decodeNoResOption)) {
            config.setDecodeResources(Config.DecodeResources.NONE);
        }
        if (cli.hasOption(decodeOnlyManifestOption)) {
            if (cli.hasOption(decodeNoResOption)) {
                printOptionConflict(decodeOnlyManifestOption, decodeNoResOption);
            } else {
                config.setDecodeResources(Config.DecodeResources.ONLY_MANIFEST);
            }
        }
        if (cli.hasOption(decodeResResolveModeOption)) {
            if (cli.hasOption(decodeNoResOption)) {
                printOptionConflict(decodeResResolveModeOption, decodeNoResOption);
            } else if (cli.hasOption(decodeOnlyManifestOption)) {
                printOptionConflict(decodeResResolveModeOption, decodeOnlyManifestOption);
            } else {
                String mode = cli.getOptionValue(decodeResResolveModeOption);
                switch (mode) {
                    case "remove":
                        config.setDecodeResolve(Config.DecodeResolve.REMOVE);
                        break;
                    case "dummy":
                        config.setDecodeResolve(Config.DecodeResolve.DUMMY);
                        break;
                    case "keep":
                        config.setDecodeResolve(Config.DecodeResolve.KEEP);
                        break;
                    default:
                        System.err.println("Unknown resolve resources mode: " + mode);
                        System.err.println("Expect: 'remove', 'dummy' or 'keep'.");
                        System.exit(1);
                        return;
                }
            }
        }
        if (cli.hasOption(decodeKeepBrokenResOption)) {
            if (cli.hasOption(decodeNoResOption)) {
                printOptionConflict(decodeKeepBrokenResOption, decodeNoResOption);
            } else if (cli.hasOption(decodeOnlyManifestOption)) {
                printOptionConflict(decodeKeepBrokenResOption, decodeOnlyManifestOption);
            } else {
                config.setKeepBrokenResources(true);
            }
        }
        if (cli.hasOption(decodeMatchOriginalOption)) {
            config.setAnalysisMode(true);
        }
        if (cli.hasOption(decodeNoAssetOption)) {
            config.setDecodeAssets(Config.DecodeAssets.NONE);
        }

        File outDir;
        if (cli.hasOption(decodeOutputOption)) {
            outDir = new File(cli.getOptionValue(decodeOutputOption));
        } else {
            outDir = new File(apkName.endsWith(".apk")
                ? apkName.substring(0, apkName.length() - 4).trim()
                : apkName + ".out");
        }

        ExtFile apkFile = new ExtFile(apkName);
        ApkDecoder decoder = new ApkDecoder(apkFile, config);
        try {
            decoder.decode(outDir);
        } catch (OutDirExistsException ex) {
            System.err.println("Destination directory (" + outDir.getAbsolutePath()
                    + ") already exists. Use -f switch if you want to overwrite it.");
            System.exit(1);
        } catch (InFileNotFoundException ex) {
            System.err.println("Input file (" + apkFile.getAbsolutePath() + ") was not found or was not readable.");
            System.exit(1);
        } catch (CantFindFrameworkResException ex) {
            System.err.println("Could not find framework resources for package of id: " + ex.getPackageId()
                    + ". You must install proper framework files, see project website for more info.");
            System.exit(1);
        }
    }

    private static void cmdBuild(String[] args) throws AndrolibException {
        CommandLine cli = parseOptions(buildOptions, args);
        List<String> argList = cli.getArgList();
        String apkDirName;
        switch (argList.size()) {
            case 0:
                apkDirName = "."; // current directory
                break;
            case 1:
                apkDirName = argList.get(0);
                break;
            default:
                System.err.println("Invalid arguments.");
                printUsage();
                System.exit(1);
                return;
        }

        if (cli.hasOption(jobsOption)) {
            config.setJobs(Integer.parseInt(cli.getOptionValue(jobsOption)));
        }
        if (cli.hasOption(frameDirOption)) {
            config.setFrameworkDirectory(cli.getOptionValue(frameDirOption));
        }
        if (cli.hasOption(libOption)) {
            config.setLibraryFiles(cli.getOptionValues(libOption));
        }
        if (cli.hasOption(buildForceOption)) {
            config.setForced(true);
        }
        if (cli.hasOption(buildDebugOption)) {
            config.setDebugMode(true);
        }
        if (cli.hasOption(buildNetSecConfOption)) {
            config.setNetSecConf(true);
        }
        if (cli.hasOption(buildCopyOriginalOption)) {
            config.setCopyOriginalFiles(true);
        }
        if (cli.hasOption(buildNoCrunchOption)) {
            config.setNoCrunch(true);
        }
        if (cli.hasOption(buildNoApkOption)) {
            config.setNoApk(true);
        }
        if (cli.hasOption(buildAaptOption)) {
            try {
                config.setAaptBinary(new File(cli.getOptionValue(buildAaptOption)));
            } catch (BrutException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        }
        if (cli.hasOption(buildUseAapt1Option)) {
            if (cli.hasOption(buildAaptOption)) {
                printOptionConflict(buildUseAapt1Option, buildAaptOption);
            } else {
                config.setAaptVersion(1);
            }
        }
        if (cli.hasOption(buildApiLevelOption)) {
            config.setBaksmaliApiLevel(Integer.parseInt(cli.getOptionValue(buildApiLevelOption)));
        }

        File outFile = null;
        if (cli.hasOption(buildOutputOption)) {
            if (cli.hasOption(buildNoApkOption)) {
                printOptionConflict(buildOutputOption, buildNoApkOption);
            } else {
                outFile = new File(cli.getOptionValue(buildOutputOption));
            }
        }

        ExtFile apkDir = new ExtFile(apkDirName);
        ApkBuilder builder = new ApkBuilder(apkDir, config);
        builder.build(outFile);
    }

    private static void cmdInstallFramework(String[] args) throws AndrolibException {
        CommandLine cli = parseOptions(installFrameworkOptions, args);
        List<String> argList = cli.getArgList();
        String apkName;
        switch (argList.size()) {
            case 0:
                System.err.println("Input apk file was not specified.");
                System.exit(1);
                return;
            case 1:
                apkName = argList.get(0);
                break;
            default:
                System.err.println("Invalid arguments.");
                printUsage();
                System.exit(1);
                return;
        }

        if (cli.hasOption(frameFrameDirOption)) {
            config.setFrameworkDirectory(cli.getOptionValue(frameFrameDirOption));
        }
        if (cli.hasOption(frameFrameTagOption)) {
            config.setFrameworkTag(cli.getOptionValue(frameFrameTagOption));
        }

        new Framework(config).install(new File(apkName));
    }

    private static void cmdEmptyFrameworkDir(String[] args) throws AndrolibException {
        CommandLine cli = parseOptions(emptyFrameworkDirOptions, args);
        List<String> argList = cli.getArgList();
        if (!argList.isEmpty()) {
            System.err.println("Invalid arguments.");
            printUsage();
            System.exit(1);
            return;
        }

        if (cli.hasOption(frameFrameDirOption)) {
            config.setFrameworkDirectory(cli.getOptionValue(frameFrameDirOption));
        }
        if (cli.hasOption(frameForceOption)) {
            config.setForced(true);
        }

        new Framework(config).emptyDirectory();
    }

    private static void cmdListFrameworks(String[] args) throws AndrolibException {
        CommandLine cli = parseOptions(listFrameworksOptions, args);
        List<String> argList = cli.getArgList();
        if (!argList.isEmpty()) {
            System.err.println("Invalid arguments.");
            printUsage();
            System.exit(1);
            return;
        }

        if (cli.hasOption(frameFrameDirOption)) {
            config.setFrameworkDirectory(cli.getOptionValue(frameFrameDirOption));
        }

        for (File file : new Framework(config).listDirectory()) {
            System.out.println(file.getName());
        }
    }

    private static void cmdPublicizeResources(String[] args) throws AndrolibException {
        CommandLine cli = parseOptions(publicizeResourcesOptions, args);
        List<String> argList = cli.getArgList();
        String arscName;
        switch (argList.size()) {
            case 0:
                System.err.println("Input arsc file was not specified.");
                System.exit(1);
                return;
            case 1:
                arscName = argList.get(0);
                break;
            default:
                System.err.println("Invalid arguments.");
                printUsage();
                System.exit(1);
                return;
        }

        new Framework(config).publicizeResources(new File(arscName));
    }

    private static void printOptionConflict(Option option, Option conflict) {
        System.err.println("Ignoring " + formatOption(option) + " (cannot be used with " + formatOption(conflict) + ")");
    }

    private static String formatOption(Option option) {
        StringBuilder sb = new StringBuilder();
        String shortName = option.getOpt();
        if (shortName != null) {
            sb.append('-').append(shortName);
        }
        String longName = option.getLongOpt();
        if (longName != null) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append("--").append(longName);
        }
        return sb.toString();
    }

    private static void printUsage() {
        // print header
        System.out.println("Apktool " + ApktoolProperties.getVersion() + " - a tool for reengineering Android apk files");
        System.out.println("with smali " + ApktoolProperties.getSmaliVersion() + " and baksmali " + ApktoolProperties.getBaksmaliVersion());
        System.out.println("Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>");
        System.out.println("Copyright 2010 Connor Tumbleson <connor.tumbleson@gmail.com>");
        if (advancedMode) {
            System.out.println("Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0)");
        }

        // print usages
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        System.out.println();
        if (loadedOptions == null || loadedOptions == decodeOptions) {
            formatter.printHelp("apktool d|decode [options] <apk-file>", decodeOptions);
            System.out.println();
        }
        if (loadedOptions == null || loadedOptions == buildOptions) {
            formatter.printHelp("apktool b|build [options] <apk-dir>", buildOptions);
            System.out.println();
        }
        if (loadedOptions == null || loadedOptions == installFrameworkOptions) {
            formatter.printHelp("apktool if|install-framework [options] <apk-file>", installFrameworkOptions);
            System.out.println();
        }
        if ((advancedMode && loadedOptions == null) || loadedOptions == emptyFrameworkDirOptions) {
            formatter.printHelp("apktool efd|empty-framework-dir [options]", emptyFrameworkDirOptions);
            System.out.println();
        }
        if ((advancedMode && loadedOptions == null) || loadedOptions == listFrameworksOptions) {
            formatter.printHelp("apktool lf|list-frameworks [options]", listFrameworksOptions);
            System.out.println();
        }
        if ((advancedMode && loadedOptions == null) || loadedOptions == publicizeResourcesOptions) {
            formatter.printHelp("apktool pr|publicize-resources <arsc-file>", publicizeResourcesOptions);
        }
        if (loadedOptions == null) {
            formatter.printHelp("apktool h|help", new Options());
            formatter.printHelp("apktool v|version", new Options());
        }

        // print footer
        System.out.println("For additional info, see: https://apktool.org");
        System.out.println("For smali/baksmali info, see: https://github.com/google/smali");
    }

    private static void printVersion() {
        System.out.println(ApktoolProperties.getVersion());
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
                    int level = record.getLevel().intValue();
                    if (level >= Level.WARNING.intValue()) {
                        System.err.write(message.getBytes());
                    } else if (level >= Level.INFO.intValue() || verbosity == Verbosity.VERBOSE) {
                        System.out.write(message.getBytes());
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
}

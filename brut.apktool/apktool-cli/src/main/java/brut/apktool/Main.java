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

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.FrameworkNotFoundException;
import brut.androlib.exceptions.InFileNotFoundException;
import brut.androlib.exceptions.OutDirExistsException;
import brut.androlib.res.AaptManager;
import brut.androlib.res.Framework;
import brut.directory.ExtFile;
import brut.util.OSDetection;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.*;

/**
 * Main entry point of apktool.
 */
public class Main {
    private enum Verbosity { NORMAL, VERBOSE, QUIET }

    private static final Option verboseOption = Option.builder("v")
        .longOpt("verbose")
        .desc("Increase output verbosity.")
        .get();

    private static final Option quietOption = Option.builder("q")
        .longOpt("quiet")
        .desc("Suppress normal output.")
        .get();

    private static final Option jobsOption = Option.builder("j")
        .longOpt("jobs")
        .desc("Set the number of jobs to execute in parallel to <num>.")
        .hasArg()
        .argName("num")
        .type(Integer.class)
        .get();

    private static final Option frameDirOption = Option.builder("p")
        .longOpt("frame-path")
        .desc("Use framework files located in <dir>.")
        .hasArg()
        .argName("dir")
        .get();

    private static final Option frameTagOption = Option.builder("t")
        .longOpt("frame-tag")
        .desc("Use framework files tagged with <tag>.")
        .hasArg()
        .argName("tag")
        .get();

    private static final Option libOption = Option.builder("l")
        .longOpt("lib")
        .desc("Use shared library <package> located in <file>.\n"
            + "Can be specified multiple times.")
        .hasArg()
        .argName("package:file")
        .get();

    private static final Option decodeForceOption = Option.builder("f")
        .longOpt("force")
        .desc("Force delete destination directory.")
        .get();

    private static final Option decodeNoSrcOption = Option.builder("s")
        .longOpt("no-src")
        .desc("Do not decode sources.")
        .get();

    private static final Option decodeOnlyMainClassesOption = Option.builder()
        .longOpt("only-main-classes")
        .desc("Only disassemble the main dex classes (classes[0-9]*.dex) in the root.")
        .get();

    private static final Option decodeNoDebugInfoOption = Option.builder()
        .longOpt("no-debug-info")
        .desc("Do not include debug info in sources (.local, .param, .line, etc.)")
        .get();

    private static final Option decodeNoResOption = Option.builder("r")
        .longOpt("no-res")
        .desc("Do not decode resources.")
        .get();

    private static final Option decodeOnlyManifestOption = Option.builder()
        .longOpt("only-manifest")
        .desc("Only decode AndroidManifest.xml without resources.")
        .get();

    private static final Option decodeResResolveModeOption = Option.builder()
        .longOpt("res-resolve-mode")
        .desc("Set the resolve mode for resources to <mode>.\n"
            + "Possible values: 'default', 'greedy' or 'lazy'.")
        .hasArg()
        .argName("mode")
        .get();

    private static final Option decodeKeepBrokenResOption = Option.builder()
        .longOpt("keep-broken-res")
        .desc("Use if there was an error and some resources were dropped, e.g.\n"
            + "\"Invalid resource config detected. Dropping resources\", but you\n"
            + "want to decode them anyway, even with errors. You will have to\n"
            + "fix them manually before building.")
        .get();

    private static final Option decodeMatchOriginalOption = Option.builder()
        .longOpt("match-original")
        .desc("Keep files closest to original as possible (prevents rebuild).")
        .get();

    private static final Option decodeNoAssetsOption = Option.builder()
        .longOpt("no-assets")
        .desc("Do not decode assets.")
        .get();

    private static final Option decodeOutputOption = Option.builder("o")
        .longOpt("output")
        .desc("Output decoded files to <dir>. (default: apk.out)")
        .hasArg()
        .argName("dir")
        .get();

    private static final Option buildForceOption = Option.builder("f")
        .longOpt("force")
        .desc("Skip changes detection and build all files.")
        .get();

    private static final Option buildNoApkOption = Option.builder()
        .longOpt("no-apk")
        .desc("Disable repacking of the built files into a new apk.")
        .get();

    private static final Option buildNoCrunchOption = Option.builder()
        .longOpt("no-crunch")
        .desc("Disable crunching of resource files during the build step.")
        .get();

    private static final Option buildCopyOriginalOption = Option.builder()
        .longOpt("copy-original")
        .desc("Copy original AndroidManifest.xml and META-INF. See project page for more info.")
        .get();

    private static final Option buildDebuggableOption = Option.builder()
        .longOpt("debuggable")
        .desc("Set android:debuggable to \"true\" in AndroidManifest.xml for the built apk.")
        .get();

    private static final Option buildNetSecConfOption = Option.builder()
        .longOpt("net-sec-conf")
        .desc("Add a generic network security configuration file to the built apk.")
        .get();

    private static final Option buildAaptOption = Option.builder()
        .longOpt("aapt")
        .desc("Use aapt2 binary located in <file>.")
        .hasArg()
        .argName("file")
        .get();

    private static final Option buildOutputOption = Option.builder("o")
        .longOpt("output")
        .desc("Output the built apk to <file>. (default: dist/name.apk)")
        .hasArg()
        .argName("file")
        .get();

    private static final Option frameFrameDirOption = Option.builder("p")
        .longOpt("frame-path")
        .desc("Set the path for framework files to <dir>.")
        .hasArg()
        .argName("dir")
        .get();

    private static final Option frameFrameTagOption = Option.builder("t")
        .longOpt("frame-tag")
        .desc("Suffix framework files with <tag>.")
        .hasArg()
        .argName("tag")
        .get();

    private static final Option frameForceAllOption = Option.builder("a")
        .longOpt("all")
        .desc("Include all framework files regardless of tag.")
        .get();

    private static final Options generalOptions = new Options();
    private static final Options decodeOptions = new Options();
    private static final Options buildOptions = new Options();
    private static final Options installFrameworkOptions = new Options();
    private static final Options cleanFrameworksOptions = new Options();
    private static final Options listFrameworksOptions = new Options();
    private static final Options publicizeResourcesOptions = new Options();

    private static final Props props = new Props();
    private static final Config config = new Config(props.getVersion());
    private static Options loadedOptions = null;
    private static boolean advancedMode = false;

    private static void loadOptions(Options options, boolean advanced) {
        loadedOptions = options;
        advancedMode = advanced;

        generalOptions.addOption(quietOption);
        generalOptions.addOption(verboseOption);

        if (options == null || options == decodeOptions) {
            decodeOptions.addOption(decodeForceOption);
            decodeOptions.addOption(decodeNoResOption);
            decodeOptions.addOption(decodeNoSrcOption);
            decodeOptions.addOption(decodeOutputOption);
            decodeOptions.addOption(frameDirOption);
            decodeOptions.addOption(frameTagOption);
            decodeOptions.addOption(jobsOption);
            decodeOptions.addOption(libOption);
            if (advanced) {
                decodeOptions.addOption(decodeKeepBrokenResOption);
                decodeOptions.addOption(decodeMatchOriginalOption);
                decodeOptions.addOption(decodeNoAssetsOption);
                decodeOptions.addOption(decodeNoDebugInfoOption);
                decodeOptions.addOption(decodeOnlyMainClassesOption);
                decodeOptions.addOption(decodeOnlyManifestOption);
                decodeOptions.addOption(decodeResResolveModeOption);
            }
        }

        if (options == null || options == buildOptions) {
            buildOptions.addOption(buildForceOption);
            buildOptions.addOption(buildOutputOption);
            buildOptions.addOption(frameDirOption);
            buildOptions.addOption(jobsOption);
            buildOptions.addOption(libOption);
            if (advanced) {
                buildOptions.addOption(buildAaptOption);
                buildOptions.addOption(buildCopyOriginalOption);
                buildOptions.addOption(buildDebuggableOption);
                buildOptions.addOption(buildNetSecConfOption);
                buildOptions.addOption(buildNoApkOption);
                buildOptions.addOption(buildNoCrunchOption);
            }
        }

        if (options == null || options == installFrameworkOptions) {
            installFrameworkOptions.addOption(frameFrameDirOption);
            installFrameworkOptions.addOption(frameFrameTagOption);
        }

        if (options == null || options == cleanFrameworksOptions) {
            cleanFrameworksOptions.addOption(frameForceAllOption);
            cleanFrameworksOptions.addOption(frameFrameDirOption);
            cleanFrameworksOptions.addOption(frameFrameTagOption);
        }

        if (options == null || options == listFrameworksOptions) {
            listFrameworksOptions.addOption(frameForceAllOption);
            listFrameworksOptions.addOption(frameFrameDirOption);
            listFrameworksOptions.addOption(frameFrameTagOption);
        }
    }

    public static void main(String[] args) throws AndrolibException {
        // Headless
        System.setProperty("java.awt.headless", "true");

        // Ignore stricter validation on zip files from java 11 onwards as this is a protection technique that
        // applications use to thwart disassembly tools. We have protections in place for directory traversal
        // and handling of bogus data in the zip header, so we can ignore this.
        System.setProperty("jdk.nio.zipfs.allowDotZipEntry", "true");
        System.setProperty("jdk.util.zip.disableZip64ExtraFieldValidation", "true");

        if (!OSDetection.is64Bit()) {
            System.err.println("Warning: Apktool no longer supports 32-bit platforms.");
        }

        if (args.length == 0) {
            loadOptions(null, false);
            printUsage();
            return;
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
            case "cf":
            case "clean-frameworks":
                cmdCleanFrameworks(cmdArgs);
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
            case "-help":
            case "--help":
                loadOptions(null, true);
                printUsage();
                break;
            case "v":
            case "version":
            case "-version":
            case "--version":
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

        Options combinedOptions = new Options();
        combinedOptions.addOptions(generalOptions);
        combinedOptions.addOptions(options);

        CommandLine cli;
        try {
            cli = new DefaultParser(false).parse(combinedOptions, args, false);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            printUsage();
            System.exit(1);
            return null;
        }

        // Check for verbose/quiet.
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
                    case "default":
                        config.setDecodeResolve(Config.DecodeResolve.DEFAULT);
                        break;
                    case "greedy":
                        config.setDecodeResolve(Config.DecodeResolve.GREEDY);
                        break;
                    case "lazy":
                        config.setDecodeResolve(Config.DecodeResolve.LAZY);
                        break;
                    default:
                        System.err.println("Unknown resolve resources mode: " + mode);
                        System.err.println("Expect: 'default', 'greedy' or 'lazy'.");
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
        if (cli.hasOption(decodeNoAssetsOption)) {
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

        try (ExtFile apkFile = new ExtFile(apkName)) {
            ApkDecoder decoder = new ApkDecoder(apkFile, config);
            decoder.decode(outDir);
        } catch (IOException ignored) {
            // Input file could not be closed, just ignore.
        } catch (InFileNotFoundException | OutDirExistsException | FrameworkNotFoundException ex) {
            System.err.println(ex.getMessage());
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
        if (cli.hasOption(buildNoApkOption)) {
            config.setNoApk(true);
        }
        if (cli.hasOption(buildNoCrunchOption)) {
            config.setNoCrunch(true);
        }
        if (cli.hasOption(buildCopyOriginalOption)) {
            config.setCopyOriginal(true);
        }
        if (cli.hasOption(buildDebuggableOption)) {
            config.setDebuggable(true);
        }
        if (cli.hasOption(buildNetSecConfOption)) {
            config.setNetSecConf(true);
        }
        if (cli.hasOption(buildAaptOption)) {
            try {
                String aaptBinary = cli.getOptionValue(buildAaptOption);
                if (AaptManager.getBinaryVersion(new File(aaptBinary)) == 1) {
                    throw new AndrolibException("Legacy aapt is no longer supported.");
                }

                config.setAaptBinary(aaptBinary);
            } catch (AndrolibException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
                return;
            }
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

    private static void cmdCleanFrameworks(String[] args) throws AndrolibException {
        CommandLine cli = parseOptions(cleanFrameworksOptions, args);
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
        if (cli.hasOption(frameFrameTagOption)) {
            config.setFrameworkTag(cli.getOptionValue(frameFrameTagOption));
        }
        if (cli.hasOption(frameForceAllOption)) {
            if (cli.hasOption(frameFrameTagOption)) {
                printOptionConflict(frameForceAllOption, frameFrameTagOption);
            } else {
                config.setForced(true);
            }
        }

        new Framework(config).cleanDirectory();
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
        if (cli.hasOption(frameFrameTagOption)) {
            config.setFrameworkTag(cli.getOptionValue(frameFrameTagOption));
        }
        if (cli.hasOption(frameForceAllOption)) {
            if (cli.hasOption(frameFrameTagOption)) {
                printOptionConflict(frameForceAllOption, frameFrameTagOption);
            } else {
                config.setForced(true);
            }
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

    @SuppressWarnings("deprecation")
    private static void printUsage() {
        PrintWriter writer = new PrintWriter(System.out);
        HelpFormatter formatter = new HelpFormatter();

        // Print header.
        writer.println("Apktool " + props.getVersion() + " - a tool for reengineering Android apk files");
        writer.println("with smali " + props.getSmaliVersion() + " and baksmali " + props.getBaksmaliVersion());
        writer.println("Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>");
        writer.println("Copyright 2010 Connor Tumbleson <connor.tumbleson@gmail.com>");
        if (advancedMode) {
            writer.println("Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0)");
        }
        writer.println();

        // Print usages.
        writer.println("General options:");
        printOptions(writer, formatter, generalOptions);
        writer.println();
        if (loadedOptions == null || loadedOptions == decodeOptions) {
            writer.println("apktool d|decode [options] <apk-file>");
            printOptions(writer, formatter, decodeOptions);
            writer.println();
        }
        if (loadedOptions == null || loadedOptions == buildOptions) {
            writer.println("apktool b|build [options] <apk-dir>");
            printOptions(writer, formatter, buildOptions);
            writer.println();
        }
        if (loadedOptions == null || loadedOptions == installFrameworkOptions) {
            writer.println("apktool if|install-framework [options] <apk-file>");
            printOptions(writer, formatter, installFrameworkOptions);
            writer.println();
        }
        if ((advancedMode && loadedOptions == null) || loadedOptions == cleanFrameworksOptions) {
            writer.println("apktool cf|clean-frameworks [options]");
            printOptions(writer, formatter, cleanFrameworksOptions);
            writer.println();
        }
        if ((advancedMode && loadedOptions == null) || loadedOptions == listFrameworksOptions) {
            writer.println("apktool lf|list-frameworks [options]");
            printOptions(writer, formatter, listFrameworksOptions);
            writer.println();
        }
        if ((advancedMode && loadedOptions == null) || loadedOptions == publicizeResourcesOptions) {
            writer.println("apktool pr|publicize-resources <arsc-file>");
            printOptions(writer, formatter, publicizeResourcesOptions);
            writer.println();
        }
        if (loadedOptions == null) {
            writer.println("apktool h|help");
            writer.println();
            writer.println("apktool v|version");
            writer.println();
        }

        // Print footer.
        writer.println("For additional info, see: https://apktool.org");
        writer.println("For smali/baksmali info, see: https://github.com/google/smali");

        writer.flush();
    }

    @SuppressWarnings("deprecation")
    private static void printOptions(PrintWriter writer, HelpFormatter formatter, Options options) {
        final int width = 120;
        final int leftPadding = 1;
        final int descPadding = 3;

        if (!options.getOptions().isEmpty()) {
            formatter.printOptions(writer, width, options, leftPadding, descPadding);
        }
    }

    private static void printVersion() {
        System.out.println(props.getVersion());
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
                                    + record.getMessage() + System.lineSeparator();
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
            public void close() throws SecurityException {
            }

            @Override
            public void flush() {
            }
        };

        logger.addHandler(handler);

        if (verbosity == Verbosity.VERBOSE) {
            handler.setLevel(Level.ALL);
            logger.setLevel(Level.ALL);
        }
    }

    private static class Props extends Properties {

        public Props() {
            load(this, "/apktool.properties");

            Properties smaliProps = new Properties();
            load(smaliProps, "/smali.properties");
            String smaliVersion = smaliProps.getProperty("application.version", "");
            if (!smaliVersion.isEmpty()) {
                put("smali.version", smaliVersion);
            }

            Properties baksmaliProps = new Properties();
            load(baksmaliProps, "/baksmali.properties");
            String baksmaliVersion = baksmaliProps.getProperty("application.version", "");
            if (!baksmaliVersion.isEmpty()) {
                put("baksmali.version", baksmaliVersion);
            }
        }

        public String getVersion() {
            return getProperty("application.version", "(unknown)");
        }

        public String getSmaliVersion() {
            return getProperty("smali.version", "(unknown)");
        }

        public String getBaksmaliVersion() {
            return getProperty("baksmali.version", "(unknown)");
        }

        private void load(Properties props, String name) {
            InputStream in = null;
            try {
                in = Main.class.getResourceAsStream(name);
                if (in == null) {
                    throw new FileNotFoundException(name);
                }
                props.load(in);
            } catch (IOException ignored) {
                System.out.println("Could not load " + name);
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }
}

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

package brut.apktool;

import brut.androlib.Androlib;
import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.androlib.ApktoolProperties;
import brut.androlib.err.CantFindFrameworkResException;
import brut.androlib.err.InFileNotFoundException;
import brut.androlib.err.OutDirExistsException;
import brut.common.BrutException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.*;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class Main {
    public static void main(String[] args) throws IOException,
            InterruptedException, BrutException {

        // set verbosity default
        Verbosity verbosity = Verbosity.NORMAL;

        // cli parser
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine = null;

        // load options
        _Options();

        try {
            commandLine = parser.parse(allOptions, args, false);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            usage(commandLine);
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
            setAdvanceMode(true);
        }

        // @todo use new ability of apache-commons-cli to check hasOption for non-prefixed items
        boolean cmdFound = false;
        for (String opt : commandLine.getArgs()) {
            if (opt.equalsIgnoreCase("d") || opt.equalsIgnoreCase("decode")) {
                cmdDecode(commandLine);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("b") || opt.equalsIgnoreCase("build")) {
                cmdBuild(commandLine);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("if") || opt.equalsIgnoreCase("install-framework")) {
                cmdInstallFramework(commandLine);
                cmdFound = true;
            } else if (opt.equalsIgnoreCase("publicize-resources")) {
                cmdPublicizeResources(commandLine);
                cmdFound = true;
            }
        }

        // if no commands ran, run the version / usage check.
        if (cmdFound == false) {
            if (commandLine.hasOption("version") || commandLine.hasOption("version")) {
                _version();
            } else {
                usage(commandLine);
            }
        }
    }

    private static void cmdDecode(CommandLine cli) throws AndrolibException {
        ApkDecoder decoder = new ApkDecoder();

        int paraCount = cli.getArgList().size();
        String apkName = (String) cli.getArgList().get(paraCount - 1);
        File outDir = null;

        // check for options
        if (cli.hasOption("s") || cli.hasOption("no-src")) {
            decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE);
        }
        if (cli.hasOption("d") || cli.hasOption("debug")) {
            decoder.setDebugMode(true);
        }
        if (cli.hasOption("debug-line-prefix")) {
            decoder.setDebugLinePrefix(cli.getOptionValue("debug-line-prefix"));
        }
        if (cli.hasOption("b") || cli.hasOption("no-debug-info")) {
            decoder.setBaksmaliDebugMode(false);
        }
        if (cli.hasOption("t") || cli.hasOption("frame-tag")) {
            decoder.setFrameworkTag(cli.getOptionValue("t"));
        }
        if (cli.hasOption("f") || cli.hasOption("force")) {
            decoder.setForceDelete(true);
        }
        if (cli.hasOption("r") || cli.hasOption("no-res")) {
            decoder.setDecodeResources(ApkDecoder.DECODE_RESOURCES_NONE);
        }
        if (cli.hasOption("k") || cli.hasOption("keep-broken-res")) {
            decoder.setKeepBrokenResources(true);
        }
        if (cli.hasOption("p") || cli.hasOption("frame-path")) {
            decoder.setFrameworkDir(cli.getOptionValue("p"));
        }
        if (cli.hasOption("m") || cli.hasOption("match-original")) {
            decoder.setAnalysisMode(true, false);
        }
        if (cli.hasOption("o") || cli.hasOption("output")) {
            decoder.setOutDir(new File(cli.getOptionValue("o")));
        } else {

            // make out folder manually using name of apk
            String outName = apkName;
            outName = outName.endsWith(".apk") ? outName.substring(0,
                    outName.length() - 4) : outName + ".out";

            // make file from path
            outName = new File(outName).getName();
            outDir = new File(outName);
            decoder.setOutDir(outDir);
        }

        decoder.setApkFile(new File(apkName));

        try {
            decoder.decode();
        } catch (OutDirExistsException ex) {
            System.out
                    .println("目标文件夹 ("
                            + outDir.getAbsolutePath()
                            + ") "
                            + "已经存在. 可以使用使用 -f 参数覆盖文件夹.");
            System.exit(1);
        } catch (InFileNotFoundException ex) {
            System.out.println("文件 (" + apkName + ") " + "未找到或无法读取.");
            System.exit(1);
        } catch (CantFindFrameworkResException ex) {
            System.out
                    .println("无法找到的框架资源包的id: "
                            + String.valueOf(ex.getPkgId())
                            + ". 你必须安装适当的框架文件, "
                            + "更多信息见项目网站.");
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("无法修改文件. 请确认是否有权限.");
            System.exit(1);
        }

    }

    private static void cmdBuild(CommandLine cli) throws BrutException {
        String[] args = cli.getArgs();
        String appDirName = args.length < 2 ? "." : args[1];
        String mAaptPath = "";
        File outFile = null;
        Androlib instance = new Androlib();

        // hold all the fields
        HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
        flags.put("forceBuildAll", false);
        flags.put("debug", false);
        flags.put("verbose", false);
        flags.put("framework", false);
        flags.put("update", false);
        flags.put("copyOriginal", false);

        // check for build options
        if (cli.hasOption("f") || cli.hasOption("force-all")) {
            flags.put("forceBuildAll", true);
        }
        if (cli.hasOption("d") || cli.hasOption("debug")) {
            flags.put("debug", true);
        }
        if (cli.hasOption("v") || cli.hasOption("verbose")) {
            flags.put("verbose", true);
        }
        if (cli.hasOption("a") || cli.hasOption("aapt")) {
            mAaptPath = cli.getOptionValue("a");
        }
        if (cli.hasOption("c") || cli.hasOption("copy-original")) {
            flags.put("copyOriginal", true);
        }
        if (cli.hasOption("p") || cli.hasOption("frame-path")) {
            instance.setFrameworkFolder(cli.getOptionValue("p"));
        }
        if (cli.hasOption("o") || cli.hasOption("output")) {
            outFile = new File(cli.getOptionValue("o"));
        } else {
            outFile = null;
        }

        // try and build apk
        instance.build(new File(appDirName), outFile, flags,mAaptPath);
    }

    private static void cmdInstallFramework(CommandLine cli)
            throws AndrolibException {
        int paraCount = cli.getArgList().size();
        String apkName = (String) cli.getArgList().get(paraCount - 1);
        String tag = null;
        String frame_path = null;

        if (cli.hasOption("p") || cli.hasOption("frame-path")) {
            frame_path = cli.getOptionValue("p");
        }
        if (cli.hasOption("t") || cli.hasOption("tag")) {
            tag = cli.getOptionValue("t");
        }
        new Androlib().installFramework(new File(apkName), tag, frame_path);
    }

    private static void cmdPublicizeResources(CommandLine cli)
            throws AndrolibException {
        int paraCount = cli.getArgList().size();
        String apkName = (String) cli.getArgList().get(paraCount - 1);

        new Androlib().publicizeResources(new File(apkName));
    }

    private static void _version() {
        System.out.println(Androlib.getVersion());
    }

    @SuppressWarnings("static-access")
    private static void _Options() {

        // create options
        Option versionOption = OptionBuilder.withLongOpt("version")
                .withDescription("显示版本\n")
                .create("version");

        Option advanceOption = OptionBuilder.withLongOpt("advanced")
                .withDescription("显示高级信息.")
                .create("advance");

        Option noSrcOption = OptionBuilder.withLongOpt("no-src")
                .withDescription("不反编译源码.")
                .create("s");

        Option noResOption = OptionBuilder.withLongOpt("no-res")
                .withDescription("不反编译 resources.")
                .create("r");

        Option debugDecOption = OptionBuilder.withLongOpt("debug")
                .withDescription("调试模式反编译.\n"
                        + "       更多信息，请参阅项目主页.")
                .create("d");

        Option analysisOption = OptionBuilder.withLongOpt("-match-original")
                .withDescription("Keeps files to closest to original as possible. Prevents rebuild.")
                .create("m");

        Option debugLinePrefix = OptionBuilder.withLongOpt("debug-line-prefix")
                .withDescription("使用反编译调试模式时Smali line 前缀.\n"
                        + "       默认 \"a=0;// \".")
                .hasArg(true)
                .withArgName("prefix")
                .create();

        Option debugBuiOption = OptionBuilder.withLongOpt("debug")
                .withDescription("调试模式下编译.\n"
                        + "       更多信息，请参阅项目主页.")
                .create("d");

        Option noDbgOption = OptionBuilder.withLongOpt("no-debug-info")
                .withDescription("不输出调试信息 (.local, .param, .line, etc.)")
                .create("b");

        Option forceDecOption = OptionBuilder.withLongOpt("force")
                .withDescription("强制删除目标目录.")
                .create("f");

        Option frameTagOption = OptionBuilder.withLongOpt("frame-tag")
                .withDescription("使用框架文件 <tag>.\n")
                .hasArg(true)
                .withArgName("tag")
                .create("t");

        Option frameDirOption = OptionBuilder.withLongOpt("frame-path")
                .withDescription("Uses framework files located in <dir>.\n")
                .hasArg(true)
                .withArgName("dir")
                .create("p");

        Option frameIfDirOption = OptionBuilder.withLongOpt("frame-path")
                .withDescription("Stores framework files into <dir>.")
                .hasArg(true)
                .withArgName("dir")
                .create("p");

        Option keepResOption = OptionBuilder.withLongOpt("keep-broken-res")
                .withDescription("如果有一个错误、一些资源丢失、无效的配置\n"
                        + "            \"或者即使有错误，也要对其进行反编译. \"\n"
                        + "            你可以删除资源. \n"
                        + "            但是编译之前你必须手动修复它们.")
                .create("k");

        Option forceBuiOption = OptionBuilder.withLongOpt("force-all")
                .withDescription("跳过变化检测，并编译所有文件.")
                .create("f");

        Option aaptOption = OptionBuilder.withLongOpt("aapt")
                .hasArg(true)
                .withArgName("loc")
                .withDescription("从指定位置加载 aapt.")
                .create("a");

        Option originalOption = OptionBuilder.withLongOpt("copy-original")
                .withDescription("复制官方 AndroidManifest.xml 和 META-INF.\n"
                        + "       更多信息，请参阅项目主页.")
                .create("c");

        Option tagOption = OptionBuilder.withLongOpt("tag")
                .withDescription("使用 <tag> 标签框架\n.")
                .hasArg(true)
                .withArgName("tag")
                .create("t");

        Option outputBuiOption = OptionBuilder.withLongOpt("output")
                .withDescription("输出的apk文件名. 默认 路径/文件名.apk")
                .hasArg(true)
                .withArgName("dir")
                .create("o");

        Option outputDecOption = OptionBuilder.withLongOpt("output")
                .withDescription("输出的文件夹名称. 默认 apk文件名.out")
                .hasArg(true)
                .withArgName("dir")
                .create("o");

        Option quietOption = OptionBuilder.withLongOpt("quiet")
                .create("q");

        Option verboseOption = OptionBuilder.withLongOpt("verbose")
                .create("v");

        // check for advance mode
        if (isAdvanceMode()) {
            DecodeOptions.addOption(debugLinePrefix);
            DecodeOptions.addOption(debugDecOption);
            DecodeOptions.addOption(noDbgOption);
            DecodeOptions.addOption(keepResOption);
            DecodeOptions.addOption(analysisOption);

            BuildOptions.addOption(debugBuiOption);
            BuildOptions.addOption(aaptOption);
            BuildOptions.addOption(originalOption);
        }

        // add global options
        normalOptions.addOption(versionOption);
        normalOptions.addOption(advanceOption);

        // add basic decode options
        DecodeOptions.addOption(frameTagOption);
        DecodeOptions.addOption(outputDecOption);
        DecodeOptions.addOption(frameDirOption);
        DecodeOptions.addOption(forceDecOption);
        DecodeOptions.addOption(noSrcOption);
        DecodeOptions.addOption(noResOption);

        // add basic build options
        BuildOptions.addOption(outputBuiOption);
        BuildOptions.addOption(frameDirOption);
        BuildOptions.addOption(forceBuiOption);

        // add basic framework options
        frameOptions.addOption(tagOption);
        frameOptions.addOption(frameIfDirOption);

        // add all, loop existing cats then manually add advance
        for (Object op : normalOptions.getOptions()) {
            allOptions.addOption((Option)op);
        }
        for (Object op : DecodeOptions.getOptions()) {
            allOptions.addOption((Option)op);
        }
        for (Object op : BuildOptions.getOptions()) {
            allOptions.addOption((Option)op);
        }
        for (Object op : frameOptions.getOptions()) {
            allOptions.addOption((Option)op);
        }
        allOptions.addOption(analysisOption);
        allOptions.addOption(debugLinePrefix);
        allOptions.addOption(debugDecOption);
        allOptions.addOption(noDbgOption);
        allOptions.addOption(keepResOption);
        allOptions.addOption(debugBuiOption);
        allOptions.addOption(aaptOption);
        allOptions.addOption(originalOption);
        allOptions.addOption(verboseOption);
        allOptions.addOption(quietOption);
    }

    private static String verbosityHelp() {
        if (isAdvanceMode()) {
            return "[-q|--隐藏信息 或者使用 -v|--详细信息] ";
        } else {
            return "";
        }
    }

    private static void usage(CommandLine commandLine) {

        // load basicOptions
        _Options();
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);

        // print out license info prior to formatter.
        System.out.println(
                "Apktool 版本" + Androlib.getVersion() + " - 一个编译Android apk文件的工具\n" +
                        "smali 版本" + ApktoolProperties.get("smaliVersion") +
                        " 和 baksmali 版本" + ApktoolProperties.get("baksmaliVersion") + "\n" +
                        "版权2010 归Ryszard Wiśniewski <brut.alll@gmail.com>所有\n" +
                        "由 Connor Tumbleson <connor.tumbleson@gmail.com> 更新\n" +
                        "由 loogeo <loogeo@gmail.com> 修改汉化" );
        if (isAdvanceMode()) {
            System.out.println("Apache 许可证 2.0 (http://www.apache.org/licenses/LICENSE-2.0)\n");
        }else {
            System.out.println("");
        }

        // 4 usage outputs (general, frameworks, decode, build)
        formatter.printHelp("apktool " + verbosityHelp(), normalOptions);
        formatter.printHelp("apktool " + verbosityHelp() + "if|安装框架文件 [参数] <framework.apk>", frameOptions);
        formatter.printHelp("apktool " + verbosityHelp() + "d[反编译] [参数] <apk文件>", DecodeOptions);
        formatter.printHelp("apktool " + verbosityHelp() + "b[编译] [参数] <apk文件>", BuildOptions);
        if (isAdvanceMode()) {
            formatter.printHelp("apktool " + verbosityHelp() + "publicize-resources <file_path>",
                    "确定所有框架资源公开.", emptyOptions, null);
        } else {
            System.out.println("");
        }

        // print out more information
        System.out.println(
                "如需更多信息，请参阅: http://code.google.com/p/android-apktool/ \n"
                        + "需更多 smali/baksmali 信息，请参阅: http://code.google.com/p/smali/");
    }

    private static void setupLogging(Verbosity verbosity) {
        Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        if (verbosity == Verbosity.QUIET) {
            return;
        }

        Handler handler = new ConsoleHandler();
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

    public static boolean isAdvanceMode() {
        return advanceMode;
    }

    public static void setAdvanceMode(boolean advanceMode) {
        Main.advanceMode = advanceMode;
    }

    private static enum Verbosity {
        NORMAL, VERBOSE, QUIET;
    }

    private static boolean advanceMode = false;

    private final static Options normalOptions;
    private final static Options DecodeOptions;
    private final static Options BuildOptions;
    private final static Options frameOptions;
    private final static Options allOptions;
    private final static Options emptyOptions;

    static {
        //normal and advance usage output
        normalOptions = new Options();
        BuildOptions = new Options();
        DecodeOptions = new Options();
        frameOptions = new Options();
        allOptions = new Options();
        emptyOptions = new Options();
    }
}

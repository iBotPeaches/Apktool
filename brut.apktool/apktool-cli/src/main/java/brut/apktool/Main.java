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
import brut.androlib.res.util.ExtFile;
import brut.common.BrutException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.*;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jf.util.ConsoleUtil;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
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
          commandLine = parser.parse(allOptions, args, true);
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
		      cmdBuild(args);
		      cmdFound = true;
		    } else if (opt.equalsIgnoreCase("if") || opt.equalsIgnoreCase("install-framework")) {
		      cmdInstallFramework(args);
		      cmdFound = true;
		    } else if (opt.equalsIgnoreCase("publicize-resources")) {
		      cmdPublicizeResources(args);
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

	private static void cmdDecode(CommandLine cli) throws InvalidArgsError,
			AndrolibException {
		ApkDecoder decoder = new ApkDecoder();
		
		int para = cli.getArgList().size();
		
		// check for options
		if (cli.hasOption("s") || cli.hasOption("no-src")) {
		  decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE);
		}
		if (cli.hasOption("d") || cli.hasOption("debug")) {
		  decoder.setDebugMode(true);
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
		if (cli.hasOption("o") || cli.hasOption("output")) {
		  decoder.setOutDir(new File(cli.getOptionValue("o")));
		} else {
		}
		
		

//		String outName = null;
//		if (args.length == i + 2) {
//			outName = args[i + 1];
//		} else if (args.length == i + 1) {
//			outName = args[i];
//			outName = outName.endsWith(".apk") ? outName.substring(0,
//					outName.length() - 4) : outName + ".out";
//			outName = new File(outName).getName();
//		} else {
//			throw new InvalidArgsError();
//		}
//		File outDir = new File(outName);
//		decoder.setOutDir(outDir);
//		decoder.setApkFile(new File(args[i]));
//
//		try {
//			decoder.decode();
//		} catch (OutDirExistsException ex) {
//			System.out
//					.println("Destination directory ("
//							+ outDir.getAbsolutePath()
//							+ ") "
//							+ "already exists. Use -f switch if you want to overwrite it.");
//			System.exit(1);
//		} catch (InFileNotFoundException ex) {
//			System.out.println("Input file (" + args[i] + ") "
//					+ "was not found or was not readable.");
//			System.exit(1);
//		} catch (CantFindFrameworkResException ex) {
//			System.out
//					.println("Can't find framework resources for package of id: "
//							+ String.valueOf(ex.getPkgId())
//							+ ". You must install proper "
//							+ "framework files, see project website for more info.");
//			System.exit(1);
//		} catch (IOException ex) {
//			System.out
//					.println("Could not modify file. Please ensure you have permission.");
//			System.exit(1);
//		}

	}

	private static void cmdBuild(String[] args) throws BrutException {

	  Androlib instance = new Androlib();
	  
		// hold all the fields
		HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("forceBuildAll", false);
		flags.put("debug", false);
		flags.put("verbose", false);
		flags.put("framework", false);
		flags.put("update", false);
		flags.put("copyOriginal", false);

		int i;
		int skip = 0;

		ExtFile mOrigApk = null;
		String mAaptPath = "";
		for (i = 0; i < args.length; i++) {
			String opt = args[i];
			if (!opt.startsWith("-")) {
				break;
			}
			if ("-f".equals(opt) || "--force-all".equals(opt)) {
				flags.put("forceBuildAll", true);
			} else if ("-d".equals(opt) || "--debug".equals(opt)) {
				flags.put("debug", true);
			} else if ("-v".equals(opt) || "--verbose".equals(opt)) {
				flags.put("verbose", true);
			} else if ("-a".equals(opt) || "--aapt".equals(opt)) {
				mAaptPath = args[i + 1];
				skip = 1;
			} else if ("-c".equals(opt) || "--copy-original".equals(opt)) {
				flags.put("copyOriginal", true);
			} else if ("--frame-path".equals(opt)) {
			    i++;
	        instance.setFrameworkFolder(args[i]);
			} else {
				throw new InvalidArgsError();
			}
		}

		String appDirName;
		File outFile = null;
		switch (args.length - i - skip) {
		case 0:
			appDirName = ".";
			break;
		case 2:
			outFile = new File(args[i + 1 + skip]);
		case 1:
			appDirName = args[i + skip];
			break;
		default:
			throw new InvalidArgsError();
		}

		instance.build(new File(appDirName), outFile, flags, mOrigApk,
				mAaptPath);
	}

	private static void cmdInstallFramework(String[] args)
			throws AndrolibException {
		String tag = null;
		String frame_path = null;
		int i = 0;
		switch (args.length) {
		case 4:
			if (args[2].equalsIgnoreCase("--frame-path")) {
				i++;
			} else {
				throw new InvalidArgsError();
			}
		case 3:
			frame_path = args[2 + i];
		case 2:
			if (!(args[1].equalsIgnoreCase("--frame-path"))) {
				tag = args[1];
			}
		case 1:
			new Androlib().installFramework(new File(args[0]), tag, frame_path);
			return;
		}

		throw new InvalidArgsError();
	}

	private static void cmdPublicizeResources(String[] args)
			throws InvalidArgsError, AndrolibException {
		if (args.length != 1) {
			throw new InvalidArgsError();
		}

		new Androlib().publicizeResources(new File(args[0]));
	}

	private static void _version() {
		System.out.println(Androlib.getVersion());
	}

	@SuppressWarnings("static-access")
  private static void _Options() {
	  
	  // create options
	  Option versionOption = OptionBuilder.withLongOpt("version")
        .withDescription("prints the version then exits")
        .create("version");
	  
	  Option advanceOption = OptionBuilder.withLongOpt("advanced")
	      .withDescription("prints advance information.")
	      .create("advance");
	  
	  Option noSrcOption = OptionBuilder.withLongOpt("no-src")
	      .withDescription("Do not decode sources.")
	      .create("s");
	  
	  Option noResOption = OptionBuilder.withLongOpt("no-res")
	      .withDescription("Do not decode resources.")
	      .create("r");
	  
	  Option debugDecOption = OptionBuilder.withLongOpt("debug")
	      .withDescription("Decode in debug mode. Check project page for more info.")
	      .create("d");
	  
	  Option debugBuiOption = OptionBuilder.withLongOpt("debug")
	      .withDescription("Builds in debug mode. Check project page for more info.")
	      .create("d");
	  
	  Option noDbgOption = OptionBuilder.withLongOpt("no-debug-info")
	      .withDescription("don't write out debug info (.local, .param, .line, etc.)")
	      .create("b");
	  
	  Option forceDecOption = OptionBuilder.withLongOpt("force")
	      .withDescription("Force delete destination directory.")
	      .create("f");
	  
	  Option frameTagOption = OptionBuilder.withLongOpt("frame-tag")
        .withDescription("Uses framework files tagged by <tag>.")
        .hasArg(true)
        .withArgName("tag")
        .create("t");
	  
	   Option frameDirOption = OptionBuilder.withLongOpt("frame-path")
	        .withDescription("Uses framework files located in <dir>.")
	        .hasArg(true)
	        .withArgName("dir")
	        .create("p");
	   
	   Option keepResOption = OptionBuilder.withLongOpt("keep-broken-res")
	       .withDescription("Use if there was an error and some resources were dropped, e.g.\n"
            + "            \"Invalid config flags detected. Dropping resources\", but you\n"
            + "            want to decode them anyway, even with errors. You will have to\n"
            + "            fix them manually before building.")
            .create("k");
	   
	   Option forceBuiOption = OptionBuilder.withLongOpt("force-all")
	       .withDescription("Skip changes detection and build all files.")
	       .create("f");
	   
	   Option aaptOption = OptionBuilder.withLongOpt("aapt")
	       .hasArg(true)
	       .withArgName("loc")
	       .withDescription("Loads aapt from specified location.")
	       .create("a");
	   
	   Option originalOption = OptionBuilder.withLongOpt("copy-original")
	       .withDescription("Copies original AndroidManifest.xml and META-INF. See project page for more info.")
	       .create("c");
	   
	   Option tagOption = OptionBuilder.withLongOpt("tag")
	       .withDescription("Tag frameworks using <tag>.")
	       .hasArg(true)
	       .withArgName("tag")
	       .create("t");
	   
	   Option outputBuiOption = OptionBuilder.withLongOpt("output")
	       .withDescription("The name of apk that gets written. Default is dist/name.apk")
	       .hasArg(true)
	       .withArgName("dir")
	       .create("o");
	   
	   Option outputDecOption = OptionBuilder.withLongOpt("output")
	       .withDescription("The name of folder that gets written. Default is apk.out")
	       .hasArg(true)
	       .withArgName("dir")
	       .create("o");
	   
	   Option decodeOption = OptionBuilder.withLongOpt("decode")
	       .create("d");
	   
	   Option buildOption = OptionBuilder.withLongOpt("build")
	       .create("b");
	         
	  // check for advance mode
    if (advanceMode) {
      DecodeOptions.addOption(debugDecOption);
      DecodeOptions.addOption(noDbgOption);
      DecodeOptions.addOption(keepResOption);
      
      BuildOptions.addOption(debugBuiOption);
      BuildOptions.addOption(aaptOption);
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
	  BuildOptions.addOption(originalOption);
	  BuildOptions.addOption(forceBuiOption);
	  
	  // add basic framework options
	  frameOptions.addOption(tagOption);
	  frameOptions.addOption(frameDirOption);
	  
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
	  allOptions.addOption(debugDecOption);
	  allOptions.addOption(noDbgOption);
	  allOptions.addOption(keepResOption);
	  allOptions.addOption(debugBuiOption);
	  allOptions.addOption(aaptOption);
	}
	
	private static String verbosityHelp() {
	  if (advanceMode) {
	    return "[-q|--quiet OR -v|--verbose] ";
	  } else {
	    return "";
	  }
	}
	
	
	private static void usage(CommandLine commandLine) {
	  
	    // load basicOptions
	    _Options();
	    HelpFormatter formatter = new HelpFormatter();
	    
	    // max their window to 120, if small.
	    int consoleWidth = ConsoleUtil.getConsoleWidth();
      if (consoleWidth <= 0) {
          consoleWidth = 120;
      }
      formatter.setWidth(consoleWidth);
      
      // print out license info prior to formatter.
      System.out.println(
          "Apktool v" + Androlib.getVersion() + " - a tool for reengineering Android apk files\n" +
          "Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>\n" +
          "Updated by @iBotPeaches <connor.tumbleson@gmail.com> \n" +
          "with smali v" + ApktoolProperties.get("smaliVersion") +
          " and baksmali v" + ApktoolProperties.get("baksmaliVersion") + "\n" +
          "Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0)\n");
      
      // two different outputs for build / decode
      formatter.printHelp("apktool " + verbosityHelp(), normalOptions);
      formatter.printHelp("apktool " + verbosityHelp() + "if|install-framework [options] <framework.apk>", frameOptions);
	    formatter.printHelp("apktool " + verbosityHelp() + "d[ecode] [options] <file_apk>", DecodeOptions);
	    formatter.printHelp("apktool " + verbosityHelp() + "b[uild] [options] <app_path>", BuildOptions);
	    
	    // print out more information
	    System.out.println(
	            "\nFor additional info, see: http://code.google.com/p/android-apktool/ \n"
            + "For smali/baksmali info, see: http://code.google.com/p/smali/");
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
  
  static {
    //normal and advance usage output
    normalOptions = new Options();
    BuildOptions = new Options();
    DecodeOptions = new Options();
    frameOptions = new Options();
    allOptions = new Options();
  }
  
	static class InvalidArgsError extends AndrolibException {

	}
}

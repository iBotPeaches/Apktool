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
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Main {
	public static void main(String[] args) throws IOException,
			InterruptedException, BrutException {
		try {
			Verbosity verbosity = Verbosity.NORMAL;
			int i;
			for (i = 0; i < args.length; i++) {
				String opt = args[i];

				if (opt.startsWith("--version") || (opt.startsWith("-version"))) {
					version_print();
					System.exit(1);
				}
				if (!opt.startsWith("-")) {
					break;
				}
				if ("-v".equals(opt) || "--verbose".equals(opt)) {
					if (verbosity != Verbosity.NORMAL) {
						throw new InvalidArgsError();
					}
					verbosity = Verbosity.VERBOSE;
				} else if ("-q".equals(opt) || "--quiet".equals(opt)) {
					if (verbosity != Verbosity.NORMAL) {
						throw new InvalidArgsError();
					}
					verbosity = Verbosity.QUIET;
				} else {
					throw new InvalidArgsError();
				}
			}
			setupLogging(verbosity);

			if (args.length <= i) {
				throw new InvalidArgsError();
			}
			String cmd = args[i];
			args = Arrays.copyOfRange(args, i + 1, args.length);

			if ("d".equals(cmd) || "decode".equals(cmd)) {
				cmdDecode(args);
			} else if ("b".equals(cmd) || "build".equals(cmd)) {
				cmdBuild(args);
			} else if ("if".equals(cmd) || "install-framework".equals(cmd)) {
				cmdInstallFramework(args);
			} else if ("publicize-resources".equals(cmd)) {
				cmdPublicizeResources(args);
			} else {
				throw new InvalidArgsError();
			}
		} catch (InvalidArgsError ex) {
			usage();
			System.exit(1);
		}
	}

	private static void cmdDecode(String[] args) throws InvalidArgsError,
			AndrolibException {
		ApkDecoder decoder = new ApkDecoder();

		int i;
		for (i = 0; i < args.length; i++) {
			String opt = args[i];
			if (!opt.startsWith("-")) {
				break;
			}
			if ("-s".equals(opt) || "--no-src".equals(opt)) {
				decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE);
			} else if ("-d".equals(opt) || "--debug".equals(opt)) {
				decoder.setDebugMode(true);
			} else if ("-b".equals(opt) || "--no-debug-info".equals(opt)) {
				decoder.setBaksmaliDebugMode(false);
			} else if ("-t".equals(opt) || "--frame-tag".equals(opt)) {
				i++;
				if (i >= args.length) {
					throw new InvalidArgsError();
				}
				decoder.setFrameworkTag(args[i]);
			} else if ("-f".equals(opt) || "--force".equals(opt)) {
				decoder.setForceDelete(true);
			} else if ("-r".equals(opt) || "--no-res".equals(opt)) {
				decoder.setDecodeResources(ApkDecoder.DECODE_RESOURCES_NONE);
			} else if ("--keep-broken-res".equals(opt)) {
				decoder.setKeepBrokenResources(true);
			} else if ("--frame-path".equals(opt)) {
				i++;
        if (i >= args.length) {
          throw new InvalidArgsError();
        }
				decoder.setFrameworkDir(args[i]);
			} else {
				throw new InvalidArgsError();
			}
		}

		String outName = null;
		if (args.length == i + 2) {
			outName = args[i + 1];
		} else if (args.length == i + 1) {
			outName = args[i];
			outName = outName.endsWith(".apk") ? outName.substring(0,
					outName.length() - 4) : outName + ".out";
			outName = new File(outName).getName();
		} else {
			throw new InvalidArgsError();
		}
		File outDir = new File(outName);
		decoder.setOutDir(outDir);
		decoder.setApkFile(new File(args[i]));

		try {
			decoder.decode();
		} catch (OutDirExistsException ex) {
			System.out
					.println("Destination directory ("
							+ outDir.getAbsolutePath()
							+ ") "
							+ "already exists. Use -f switch if you want to overwrite it.");
			System.exit(1);
		} catch (InFileNotFoundException ex) {
			System.out.println("Input file (" + args[i] + ") "
					+ "was not found or was not readable.");
			System.exit(1);
		} catch (CantFindFrameworkResException ex) {
			System.out
					.println("Can't find framework resources for package of id: "
							+ String.valueOf(ex.getPkgId())
							+ ". You must install proper "
							+ "framework files, see project website for more info.");
			System.exit(1);
		} catch (IOException ex) {
			System.out
					.println("Could not modify file. Please ensure you have permission.");
			System.exit(1);
		}

	}

	private static void cmdBuild(String[] args) throws BrutException {

	  Androlib instance = new Androlib();
	  
		// hold all the fields
		HashMap<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put("forceBuildAll", false);
		flags.put("debug", false);
		flags.put("verbose", false);
		flags.put("injectOriginal", false);
		flags.put("framework", false);
		flags.put("update", false);

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
			} else if ("--frame-path".equals(opt)) {
			    i++;
	        instance.setFrameworkFolder(args[i]);
			} else if ("-o".equals(opt) || "--original".equals(opt)) {
				if (args.length >= 4) {
					throw new InvalidArgsError();
				} else {
					flags.put("injectOriginal", true);
					mOrigApk = new ExtFile(args[i + 1]);
					skip = 1;
				}
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

	private static void version_print() {
		System.out.println(Androlib.getVersion());
	}

	private static void usage() {
		System.out
				.println("Apktool v"
						+ Androlib.getVersion()
						+ " - a tool for reengineering Android apk files\n"
						+ "Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>\n"
						+ "with smali v"
						+ ApktoolProperties.get("smaliVersion")
						+ ", and baksmali v"
						+ ApktoolProperties.get("baksmaliVersion")
						+ "\n"
						+ "Updated by @iBotPeaches <connor.tumbleson@gmail.com> \n"
						+ "Apache License 2.0 (http://www.apache.org/licenses/LICENSE-2.0)\n"
						+ "\n"
						+ "Usage: apktool [-q|--quiet OR -v|--verbose] COMMAND [...]\n"
						+ "\n"
						+ "COMMANDs are:\n"
						+ "\n"
						+ "    d[ecode] [OPTS] <file.apk> [<dir>]\n"
						+ "        Decode <file.apk> to <dir>.\n"
						+ "\n"
						+ "        OPTS:\n"
						+ "\n"
						+ "        -s, --no-src\n"
						+ "            Do not decode sources.\n"
						+ "        -r, --no-res\n"
						+ "            Do not decode resources.\n"
						+ "        -d, --debug\n"
						+ "            Decode in debug mode. Check project page for more info.\n"
						+ "        -b, --no-debug-info\n"
						+ "            Baksmali -- don't write out debug info (.local, .param, .line, etc.)\n"
						+ "        -f, --force\n"
						+ "            Force delete destination directory.\n"
						+ "        -t <tag>, --frame-tag <tag>\n"
						+ "            Try to use framework files tagged by <tag>.\n"
						+ "        --frame-path <dir>\n"
						+ "            Use the specified directory for framework files\n"
						+ "        --keep-broken-res\n"
						+ "            Use if there was an error and some resources were dropped, e.g.:\n"
						+ "            \"Invalid config flags detected. Dropping resources\", but you\n"
						+ "            want to decode them anyway, even with errors. You will have to\n"
						+ "            fix them manually before building."
						+ "\n\n"
						+ "    b[uild] [OPTS] [<app_path>] [<out_file>]\n"
						+ "        Build an apk from already decoded application located in <app_path>.\n"
						+ "\n"
						+ "        It will automatically detect, whether files was changed and perform\n"
						+ "        needed steps only.\n"
						+ "\n"
						+ "        If you omit <app_path> then current directory will be used.\n"
						+ "        If you omit <out_file> then <app_path>/dist/<name_of_original.apk>\n"
						+ "        will be used.\n"
						+ "\n"
						+ "        OPTS:\n"
						+ "\n"
						+ "        -f, --force-all\n"
						+ "            Skip changes detection and build all files.\n"
						+ "        -d, --debug\n"
						+ "            Build in debug mode. Check project page for more info.\n"
						+ "        -a, --aapt\n"
						+ "            Loads aapt from specified location.\n"
            + "        --frame-path <dir>\n"
            + "            Use the specified directory for framework files\n"
						+ "\n"
						+ "    if|install-framework <framework.apk> [<tag>] --frame-path [<location>] \n"
						+ "        Install framework file to your system.\n"
						+ "\n"
						+ "For additional info, see: http://code.google.com/p/android-apktool/"
						+ "\n"
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

	private static enum Verbosity {
		NORMAL, VERBOSE, QUIET;
	}

	private static boolean Advanced = false;

	static class InvalidArgsError extends AndrolibException {

	}
}

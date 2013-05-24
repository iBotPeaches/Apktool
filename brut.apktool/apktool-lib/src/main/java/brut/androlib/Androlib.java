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

package brut.androlib;

import brut.androlib.java.AndrolibJava;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.util.ExtFile;
import brut.androlib.src.SmaliBuilder;
import brut.androlib.src.SmaliDecoder;
import brut.common.BrutException;
import brut.directory.*;
import brut.util.BrutIO;
import brut.util.OS;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.nio.file.Files;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Androlib {
	private final AndrolibResources mAndRes = new AndrolibResources();

	public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
		return mAndRes.getResTable(apkFile, true);
	}

	public ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
			throws AndrolibException {
		return mAndRes.getResTable(apkFile, loadMainPkg);
	}

	public void decodeSourcesRaw(ExtFile apkFile, File outDir, boolean debug)
			throws AndrolibException {
		try {
			Directory apk = apkFile.getDirectory();
			LOGGER.info("Copying raw classes.dex file...");
			apkFile.getDirectory().copyToDir(outDir, "classes.dex");
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void decodeSourcesSmali(File apkFile, File outDir, boolean debug, String debugLinePrefix,
			boolean bakdeb) throws AndrolibException {
		try {
			File smaliDir = new File(outDir, SMALI_DIRNAME);
			OS.rmdir(smaliDir);
			smaliDir.mkdirs();
			LOGGER.info("Baksmaling...");
			SmaliDecoder.decode(apkFile, smaliDir, debug, debugLinePrefix, bakdeb);
		} catch (BrutException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void decodeSourcesJava(ExtFile apkFile, File outDir, boolean debug)
			throws AndrolibException {
		LOGGER.info("Decoding Java sources...");
		new AndrolibJava().decode(apkFile, outDir);
	}

	public void decodeManifestRaw(ExtFile apkFile, File outDir)
			throws AndrolibException {
		try {
			Directory apk = apkFile.getDirectory();
			LOGGER.info("Copying raw manifest...");
			apkFile.getDirectory().copyToDir(outDir, APK_MANIFEST_FILENAMES);
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void decodeManifestFull(ExtFile apkFile, File outDir,
			ResTable resTable) throws AndrolibException {
		mAndRes.decodeManifest(resTable, apkFile, outDir);
	}

	public void decodeResourcesRaw(ExtFile apkFile, File outDir)
			throws AndrolibException {
		try {
			// Directory apk = apkFile.getDirectory();
			LOGGER.info("Copying raw resources...");
			apkFile.getDirectory().copyToDir(outDir, APK_RESOURCES_FILENAMES);
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void decodeResourcesFull(ExtFile apkFile, File outDir,
			ResTable resTable) throws AndrolibException {
		mAndRes.decode(resTable, apkFile, outDir);
	}

	public void decodeRawFiles(ExtFile apkFile, File outDir)
			throws AndrolibException {
		LOGGER.info("Copying assets and libs...");
		try {
			Directory in = apkFile.getDirectory();
			if (in.containsDir("assets")) {
				in.copyToDir(outDir, "assets");
			}
			if (in.containsDir("lib")) {
				in.copyToDir(outDir, "lib");
			}
            if (in.containsDir("libs")) {
                in.copyToDir(outDir, "libs");
            }
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

    private boolean isAPKFileNames(String file) {
        for (String apkFile : APK_STANDARD_ALL_FILENAMES) {
            if (apkFile.equals(file) || file.startsWith(apkFile)) {
                return true;
            }
        }
        return false;
    }

    public void decodeUnknownFiles(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        LOGGER.info("Copying unknown files/dir...");
        File unknownOut = new File(outDir, UNK_DIRNAME);
        ZipEntry invZipFile;

        // have to use container of ZipFile to help identify compression type
        // with regular looping of apkFile for easy copy
        try {
            Directory unk = apkFile.getDirectory();
            ZipExtFile apkZipFile = new ZipExtFile(apkFile.getAbsolutePath());

            // loop all items in container recursively, ignoring any that are pre-defined by aapt
            Set<String> files = unk.getFiles(true);
            for (String file : files) {
                if (!isAPKFileNames(file)) {

                    // copy file out of archive into special "unknown" folder
                    // to be re-included on build
                    unk.copyToDir(unknownOut,file);
                    try {
                        // ignore encryption
                        apkZipFile.getEntry(file.toString()).getGeneralPurposeBit().useEncryption(false);
                        invZipFile = apkZipFile.getEntry(file.toString());

                        // lets record the name of the file, and its compression type
                        // so that we may re-include it the same way
                        if (invZipFile != null) {
                            resTable.addUnknownFileInfo(invZipFile.getName(), String.valueOf(invZipFile.getMethod()));
                        }
                    } catch (NullPointerException ignored) {

                    }
                }
            }
            apkZipFile.close();
        }
        catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
        catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

	public void writeOriginalFiles(ExtFile apkFile, File outDir)
			throws AndrolibException {
		LOGGER.info("Copying original files...");
		File originalDir = new File(outDir, "original");
		if (!originalDir.exists()) {
			originalDir.mkdirs();
		}

		try {
			Directory in = apkFile.getDirectory();
			if(in.containsFile("AndroidManifest.xml")) {
				in.copyToDir(originalDir, "AndroidManifest.xml");
			}
			if (in.containsDir("META-INF")) {
				in.copyToDir(originalDir, "META-INF");
			}
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public void writeMetaFile(File mOutDir, Map<String, Object> meta)
			throws AndrolibException {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		// options.setIndent(4);
		Yaml yaml = new Yaml(options);

		FileWriter writer = null;
		try {
			writer = new FileWriter(new File(mOutDir, "apktool.yml"));
			yaml.dump(meta, writer);
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	public Map<String, Object> readMetaFile(ExtFile appDir)
			throws AndrolibException {
		InputStream in = null;
		try {
			in = appDir.getDirectory().getFileInput("apktool.yml");
			Yaml yaml = new Yaml();
			return (Map<String, Object>) yaml.load(in);
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	public void build(File appDir, File outFile,
			HashMap<String, Boolean> flags, String aaptPath)
			throws BrutException {
		build(new ExtFile(appDir), outFile, flags, aaptPath);
	}

	public void build(ExtFile appDir, File outFile,
			HashMap<String, Boolean> flags, String aaptPath)
			throws BrutException {

		mAaptPath = aaptPath;
		Map<String, Object> meta = readMetaFile(appDir);
		Object t1 = meta.get("isFrameworkApk");
		flags.put("framework", t1 == null ? false : (Boolean) t1);
		flags.put("compression", meta.get("compressionType") == null ? false
				: (Boolean) meta.get("compressionType"));
		mAndRes.setSdkInfo((Map<String, String>) meta.get("sdkInfo"));
		mAndRes.setPackageId((Map<String, String>) meta.get("packageInfo"));
		mAndRes.setVersionInfo((Map<String, String>) meta.get("versionInfo"));

		if (outFile == null) {
			String outFileName = (String) meta.get("apkFileName");
			outFile = new File(appDir, "dist" + File.separator
					+ (outFileName == null ? "out.apk" : outFileName));
		}

		new File(appDir, APK_DIRNAME).mkdirs();
		buildSources(appDir, flags);
		buildResources(appDir, flags,
				(Map<String, Object>) meta.get("usesFramework"));
		buildLib(appDir, flags);
		buildCopyOriginalFiles(appDir, flags);
		buildApk(appDir, outFile, flags);

        // we must go after the Apk is built, and copy the files in via Zip
        // this is because Aapt won't add files it doesn't know (ex unknown files)
        buildUnknownFiles(appDir,outFile,meta);
	}

	public void buildSources(File appDir, HashMap<String, Boolean> flags)
			throws AndrolibException {
		if (!buildSourcesRaw(appDir, flags)
				&& !buildSourcesSmali(appDir, flags)
				&& !buildSourcesJava(appDir, flags)) {
			LOGGER.warning("Could not find sources");
		}
	}

	public boolean buildSourcesRaw(File appDir, HashMap<String, Boolean> flags)
			throws AndrolibException {
		try {
			File working = new File(appDir, "classes.dex");
			if (!working.exists()) {
				return false;
			}
			File stored = new File(appDir, APK_DIRNAME + "/classes.dex");
			if (flags.get("forceBuildAll") || isModified(working, stored)) {
				LOGGER.info("Copying classes.dex file...");
				BrutIO.copyAndClose(new FileInputStream(working),
						new FileOutputStream(stored));
			}
			return true;
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		}
	}

	public boolean buildSourcesSmali(File appDir, HashMap<String, Boolean> flags)
			throws AndrolibException {
		ExtFile smaliDir = new ExtFile(appDir, "smali");
		if (!smaliDir.exists()) {
			return false;
		}
		File dex = new File(appDir, APK_DIRNAME + "/classes.dex");
		if (!flags.get("forceBuildAll")) {
			LOGGER.info("Checking whether sources has changed...");
		}
		if (flags.get("forceBuildAll") || isModified(smaliDir, dex)) {
			LOGGER.info("Smaling...");
			dex.delete();
			SmaliBuilder.build(smaliDir, dex, flags);
		}
		return true;
	}

	public boolean buildSourcesJava(File appDir, HashMap<String, Boolean> flags)
			throws AndrolibException {
		File javaDir = new File(appDir, "src");
		if (!javaDir.exists()) {
			return false;
		}
		File dex = new File(appDir, APK_DIRNAME + "/classes.dex");
		if (!flags.get("forceBuildAll")) {
			LOGGER.info("Checking whether sources has changed...");
		}
		if (flags.get("forceBuildAll") || isModified(javaDir, dex)) {
			LOGGER.info("Building java sources...");
			dex.delete();
			new AndrolibJava().build(javaDir, dex);
		}
		return true;
	}

	public void buildResources(ExtFile appDir, HashMap<String, Boolean> flags,
			Map<String, Object> usesFramework) throws BrutException {
		if (!buildResourcesRaw(appDir, flags)
				&& !buildResourcesFull(appDir, flags, usesFramework)
				&& !buildManifest(appDir, flags, usesFramework)) {
			LOGGER.warning("Could not find resources");
		}
	}

	public boolean buildResourcesRaw(ExtFile appDir,
			HashMap<String, Boolean> flags) throws AndrolibException {
		try {
			if (!new File(appDir, "resources.arsc").exists()) {
				return false;
			}
			File apkDir = new File(appDir, APK_DIRNAME);
			if (!flags.get("forceBuildAll")) {
				LOGGER.info("Checking whether resources has changed...");
			}
			if (flags.get("forceBuildAll")
					|| isModified(newFiles(APK_RESOURCES_FILENAMES, appDir),
							newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
				LOGGER.info("Copying raw resources...");
				appDir.getDirectory()
						.copyToDir(apkDir, APK_RESOURCES_FILENAMES);
			}
			return true;
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public boolean buildResourcesFull(File appDir,
			HashMap<String, Boolean> flags, Map<String, Object> usesFramework)
			throws AndrolibException {
		try {
			if (!new File(appDir, "res").exists()) {
				return false;
			}
			if (!flags.get("forceBuildAll")) {
				LOGGER.info("Checking whether resources has changed...");
			}
			File apkDir = new File(appDir, APK_DIRNAME);
			if (flags.get("forceBuildAll")
					|| isModified(newFiles(APP_RESOURCES_FILENAMES, appDir),
							newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
				LOGGER.info("Building resources...");

				File apkFile = File.createTempFile("APKTOOL", null);
				apkFile.delete();

				File ninePatch = new File(appDir, "9patch");
				if (!ninePatch.exists()) {
					ninePatch = null;
				}
				mAndRes.aaptPackage(apkFile, new File(appDir,
						"AndroidManifest.xml"), new File(appDir, "res"),
						ninePatch, null, parseUsesFramework(usesFramework),
						flags, mAaptPath);

				Directory tmpDir = new ExtFile(apkFile).getDirectory();
				tmpDir.copyToDir(apkDir,
						tmpDir.containsDir("res") ? APK_RESOURCES_FILENAMES
								: APK_RESOURCES_WITHOUT_RES_FILENAMES);

				// delete tmpDir
				apkFile.delete();
			}
			return true;
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		} catch (BrutException ex) {
			throw new AndrolibException(ex);
		}
	}

	public boolean buildManifestRaw(ExtFile appDir,
			HashMap<String, Boolean> flags) throws AndrolibException {
		try {
			File apkDir = new File(appDir, APK_DIRNAME);
			LOGGER.info("Copying raw AndroidManifest.xml...");
			appDir.getDirectory().copyToDir(apkDir, APK_MANIFEST_FILENAMES);
			return true;
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		}
	}

	public boolean buildManifest(ExtFile appDir,
			HashMap<String, Boolean> flags, Map<String, Object> usesFramework)
			throws BrutException {
		try {
			if (!new File(appDir, "AndroidManifest.xml").exists()) {
				return false;
			}
			if (!flags.get("forceBuildAll")) {
				LOGGER.info("Checking whether resources has changed...");
			}
			
			File apkDir = new File(appDir, APK_DIRNAME);
			
			if (flags.get("debug")) {
				mAndRes.remove_application_debug(new File(apkDir,"AndroidManifest.xml").getAbsolutePath());
			}

			if (flags.get("forceBuildAll")
					|| isModified(newFiles(APK_MANIFEST_FILENAMES, appDir),
							newFiles(APK_MANIFEST_FILENAMES, apkDir))) {
				LOGGER.info("Building AndroidManifest.xml...");

				File apkFile = File.createTempFile("APKTOOL", null);
				apkFile.delete();

				File ninePatch = new File(appDir, "9patch");
				if (!ninePatch.exists()) {
					ninePatch = null;
				}

				mAndRes.aaptPackage(apkFile, new File(appDir,
						"AndroidManifest.xml"), null, ninePatch, null,
						parseUsesFramework(usesFramework), flags, mAaptPath);

				Directory tmpDir = new ExtFile(apkFile).getDirectory();
				tmpDir.copyToDir(apkDir, APK_MANIFEST_FILENAMES);

			}
			return true;
		} catch (IOException ex) {
			throw new AndrolibException(ex);
		} catch (DirectoryException ex) {
			throw new AndrolibException(ex);
		} catch (AndrolibException ex) {
			LOGGER.warning("Parse AndroidManifest.xml failed, treat it as raw file.");
			return buildManifestRaw(appDir, flags);
		}
	}

	public void buildLib(File appDir, HashMap<String, Boolean> flags)
			throws AndrolibException {
		File working = new File(appDir, "lib");
		if (!working.exists()) {
			return;
		}
		File stored = new File(appDir, APK_DIRNAME + "/lib");
		if (flags.get("forceBuildAll") || isModified(working, stored)) {
			LOGGER.info("Copying libs...");
			try {
				OS.rmdir(stored);
				OS.cpdir(working, stored);
			} catch (BrutException ex) {
				throw new AndrolibException(ex);
			}
		}
	}

	public void buildCopyOriginalFiles(File appDir,
			HashMap<String, Boolean> flags) throws AndrolibException {
		if (flags.get("copyOriginal")) {
			File originalDir = new File(appDir, "original");
			if(originalDir.exists()) {
				try {
					LOGGER.info("Copy original files...");
					Directory in = (new ExtFile(originalDir)).getDirectory();
					if(in.containsFile("AndroidManifest.xml")) {
						LOGGER.info("Copy AndroidManifest.xml...");
						in.copyToDir(new File(appDir, APK_DIRNAME), "AndroidManifest.xml");
					}
					if (in.containsDir("META-INF")) {
						LOGGER.info("Copy META-INF...");
						in.copyToDir(new File(appDir, APK_DIRNAME), "META-INF");
					}
				} catch (DirectoryException ex) {
					throw new AndrolibException(ex);
				}
			}
		}
	}

    public void buildUnknownFiles(File appDir, File outFile, Map<String, Object> meta)
        throws AndrolibException {
        File file;
        mPath = Paths.get(appDir.getPath() + File.separatorChar + UNK_DIRNAME);

        if (meta.containsKey("unknownFiles")) {
            LOGGER.info("Copying unknown files/dir...");

            Map<String, String> files = (Map<String, String>)meta.get("unknownFiles");

            try {
                // set our filesystem options
                Map<String, String> zip_properties = new HashMap<>();
                zip_properties.put("create", "false");
                zip_properties.put("encoding", "UTF-8");

                // create filesystem
                Path path = Paths.get(outFile.getAbsolutePath());
                URI apkFileSystem = URI.create("jar:file:" + path.toUri().getPath());
                try(FileSystem zipFS = FileSystems.newFileSystem(apkFileSystem, zip_properties)) {

                    // loop through files inside
                    for (Map.Entry<String,String> entry : files.entrySet()) {

                        // check if file exists
                        file = new File(mPath.toFile(), entry.getKey());
                        if (file.isFile() && file.exists()) {
                            insertFile(zipFS, file, entry.getValue(),mPath.toAbsolutePath());
                        }
                    }
                    zipFS.close();
                }
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
        }

    }

    public void insertFile(FileSystem zipfs, File insert, String method, Path root)
            throws AndrolibException, IOException {
        Path zipRoot = zipfs.getPath(zipfs.getSeparator());
        Path zipPath = zipfs.getPath(zipRoot.toString() + insert.getAbsolutePath().replace(root.toString(),""));
        Path tmp = zipPath.normalize().getParent();

        if (!Files.isDirectory(tmp, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(tmp);
        }
        Path newFile = Paths.get(insert.getAbsolutePath());
        Files.copy(newFile,zipPath, StandardCopyOption.REPLACE_EXISTING);
    }
  
	public void buildApk(File appDir, File outApk,
			HashMap<String, Boolean> flags) throws AndrolibException {
		LOGGER.info("Building apk file...");
		if (outApk.exists()) {
			outApk.delete();
		} else {
			File outDir = outApk.getParentFile();
			if (outDir != null && !outDir.exists()) {
				outDir.mkdirs();
			}
		}
		File assetDir = new File(appDir, "assets");
		if (!assetDir.exists()) {
			assetDir = null;
		}
		mAndRes.aaptPackage(outApk, null, null, new File(appDir, APK_DIRNAME),
				assetDir, null, flags, mAaptPath);
	}

	public void publicizeResources(File arscFile) throws AndrolibException {
		mAndRes.publicizeResources(arscFile);
	}

	public void installFramework(File frameFile, String tag, String frame_path)
			throws AndrolibException {
		mAndRes.setFrameworkFolder(frame_path);
		mAndRes.installFramework(frameFile, tag);
	}

	public boolean isFrameworkApk(ResTable resTable) {
		for (ResPackage pkg : resTable.listMainPackages()) {
			if (pkg.getId() < 64) {
				return true;
			}
		}
		return false;
	}

	public static String getVersion() {
		String version = ApktoolProperties.get("application.version");
		return version.endsWith("-SNAPSHOT") ? version.substring(0,
				version.length() - 9)
				+ '.' + ApktoolProperties.get("git.commit.id.abbrev") : version;
	}

	private File[] parseUsesFramework(Map<String, Object> usesFramework)
			throws AndrolibException {
		if (usesFramework == null) {
			return null;
		}

		List<Integer> ids = (List<Integer>) usesFramework.get("ids");
		if (ids == null || ids.isEmpty()) {
			return null;
		}

		String tag = (String) usesFramework.get("tag");
		File[] files = new File[ids.size()];
		int i = 0;
		for (int id : ids) {
			files[i++] = mAndRes.getFrameworkApk(id, tag);
		}
		return files;
	}

	private boolean isModified(File working, File stored) {
		if (!stored.exists()) {
			return true;
		}
		return BrutIO.recursiveModifiedTime(working) > BrutIO
				.recursiveModifiedTime(stored);
	}

	private boolean isModified(File[] working, File[] stored) {
		for (int i = 0; i < stored.length; i++) {
			if (!stored[i].exists()) {
				return true;
			}
		}
		return BrutIO.recursiveModifiedTime(working) > BrutIO
				.recursiveModifiedTime(stored);
	}

	private File[] newFiles(String[] names, File dir) {
		File[] files = new File[names.length];
		for (int i = 0; i < names.length; i++) {
			files[i] = new File(dir, names[i]);
		}
		return files;
	}
	
	public void setFrameworkFolder(String path) {
	  mAndRes.setFrameworkFolder(path);
	}

	private String mAaptPath = null;
    private Path mPath = null;

	private final static Logger LOGGER = Logger.getLogger(Androlib.class
			.getName());

	private final static String SMALI_DIRNAME = "smali";
	private final static String APK_DIRNAME = "build/apk";
    private final static String UNK_DIRNAME = "unknown";
	private final static String[] APK_RESOURCES_FILENAMES = new String[] {
			"resources.arsc", "AndroidManifest.xml", "res" };
	private final static String[] APK_RESOURCES_WITHOUT_RES_FILENAMES = new String[] {
			"resources.arsc", "AndroidManifest.xml" };
	private final static String[] APP_RESOURCES_FILENAMES = new String[] {
			"AndroidManifest.xml", "res" };
	private final static String[] APK_MANIFEST_FILENAMES = new String[] {
            "AndroidManifest.xml" };
    private final static String[] APK_STANDARD_ALL_FILENAMES = new String[] {
            "classes.dex", "AndroidManifest.xml", "resources.arsc","res","lib", "libs","assets","META-INF" };
}

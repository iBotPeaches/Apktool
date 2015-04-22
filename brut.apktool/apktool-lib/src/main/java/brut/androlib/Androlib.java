/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
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
import brut.androlib.res.data.ResUnknownFiles;
import brut.androlib.res.util.ExtFile;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.androlib.src.SmaliBuilder;
import brut.androlib.src.SmaliDecoder;
import brut.common.BrutException;
import brut.directory.*;
import brut.util.BrutIO;
import brut.util.OS;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Androlib {
    private final AndrolibResources mAndRes = new AndrolibResources();
    protected final ResUnknownFiles mResUnknownFiles = new ResUnknownFiles();
    public ApkOptions apkOptions;

    public Androlib(ApkOptions apkOptions) {
        this.apkOptions = apkOptions;
        mAndRes.apkOptions = apkOptions;
    }

    public Androlib() {
        this.apkOptions = new ApkOptions();
        mAndRes.apkOptions = this.apkOptions;
    }

    public ResTable getResTable(ExtFile apkFile)
            throws AndrolibException {
        return mAndRes.getResTable(apkFile, true);
    }

    public ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
            throws AndrolibException {
        return mAndRes.getResTable(apkFile, loadMainPkg);
    }

    public void decodeSourcesRaw(ExtFile apkFile, File outDir, String filename)
            throws AndrolibException {
        try {
            LOGGER.info("Copying raw " + filename + " file...");
            apkFile.getDirectory().copyToDir(outDir, filename);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeSourcesSmali(File apkFile, File outDir, String filename, boolean debug, String debugLinePrefix,
                                   boolean bakdeb, int api) throws AndrolibException {
        try {
            File smaliDir;
            if (filename.equalsIgnoreCase("classes.dex")) {
                smaliDir = new File(outDir, SMALI_DIRNAME);
            } else {
                smaliDir = new File(outDir, SMALI_DIRNAME + "_" + filename.substring(0, filename.indexOf(".")));
            }
            OS.rmdir(smaliDir);
            smaliDir.mkdirs();
            LOGGER.info("Baksmaling " + filename + "...");
            SmaliDecoder.decode(apkFile, smaliDir, filename, debug, debugLinePrefix, bakdeb, api);
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

    public void decodeManifestFull(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decodeManifest(resTable, apkFile, outDir);
    }

    public void decodeResourcesRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            LOGGER.info("Copying raw resources...");
            apkFile.getDirectory().copyToDir(outDir, APK_RESOURCES_FILENAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeResourcesFull(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decode(resTable, apkFile, outDir);
    }

    public void decodeManifestWithResources(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decodeManifestWithResources(resTable, apkFile, outDir);
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
            if (apkFile.equals(file) || file.startsWith(apkFile + "/")) {
                return true;
            }
        }
        return false;
    }

    public void decodeUnknownFiles(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        LOGGER.info("Copying unknown files...");
        File unknownOut = new File(outDir, UNK_DIRNAME);
        ZipEntry invZipFile;

        // have to use container of ZipFile to help identify compression type
        // with regular looping of apkFile for easy copy
        try {
            Directory unk = apkFile.getDirectory();
            ZipFile apkZipFile = new ZipFile(apkFile.getAbsolutePath());

            // loop all items in container recursively, ignoring any that are pre-defined by aapt
            Set<String> files = unk.getFiles(true);
            for (String file : files) {
                if (!isAPKFileNames(file) && !file.endsWith(".dex")) {

                    // copy file out of archive into special "unknown" folder
                    unk.copyToDir(unknownOut, file);
                    try {
                        invZipFile = apkZipFile.getEntry(file);

                        // lets record the name of the file, and its compression type
                        // so that we may re-include it the same way
                        if (invZipFile != null) {
                            mResUnknownFiles.addUnknownFileInfo(invZipFile.getName(), String.valueOf(invZipFile.getMethod()));
                        }
                    } catch (NullPointerException ignored) { }
                }
            }
            apkZipFile.close();
        } catch (DirectoryException | IOException ex) {
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
        Yaml yaml = new Yaml(options);

        try (
                Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                        new File(mOutDir, "apktool.yml")), "UTF-8"))
        ) {
            yaml.dump(meta, writer);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public Map<String, Object> readMetaFile(ExtFile appDir)
            throws AndrolibException {
        try(
                InputStream in = appDir.getDirectory().getFileInput("apktool.yml")
        ) {
            Yaml yaml = new Yaml();
            return (Map<String, Object>) yaml.load(in);
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void build(File appDir, File outFile) throws BrutException {
        build(new ExtFile(appDir), outFile);
    }

    public void build(ExtFile appDir, File outFile)
            throws BrutException {
        LOGGER.info("Using Apktool " + Androlib.getVersion());

        Map<String, Object> meta = readMetaFile(appDir);
        Object t1 = meta.get("isFrameworkApk");
        apkOptions.isFramework = (t1 == null ? false : (Boolean) t1);
        apkOptions.resourcesAreCompressed = meta.get("compressionType") == null
                ? false
                : Boolean.valueOf(meta.get("compressionType").toString());

        mAndRes.setSdkInfo((Map<String, String>) meta.get("sdkInfo"));
        mAndRes.setPackageId((Map<String, String>) meta.get("packageInfo"));
        mAndRes.setPackageInfo((Map<String, String>) meta.get("packageInfo"));
        mAndRes.setVersionInfo((Map<String, String>) meta.get("versionInfo"));
        mAndRes.setSharedLibrary((boolean) (meta.get("sharedLibrary") == null ? false : meta.get("sharedLibrary")));

        if (outFile == null) {
            String outFileName = (String) meta.get("apkFileName");
            outFile = new File(appDir, "dist" + File.separator + (outFileName == null ? "out.apk" : outFileName));
        }

        new File(appDir, APK_DIRNAME).mkdirs();
        buildSources(appDir);
        buildNonDefaultSources(appDir);
        ResXmlPatcher.fixingPublicAttrsInProviderAttributes(new File(appDir, "AndroidManifest.xml"));
        buildResources(appDir, (Map<String, Object>) meta.get("usesFramework"));

        buildLib(appDir);
        buildLibs(appDir);
        buildCopyOriginalFiles(appDir);
        buildApk(appDir, outFile);

        // we must go after the Apk is built, and copy the files in via Zip
        // this is because Aapt won't add files it doesn't know (ex unknown files)
        buildUnknownFiles(appDir, outFile, meta);
    }

    public void buildSources(File appDir)
            throws AndrolibException {
        if (!buildSourcesRaw(appDir, "classes.dex")
                && !buildSourcesSmali(appDir, "smali", "classes.dex")
                && !buildSourcesJava(appDir)) {
            LOGGER.warning("Could not find sources");
        }
    }

    public void buildNonDefaultSources(ExtFile appDir)
            throws AndrolibException {
        try {
            // loop through any smali_ directories for multi-dex apks
            Map<String, Directory> dirs = appDir.getDirectory().getDirs();
            for (Map.Entry<String, Directory> directory : dirs.entrySet()) {
                String name = directory.getKey();
                if (name.startsWith("smali_")) {
                    String filename = name.substring(name.indexOf("_") + 1) + ".dex";

                    if (!buildSourcesRaw(appDir, filename)
                            && !buildSourcesSmali(appDir, name, filename)
                            && !buildSourcesJava(appDir)) {
                        LOGGER.warning("Could not find sources");
                    }
                }
            }

            // loop through any classes#.dex files for multi-dex apks
            File[] dexFiles = appDir.listFiles();
            if (dexFiles != null) {
                for (File dex : dexFiles) {

                    // skip classes.dex because we have handled it in buildSources()
                    if (dex.getName().endsWith(".dex") && ! dex.getName().equalsIgnoreCase("classes.dex")) {
                        buildSourcesRaw(appDir, dex.getName());
                    }
                }
            }
        } catch(DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildSourcesRaw(File appDir, String filename)
            throws AndrolibException {
        File working = new File(appDir, filename);
        if (!working.exists()) {
            return false;
        }
        File stored = new File(appDir, APK_DIRNAME + "/" + filename);
        if (apkOptions.forceBuildAll || isModified(working, stored)) {
            LOGGER.info("Copying " + appDir.toString() + " " + filename + " file...");
            try {
                BrutIO.copyAndClose(new FileInputStream(working), new FileOutputStream(stored));
                return true;
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
        }
        return true;
    }

    public boolean buildSourcesSmali(File appDir, String folder, String filename)
            throws AndrolibException {
        ExtFile smaliDir = new ExtFile(appDir, folder);
        if (!smaliDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/" + filename);
        if (! apkOptions.forceBuildAll) {
            LOGGER.info("Checking whether sources has changed...");
        }
        if (apkOptions.forceBuildAll || isModified(smaliDir, dex)) {
            LOGGER.info("Smaling " + folder + " folder into " + filename +"...");
            dex.delete();
            SmaliBuilder.build(smaliDir, dex, apkOptions.debugMode);
        }
        return true;
    }

    public boolean buildSourcesJava(File appDir)
            throws AndrolibException {
        File javaDir = new File(appDir, "src");
        if (!javaDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/classes.dex");
        if (! apkOptions.forceBuildAll) {
            LOGGER.info("Checking whether sources has changed...");
        }
        if (apkOptions.forceBuildAll || isModified(javaDir, dex)) {
            LOGGER.info("Building java sources...");
            dex.delete();
            new AndrolibJava().build(javaDir, dex);
        }
        return true;
    }

    public void buildResources(ExtFile appDir, Map<String, Object> usesFramework)
            throws BrutException {
        if (!buildResourcesRaw(appDir) && !buildResourcesFull(appDir, usesFramework)
                && !buildManifest(appDir, usesFramework)) {
            LOGGER.warning("Could not find resources");
        }
    }

    public boolean buildResourcesRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            if (!new File(appDir, "resources.arsc").exists()) {
                return false;
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (! apkOptions.forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            if (apkOptions.forceBuildAll || isModified(newFiles(APK_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
                LOGGER.info("Copying raw resources...");
                appDir.getDirectory().copyToDir(apkDir, APK_RESOURCES_FILENAMES);
            }
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildResourcesFull(File appDir, Map<String, Object> usesFramework)
            throws AndrolibException {
        try {
            if (!new File(appDir, "res").exists()) {
                return false;
            }
            if (! apkOptions.forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (apkOptions.forceBuildAll || isModified(newFiles(APP_RESOURCES_FILENAMES, appDir),
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
                        ninePatch, null, parseUsesFramework(usesFramework));

                Directory tmpDir = new ExtFile(apkFile).getDirectory();
                tmpDir.copyToDir(apkDir,
                        tmpDir.containsDir("res") ? APK_RESOURCES_FILENAMES
                                : APK_RESOURCES_WITHOUT_RES_FILENAMES);

                // delete tmpDir
                apkFile.delete();
            }
            return true;
        } catch (IOException | BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildManifestRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            File apkDir = new File(appDir, APK_DIRNAME);
            LOGGER.info("Copying raw AndroidManifest.xml...");
            appDir.getDirectory().copyToDir(apkDir, APK_MANIFEST_FILENAMES);
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildManifest(ExtFile appDir, Map<String, Object> usesFramework)
            throws BrutException {
        try {
            if (!new File(appDir, "AndroidManifest.xml").exists()) {
                return false;
            }
            if (! apkOptions.forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }

            File apkDir = new File(appDir, APK_DIRNAME);

            if (apkOptions.debugMode) {
                ResXmlPatcher.removeApplicationDebugTag(new File(apkDir,"AndroidManifest.xml"));
            }

            if (apkOptions.forceBuildAll || isModified(newFiles(APK_MANIFEST_FILENAMES, appDir),
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
                        parseUsesFramework(usesFramework));

                Directory tmpDir = new ExtFile(apkFile).getDirectory();
                tmpDir.copyToDir(apkDir, APK_MANIFEST_FILENAMES);

            }
            return true;
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException(ex);
        } catch (AndrolibException ex) {
            LOGGER.warning("Parse AndroidManifest.xml failed, treat it as raw file.");
            return buildManifestRaw(appDir);
        }
    }

    public void buildLib(File appDir) throws AndrolibException {
        buildLibrary(appDir, "lib");
    }

    public void buildLibs(File appDir) throws AndrolibException {
        buildLibrary(appDir, "libs");
    }

    public void buildLibrary(File appDir, String folder) throws AndrolibException {
        File working = new File(appDir, folder);

        if (! working.exists()) {
            return;
        }

        File stored = new File(appDir, APK_DIRNAME + "/" + folder);
        if (apkOptions.forceBuildAll || isModified(working, stored)) {
            LOGGER.info("Copying libs... (/" + folder + ")");
            try {
                OS.rmdir(stored);
                OS.cpdir(working, stored);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    public void buildCopyOriginalFiles(File appDir)
            throws AndrolibException {
        if (apkOptions.copyOriginalFiles) {
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
        if (meta.containsKey("unknownFiles")) {
            LOGGER.info("Copying unknown files/dir...");

            Map<String, String> files = (Map<String, String>)meta.get("unknownFiles");
            File tempFile = new File(outFile.getParent(), outFile.getName() + ".apktool_temp");
            boolean renamed = outFile.renameTo(tempFile);
            if(!renamed) {
                throw new AndrolibException("Unable to rename temporary file");
            }

            try (
                    ZipFile inputFile = new ZipFile(tempFile);
                    ZipOutputStream actualOutput = new ZipOutputStream(new FileOutputStream(outFile))
            ) {
                copyExistingFiles(inputFile, actualOutput);
                copyUnknownFiles(appDir, actualOutput, files);
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }

            // Remove our temporary file.
            tempFile.delete();
        }
    }

    private void copyExistingFiles(ZipFile inputFile, ZipOutputStream outputFile) throws IOException {
        // First, copy the contents from the existing outFile:
        Enumeration<? extends ZipEntry> entries = inputFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = new ZipEntry(entries.nextElement());

            // We can't reuse the compressed size because it depends on compression sizes.
            entry.setCompressedSize(-1);
            outputFile.putNextEntry(entry);

            // No need to create directory entries in the final apk
            if (! entry.isDirectory()) {
                BrutIO.copy(inputFile, outputFile, entry);
            }

            outputFile.closeEntry();
        }
    }

    private void copyUnknownFiles(File appDir, ZipOutputStream outputFile, Map<String, String> files)
            throws IOException {
        File unknownFileDir = new File(appDir, UNK_DIRNAME);

        // loop through unknown files
        for (Map.Entry<String,String> unknownFileInfo : files.entrySet()) {
            File inputFile = new File(unknownFileDir, unknownFileInfo.getKey());
            if (inputFile.isDirectory()) {
                continue;
            }

            ZipEntry newEntry = new ZipEntry(unknownFileInfo.getKey());
            int method = Integer.valueOf(unknownFileInfo.getValue());
            LOGGER.fine(String.format("Copying unknown file %s with method %d", unknownFileInfo.getKey(), method));
            if (method == ZipEntry.STORED) {
                newEntry.setMethod(ZipEntry.STORED);
                newEntry.setSize(inputFile.length());
                newEntry.setCompressedSize(-1);
                BufferedInputStream unknownFile = new BufferedInputStream(new FileInputStream(inputFile));
                CRC32 crc = BrutIO.calculateCrc(unknownFile);
                newEntry.setCrc(crc.getValue());
            } else {
                newEntry.setMethod(ZipEntry.DEFLATED);
            }
            outputFile.putNextEntry(newEntry);

            BrutIO.copy(inputFile, outputFile);
            outputFile.closeEntry();
        }
    }

    public void buildApk(File appDir, File outApk) throws AndrolibException {
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
        mAndRes.aaptPackage(outApk, null, null, new File(appDir, APK_DIRNAME), assetDir, null);
    }

    public void publicizeResources(File arscFile) throws AndrolibException {
        mAndRes.publicizeResources(arscFile);
    }

    public void installFramework(File frameFile)
            throws AndrolibException {
        mAndRes.installFramework(frameFile);
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
        return ApktoolProperties.get("application.version");
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
        return ! stored.exists() || BrutIO.recursiveModifiedTime(working) > BrutIO .recursiveModifiedTime(stored);
    }

    private boolean isModified(File[] working, File[] stored) {
        for (int i = 0; i < stored.length; i++) {
            if (!stored[i].exists()) {
                return true;
            }
        }
        return BrutIO.recursiveModifiedTime(working) > BrutIO.recursiveModifiedTime(stored);
    }

    private File[] newFiles(String[] names, File dir) {
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(dir, names[i]);
        }
        return files;
    }

    private final static Logger LOGGER = Logger.getLogger(Androlib.class.getName());

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
            "classes.dex", "AndroidManifest.xml", "resources.arsc", "res", "lib", "libs", "assets", "META-INF" };
}

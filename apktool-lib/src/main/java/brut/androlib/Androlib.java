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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Androlib {
    private final AndrolibResources mAndRes = new AndrolibResources();

    public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
        return mAndRes.getResTable(apkFile);
    }

    public void decodeSourcesRaw(ExtFile apkFile, File outDir, boolean debug)
            throws AndrolibException {
        try {
            if (debug) {
                LOGGER.warning("Debug mode not available.");
            }
            Directory apk = apkFile.getDirectory();
            LOGGER.info("Copying raw classes.dex file...");
            apkFile.getDirectory().copyToDir(outDir, "classes.dex");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeSourcesSmali(File apkFile, File outDir, boolean debug)
            throws AndrolibException {
        try {
            File smaliDir = new File(outDir, SMALI_DIRNAME);
            OS.rmdir(smaliDir);
            smaliDir.mkdirs();
            LOGGER.info("Baksmaling...");
            SmaliDecoder.decode(apkFile, smaliDir, debug);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeSourcesJava(ExtFile apkFile, File outDir, boolean debug)
            throws AndrolibException {
        LOGGER.info("Decoding Java sources...");
        new AndrolibJava().decode(apkFile, outDir);
    }

    public void decodeResourcesRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            Directory apk = apkFile.getDirectory();
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
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void writeMetaFile(File mOutDir, Map<String, Object> meta)
            throws AndrolibException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//        options.setIndent(4);
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
                } catch (IOException ex) {}
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
                } catch (IOException ex) {}
            }
        }
    }

    public void build(File appDir, File outFile, boolean forceBuildAll,
            boolean debug) throws AndrolibException {
        build(new ExtFile(appDir), outFile, forceBuildAll, debug);
    }

    public void build(ExtFile appDir, File outFile, boolean forceBuildAll,
            boolean debug) throws AndrolibException {
        Map<String, Object> meta = readMetaFile(appDir);
        Object t1 = meta.get("isFrameworkApk");
        boolean framework = t1 == null ? false : (Boolean) t1;

        if (outFile == null) {
            String outFileName = (String) meta.get("apkFileName");
            outFile = new File(appDir, "dist" + File.separator +
                (outFileName == null ? "out.apk" : outFileName));
        }

        new File(appDir, APK_DIRNAME).mkdirs();
        buildSources(appDir, forceBuildAll, debug);
        buildResources(appDir, forceBuildAll, framework,
            (Map<String, Object>) meta.get("usesFramework"));
        buildLib(appDir, forceBuildAll);
        buildApk(appDir, outFile, framework);
    }

    public void buildSources(File appDir, boolean forceBuildAll, boolean debug)
            throws AndrolibException {
        if (! buildSourcesRaw(appDir, forceBuildAll, debug)
                && ! buildSourcesSmali(appDir, forceBuildAll, debug)
                && ! buildSourcesJava(appDir, forceBuildAll, debug)
            ) {
            LOGGER.warning("Could not find sources");
        }
    }

    public boolean buildSourcesRaw(File appDir, boolean forceBuildAll,
            boolean debug) throws AndrolibException {
        try {
            File working = new File(appDir, "classes.dex");
            if (! working.exists()) {
                return false;
            }
            if (debug) {
                LOGGER.warning("Debug mode not available.");
            }
            File stored = new File(appDir, APK_DIRNAME + "/classes.dex");
            if (forceBuildAll || isModified(working, stored)) {
                LOGGER.info("Copying classes.dex file...");
                BrutIO.copyAndClose(new FileInputStream(working),
                    new FileOutputStream(stored));
            }
            return true;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildSourcesSmali(File appDir, boolean forceBuildAll,
            boolean debug) throws AndrolibException {
        ExtFile smaliDir = new ExtFile(appDir, "smali");
        if (! smaliDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/classes.dex");
        if (! forceBuildAll) {
            LOGGER.info("Checking whether sources has changed...");
        }
        if (forceBuildAll || isModified(smaliDir, dex)) {
            LOGGER.info("Smaling...");
            dex.delete();
            SmaliBuilder.build(smaliDir, dex, debug);
        }
        return true;
    }

    public boolean buildSourcesJava(File appDir, boolean forceBuildAll,
            boolean debug) throws AndrolibException {
        File javaDir = new File(appDir, "src");
        if (! javaDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/classes.dex");
        if (! forceBuildAll) {
            LOGGER.info("Checking whether sources has changed...");
        }
        if (forceBuildAll || isModified(javaDir, dex)) {
            LOGGER.info("Building java sources...");
            dex.delete();
            new AndrolibJava().build(javaDir, dex);
        }
        return true;
    }

    public void buildResources(ExtFile appDir, boolean forceBuildAll,
            boolean framework, Map<String, Object> usesFramework)
            throws AndrolibException {
        if (! buildResourcesRaw(appDir, forceBuildAll)
                && ! buildResourcesFull(appDir, forceBuildAll, framework,
                    usesFramework)) {
            LOGGER.warning("Could not find resources");
        }
    }

    public boolean buildResourcesRaw(ExtFile appDir, boolean forceBuildAll)
            throws AndrolibException {
        try {
            if (! new File(appDir, "resources.arsc").exists()) {
                return false;
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (! forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            if (forceBuildAll || isModified(
                    newFiles(APK_RESOURCES_FILENAMES, appDir),
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

    public boolean buildResourcesFull(File appDir, boolean forceBuildAll,
            boolean framework, Map<String, Object> usesFramework)
            throws AndrolibException {
        try {
            if (! new File(appDir, "res").exists()) {
                return false;
            }
            if (! forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (forceBuildAll || isModified(
                    newFiles(APP_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
                LOGGER.info("Building resources...");

                File apkFile = File.createTempFile("APKTOOL", null);
                apkFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (! ninePatch.exists()) {
                    ninePatch = null;
                }
                mAndRes.aaptPackage(
                    apkFile,
                    new File(appDir, "AndroidManifest.xml"),
                    new File(appDir, "res"),
                    ninePatch, null, parseUsesFramework(usesFramework),
                    false, framework
                );

                Directory tmpDir = new ExtFile(apkFile).getDirectory();
                tmpDir.copyToDir(apkDir,
                    tmpDir.containsDir("res") ? APK_RESOURCES_FILENAMES :
                    APK_RESOURCES_WITHOUT_RES_FILENAMES);
            }
            return true;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void buildLib(File appDir, boolean forceBuildAll)
            throws AndrolibException {
        File working = new File(appDir, "lib");
        if (! working.exists()) {
            return;
        }
        File stored = new File(appDir, APK_DIRNAME + "/lib");
        if (forceBuildAll || isModified(working, stored)) {
            LOGGER.info("Copying libs...");
            try {
                OS.rmdir(stored);
                OS.cpdir(working, stored);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    public void buildApk(File appDir, File outApk, boolean framework)
            throws AndrolibException {
        LOGGER.info("Building apk file...");
        if (outApk.exists()) {
            outApk.delete();
        } else {
            File outDir = outApk.getParentFile();
            if (outDir != null && ! outDir.exists()) {
                outDir.mkdirs();
            }
        }
        File assetDir = new File(appDir, "assets");
        if (! assetDir.exists()) {
            assetDir = null;
        }
        mAndRes.aaptPackage(outApk, null, null,
            new File(appDir, APK_DIRNAME), assetDir, null, false, framework);
    }

    public void publicizeResources(File arscFile) throws AndrolibException {
        mAndRes.publicizeResources(arscFile);
    }

    public void installFramework(File frameFile, String tag)
            throws AndrolibException {
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
        String version = ApktoolProperties.get("version");
        return version.endsWith("-SNAPSHOT") ?
                version.substring(0, version.length() - 9) + '.' +
                    ApktoolProperties.get("git.commit.id.abbrev")
                : version;
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
        if (! stored.exists()) {
            return true;
        }
        return BrutIO.recursiveModifiedTime(working) >
            BrutIO.recursiveModifiedTime(stored);
    }

    private boolean isModified(File[] working, File[] stored) {
        for (int i = 0; i < stored.length; i++) {
            if (! stored[i].exists()) {
                return true;
            }
        }
        return BrutIO.recursiveModifiedTime(working) >
            BrutIO.recursiveModifiedTime(stored);
    }

    private File[] newFiles(String[] names, File dir) {
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(dir, names[i]);
        }
        return files;
    }

    private final static Logger LOGGER =
        Logger.getLogger(Androlib.class.getName());

    private final static String SMALI_DIRNAME = "smali";
    private final static String APK_DIRNAME = "build/apk";
    private final static String[] APK_RESOURCES_FILENAMES =
        new String[]{"resources.arsc", "AndroidManifest.xml", "res"};
    private final static String[] APK_RESOURCES_WITHOUT_RES_FILENAMES =
        new String[]{"resources.arsc", "AndroidManifest.xml"};
    private final static String[] APP_RESOURCES_FILENAMES =
        new String[]{"AndroidManifest.xml", "res"};
}

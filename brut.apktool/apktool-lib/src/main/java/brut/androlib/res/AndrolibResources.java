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

package brut.androlib.res;

import brut.androlib.AndrolibException;
import brut.androlib.ApkOptions;
import brut.androlib.err.CantFindFrameworkResException;
import brut.androlib.res.data.*;
import brut.androlib.res.decoder.*;
import brut.androlib.res.decoder.ARSCDecoder.ARSCData;
import brut.androlib.res.decoder.ARSCDecoder.FlagsOffset;
import brut.androlib.res.util.*;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.common.BrutException;
import brut.directory.*;
import brut.util.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.*;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
final public class AndrolibResources {
    public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
        return getResTable(apkFile, true);
    }

    public ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
            throws AndrolibException {
        ResTable resTable = new ResTable(this);
        if (loadMainPkg) {
            loadMainPkg(resTable, apkFile);
        }
        return resTable;
    }

    public ResPackage loadMainPkg(ResTable resTable, ExtFile apkFile)
            throws AndrolibException {
        LOGGER.info("Loading resource table...");
        ResPackage[] pkgs = getResPackagesFromApk(apkFile, resTable, sKeepBroken);
        ResPackage pkg = null;

        switch (pkgs.length) {
            case 1:
                pkg = pkgs[0];
                break;
            case 2:
                if (pkgs[0].getName().equals("android")) {
                    LOGGER.warning("Skipping \"android\" package group");
                    pkg = pkgs[1];
                    break;
                } else if (pkgs[0].getName().equals("com.htc")) {
                    LOGGER.warning("Skipping \"htc\" package group");
                    pkg = pkgs[1];
                    break;
                }

            default:
                pkg = selectPkgWithMostResSpecs(pkgs);
                break;
        }

        if (pkg == null) {
            throw new AndrolibException("arsc files with zero packages or no arsc file found.");
        }

        resTable.addPackage(pkg, true);
        return pkg;
    }

    public ResPackage selectPkgWithMostResSpecs(ResPackage[] pkgs)
        throws AndrolibException {
        int id = 0;
        int value = 0;

        for (ResPackage resPackage : pkgs) {
            if (resPackage.getResSpecCount() > value && ! resPackage.getName().equalsIgnoreCase("android")) {
                value = resPackage.getResSpecCount();
                id = resPackage.getId();
            }
        }

        // if id is still 0, we only have one pkgId which is "android" -> 1
        return (id == 0) ? pkgs[0] : pkgs[1];
    }

    public ResPackage loadFrameworkPkg(ResTable resTable, int id, String frameTag)
            throws AndrolibException {
        File apk = getFrameworkApk(id, frameTag);

        LOGGER.info("Loading resource table from file: " + apk);
        ResPackage[] pkgs = getResPackagesFromApk(new ExtFile(apk), resTable, true);

        ResPackage pkg;
        if (pkgs.length > 1) {
            pkg = selectPkgWithMostResSpecs(pkgs);
        } else if (pkgs.length == 0) {
            throw new AndrolibException("Arsc files with zero or multiple packages");
        } else {
            pkg = pkgs[0];
        }

        if (pkg.getId() != id) {
            throw new AndrolibException("Expected pkg of id: " + String.valueOf(id) + ", got: " + pkg.getId());
        }

        resTable.addPackage(pkg, false);
        return pkg;
    }

    public void decodeManifest(ResTable resTable, ExtFile apkFile, File outDir)
            throws AndrolibException {

        Duo<ResFileDecoder, AXmlResourceParser> duo = getManifestFileDecoder();
        ResFileDecoder fileDecoder = duo.m1;

        // Set ResAttrDecoder
        duo.m2.setAttrDecoder(new ResAttrDecoder());
        ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

        // Fake ResPackage
        attrDecoder.setCurrentPackage(new ResPackage(resTable, 0, null));

        Directory inApk, out;
        try {
            inApk = apkFile.getDirectory();
            out = new FileDirectory(outDir);

            LOGGER.info("Decoding AndroidManifest.xml with only framework resources...");
            fileDecoder.decodeManifest(inApk, "AndroidManifest.xml", out, "AndroidManifest.xml");

        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void remove_application_debug(String filePath)
            throws AndrolibException {

        // change application:debug to true
        try {
            Document doc = loadDocument(filePath);
            Node application = doc.getElementById("application");

            // load attr
            NamedNodeMap attr = application.getAttributes();
            Node debugAttr = attr.getNamedItem("debug");

            // remove application:debug
            if (debugAttr != null) {
                attr.removeNamedItem("debug");
            }

            saveDocument(filePath, doc);

        } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
        }
    }

    public void adjust_package_manifest(ResTable resTable, String filePath)
            throws AndrolibException {

        // compare resources.arsc package name to the one present in AndroidManifest
        ResPackage resPackage = resTable.getCurrentResPackage();
        mPackageOriginal = resPackage.getName();
        mPackageRenamed = resTable.getPackageRenamed();

        resTable.setPackageId(resPackage.getId());
        resTable.setPackageOriginal(mPackageOriginal);

        // 1) Check if mPackageOriginal === mPackageRenamed
        // 2) Check if mPackageOriginal is ignored via IGNORED_PACKAGES
        // 2a) If its ignored, make sure the mPackageRenamed isn't explicitly allowed
        if (mPackageOriginal.equalsIgnoreCase(mPackageRenamed) ||
                (Arrays.asList(IGNORED_PACKAGES).contains(mPackageOriginal) &&
                ! Arrays.asList(ALLOWED_PACKAGES).contains(mPackageRenamed))) {
            LOGGER.info("Regular manifest package...");
        } else {
            try {
                LOGGER.info("Renamed manifest package found! Replacing " + mPackageRenamed + " with " + mPackageOriginal);
                Document doc = loadDocument(filePath);

                // Get the manifest line
                Node manifest = doc.getFirstChild();

                // update package attribute
                NamedNodeMap attr = manifest.getAttributes();
                Node nodeAttr = attr.getNamedItem("package");
                nodeAttr.setNodeValue(mPackageOriginal);
                saveDocument(filePath, doc);

            } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
            }
        }
    }

    public void remove_manifest_versions(String filePath)
            throws AndrolibException {

        File f = new File(filePath);

        if (f.exists()) {
            try {
                Document doc = loadDocument(filePath);
                Node manifest = doc.getFirstChild();

                // load attr
                NamedNodeMap attr = manifest.getAttributes();
                Node vCode = attr.getNamedItem("android:versionCode");
                Node vName = attr.getNamedItem("android:versionName");

                // remove versionCode
                if (vCode != null) {
                    attr.removeNamedItem("android:versionCode");
                }
                if (vName != null) {
                    attr.removeNamedItem("android:versionName");
                }
                saveDocument(filePath, doc);

            } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
            }
        }
    }

    private Document loadDocument(String filename)
            throws IOException, SAXException, ParserConfigurationException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        return docBuilder.parse(filename);
    }

    private void saveDocument(String filename, Document doc)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE,"yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filename));
        transformer.transform(source, result);
    }

    public void decodeManifestWithResources(ResTable resTable, ExtFile apkFile, File outDir)
            throws AndrolibException {

        Duo<ResFileDecoder, AXmlResourceParser> duo = getResFileDecoder();
        ResFileDecoder fileDecoder = duo.m1;
        ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

        attrDecoder.setCurrentPackage(resTable.listMainPackages().iterator().next());

        Directory inApk, in = null, out;
        try {
            inApk = apkFile.getDirectory();
            out = new FileDirectory(outDir);
            LOGGER.info("Decoding AndroidManifest.xml with resources...");

            fileDecoder.decodeManifest(inApk, "AndroidManifest.xml", out, "AndroidManifest.xml");

            // Remove versionName / versionCode (aapt API 16)
            if (!resTable.getAnalysisMode()) {

                // check for a mismatch between resources.arsc package and the package listed in AndroidManifest
                // also remove the android::versionCode / versionName from manifest for rebuild
                // this is a required change to prevent aapt warning about conflicting versions
                // it will be passed as a parameter to aapt like "--min-sdk-version" via apktool.yml
                adjust_package_manifest(resTable, outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");
                remove_manifest_versions(outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");
                mPackageId = String.valueOf(resTable.getPackageId());
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decode(ResTable resTable, ExtFile apkFile, File outDir)
            throws AndrolibException {
        Duo<ResFileDecoder, AXmlResourceParser> duo = getResFileDecoder();
        ResFileDecoder fileDecoder = duo.m1;
        ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

        attrDecoder.setCurrentPackage(resTable.listMainPackages().iterator().next());
        Directory inApk, in = null, out;

        try {
            out = new FileDirectory(outDir);

            inApk = apkFile.getDirectory();
            if (inApk.containsDir("res")) {
                in = inApk.getDir("res");
            }
            out = out.createDir("res");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        ExtMXSerializer xmlSerializer = getResXmlSerializer();
        for (ResPackage pkg : resTable.listMainPackages()) {
            attrDecoder.setCurrentPackage(pkg);

            LOGGER.info("Decoding file-resources...");
            for (ResResource res : pkg.listFiles()) {
                fileDecoder.decode(res, in, out);
            }

            LOGGER.info("Decoding values */* XMLs...");
            for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
                generateValuesFile(valuesFile, out, xmlSerializer);
            }
            generatePublicXml(pkg, out, xmlSerializer);
        }

        AndrolibException decodeError = duo.m2.getFirstError();
        if (decodeError != null) {
            throw decodeError;
        }
    }

    public void setSdkInfo(Map<String, String> map) {
        if (map != null) {
            mMinSdkVersion = map.get("minSdkVersion");
            mTargetSdkVersion = map.get("targetSdkVersion");
            mMaxSdkVersion = map.get("maxSdkVersion");
        }
    }

    public void setVersionInfo(Map<String, String> map) {
        if (map != null) {
            mVersionCode = map.get("versionCode");
            mVersionName = map.get("versionName");
        }
    }

    public void setPackageInfo(Map<String, String> map) {
        if (map != null) {
            mPackageRenamed = map.get("rename-manifest-package");
        }
    }

    public void setPackageId(Map<String, String> map) {
        if (map != null) {
            mPackageId = map.get("forced-package-id");
        }
    }

    public void setSharedLibrary(boolean flag) {
        mSharedLibrary = flag;
    }

    public void aaptPackage(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include)
            throws AndrolibException {

        boolean customAapt = false;
        String aaptPath = apkOptions.aaptPath;
        List<String> cmd = new ArrayList<String>();

        // path for aapt binary
        if (! aaptPath.isEmpty()) {
            File aaptFile = new File(aaptPath);
            if (aaptFile.canRead() && aaptFile.exists()) {
                aaptFile.setExecutable(true);
                cmd.add(aaptFile.getPath());
                customAapt = true;

                if (apkOptions.verbose) {
                    LOGGER.info(aaptFile.getPath() + " being used as aapt location.");
                }
            } else {
                LOGGER.warning("aapt location could not be found. Defaulting back to default");

                try {
                    cmd.add(getAaptBinaryFile().getAbsolutePath());
                } catch (BrutException ignored) {
                    cmd.add("aapt");
                }
            }
        } else {
            try {
                cmd.add(getAaptBinaryFile().getAbsolutePath());
            } catch (BrutException ignored) {
                cmd.add("aapt");
            }
        }

        cmd.add("p");

        if (apkOptions.verbose) { // output aapt verbose
            cmd.add("-v");
        }
        if (apkOptions.updateFiles) {
            cmd.add("-u");
        }
        if (apkOptions.debugMode) { // inject debuggable="true" into manifest
            cmd.add("--debug-mode");
        }

        // force package id so that some frameworks build with correct id
        // disable if user adds own aapt (can't know if they have this feature)
        if (mPackageId != null && ! customAapt && ! mSharedLibrary) {
            cmd.add("--forced-package-id");
            cmd.add(mPackageId);
        }
        if (mSharedLibrary) {
            cmd.add("--shared-lib");
        }
        if (mMinSdkVersion != null) {
            cmd.add("--min-sdk-version");
            cmd.add(mMinSdkVersion);
        }
        if (mTargetSdkVersion != null) {
            cmd.add("--target-sdk-version");
            cmd.add(mTargetSdkVersion);
        }
        if (mMaxSdkVersion != null) {
            cmd.add("--max-sdk-version");
            cmd.add(mMaxSdkVersion);

            // if we have max sdk version, set --max-res-version
            // so we can ignore anything over that during build.
            cmd.add("--max-res-version");
            cmd.add(mMaxSdkVersion);
        }
        if (mPackageRenamed != null) {
            cmd.add("--rename-manifest-package");
            cmd.add(mPackageRenamed);
        }
        if (mVersionCode != null) {
            cmd.add("--version-code");
            cmd.add(mVersionCode);
        }
        if (mVersionName != null) {
            cmd.add("--version-name");
            cmd.add(mVersionName);
        }
        cmd.add("-F");
        cmd.add(apkFile.getAbsolutePath());

        if (apkOptions.isFramework) {
            cmd.add("-x");
        }

        if (! apkOptions.resourcesAreCompressed) {
            cmd.add("-0");
            cmd.add("arsc");
        }

        if (include != null) {
            for (File file : include) {
                cmd.add("-I");
                cmd.add(file.getPath());
            }
        }
        if (resDir != null) {
            cmd.add("-S");
            cmd.add(resDir.getAbsolutePath());
        }
        if (manifest != null) {
            cmd.add("-M");
            cmd.add(manifest.getAbsolutePath());
        }
        if (assetDir != null) {
            cmd.add("-A");
            cmd.add(assetDir.getAbsolutePath());
        }
        if (rawDir != null) {
            cmd.add(rawDir.getAbsolutePath());
        }
        try {
            OS.exec(cmd.toArray(new String[0]));
            if (apkOptions.verbose) {
                LOGGER.info("command ran: ");
                LOGGER.info(cmd.toString());
            }
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean detectWhetherAppIsFramework(File appDir)
            throws AndrolibException {
        File publicXml = new File(appDir, "res/values/public.xml");
        if (! publicXml.exists()) {
            return false;
        }

        Iterator<String> it;
        try {
            it = IOUtils.lineIterator(new FileReader(new File(appDir,
                    "res/values/public.xml")));
        } catch (FileNotFoundException ex) {
            throw new AndrolibException(
                    "Could not detect whether app is framework one", ex);
        }
        it.next();
        it.next();
        return it.next().contains("0x01");
    }

    public void tagSmaliResIDs(ResTable resTable, File smaliDir)
            throws AndrolibException {
        new ResSmaliUpdater().tagResIDs(resTable, smaliDir);
    }

    public void updateSmaliResIDs(ResTable resTable, File smaliDir)
            throws AndrolibException {
        new ResSmaliUpdater().updateResIDs(resTable, smaliDir);
    }

    public Duo<ResFileDecoder, AXmlResourceParser> getResFileDecoder() {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());
        decoders.setDecoder("9patch", new Res9patchStreamDecoder());

        AXmlResourceParser axmlParser = new AXmlResourceParser();
        axmlParser.setAttrDecoder(new ResAttrDecoder());
        decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser, getResXmlSerializer()));

        return new Duo<ResFileDecoder, AXmlResourceParser>(new ResFileDecoder(decoders), axmlParser);
    }

    public Duo<ResFileDecoder, AXmlResourceParser> getManifestFileDecoder() {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();

        AXmlResourceParser axmlParser = new AXmlResourceParser();

        decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser,getResXmlSerializer()));

        return new Duo<ResFileDecoder, AXmlResourceParser>(new ResFileDecoder(decoders), axmlParser);
    }

    public ExtMXSerializer getResXmlSerializer() {
        ExtMXSerializer serial = new ExtMXSerializer();
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_INDENTATION, "    ");
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_LINE_SEPARATOR, System.getProperty("line.separator"));
        serial.setProperty(ExtXmlSerializer.PROPERTY_DEFAULT_ENCODING, "utf-8");
        serial.setDisabledAttrEscape(true);
        return serial;
    }

    private void generateValuesFile(ResValuesFile valuesFile, Directory out,
                                    ExtXmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput(valuesFile.getPath());
            serial.setOutput((outStream), null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResResource res : valuesFile.listResources()) {
                if (valuesFile.isSynthesized(res)) {
                    continue;
                }
                ((ResValuesXmlSerializable) res.getValue()).serializeToResValuesXml(serial, res);
            }

            serial.endTag(null, "resources");
            serial.newLine();
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException("Could not generate: " + valuesFile.getPath(), ex);
        }
    }

    private void generatePublicXml(ResPackage pkg, Directory out,
                                   XmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput("values/public.xml");
            serial.setOutput(outStream, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResResSpec spec : pkg.listResSpecs()) {
                serial.startTag(null, "public");
                serial.attribute(null, "type", spec.getType().getName());
                serial.attribute(null, "name", spec.getName());
                serial.attribute(null, "id", String.format("0x%08x", spec.getId().id));
                serial.endTag(null, "public");
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException("Could not generate public.xml file", ex);
        }
    }

    private ResPackage[] getResPackagesFromApk(ExtFile apkFile,ResTable resTable, boolean keepBroken)
            throws AndrolibException {
        try {
            BufferedInputStream bfi = new BufferedInputStream(apkFile.getDirectory().getFileInput("resources.arsc"));
            return ARSCDecoder.decode(bfi, false, keepBroken, resTable).getPackages();
        } catch (DirectoryException ex) {
            throw new AndrolibException("Could not load resources.arsc from file: " + apkFile, ex);
        }
    }

    public File getFrameworkApk(int id, String frameTag)
            throws AndrolibException {
        File dir = getFrameworkDir();
        File apk;

        if (frameTag != null) {
            apk = new File(dir, String.valueOf(id) + '-' + frameTag + ".apk");
            if (apk.exists()) {
                return apk;
            }
        }

        apk = new File(dir, String.valueOf(id) + ".apk");
        if (apk.exists()) {
            return apk;
        }

        if (id == 1) {
            try (InputStream in = AndrolibResources.class.getResourceAsStream("/brut/androlib/android-framework.jar");
                 OutputStream out = new FileOutputStream(apk)) {
                IOUtils.copy(in, out);
                return apk;
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
        }

        throw new CantFindFrameworkResException(id);
    }

    public void installFramework(File frameFile) throws AndrolibException {
        installFramework(frameFile, apkOptions.frameworkTag);
    }

    public void installFramework(File frameFile, String tag)
            throws AndrolibException {
        InputStream in = null;
        ZipOutputStream out = null;
        try {
            ZipFile zip = new ZipFile(frameFile);
            ZipEntry entry = zip.getEntry("resources.arsc");

            if (entry == null) {
                throw new AndrolibException("Can't find resources.arsc file");
            }

            in = zip.getInputStream(entry);
            byte[] data = IOUtils.toByteArray(in);

            ARSCData arsc = ARSCDecoder.decode(new ByteArrayInputStream(data), true, true);
            publicizeResources(data, arsc.getFlagsOffsets());

            File outFile = new File(getFrameworkDir(), String.valueOf(arsc
                    .getOnePackage().getId())
                    + (tag == null ? "" : '-' + tag)
                    + ".apk");

            out = new ZipOutputStream(new FileOutputStream(outFile));
            out.setMethod(ZipOutputStream.STORED);
            CRC32 crc = new CRC32();
            crc.update(data);
            entry = new ZipEntry("resources.arsc");
            entry.setSize(data.length);
            entry.setCrc(crc.getValue());
            out.putNextEntry(entry);
            out.write(data);
            zip.close();
            LOGGER.info("Framework installed to: " + outFile);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    public void publicizeResources(File arscFile) throws AndrolibException {
        byte[] data = new byte[(int) arscFile.length()];

        try(InputStream in = new FileInputStream(arscFile);
            OutputStream out = new FileOutputStream(arscFile)) {
            in.read(data);
            publicizeResources(data);
            out.write(data);
        } catch (IOException ex){
            throw new AndrolibException(ex);
        }
    }

    public void publicizeResources(byte[] arsc) throws AndrolibException {
        publicizeResources(arsc, ARSCDecoder.decode(new ByteArrayInputStream(arsc), true, true).getFlagsOffsets());
    }

    public void publicizeResources(byte[] arsc, FlagsOffset[] flagsOffsets)
            throws AndrolibException {
        for (FlagsOffset flags : flagsOffsets) {
            int offset = flags.offset + 3;
            int end = offset + 4 * flags.count;
            while (offset < end) {
                arsc[offset] |= (byte) 0x40;
                offset += 4;
            }
        }
    }

    private File getFrameworkDir() throws AndrolibException {
        if (mFrameworkDirectory != null) {
            return mFrameworkDirectory;
        }

        String path;

        // if a framework path was specified on the command line, use it
        if (apkOptions.frameworkFolderLocation != null) {
            path = apkOptions.frameworkFolderLocation;
        } else {
            File parentPath = new File(System.getProperty("user.home"));
            if (! parentPath.canWrite()) {
                LOGGER.severe(String.format("WARNING: Could not write to $HOME (%s), using %s instead...",
                        parentPath.getAbsolutePath(), System.getProperty("java.io.tmpdir")));
                LOGGER.severe("Please be aware this is a volatile directory and frameworks could go missing, " +
                        "please utilize --frame-path if the default storage directory is unavailable");

                parentPath = new File(System.getProperty("java.io.tmpdir"));
            }

            if (OSDetection.isMacOSX()) {
                path = parentPath.getAbsolutePath() + String.format("%1$sLibrary%1$sapktool%1$sframework", File.separatorChar);
            } else {
                path = parentPath.getAbsolutePath() + String.format("%1$sapktool%1$sframework", File.separatorChar);
            }
        }

        File dir = new File(path);

        if (dir.getParentFile() != null && dir.getParentFile().isFile()) {
            LOGGER.severe("Please remove file at " + dir.getParentFile());
            System.exit(1);
        }

        if (! dir.exists()) {
            if (! dir.mkdirs()) {
                if (apkOptions.frameworkFolderLocation != null) {
                    LOGGER.severe("Can't create Framework directory: " + dir);
                }
                throw new AndrolibException("Can't create directory: " + dir);
            }
        }

        mFrameworkDirectory = dir;
        return dir;
    }

    /**
     * Using a prebuilt aapt and forcing its use, allows us to prevent bugs from older aapt's
     * along with having a finer control over the build procedure.
     *
     * Aapt can still be overridden via --aapt/-a on build, but specific features will be disabled
     *
     * @url https://github.com/iBotPeaches/platform_frameworks_base
     * @throws AndrolibException
     */
    public File getAaptBinaryFile() throws AndrolibException {
        try {
            if (OSDetection.isMacOSX()) {
                mAaptBinary = Jar.getResourceAsFile("/prebuilt/aapt/macosx/aapt");
            } else if (OSDetection.isUnix()) {
                mAaptBinary = Jar.getResourceAsFile("/prebuilt/aapt/linux/aapt");
            } else if (OSDetection.isWindows()) {
                mAaptBinary = Jar.getResourceAsFile("/prebuilt/aapt/windows/aapt.exe");
            } else {
                LOGGER.warning("Unknown Operating System: " + OSDetection.returnOS());
                return null;
            }
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
        if (mAaptBinary.setExecutable(true)) {
            return mAaptBinary;
        }

        System.err.println("Can't set aapt binary as executable");
        throw new AndrolibException("Can't set aapt binary as executable");
    }

    public File getAndroidResourcesFile() throws AndrolibException {
        try {
            return Jar.getResourceAsFile("/brut/androlib/android-framework.jar");
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public ApkOptions apkOptions;

    // TODO: dirty static hack. I have to refactor decoding mechanisms.
    public static boolean sKeepBroken = false;

    private final static Logger LOGGER = Logger.getLogger(AndrolibResources.class.getName());

    private File mFrameworkDirectory = null;

    private String mMinSdkVersion = null;
    private String mMaxSdkVersion = null;
    private String mTargetSdkVersion = null;
    private String mVersionCode = null;
    private String mVersionName = null;
    private String mPackageRenamed = null;
    private String mPackageOriginal = null;
    private String mPackageId = null;

    private boolean mSharedLibrary = false;

    private File mAaptBinary = null;

    private final static String[] IGNORED_PACKAGES = new String[] {
            "android", "com.htc", "miui", "com.lge", "com.lge.internal", "yi" };

    private final static String[] ALLOWED_PACKAGES = new String[] {
            "com.miui" };
}

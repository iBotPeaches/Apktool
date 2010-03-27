/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib.res;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.*;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResXmlSerializable;
import brut.androlib.res.decoder.*;
import brut.androlib.res.util.ExtFile;
import brut.androlib.res.util.ExtMXSerializer;
import brut.common.BrutException;
import brut.directory.*;
import brut.util.*;
import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
final public class AndrolibResources {
    public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
        ResTable resTable = new ResTable();
        decodeArsc(resTable, new ExtFile(getAndroidResourcesFile()), false);
        decodeArsc(resTable, apkFile, true);
        return resTable;
    }

    public void decode(ResTable resTable, ExtFile apkFile, File outDir)
            throws AndrolibException {
        Duo<ResFileDecoder, ResAttrDecoder> duo = getResFileDecoder();
        ResFileDecoder fileDecoder = duo.m1;
        ResAttrDecoder attrDecoder = duo.m2;

        attrDecoder.setCurrentPackage(
            resTable.listMainPackages().iterator().next());

        Directory in, out;
        try {
            in = apkFile.getDirectory();
            out = new FileDirectory(outDir);

            fileDecoder.decode(
                in, "AndroidManifest.xml", out, "AndroidManifest.xml", "xml");

            in = in.getDir("res");
            out = out.createDir("res");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        ExtMXSerializer xmlSerializer = getResXmlSerializer();
        for (ResPackage pkg : resTable.listMainPackages()) {
            attrDecoder.setCurrentPackage(pkg);
            for (ResResource res : pkg.listFiles()) {
                ResFileValue fileValue = (ResFileValue) res.getValue();
                fileDecoder.decode(in, fileValue.getStrippedPath(),
                    out, res.getFilePath());
            }
            for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
                generateValuesFile(valuesFile, out, xmlSerializer);
            }
            generatePublicXml(pkg, out, xmlSerializer);
        }
    }

    public void aaptPackage(File apkFile, File manifest, File resDir)
            throws AndrolibException {
        aaptPackage(apkFile, manifest, resDir, null, null, false);
    }

    public void aaptPackage(File apkFile, File manifest, File resDir,
            File rawDir, File assetDir, boolean update)
            throws AndrolibException {
        String[] cmd = new String[13];
        int i = 0;
        cmd[i++] = "aapt";
        cmd[i++] = "p";
        if (update) {
            cmd[i++] = "-u";
        }
        cmd[i++] = "-F";
        cmd[i++] = apkFile.getAbsolutePath();
        cmd[i++] = "-I";
        cmd[i++] = getAndroidResourcesFile().getAbsolutePath();
        if (manifest != null) {
            cmd[i++] = "-M";
            cmd[i++] = manifest.getAbsolutePath();
        }
        if (resDir != null) {
            cmd[i++] = "-S";
            cmd[i++] = resDir.getAbsolutePath();
        }
        if (assetDir != null) {
            cmd[i++] = "-A";
            cmd[i++] = assetDir.getAbsolutePath();
        }
        if (rawDir != null) {
            cmd[i++] = rawDir.getAbsolutePath();
        }

        try {
            OS.exec(Arrays.copyOf(cmd, i));
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void tagSmaliResIDs(ResTable resTable, File smaliDir)
            throws AndrolibException {
        new ResSmaliUpdater().tagResIDs(resTable, smaliDir);
    }

    public void updateSmaliResIDs(ResTable resTable, File smaliDir) throws AndrolibException {
        new ResSmaliUpdater().updateResIDs(resTable, smaliDir);
    }

    public Duo<ResFileDecoder, ResAttrDecoder> getResFileDecoder() {
        ResStreamDecoderContainer decoders =
            new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());

        ResAttrDecoder attrDecoder = new ResAttrDecoder();
        AXmlResourceParser axmlParser = new AXmlResourceParser();
        axmlParser.setAttrDecoder(attrDecoder);
        decoders.setDecoder("xml",
            new XmlPullStreamDecoder(axmlParser, getResXmlSerializer()));

        return new Duo<ResFileDecoder, ResAttrDecoder>(
            new ResFileDecoder(decoders), attrDecoder);
    }

    public ExtMXSerializer getResXmlSerializer() {
        ExtMXSerializer serial = new ExtMXSerializer();
        serial.setProperty(serial.PROPERTY_SERIALIZER_INDENTATION, "    ");
        serial.setProperty(serial.PROPERTY_SERIALIZER_LINE_SEPARATOR,
            System.getProperty("line.separator"));
        serial.setProperty(ExtMXSerializer.PROPERTY_DEFAULT_ENCODING, "UTF-8");
        return serial;
    }

    private void generateValuesFile(ResValuesFile valuesFile, Directory out,
            XmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput(valuesFile.getPath());
            serial.setOutput((outStream), null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResResource res : valuesFile.listResources()) {

                ((ResXmlSerializable) res.getValue())
                    .serializeToXml(serial, res);
            }
            
            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (IOException ex) {
            throw new AndrolibException(
                "Could not generate: " + valuesFile.getPath(), ex);
        } catch (DirectoryException ex) {
            throw new AndrolibException(
                "Could not generate: " + valuesFile.getPath(), ex);
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
                serial.attribute(null, "id", String.format(
                    "0x%08x", spec.getId().id));
                serial.endTag(null, "public");
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (IOException ex) {
            throw new AndrolibException(
                "Could not generate public.xml file", ex);
        } catch (DirectoryException ex) {
            throw new AndrolibException(
                "Could not generate public.xml file", ex);
        }
    }

    private void decodeArsc(ResTable resTable, ExtFile apkFile, boolean main)
            throws AndrolibException {
        try {
            loadArsc(resTable, apkFile.getDirectory()
                .getFileInput("resources.arsc"), main);
        } catch (DirectoryException ex) {
            throw new AndrolibException(
                "Could not load resources.arsc from file: " + apkFile, ex);
        }

    }

    private void loadArsc(ResTable resTable, InputStream arscStream,
            boolean main) throws AndrolibException {
        ResPackage[] groups = ARSCDecoder.decode(arscStream, resTable);
        
        if (groups.length == 0) {
            throw new AndrolibException(
                "Arsc file with zero package groups");
        }
        if (groups.length > 1) {
            LOGGER.warning("Arsc file with multiple package groups");
        }
        for (int i = 0; i < groups.length; i++) {
            if (groups.length != 1 && i == 0
                    && "android".equals(groups[i].getName())) {
                LOGGER.warning("Skipping \"android\" package group");
                continue;
            }
            resTable.addPackage(groups[i], main);
        }
    }

    private File getAndroidResourcesFile() throws AndrolibException {
        try {
            return Jar.getResourceAsFile("/brut/androlib/android-framework.jar");
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public static String escapeForResXml(String value) {
        value = value.replace("'", "\\'");
        value = value.replace("\n", "\\n\n");
        char c = value.charAt(0);
        if (c == '@' || c == '#' || c == '?') {
            return '\\' + value;
        }
        return value;
    }

    private final static Logger LOGGER =
        Logger.getLogger(AndrolibResources.class.getName());
}

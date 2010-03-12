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

import brut.androlib.*;
import brut.androlib.res.data.*;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResXmlSerializable;
import brut.androlib.res.decoder.*;
import brut.androlib.res.jni.JniPackage;
import brut.androlib.res.jni.JniPackageGroup;
import brut.common.BrutException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import brut.directory.ZipRODirectory;
import brut.util.Jar;
import brut.util.OS;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
final public class AndrolibResources {
    static {
        Jar.load("/libAndroid.so");
    }

    public ResTable getResTable(File apkFile) throws AndrolibException {
        ResTable resTable = new ResTable();
        loadApk(resTable, getAndroidResourcesFile(), false);
        loadApk(resTable, apkFile, true);
        return resTable;
    }

    public void decode(ResTable resTable, File apkFile, File outDir)
            throws AndrolibException {
        ResXmlSerializer serial = getResXmlSerializer(resTable);
        ResFileDecoder fileDecoder = getResFileDecoder(serial);
        serial.setCurrentPackage(
            resTable.listMainPackages().iterator().next());

        Directory in, out;
        try {
            in = new ZipRODirectory(apkFile);
            out = new FileDirectory(outDir);

            fileDecoder.decode(
                in, "AndroidManifest.xml", out, "AndroidManifest.xml", "xml");

            in = in.getDir("res");
            out = out.createDir("res");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        for (ResPackage pkg : resTable.listMainPackages()) {
            serial.setCurrentPackage(pkg);
            for (ResResource res : pkg.listFiles()) {
                ResFileValue fileValue = (ResFileValue) res.getValue();
                fileDecoder.decode(in, fileValue.getStrippedPath(),
                    out, res.getFilePath());
            }
            for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
                generateValuesFile(valuesFile, out, serial);
            }
        }
    }

    public void aaptPackage(File apkFile, File manifest, File resDir)
            throws AndrolibException {
        aaptPackage(apkFile, manifest, resDir, null, false);
    }

    public void aaptPackage(File apkFile, File manifest, File resDir,
            File rawDir, boolean update) throws AndrolibException {
        String[] cmd = new String[12];
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

    public ResFileDecoder getResFileDecoder(ResXmlSerializer serializer) {
        ResStreamDecoderContainer decoders =
            new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());
        decoders.setDecoder("xml",
            new ResXmlStreamDecoder(serializer));
        return new ResFileDecoder(decoders);
    }

    public ResXmlSerializer getResXmlSerializer(ResTable resTable) {
        ResXmlSerializer serial = new ResXmlSerializer();
        serial.setProperty(serial.PROPERTY_SERIALIZER_INDENTATION, "    ");
        return serial;
    }

    private void generateValuesFile(ResValuesFile valuesFile, Directory out,
            ResXmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput(valuesFile.getPath());
            serial.setOutput((outStream), null);
            serial.setDecodingEnabled(false);
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

    private void loadApk(ResTable resTable, File apkFile, boolean main)
            throws AndrolibException {
        JniPackageGroup[] groups =
            nativeGetPackageGroups(apkFile.getAbsolutePath());
        if (groups.length != 1) {
            throw new AndrolibException(
                "Apk's with multiple or zero package groups not supported");
        }
        for (int i = 0; i < groups.length; i++) {
//            if (groups.length != 1 && i == 0) {
//                continue;
//            }
            for (JniPackage jniPkg : groups[i].packages) {
                ResPackage pkg = new JniPackageDecoder().decode(jniPkg, resTable);
                resTable.addPackage(pkg, main);
            }
        }
    }

    private File getAndroidResourcesFile() {
        return new File(getClass().getProtectionDomain().getCodeSource()
            .getLocation().getPath());
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

    private static final native JniPackageGroup[] nativeGetPackageGroups(
        String apkFileName);
}

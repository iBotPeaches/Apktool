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
package brut.androlib.res;

import brut.androlib.exceptions.AndrolibException;
import brut.common.BrutException;
import brut.util.Jar;
import brut.util.OS;
import brut.util.OSDetection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class AaptManager {

    private AaptManager() {
        // Private constructor for utility class.
    }

    public static String getBinaryName() {
        return "aapt2";
    }

    public static File getBinaryFile() throws AndrolibException {
        String binName = getBinaryName();

        if (!OSDetection.is64Bit()) {
            throw new AndrolibException(binName + " binaries are not available for 32-bit platforms.");
        }

        StringBuilder binPath = new StringBuilder("/prebuilt/");
        if (OSDetection.isUnix()) {
            binPath.append("linux"); // ELF 64-bit LSB executable, x86-64
        } else if (OSDetection.isMacOSX()) {
            binPath.append("macosx"); // fat binary x86_64 + arm64
        } else if (OSDetection.isWindows()) {
            binPath.append("windows"); // x86_64
        } else {
            throw new AndrolibException("Could not identify platform: " + OSDetection.returnOS());
        }
        binPath.append('/');
        binPath.append(binName);
        if (OSDetection.isWindows()) {
            binPath.append(".exe");
        }

        File binFile;
        try {
            binFile = Jar.getResourceAsFile(AaptManager.class, binPath.toString(), binName + "_");
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
        setBinaryExecutable(binFile);
        return binFile;
    }

    private static void setBinaryExecutable(File binFile) throws AndrolibException {
        if (!binFile.isFile() || !binFile.canRead()) {
            throw new AndrolibException("Could not read aapt binary: " + binFile.getPath());
        }
        if (!binFile.setExecutable(true)) {
            throw new AndrolibException("Could not set aapt binary as executable: " + binFile.getPath());
        }
    }

    public static int getBinaryVersion(File binFile) throws AndrolibException {
        setBinaryExecutable(binFile);

        List<String> cmd = new ArrayList<>();
        cmd.add(binFile.getPath());
        cmd.add("version");

        String versionStr = OS.execAndReturn(cmd.toArray(new String[0]));
        if (versionStr == null) {
            throw new AndrolibException("Could not execute aapt binary at location: " + binFile.getPath());
        }

        return getVersionFromString(versionStr);
    }

    public static int getVersionFromString(String versionStr) throws AndrolibException {
        if (versionStr.startsWith("Android Asset Packaging Tool (aapt) 2:")) {
            return 2;
        } else if (versionStr.startsWith("Android Asset Packaging Tool (aapt) 2.")) {
            return 2; // Prior to Android SDK 26.0.2
        } else if (versionStr.startsWith("Android Asset Packaging Tool, v0.")) {
            return 1;
        }

        throw new AndrolibException("Could not identify aapt binary version: " + versionStr);
    }
}

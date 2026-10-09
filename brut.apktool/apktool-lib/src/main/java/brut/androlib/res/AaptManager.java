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
import brut.util.JarUtils;
import brut.util.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public final class AaptManager {

    private AaptManager() {}

    public static String getBinaryName() {
        return "aapt2";
    }

    public static Path getBinaryFile() throws AndrolibException {
        String binName = getBinaryName();

        if (!SystemUtils.is64Bit()) {
            throw new AndrolibException(binName + " binaries are not available for 32-bit platforms.");
        }

        StringBuilder binPath = new StringBuilder("/prebuilt/");
        if (SystemUtils.isUnix()) {
            binPath.append("linux"); // ELF 64-bit LSB executable, x86-64
        } else if (SystemUtils.isMac()) {
            binPath.append("macosx"); // fat binary x86_64 + arm64
        } else if (SystemUtils.isWindows()) {
            binPath.append("windows"); // x86_64
        } else {
            throw new AndrolibException("Could not identify platform: " + SystemUtils.getOSName());
        }
        binPath.append('/');
        binPath.append(binName);
        if (SystemUtils.isWindows()) {
            binPath.append(".exe");
        }

        Path binFile;
        try {
            binFile = JarUtils.getResourceAsFile(AaptManager.class, binPath.toString(), binName + "_");
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
        setBinaryExecutable(binFile);
        return binFile;
    }

    private static void setBinaryExecutable(Path binFile) throws AndrolibException {
        if (!Files.isRegularFile(binFile) || !Files.isReadable(binFile)) {
            throw new AndrolibException("Could not read aapt binary: " + binFile);
        }
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(binFile);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(binFile, perms);
        } catch (IOException ex) {
            throw new AndrolibException("Could not set aapt binary as executable: " + binFile);
        } catch (UnsupportedOperationException ignored) {
            // This is expected on Windows.
        }
    }

    public static int getBinaryVersion(Path binFile) throws AndrolibException {
        setBinaryExecutable(binFile);
        String versionStr;
        try {
            versionStr = SystemUtils.executeAndReturn(new String[] { binFile.toString(), "version" });
        } catch (IOException | InterruptedException ex) {
            throw new AndrolibException("Could not execute aapt binary: " + binFile, ex);
        }
        return getVersionFromString(versionStr);
    }

    public static int getVersionFromString(String versionStr) throws AndrolibException {
        if (versionStr.startsWith("Android Asset Packaging Tool (aapt) 2:")) {
            return 2;
        }
        if (versionStr.startsWith("Android Asset Packaging Tool (aapt) 2.")) {
            return 2; // Prior to Android SDK 26.0.2
        }
        if (versionStr.startsWith("Android Asset Packaging Tool, v0.")) {
            return 1;
        }
        throw new AndrolibException("Could not identify aapt binary version: " + versionStr);
    }
}

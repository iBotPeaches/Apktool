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
package brut.util;

import brut.common.BrutException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class AaptManager {

    private AaptManager() {
        // Private constructor for utility class
    }

    public static String getAaptName(int version) {
        switch (version) {
            case 2:
                return "aapt2";
            default:
                return "aapt";
        }
    }

    public static File getAaptBinary(int version) throws BrutException {
        String aaptName = getAaptName(version);

        if (!OSDetection.is64Bit() && OSDetection.isMacOSX()) {
            throw new BrutException(aaptName + " binary is not available for 32-bit platform: " + OSDetection.returnOS());
        }

        StringBuilder aaptPath = new StringBuilder("/prebuilt/");
        if (OSDetection.isUnix()) {
            aaptPath.append("linux");
        } else if (OSDetection.isMacOSX()) {
            aaptPath.append("macosx");
        } else if (OSDetection.isWindows()) {
            aaptPath.append("windows");
        } else {
            throw new BrutException("Could not identify platform: " + OSDetection.returnOS());
        }
        aaptPath.append("/");
        aaptPath.append(aaptName);
        if (OSDetection.is64Bit()) {
            aaptPath.append("_64");
        }
        if (OSDetection.isWindows()) {
            aaptPath.append(".exe");
        }

        File aaptBinary = Jar.getResourceAsFile(AaptManager.class, aaptPath.toString());
        setAaptBinaryExecutable(aaptBinary);
        return aaptBinary;
    }

    public static int getAaptVersion(File aaptBinary) throws BrutException {
        setAaptBinaryExecutable(aaptBinary);

        List<String> cmd = new ArrayList<>();
        cmd.add(aaptBinary.getPath());
        cmd.add("version");

        String versionStr = OS.execAndReturn(cmd.toArray(new String[0]));
        if (versionStr == null) {
            throw new BrutException("Could not execute aapt binary at location: " + aaptBinary.getPath());
        }

        return getAaptVersionFromString(versionStr);
    }

    public static int getAaptVersionFromString(String versionStr) throws BrutException {
        if (versionStr.startsWith("Android Asset Packaging Tool (aapt) 2:")) {
            return 2;
        } else if (versionStr.startsWith("Android Asset Packaging Tool (aapt) 2.")) {
            return 2; // Prior to Android SDK 26.0.2
        } else if (versionStr.startsWith("Android Asset Packaging Tool, v0.")) {
            return 1;
        }

        throw new BrutException("aapt version could not be identified: " + versionStr);
    }

    private static void setAaptBinaryExecutable(File aaptBinary) throws BrutException {
        if (!aaptBinary.isFile() || !aaptBinary.canRead()) {
            throw new BrutException("Could not read aapt binary: " + aaptBinary.getPath());
        }
        if (!aaptBinary.setExecutable(true)) {
            throw new BrutException("Could not set aapt binary as executable: " + aaptBinary.getPath());
        }
    }
}

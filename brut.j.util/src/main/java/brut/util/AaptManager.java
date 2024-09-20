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

public class AaptManager {
    public static final int AAPT_VERSION_MIN = 1;
    public static final int AAPT_VERSION_MAX = 2;

    public static File getAapt2() throws BrutException {
        return getAapt(2);
    }

    public static File getAapt1() throws BrutException {
        return getAapt(1);
    }

    private static File getAapt(int version) throws BrutException {
        String aaptName = getAaptBinaryName(version);

        if (!OSDetection.is64Bit() && OSDetection.isMacOSX()) {
            throw new BrutException(aaptName + " binaries are not available for 32-bit platform: " + OSDetection.returnOS());
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

        File aaptBinary = Jar.getResourceAsFile(aaptPath.toString(), AaptManager.class);
        if (!aaptBinary.setExecutable(true)) {
            throw new BrutException("Can't set aapt binary as executable");
        }

        return aaptBinary;
    }

    public static String getAaptBinaryName(int version) {
        switch (version) {
            case 2:
                return "aapt2";
            default:
                return "aapt";
        }
    }

    public static int getAaptVersion(String aaptPath) throws BrutException {
        return getAaptVersion(new File(aaptPath));
    }

    public static int getAaptVersion(File aaptBinary) throws BrutException {
        if (!aaptBinary.isFile() || !aaptBinary.canRead()) {
            throw new BrutException("Can't read aapt binary: " + aaptBinary.getAbsolutePath());
        }
        if (!aaptBinary.setExecutable(true)) {
            throw new BrutException("Can't set aapt binary as executable: " + aaptBinary.getAbsolutePath());
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(aaptBinary.getAbsolutePath());
        cmd.add("version");

        String version = OS.execAndReturn(cmd.toArray(new String[0]));
        if (version == null) {
            throw new BrutException("Could not execute aapt binary at location: " + aaptBinary.getAbsolutePath());
        }

        return getAppVersionFromString(version);
    }

    public static int getAppVersionFromString(String version) throws BrutException {
        if (version.startsWith("Android Asset Packaging Tool (aapt) 2:")) {
            return 2;
        } else if (version.startsWith("Android Asset Packaging Tool (aapt) 2.")) {
            return 2; // Prior to Android SDK 26.0.2
        } else if (version.startsWith("Android Asset Packaging Tool, v0.")) {
            return 1;
        }

        throw new BrutException("aapt version could not be identified: " + version);
    }

    public static String getAaptExecutionCommand(String aaptPath, File aaptBinary) throws BrutException {
        if (aaptPath.isEmpty()) {
            return aaptBinary.getAbsolutePath();
        }

        aaptBinary = new File(aaptPath);
        if (!aaptBinary.isFile() || !aaptBinary.canRead()) {
            throw new BrutException("Can't read aapt binary: " + aaptBinary.getAbsolutePath());
        }
        if (!aaptBinary.setExecutable(true)) {
            throw new BrutException("Can't set aapt binary as executable: " + aaptBinary.getAbsolutePath());
        }

        return aaptBinary.getPath();
    }
}

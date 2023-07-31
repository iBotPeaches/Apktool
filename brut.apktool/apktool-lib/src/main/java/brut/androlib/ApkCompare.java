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
package brut.androlib;

import brut.androlib.apk.ApkInfo;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.InFileNotFoundException;
import brut.androlib.res.ResourcesDecoder;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResUnknownFiles;
import brut.androlib.src.SmaliDecoder;
import brut.common.BrutException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.OS;
import com.android.tools.smali.dexlib2.iface.DexFile;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ApkCompare {
    private final static Logger LOGGER = Logger.getLogger(ApkCompare.class.getName());

    private final Config mConfig;
    private final ExtFile mOriginalApkFile;
    private final ExtFile mNewApkFile;
    private Directory mOriginalDir;
    private Directory mNewDir;
    private Set<String> mOriginalItems;
    private Set<String> mNewItems;

    private final static String SMALI_DIRNAME = "smali";
    private final static String UNK_DIRNAME = "unknown";
    private final static String[] APK_STANDARD_ALL_FILENAMES = new String[] {
        "classes.dex", "AndroidManifest.xml", "resources.arsc", "res", "r", "R",
        "lib", "libs", "assets", "META-INF", "kotlin" };
    private final static String[] APK_RESOURCES_FILENAMES = new String[] {
        "resources.arsc", "res", "r", "R" };
    private final static String[] APK_MANIFEST_FILENAMES = new String[] {
        "AndroidManifest.xml" };
    private final static Pattern NO_COMPRESS_PATTERN = Pattern.compile("(" +
        "jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|rtttl|imy|xmf|mp4|" +
        "m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|webp|mkv)$");

    public ApkCompare(Config config, ExtFile originalApk, ExtFile newApk) {
        mConfig = config;
        mOriginalApkFile = originalApk;
        mNewApkFile = newApk;
    }

    public void compare() throws AndrolibException {
        try {
            if (!mOriginalApkFile.isFile() || !mOriginalApkFile.canRead()) {
                throw new InFileNotFoundException("Input file (" +
                    mOriginalApkFile.getAbsolutePath() + ") " + "was not found or was not readable.");
            }
            if (!mNewApkFile.isFile() || !mNewApkFile.canRead()) {
                throw new InFileNotFoundException("Input file (" +
                    mNewApkFile.getAbsolutePath() + ") " + "was not found or was not readable.");
            }

            LOGGER.info("Using Apktool " + ApktoolProperties.getVersion() + " for compare "
                + mOriginalApkFile.getName() + " and "
                + mNewApkFile.getName());

            // load resources
            mOriginalDir = mOriginalApkFile.getDirectory();
            mNewDir = mNewApkFile.getDirectory();
            mOriginalItems = mOriginalDir.getFiles();
            mNewItems = mNewDir.getFiles();

            // compare resources
            compareResources();
            // compare unknown items

            // compare dex

        } catch (DirectoryException ex){
            throw new AndrolibException(ex);
        } finally {
            try {
                if (mOriginalDir != null) {
                    mOriginalDir.close();
                }
                if (mNewDir != null) {
                    mNewDir.close();
                }
                mOriginalApkFile.close();
                mNewApkFile.close();
            } catch (IOException ignored) {}
        }
    }

    private void compareResources() throws AndrolibException {
        String originalName = mOriginalApkFile.getName();
        String newName = mNewApkFile.getName();
        boolean originalHasResources = mOriginalDir.containsFile("resources.arsc");
        if (!originalHasResources) {
            LOGGER.info(originalName + " don't have resources.arsc");
            return;
        }
        boolean newHasResources = mNewDir.containsFile("resources.arsc");
        if (!newHasResources) {
            LOGGER.info(newName + " don't have resources.arsc");
            return;
        }

        // load main package
        ResourcesDecoder originalResDecoder = new ResourcesDecoder(mConfig, mOriginalApkFile);
        ResourcesDecoder newResDecoder = new ResourcesDecoder(mConfig, mNewApkFile);
        originalResDecoder.loadMainPkg();
        newResDecoder.loadMainPkg();

        originalResDecoder.
        if (originalPkgs.length != newPkgs.length) {
            LOGGER.info("resources.arsc have different packages length");
        }

        String originalPackages = Arrays.stream(originalPkgs)
            .map(Object::toString)
            .collect(Collectors.joining(","));
        String newPackages = Arrays.stream(newPkgs)
            .map(Object::toString)
            .collect(Collectors.joining(","));

        LOGGER.info(originalName + " resources.arsc have " + originalPkgs.length + " packages: "
            + originalPackages);
        LOGGER.info(newName + " resources.arsc have " + newPkgs.length + " packages: "
            + newPackages);

        // compare packages
        Map<String, ResPackage> newPackagesMap = Arrays.stream(newPkgs)
            .collect(Collectors.toMap(ResPackage::getName, Function.identity()));
        for (ResPackage orig: originalPkgs) {
            ResPackage n = newPackagesMap.get(orig.getName());
            comparePackages(orig, n);
        }
    }

    private void comparePackages(ResPackage origPackage, ResPackage newPackage) {
        boolean equals = origPackage.getId() == newPackage.getId()
            && Objects.equals(origPackage.getName(), newPackage.getName())
            && origPackage.getResSpecCount() == newPackage.getResSpecCount();
        LOGGER.info("compare packages: " + origPackage.getName()
            + " equals: " + equals);

    }

    private boolean hasManifest(ExtFile apkFile) throws AndrolibException {
        try {
            return apkFile.getDirectory().containsFile("AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean hasResources(ExtFile apkFile) throws AndrolibException {
        try {
            return apkFile.getDirectory().containsFile("resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean hasSources(ExtFile apkFile) throws AndrolibException {
        try {
            return apkFile.getDirectory().containsFile("classes.dex");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean hasMultipleSources(ExtFile apkFile) throws AndrolibException {
        try {
            Set<String> files = apkFile.getDirectory().getFiles(false);
            for (String file : files) {
                if (file.endsWith(".dex")) {
                    if (! file.equalsIgnoreCase("classes.dex")) {
                        return true;
                    }
                }
            }

            return false;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }
}

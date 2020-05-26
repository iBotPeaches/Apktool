/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import brut.common.BrutException;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
abstract public class Jar {
    private final static Set<String> mLoaded = new HashSet<String>();
    private final static Map<String, File> mExtracted = new HashMap<String, File>();

    public static File getResourceAsFile(String name, Class clazz) throws BrutException {
        File file = mExtracted.get(name);
        if (file == null) {
            file = extractToTmp(name, clazz);
            mExtracted.put(name, file);
        }
        return file;
    }

    public static File getResourceAsFile(String name) throws BrutException {
        return getResourceAsFile(name, Class.class);
    }

    public static void load(String libPath) {
        if (mLoaded.contains(libPath)) {
            return;
        }

        File libFile;
        try {
            libFile = getResourceAsFile(libPath);
        } catch (BrutException ex) {
            throw new UnsatisfiedLinkError(ex.getMessage());
        }

        System.load(libFile.getAbsolutePath());
    }

    public static File extractToTmp(String resourcePath) throws BrutException {
        return extractToTmp(resourcePath, Class.class);
    }

    public static File extractToTmp(String resourcePath, Class clazz) throws BrutException {
        return extractToTmp(resourcePath, "brut_util_Jar_", clazz);
    }

    public static File extractToTmp(String resourcePath, String tmpPrefix) throws BrutException {
        return extractToTmp(resourcePath, tmpPrefix, Class.class);
    }

    public static File extractToTmp(String resourcePath, String tmpPrefix, Class clazz) throws BrutException {
        try {
            InputStream in = clazz.getResourceAsStream(resourcePath);
            if (in == null) {
                throw new FileNotFoundException(resourcePath);
            }
            long suffix = ThreadLocalRandom.current().nextLong();
            suffix = suffix == Long.MIN_VALUE ? 0 : Math.abs(suffix);
            File fileOut = File.createTempFile(tmpPrefix, suffix + ".tmp");
            fileOut.deleteOnExit();

            OutputStream out = new FileOutputStream(fileOut);
            IOUtils.copy(in, out);
            in.close();
            out.close();

            return fileOut;
        } catch (IOException ex) {
            throw new BrutException("Could not extract resource: " + resourcePath, ex);
        }
    }
}

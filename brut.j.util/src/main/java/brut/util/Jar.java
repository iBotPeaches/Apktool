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
import brut.util.BrutIO;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class Jar {
    private static final Map<String, File> sExtracted = new HashMap<>();

    private Jar() {
        // Private constructor for utility class
    }

    public static File getResourceAsFile(Class<?> clz, String name) throws BrutException {
        File file = sExtracted.get(name);
        if (file == null) {
            file = extractToTmp(clz, name);
            sExtracted.put(name, file);
        }
        return file;
    }

    public static File extractToTmp(Class<?> clz, String name) throws BrutException {
        return extractToTmp(clz, name, "brut_util_Jar_");
    }

    public static File extractToTmp(Class<?> clz, String name, String tmpPrefix) throws BrutException {
        InputStream in = null;
        try {
            in = clz.getResourceAsStream(name);
            if (in == null) {
                throw new FileNotFoundException(name);
            }
            long suffix = ThreadLocalRandom.current().nextLong();
            suffix = suffix > Long.MIN_VALUE ? Math.abs(suffix) : 0;
            File fileOut = File.createTempFile(tmpPrefix, suffix + ".tmp");
            fileOut.deleteOnExit();

            BrutIO.copyAndClose(in, Files.newOutputStream(fileOut.toPath()));

            return fileOut;
        } catch (IOException ex) {
            throw new BrutException("Could not extract resource: " + name, ex);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}

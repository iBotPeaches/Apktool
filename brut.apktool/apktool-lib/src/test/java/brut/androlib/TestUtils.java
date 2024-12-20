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

import brut.common.BrutException;
import brut.directory.DirUtils;
import brut.directory.Directory;
import brut.directory.FileDirectory;
import brut.util.OS;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;

public final class TestUtils {

    private TestUtils() {
        // Private constructor for utility class
    }

    public static void copyResourceDir(Class<?> clz, String dirPath, File out) throws BrutException {
        OS.mkdir(out);
        copyResourceDir(clz, dirPath, new FileDirectory(out));
    }

    public static void copyResourceDir(Class<?> clz, String dirPath, Directory out) throws BrutException {
        if (clz == null) {
            clz = Class.class;
        }

        URL dirURL = clz.getClassLoader().getResource(dirPath);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            try {
                DirUtils.copyToDir(new FileDirectory(dirURL.getFile()), out);
            } catch (UnsupportedEncodingException ex) {
                throw new BrutException(ex);
            }
            return;
        }

        if (dirURL == null) {
            String className = clz.getName().replace(".", "/") + ".class";
            dirURL = clz.getClassLoader().getResource(className);
        }

        if (dirURL.getProtocol().equals("jar")) {
            String jarPath;
            try {
                jarPath = URLDecoder.decode(dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")), "UTF-8");
                DirUtils.copyToDir(new FileDirectory(jarPath), out);
            } catch (UnsupportedEncodingException ex) {
                throw new BrutException(ex);
            }
        }
    }

    public static byte[] readHeaderOfFile(File file, int size) throws IOException {
        byte[] buffer = new byte[size];

        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in.read(buffer) != buffer.length) {
                throw new IOException("File size too small for buffer length: " + size);
            }
        }

        return buffer;
    }

    public static String replaceNewlines(String value) {
        return value.replace("\n", "").replace("\r", "");
    }
}

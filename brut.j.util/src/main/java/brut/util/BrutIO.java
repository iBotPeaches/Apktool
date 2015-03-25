/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
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

import java.io.*;
import java.util.zip.CRC32;

import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class BrutIO {
    public static void copyAndClose(InputStream in, OutputStream out)
            throws IOException {
        try {
            IOUtils.copy(in, out);
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ex) {}
        }
    }

    public static long recursiveModifiedTime(File[] files) {
        long modified = 0;
        for (int i = 0; i < files.length; i++) {
            long submodified = recursiveModifiedTime(files[i]);
            if (submodified > modified) {
                modified = submodified;
            }
        }
        return modified;
    }

    public static long recursiveModifiedTime(File file) {
        long modified = file.lastModified();
        if (file.isDirectory()) {
            File[] subfiles = file.listFiles();
            for (int i = 0; i < subfiles.length; i++) {
                long submodified = recursiveModifiedTime(subfiles[i]);
                if (submodified > modified) {
                    modified = submodified;
                }
            }
        }
        return modified;
    }

    public static CRC32 calculateCrc(InputStream input, byte[] buffer) throws IOException {
        CRC32 crc = new CRC32();
        int bytesRead = 0;
        while((bytesRead = input.read(buffer)) != -1) {
            crc.update(buffer, 0, bytesRead);
        }
        return crc;
    }

    public static void copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        int bytesRead;
        while((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}

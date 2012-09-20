/**
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
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
}

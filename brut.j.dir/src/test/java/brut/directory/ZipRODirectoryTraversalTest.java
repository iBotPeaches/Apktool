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
package brut.directory;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZipRODirectoryTraversalTest {

    @Test
    public void skipBackslashTraversalZipEntryTest() throws Exception {
        File apk = File.createTempFile("zipro-traversal", ".apk");
        apk.deleteOnExit();

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(apk))) {
            out.putNextEntry(new ZipEntry("classes.dex"));
            out.write("dex".getBytes());
            out.closeEntry();

            out.putNextEntry(new ZipEntry("..\\..\\evil.dex"));
            out.write("dex".getBytes());
            out.closeEntry();
        }

        try (ZipRODirectory zipDirectory = new ZipRODirectory(apk)) {
            Set<String> files = zipDirectory.getFiles(true);
            assertTrue(files.contains("classes.dex"));
            assertFalse(files.contains("..\\..\\evil.dex"));
        }
    }
}

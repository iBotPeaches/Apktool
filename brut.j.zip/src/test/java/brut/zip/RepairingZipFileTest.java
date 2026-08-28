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
package brut.zip;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

public class RepairingZipFileTest {
    // Valid ZIP with the encrypted flag set in both local and central-directory headers.
    private static final String ENCRYPTED_ENTRY_ZIP =
        "UEsDBBQAAQAIAKuFHF3SG9QNFgAAABQAAAAJAAAAZW50cnkudHh0Ky7JL0pNUUjNKymqVChIrMzJT0wBAFBLAQIUAxQAAQAIAKuFHF3SG9QNFgAAABQAAAAJAAAAAAAAAAAAAACAAQAAAABlbnRyeS50eHRQSwUGAAAAAAEAAQA3AAAAPQAAAAAA";

    @Test
    public void spuriousEncryptedBitIsToleratedWithoutChangingSource() throws IOException {
        File zip = encryptedEntryFixture();
        byte[] source = Files.readAllBytes(zip.toPath());

        assertThrows(ZipException.class, () -> new java.util.zip.ZipFile(zip));
        try (RepairingZipFile file = new RepairingZipFile(zip)) {
            ZipEntry entry = file.getEntry("entry.txt");
            assertNotNull(entry);
            try (InputStream in = file.getInputStream(entry)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[64];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                assertEquals("stored entry payload", new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
        }

        assertArrayEquals(source, Files.readAllBytes(zip.toPath()));
    }

    @Test
    public void garbageArchiveStillFails() throws IOException {
        File garbage = File.createTempFile("garbage-", ".zip");
        garbage.deleteOnExit();
        Files.write(garbage.toPath(), "not a zip archive".getBytes(StandardCharsets.US_ASCII));

        assertThrows(ZipException.class, () -> new RepairingZipFile(garbage));
    }

    private static File encryptedEntryFixture() throws IOException {
        File zip = File.createTempFile("encrypted-entry-", ".zip");
        zip.deleteOnExit();
        Files.write(zip.toPath(), Base64.getDecoder().decode(ENCRYPTED_ENTRY_ZIP));
        return zip;
    }
}

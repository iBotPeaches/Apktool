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

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class OSTest {

    @Test
    public void testRmdirDoesNotFollowSymlinkToDirectory() throws Exception {
        // Create a real target directory with a file in it
        Path targetDir = Files.createTempDirectory("apktool-test-target");
        Path fileInTarget = targetDir.resolve("sensitive-file.txt");
        Files.write(fileInTarget, "sensitive content".getBytes());

        // Create a symlink pointing to the target directory
        Path symlinkDir = Files.createTempDirectory("apktool-test-base");
        Files.delete(symlinkDir);
        Files.createSymbolicLink(symlinkDir, targetDir);

        assertTrue("Symlink should exist before rmdir", Files.exists(symlinkDir));
        assertTrue("Target file should exist before rmdir", Files.exists(fileInTarget));

        // Call rmdir on the symlink
        OS.rmdir(symlinkDir.toFile());

        // The symlink should be removed
        assertFalse("Symlink should be removed by rmdir", Files.exists(symlinkDir));
        assertFalse("Symlink itself should not exist (not following)", Files.isSymbolicLink(symlinkDir));

        // The real target directory and its contents should NOT be deleted
        assertTrue("Target directory should still exist", Files.exists(targetDir));
        assertTrue("File inside target directory should still exist", Files.exists(fileInTarget));

        // Cleanup
        Files.delete(fileInTarget);
        Files.delete(targetDir);
    }

    @Test
    public void testRmdirDoesNotFollowSymlinkInsideDirectory() throws Exception {
        // Create a real target directory with a file in it
        Path targetDir = Files.createTempDirectory("apktool-test-target");
        Path fileInTarget = targetDir.resolve("sensitive-file.txt");
        Files.write(fileInTarget, "sensitive content".getBytes());

        // Create a directory that contains a symlink pointing to the target
        Path baseDir = Files.createTempDirectory("apktool-test-base");
        Path symlinkInDir = baseDir.resolve("symlink-to-target");
        Files.createSymbolicLink(symlinkInDir, targetDir);

        assertTrue("Base dir should exist before rmdir", Files.exists(baseDir));
        assertTrue("Symlink inside dir should exist before rmdir", Files.exists(symlinkInDir));
        assertTrue("Target file should exist before rmdir", Files.exists(fileInTarget));

        // Call rmdir on the base directory (which contains the symlink)
        OS.rmdir(baseDir.toFile());

        // The base directory and symlink should be removed
        assertFalse("Base dir should be removed by rmdir", Files.exists(baseDir));

        // The real target directory and its contents should NOT be deleted
        assertTrue("Target directory should still exist", Files.exists(targetDir));
        assertTrue("File inside target directory should still exist", Files.exists(fileInTarget));

        // Cleanup
        Files.delete(fileInTarget);
        Files.delete(targetDir);
    }
}

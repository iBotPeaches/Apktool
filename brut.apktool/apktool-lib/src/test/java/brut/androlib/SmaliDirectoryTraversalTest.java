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

import brut.androlib.exceptions.AndrolibException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.*;

public class SmaliDirectoryTraversalTest extends BaseTest {

    @Test(expected = AndrolibException.class)
    public void buildWithMaliciousSmaliDirShouldThrow() throws Exception {
        File projectDir = new File(sTmpDir, "issue4181-project");
        projectDir.mkdirs();

        // Write a minimal apktool.yml so ApkInfo can be loaded.
        File apktoolYml = new File(projectDir, "apktool.yml");
        Files.write(apktoolYml.toPath(), "version: 2.0.0\napkFileName: issue4181.apk\n"
            .getBytes(StandardCharsets.UTF_8));

        // Create a malicious smali directory name that encodes a path traversal via '@' characters.
        // After '@' -> File.separatorChar replacement, "smali_..@..@..@outside" becomes
        // "../../../outside.dex", which escapes the build output directory.
        File maliciousSmaliDir = new File(projectDir, "smali_..@..@..@outside");
        maliciousSmaliDir.mkdirs();

        sConfig.setNoApk(true);
        new ApkBuilder(projectDir, sConfig).build(null);
    }
}

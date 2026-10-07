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
package brut.androlib.util;

import brut.androlib.BaseTest;
import brut.util.IOUtils;

import java.nio.file.Paths;

import org.junit.*;
import static org.junit.Assert.*;

public class InvalidDirectoryTraversalTest extends BaseTest {

    @Test(expected = IllegalArgumentException.class)
    public void emptyPathTest() throws Exception {
        IOUtils.sanitizePath(sTmpDir, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidTraversalTest() throws Exception {
        IOUtils.sanitizePath(sTmpDir, Paths.get("..", "file").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAbsoluteTest() throws Exception {
        IOUtils.sanitizePath(sTmpDir, Paths.get("").toAbsolutePath().getRoot().resolve("file").toString());
    }

    @Test
    public void validFileTest() throws Exception {
        assertEquals("file", IOUtils.sanitizePath(sTmpDir, "file"));
    }

    @Test
    public void validNestedFileTest() throws Exception {
        String path = Paths.get("dir", "file").toString();
        assertEquals(path, IOUtils.sanitizePath(sTmpDir, path));
    }
}

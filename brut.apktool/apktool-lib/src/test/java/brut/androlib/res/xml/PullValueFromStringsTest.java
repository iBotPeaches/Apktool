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
package brut.androlib.res.xml;

import brut.androlib.BaseTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.*;
import static org.junit.Assert.*;

public class PullValueFromStringsTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        Path valuesDir = sTmpDir.resolve("res/values");
        Files.createDirectories(valuesDir);
        Files.write(valuesDir.resolve("strings.xml"),
            "<resources><string name=\"version\">1.0</string></resources>".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void referenceIsResolvedTest() throws Exception {
        assertEquals("1.0", ResXmlUtils.pullValueFromStrings(sTmpDir, "@string/version"));
    }

    @Test
    public void quoteInKeyReturnsNullTest() throws Exception {
        assertNull(ResXmlUtils.pullValueFromStrings(sTmpDir, "1.0 (it's @beta)"));
    }
}

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

import org.junit.*;
import static org.junit.Assert.*;

public class PositionalSpecifiersTest extends BaseTest {

    @Test
    public void noSpecifiersTest() {
        assertEquals("test", normalize("test"));
    }

    @Test
    public void twoSpecifiersTest() {
        assertEquals("%1$s, %2$s, and 1 other.", normalize("%s, %s, and 1 other."));
    }

    @Test
    public void twoPositionalSpecifiersTest() {
        assertEquals("%1$s, %2$s and 1 other", normalize("%1$s, %2$s and 1 other"));
    }

    @Test
    public void threeSpecifiersTest() {
        assertEquals("%1$s, %2$s, and %3$d other.", normalize("%s, %s, and %d other."));
    }

    @Test
    public void threePositionalSpecifiersTest() {
        assertEquals(" %1$s, %2$s and %3$d other", normalize(" %1$s, %2$s and %3$d other"));
    }

    @Test
    public void fourSpecifiersTest() {
        assertEquals("%1$s, %2$s, and %3$d other and %4$d.", normalize("%s, %s, and %d other and %d."));
    }

    @Test
    public void fourPositionalSpecifiersTest() {
        assertEquals(" %1$s, %2$s and %3$d other and %4$d.", normalize(" %1$s, %2$s and %3$d other and %4$d."));
    }

    private String normalize(String value) {
        return ResStringEncoder.normalizeFormatSpecifiers(value);
    }
}

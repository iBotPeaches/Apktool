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
package brut.androlib.apk;

import org.junit.Test;

import static org.junit.Assert.*;

public class YamlLineTest {

    @Test
    public void testEmptyLine()  {
        YamlLine line = new YamlLine("");
        assertEquals(0, line.indent);
        assertTrue(line.isEmpty);

        line = new YamlLine(" ");
        assertEquals(0, line.indent);
        assertTrue(line.isEmpty);
    }

    @Test
    public void testComment()  {
        YamlLine line = new YamlLine("!ApkInfo.class");
        assertTrue(line.isComment);

        line = new YamlLine("# This is comment");
        assertEquals(0, line.indent);
        assertTrue(line.isComment);
        assertEquals("", line.getKey());
        assertEquals("This is comment", line.getValue());

        line = new YamlLine("  # This is comment");
        assertEquals(2, line.indent);
        assertTrue(line.isComment);
        assertEquals("", line.getKey());
        assertEquals("This is comment", line.getValue());
    }

    @Test
    public void testKeyLine()  {
        YamlLine line = new YamlLine("name:");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("name", line.getKey());
        assertEquals("", line.getValue());

        line = new YamlLine("  name:");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("name", line.getKey());
        assertEquals("", line.getValue());

        line = new YamlLine(":value");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("", line.getKey());
        assertEquals("value", line.getValue());

        line = new YamlLine("  : value ");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("", line.getKey());
        assertEquals("value", line.getValue());

        line = new YamlLine("name  : value ");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("name", line.getKey());
        assertEquals("value", line.getValue());

        line = new YamlLine("  name  : value ");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("name", line.getKey());
        assertEquals("value", line.getValue());

        line = new YamlLine("  name  : value ::");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("name", line.getKey());
        assertEquals("value", line.getValue());

        // split this gives parts.length = 0!!
        line = new YamlLine(":::");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("", line.getKey());
        assertEquals("", line.getValue());
    }

    @Test
    public void testItemLine() {
        YamlLine line = new YamlLine("- val1");
        assertTrue(line.isItem);
        assertEquals(0, line.indent);
        assertEquals("", line.getKey());
        assertEquals("val1", line.getValue());

        line = new YamlLine("  - val1: ff");
        assertTrue(line.isItem);
        assertEquals(2, line.indent);
        assertEquals("", line.getKey());
        assertEquals("val1: ff", line.getValue());
    }
}

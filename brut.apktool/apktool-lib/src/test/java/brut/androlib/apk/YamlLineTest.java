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
        assertEquals("", line.key);
        assertEquals("This is comment", line.value);

        line = new YamlLine("  # This is comment");
        assertEquals(2, line.indent);
        assertTrue(line.isComment);
        assertEquals("", line.key);
        assertEquals("This is comment", line.value);
    }

    @Test
    public void testKeyLine()  {
        YamlLine line = new YamlLine("name:");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("name", line.key);
        assertEquals("", line.value);

        line = new YamlLine("  name:");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("name", line.key);
        assertEquals("", line.value);

        line = new YamlLine(":value");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("", line.key);
        assertEquals("value", line.value);

        line = new YamlLine("  : value ");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("", line.key);
        assertEquals("value", line.value);

        line = new YamlLine("name  : value ");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("name", line.key);
        assertEquals("value", line.value);

        line = new YamlLine("  name  : value ");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("name", line.key);
        assertEquals("value", line.value);

        line = new YamlLine("  name  : value ::");
        assertFalse(line.isComment);
        assertEquals(2, line.indent);
        assertEquals("name", line.key);
        assertEquals("value", line.value);

        // split this gives parts.length = 0!!
        line = new YamlLine(":::");
        assertFalse(line.isComment);
        assertEquals(0, line.indent);
        assertEquals("", line.key);
        assertEquals("", line.value);
    }

    @Test
    public void testItemLine() {
        YamlLine line = new YamlLine("- val1");
        assertTrue(line.isItem);
        assertEquals(0, line.indent);
        assertEquals("", line.key);
        assertEquals("val1", line.value);

        line = new YamlLine("  - val1: ff");
        assertTrue(line.isItem);
        assertEquals(2, line.indent);
        assertEquals("", line.key);
        assertEquals("val1: ff", line.value);
    }
}

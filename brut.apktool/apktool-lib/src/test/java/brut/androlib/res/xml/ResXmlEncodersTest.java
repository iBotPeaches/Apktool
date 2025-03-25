package brut.androlib.res.xml;

import brut.androlib.BaseTest;
import brut.xml.XmlUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.junit.Assert.assertEquals;

public class ResXmlEncodersTest extends BaseTest {
    @Test
    public void escapeXmlCharsEscapeExpected() {
        assertEquals("foo", ResXmlEncoders.escapeXmlChars("foo"));
        assertEquals("foo&amp;bar", ResXmlEncoders.escapeXmlChars("foo&bar"));
        assertEquals("&lt;foo>", ResXmlEncoders.escapeXmlChars("<foo>"));
        assertEquals("&lt;![CDATA[foo]]&gt;", ResXmlEncoders.escapeXmlChars("<![CDATA[foo]]>"));
    }

    @Test
    public void escapeXmlCharsRoundtrip() throws Throwable {
        assertRoundtrips("foo");
        assertRoundtrips("'foo'");
        assertRoundtrips("\"foo\"");
        assertRoundtrips("foo&bar");
        assertRoundtrips("<foo>");
        assertRoundtrips("<![CDATA[foo]]>");
    }

    private static void assertRoundtrips(String value) throws Throwable {
        String escaped = ResXmlEncoders.escapeXmlChars(value);
        Document doc = XmlUtils.loadDocumentContent("<root>" + escaped + "</root>", false);
        Node node = doc.getElementsByTagName("root").item(0);
        assertEquals(value, node.getTextContent());
    }
}

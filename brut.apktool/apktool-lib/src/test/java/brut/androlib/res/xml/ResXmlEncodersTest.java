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
import brut.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.junit.*;
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

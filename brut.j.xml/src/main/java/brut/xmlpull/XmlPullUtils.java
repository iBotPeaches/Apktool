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
package brut.xmlpull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;

public final class XmlPullUtils {
    private static final String PROPERTY_XMLDECL_STANDALONE
            = "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";

    public interface EventHandler {
        boolean onEvent(XmlPullParser in, XmlSerializer out) throws XmlPullParserException;
    }

    private XmlPullUtils() {
        // Private constructor for utility class
    }

    public static void copy(XmlPullParser in, XmlSerializer out)
            throws XmlPullParserException, IOException {
        copy(in, out, null);
    }

    public static void copy(XmlPullParser in, XmlSerializer out, EventHandler handler)
            throws XmlPullParserException, IOException {
        Boolean standalone = (Boolean) in.getProperty(PROPERTY_XMLDECL_STANDALONE);

        // Some parsers may have already consumed the event that starts the
        // document, so we manually emit that event here for consistency
        if (in.getEventType() == XmlPullParser.START_DOCUMENT) {
            out.startDocument(in.getInputEncoding(), standalone);
        }

        while (true) {
            int event = in.nextToken();
            if (event == XmlPullParser.START_DOCUMENT) {
                out.startDocument(in.getInputEncoding(), standalone);
                continue;
            }
            if (event == XmlPullParser.END_DOCUMENT) {
                out.endDocument();
                break;
            }
            if (handler != null && handler.onEvent(in, out)) {
                continue;
            }
            switch (event) {
                case XmlPullParser.START_TAG:
                    if (!in.getFeature(XmlPullParser.FEATURE_REPORT_NAMESPACE_ATTRIBUTES)) {
                        int nsStart = in.getNamespaceCount(in.getDepth() - 1);
                        int nsEnd = in.getNamespaceCount(in.getDepth());
                        for (int i = nsStart; i < nsEnd; i++) {
                            String prefix = in.getNamespacePrefix(i);
                            String ns = in.getNamespaceUri(i);
                            out.setPrefix(prefix, ns);
                        }
                    }
                    out.startTag(normalizeNamespace(in.getNamespace()), in.getName());
                    for (int i = 0; i < in.getAttributeCount(); i++) {
                        String ns = normalizeNamespace(in.getAttributeNamespace(i));
                        String name = in.getAttributeName(i);
                        String value = in.getAttributeValue(i);
                        out.attribute(ns, name, value);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    out.endTag(normalizeNamespace(in.getNamespace()), in.getName());
                    break;
                case XmlPullParser.TEXT:
                    out.text(in.getText());
                    break;
                case XmlPullParser.CDSECT:
                    out.cdsect(in.getText());
                    break;
                case XmlPullParser.ENTITY_REF:
                    out.entityRef(in.getName());
                    break;
                case XmlPullParser.IGNORABLE_WHITESPACE:
                    out.ignorableWhitespace(in.getText());
                    break;
                case XmlPullParser.PROCESSING_INSTRUCTION:
                    out.processingInstruction(in.getText());
                    break;
                case XmlPullParser.COMMENT:
                    out.comment(in.getText());
                    break;
                case XmlPullParser.DOCDECL:
                    out.docdecl(in.getText());
                    break;
                default:
                    throw new IllegalStateException("Unknown event: " + event);
            }
        }
    }

    /**
     * Some parsers may return an empty string when a namespace in unsupported,
     * which can confuse serializers. This method normalizes empty strings to
     * be null.
     */
    private static String normalizeNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            return null;
        } else {
            return namespace;
        }
    }
}

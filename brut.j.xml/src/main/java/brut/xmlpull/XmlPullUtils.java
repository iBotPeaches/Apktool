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

import brut.xml.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;

public final class XmlPullUtils {
    private static final String PROPERTY_XMLDECL_STANDALONE
            = "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";

    private XmlPullUtils() {}

    public static void copy(XmlPullParser in, XmlSerializer out) throws XmlPullParserException, IOException {
        copy(in, out, null);
    }

    public static void copy(XmlPullParser in, XmlSerializer out, XmlPullEventHandler handler)
            throws XmlPullParserException, IOException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(out, "out");
        Boolean standalone = (Boolean) in.getProperty(PROPERTY_XMLDECL_STANDALONE);
        if (handler == null) {
            handler = new SimpleXmlPullEventHandler();
        }

        // Some parsers may have already consumed the event that starts the document, so we manually emit that
        // event here for consistency.
        if (in.getEventType() == XmlPullParser.START_DOCUMENT) {
            out.startDocument(in.getInputEncoding(), standalone);
        }

        for (;;) {
            int event = in.nextToken();
            if (event == -1) {
                break;
            }
            if (event == XmlPullParser.END_DOCUMENT) {
                out.endDocument();
                break;
            }
            if (event == XmlPullParser.START_DOCUMENT) {
                out.startDocument(in.getInputEncoding(), standalone);
                continue;
            }
            if (handler.onEvent(in, out)) {
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
                    out.startTag(XmlUtils.normalizeNamespace(in.getNamespace()), in.getName());
                    handler.beforeAttributes(in, out);
                    for (int i = 0; i < in.getAttributeCount(); i++) {
                        String ns = XmlUtils.normalizeNamespace(in.getAttributeNamespace(i));
                        String name = in.getAttributeName(i);
                        String value = in.getAttributeValue(i);
                        if (handler.onAttribute(in, out, ns, name, value)) {
                            continue;
                        }
                        out.attribute(ns, name, value);
                    }
                    handler.afterAttributes(in, out);
                    break;
                case XmlPullParser.END_TAG:
                    out.endTag(XmlUtils.normalizeNamespace(in.getNamespace()), in.getName());
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
}

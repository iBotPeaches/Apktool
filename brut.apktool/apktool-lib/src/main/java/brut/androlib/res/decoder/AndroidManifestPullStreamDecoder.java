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
package brut.androlib.res.decoder;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.AXmlDecodingException;
import brut.androlib.exceptions.RawXmlEncounteredException;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.util.ExtXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.wrapper.XmlPullParserWrapper;
import org.xmlpull.v1.wrapper.XmlPullWrapperFactory;
import org.xmlpull.v1.wrapper.XmlSerializerWrapper;
import org.xmlpull.v1.wrapper.classic.StaticXmlSerializerWrapper;

import java.io.*;

public class AndroidManifestPullStreamDecoder implements ResStreamDecoder {
    public AndroidManifestPullStreamDecoder(AXmlResourceParser parser,
                                ExtXmlSerializer serializer) {
        this.mParser = parser;
        this.mSerial = serializer;
    }

    @Override
    public void decode(InputStream in, OutputStream out)
            throws AndrolibException {
        try {
            XmlPullWrapperFactory factory = XmlPullWrapperFactory.newInstance();
            XmlPullParserWrapper par = factory.newPullParserWrapper(mParser);
            final ResTable resTable = mParser.getResTable();

            XmlSerializerWrapper ser = new StaticXmlSerializerWrapper(mSerial, factory) {
                final boolean hideSdkInfo = !resTable.getAnalysisMode();

                @Override
                public void event(XmlPullParser pp)
                        throws XmlPullParserException, IOException {
                    int type = pp.getEventType();

                    if (type == XmlPullParser.START_TAG) {
                        if ("manifest".equals(pp.getName())) {
                            try {
                                parseManifest(pp);
                            } catch (AndrolibException ignored) {}
                        } else if ("uses-sdk".equals(pp.getName())) {
                            try {
                                parseUsesSdk(pp);
                            } catch (AndrolibException ignored) {}
                            if (hideSdkInfo) {
                                return;
                            }
                        }
                    } else if (type == XmlPullParser.END_TAG
                            && "uses-sdk".equals(pp.getName())) {
                        if (hideSdkInfo) {
                            return;
                        }
                    }

                    super.event(pp);
                }

                private void parseManifest(XmlPullParser pp)
                        throws AndrolibException {
                    for (int i = 0; i < pp.getAttributeCount(); i++) {
                        String ns = pp.getAttributeNamespace(i);
                        String name = pp.getAttributeName(i);
                        String value = pp.getAttributeValue(i);

                        if (value.isEmpty()) {
                            continue;
                        }

                        if (ns.isEmpty()) {
                            if (name.equals("package")) {
                                resTable.setPackageRenamed(value);
                            }
                        } else if (ns.equals(AXmlResourceParser.ANDROID_RES_NS)) {
                            switch (name) {
                                case "versionCode":
                                    resTable.setVersionCode(value);
                                    break;
                                case "versionName":
                                    resTable.setVersionName(value);
                                    break;
                            }
                        }
                    }
                }

                private void parseUsesSdk(XmlPullParser pp)
                        throws AndrolibException {
                    for (int i = 0; i < pp.getAttributeCount(); i++) {
                        String ns = pp.getAttributeNamespace(i);
                        String name = pp.getAttributeName(i);
                        String value = pp.getAttributeValue(i);

                        if (value.isEmpty()) {
                            continue;
                        }

                        if (ns.equals(AXmlResourceParser.ANDROID_RES_NS)) {
                            switch (name) {
                                case "minSdkVersion":
                                case "targetSdkVersion":
                                case "maxSdkVersion":
                                case "compileSdkVersion":
                                    resTable.addSdkInfo(name, value);
                                    break;
                            }
                        }
                    }
                }
            };

            par.setInput(in, null);
            ser.setOutput(out, null);

            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                ser.event(par);
            }
            ser.flush();
        } catch (XmlPullParserException ex) {
            throw new AXmlDecodingException("Could not decode XML", ex);
        } catch (IOException ex) {
            throw new RawXmlEncounteredException("Could not decode XML", ex);
        }
    }

    private final AXmlResourceParser mParser;
    private final ExtXmlSerializer mSerial;
}

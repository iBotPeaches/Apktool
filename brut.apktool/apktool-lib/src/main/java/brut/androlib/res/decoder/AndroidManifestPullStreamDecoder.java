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
import brut.xmlpull.XmlPullUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;

public class AndroidManifestPullStreamDecoder implements ResStreamDecoder {
    private final AXmlResourceParser mParser;
    private final XmlSerializer mSerial;

    public AndroidManifestPullStreamDecoder(AXmlResourceParser parser, XmlSerializer serial) {
        mParser = parser;
        mSerial = serial;
    }

    @Override
    public void decode(InputStream in, OutputStream out) throws AndrolibException {
        try {
            mParser.setInput(in, null);
            mSerial.setOutput(out, null);
            XmlPullUtils.copy(mParser, mSerial, new EventHandler(mParser.getResTable()));
        } catch (XmlPullParserException ex) {
            throw new AXmlDecodingException("Could not decode XML", ex);
        } catch (IOException ex) {
            throw new RawXmlEncounteredException("Could not decode XML", ex);
        }
    }

    private static class EventHandler implements XmlPullUtils.EventHandler {
        private final ResTable mResTable;
        private final boolean mHideSdkInfo;

        public EventHandler(ResTable resTable) {
            mResTable = resTable;
            mHideSdkInfo = !resTable.getConfig().isAnalysisMode();
        }

        @Override
        public boolean onEvent(XmlPullParser in, XmlSerializer out) throws XmlPullParserException {
            int type = in.getEventType();

            if (type == XmlPullParser.START_TAG) {
                String name = in.getName();

                if (name.equals("manifest")) {
                    parseManifest(in);
                } else if (name.equals("uses-sdk")) {
                    parseUsesSdk(in);

                    if (mHideSdkInfo) {
                        return true;
                    }
                }
            } else if (type == XmlPullParser.END_TAG) {
                String name = in.getName();

                if (name.equals("uses-sdk")) {
                    if (mHideSdkInfo) {
                        return true;
                    }
                }
            }

            return false;
        }

        private void parseManifest(XmlPullParser in) {
            for (int i = 0; i < in.getAttributeCount(); i++) {
                String ns = in.getAttributeNamespace(i);
                String name = in.getAttributeName(i);
                String value = in.getAttributeValue(i);

                if (value.isEmpty()) {
                    continue;
                }
                if (ns.isEmpty()) {
                    if (name.equals("package")) {
                        mResTable.setPackageRenamed(value);
                    }
                } else if (ns.equals(AXmlResourceParser.ANDROID_RES_NS)) {
                    switch (name) {
                        case "versionCode":
                            mResTable.setVersionCode(value);
                            break;
                        case "versionName":
                            mResTable.setVersionName(value);
                            break;
                    }
                }
            }
        }

        private void parseUsesSdk(XmlPullParser in) {
            for (int i = 0; i < in.getAttributeCount(); i++) {
                String ns = in.getAttributeNamespace(i);
                String name = in.getAttributeName(i);
                String value = in.getAttributeValue(i);

                if (value.isEmpty()) {
                    continue;
                }
                if (ns.equals(AXmlResourceParser.ANDROID_RES_NS)) {
                    switch (name) {
                        case "minSdkVersion":
                        case "targetSdkVersion":
                        case "maxSdkVersion":
                        case "compileSdkVersion":
                            mResTable.addSdkInfo(name, value);
                            break;
                    }
                }
            }
        }
    }
}

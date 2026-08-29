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

import brut.androlib.meta.ApkInfo;
import brut.androlib.res.xml.ResXmlUtils;
import brut.xmlpull.XmlPullUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ResXmlPullEventHandler implements XmlPullUtils.EventHandler {
    protected final ApkInfo mApkInfo;

    public ResXmlPullEventHandler(ApkInfo apkInfo) {
        mApkInfo = apkInfo;
    }

    @Override
    public boolean onEvent(XmlPullParser in, XmlSerializer out) throws XmlPullParserException {
        int depth = in.getDepth();
        int type = in.getEventType();

        if (depth > 1 && type == XmlPullParser.START_TAG) {
            for (int i = 0; i < in.getAttributeCount(); i++) {
                String ns = in.getAttributeNamespace(i);

                if (ns.equals(ResXmlUtils.ANDROID_RES_NS)) {
                    String name = in.getAttributeName(i);

                    if (name.equals("featureFlag")) {
                        String value = in.getAttributeValue(i);

                        if (value.isEmpty()) {
                            continue;
                        }
                        if (value.startsWith("!")) {
                            value = value.substring(1);
                            if (value.isEmpty()) {
                                continue;
                            }
                        }

                        mApkInfo.getFeatureFlags().add(value);
                    }
                }
            }
        }

        return false;
    }
}

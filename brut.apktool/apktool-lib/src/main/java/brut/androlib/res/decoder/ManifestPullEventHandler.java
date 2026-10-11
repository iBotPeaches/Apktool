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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ManifestPullEventHandler extends ResXmlPullEventHandler {
    private final boolean mAnalysisMode;

    public ManifestPullEventHandler(ApkInfo apkInfo, boolean analysisMode) {
        super(apkInfo);
        mAnalysisMode = analysisMode;
    }

    @Override
    public boolean onEvent(XmlPullParser in, XmlSerializer out) throws XmlPullParserException {
        int depth = in.getDepth();
        int type = in.getEventType();

        if (depth == 2 && (type == XmlPullParser.START_TAG || type == XmlPullParser.END_TAG)
                && in.getName().equals("uses-sdk")) {
            if (type == XmlPullParser.START_TAG) {
                for (int i = 0; i < in.getAttributeCount(); i++) {
                    if (ResXmlUtils.ANDROID_RES_NS.equals(in.getAttributeNamespace(i))) {
                        String name = in.getAttributeName(i);
                        if (name.equals("minSdkVersion")) {
                            mApkInfo.getSdkInfo().setMinSdkVersion(in.getAttributeValue(i));
                        } else if (name.equals("targetSdkVersion")) {
                            mApkInfo.getSdkInfo().setTargetSdkVersion(in.getAttributeValue(i));
                        } else if (name.equals("maxSdkVersion")) {
                            mApkInfo.getSdkInfo().setMaxSdkVersion(in.getAttributeValue(i));
                        }
                    }
                }
            }
            // Exclude the tag: injected in build time.
            if (!mAnalysisMode) {
                return true;
            }
        }

        return super.onEvent(in, out);
    }

    @Override
    public boolean onAttribute(XmlPullParser in, XmlSerializer out, String ns, String name, String value)
            throws XmlPullParserException {
        int depth = in.getDepth();

        if (depth == 1 && in.getName().equals("manifest")) {
            if (ns == null) {
                if (name.equals("package")) {
                    // This is temporary and will be compared to actual resources package later.
                    mApkInfo.getResourcesInfo().setPackageName(value);
                }
            } else if (ResXmlUtils.ANDROID_RES_NS.equals(ns)) {
                if (name.equals("versionCode")) {
                    mApkInfo.getVersionInfo().setVersionCode(Integer.parseInt(value));
                    // Exclude the attribute: injected in build time.
                    if (!mAnalysisMode) {
                        return true;
                    }
                } else if (name.equals("versionName")) {
                    mApkInfo.getVersionInfo().setVersionName(value);
                    // Exclude the attribute: injected in build time.
                    if (!mAnalysisMode) {
                        return true;
                    }
                }
            }
        }

        return super.onAttribute(in, out, ns, name, value);
    }
}

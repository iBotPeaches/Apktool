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
import brut.androlib.meta.ResourcesInfo;
import brut.androlib.meta.SdkInfo;
import brut.androlib.meta.VersionInfo;
import brut.androlib.res.xml.ResXmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ManifestPullEventHandler extends ResXmlPullEventHandler {
    private final boolean mHideSdkInfo;

    public ManifestPullEventHandler(ApkInfo apkInfo, boolean hideSdkInfo) {
        super(apkInfo);
        mHideSdkInfo = hideSdkInfo;
    }

    @Override
    public boolean onEvent(XmlPullParser in, XmlSerializer out) throws XmlPullParserException {
        int depth = in.getDepth();
        int type = in.getEventType();

        if (depth == 1) {
            if (type == XmlPullParser.START_TAG) {
                if (in.getName().equals("manifest")) {
                    parseManifest(in);
                    return false;
                }
            }
        } else if (depth == 2) {
            if (type == XmlPullParser.START_TAG || type == XmlPullParser.END_TAG) {
                if (in.getName().equals("uses-sdk")) {
                    if (type == XmlPullParser.START_TAG) {
                        parseUsesSdk(in);
                    }
                    return mHideSdkInfo;
                }
            }
        }

        return super.onEvent(in, out);
    }

    private void parseManifest(XmlPullParser in) {
        ResourcesInfo resourcesInfo = mApkInfo.getResourcesInfo();
        VersionInfo versionInfo = mApkInfo.getVersionInfo();

        for (int i = 0; i < in.getAttributeCount(); i++) {
            String ns = in.getAttributeNamespace(i);

            if (ns.isEmpty()) {
                String name = in.getAttributeName(i);

                if (name.equals("package")) {
                    // This is temporary and will be compared to actual resources package later.
                    resourcesInfo.setPackageName(in.getAttributeValue(i));
                }
            } else if (ns.equals(ResXmlUtils.ANDROID_RES_NS)) {
                String name = in.getAttributeName(i);

                if (name.equals("versionCode")) {
                    versionInfo.setVersionCode(Integer.parseInt(in.getAttributeValue(i)));
                } else if (name.equals("versionName")) {
                    versionInfo.setVersionName(in.getAttributeValue(i));
                }
            }
        }
    }

    private void parseUsesSdk(XmlPullParser in) {
        SdkInfo sdkInfo = mApkInfo.getSdkInfo();

        for (int i = 0; i < in.getAttributeCount(); i++) {
            String ns = in.getAttributeNamespace(i);

            if (ns.equals(ResXmlUtils.ANDROID_RES_NS)) {
                String name = in.getAttributeName(i);

                if (name.equals("minSdkVersion")) {
                    sdkInfo.setMinSdkVersion(in.getAttributeValue(i));
                } else if (name.equals("targetSdkVersion")) {
                    sdkInfo.setTargetSdkVersion(in.getAttributeValue(i));
                } else if (name.equals("maxSdkVersion")) {
                    sdkInfo.setMaxSdkVersion(in.getAttributeValue(i));
                }
            }
        }
    }
}

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
import brut.xmlpull.XmlPullUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ManifestPullEventHandler implements XmlPullUtils.EventHandler {
    private final ApkInfo mApkInfo;
    private final boolean mHideSdkInfo;

    public ManifestPullEventHandler(ApkInfo apkInfo, boolean hideSdkInfo) {
        mApkInfo = apkInfo;
        mHideSdkInfo = hideSdkInfo;
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
        ResourcesInfo resourcesInfo = mApkInfo.getResourcesInfo();
        VersionInfo versionInfo = mApkInfo.getVersionInfo();

        for (int i = 0; i < in.getAttributeCount(); i++) {
            String ns = in.getAttributeNamespace(i);
            String name = in.getAttributeName(i);
            String value = in.getAttributeValue(i);

            if (value.isEmpty()) {
                continue;
            }
            if (ns.isEmpty()) {
                if (name.equals("package")) {
                    // This is temporary and will be compared to actual resources package later.
                    resourcesInfo.setPackageName(value);
                }
            } else if (ns.equals(ResXmlUtils.ANDROID_RES_NS)) {
                switch (name) {
                    case "versionCode":
                        versionInfo.setVersionCode(Integer.parseInt(value));
                        break;
                    case "versionName":
                        versionInfo.setVersionName(value);
                        break;
                }
            }
        }
    }

    private void parseUsesSdk(XmlPullParser in) {
        SdkInfo sdkInfo = mApkInfo.getSdkInfo();

        for (int i = 0; i < in.getAttributeCount(); i++) {
            String ns = in.getAttributeNamespace(i);
            String name = in.getAttributeName(i);
            String value = in.getAttributeValue(i);

            if (value.isEmpty()) {
                continue;
            }
            if (ns.equals(ResXmlUtils.ANDROID_RES_NS)) {
                switch (name) {
                    case "minSdkVersion":
                        sdkInfo.setMinSdkVersion(value);
                        break;
                    case "targetSdkVersion":
                        sdkInfo.setTargetSdkVersion(value);
                        break;
                    case "maxSdkVersion":
                        sdkInfo.setMaxSdkVersion(value);
                        break;
                }
            }
        }
    }
}

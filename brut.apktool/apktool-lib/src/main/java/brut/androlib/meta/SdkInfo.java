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
package brut.androlib.meta;

import brut.androlib.res.table.ResConfig;
import brut.yaml.*;

public class SdkInfo implements YamlSerializable {
    private String mMinSdkVersion;
    private String mTargetSdkVersion;
    private String mMaxSdkVersion;

    public SdkInfo() {
        clear();
    }

    public void clear() {
        mMinSdkVersion = null;
        mTargetSdkVersion = null;
        mMaxSdkVersion = null;
    }

    public boolean isEmpty() {
        return mMinSdkVersion == null && mTargetSdkVersion == null && mMaxSdkVersion == null;
    }

    @Override
    public void readItem(YamlReader reader) {
        YamlLine line = reader.getLine();
        switch (line.getKey()) {
            case "minSdkVersion": {
                mMinSdkVersion = line.getValue();
                break;
            }
            case "targetSdkVersion": {
                mTargetSdkVersion = line.getValue();
                break;
            }
            case "maxSdkVersion": {
                mMaxSdkVersion = line.getValue();
                break;
            }
        }
    }

    @Override
    public void write(YamlWriter writer) {
        if (mMinSdkVersion != null) {
            writer.writeString("minSdkVersion", mMinSdkVersion);
        }
        if (mTargetSdkVersion != null) {
            writer.writeString("targetSdkVersion", mTargetSdkVersion);
        }
        if (mMaxSdkVersion != null) {
            writer.writeString("maxSdkVersion", mMaxSdkVersion);
        }
    }

    public String getMinSdkVersion() {
        return mMinSdkVersion;
    }

    public void setMinSdkVersion(String minSdkVersion) {
        mMinSdkVersion = minSdkVersion;
    }

    public String getTargetSdkVersion() {
        return mTargetSdkVersion;
    }

    public String getTargetSdkVersionBounded() {
        int target = parseSdkInt(mTargetSdkVersion);
        int min = mMinSdkVersion != null ? parseSdkInt(mMinSdkVersion) : 0;
        int max = mMaxSdkVersion != null ? parseSdkInt(mMaxSdkVersion) : target;
        return Integer.toString(Math.max(min, Math.min(max, target)));
    }

    public void setTargetSdkVersion(String targetSdkVersion) {
        mTargetSdkVersion = targetSdkVersion;
    }

    public String getMaxSdkVersion() {
        return mMaxSdkVersion;
    }

    public void setMaxSdkVersion(String maxSdkVersion) {
        mMaxSdkVersion = maxSdkVersion;
    }

    public static int parseSdkInt(String sdkVersion) {
        switch (sdkVersion.toUpperCase()) {
            case "M":
                return ResConfig.SDK_MNC;
            case "N":
                return ResConfig.SDK_NOUGAT;
            case "O":
                return ResConfig.SDK_OREO;
            case "P":
                return ResConfig.SDK_P;
            case "Q":
                return ResConfig.SDK_Q;
            case "R":
                return ResConfig.SDK_R;
            case "S":
                return ResConfig.SDK_S;
            case "SV2":
                return ResConfig.SDK_S_V2;
            case "T":
            case "TIRAMISU":
                return ResConfig.SDK_TIRAMISU;
            case "UPSIDEDOWNCAKE":
            case "UPSIDE_DOWN_CAKE":
                return ResConfig.SDK_UPSIDEDOWN_CAKE;
            case "VANILLAICECREAM":
            case "VANILLA_ICE_CREAM":
                return ResConfig.SDK_VANILLA_ICE_CREAM;
            case "BAKLAVA":
                return ResConfig.SDK_BAKLAVA;
            case "SDK_CUR_DEVELOPMENT":
                return ResConfig.SDK_DEVELOPMENT;
            default:
                return Integer.parseInt(sdkVersion);
        }
    }
}

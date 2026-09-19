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

import brut.yaml.*;

import java.io.IOException;

public class SdkInfo implements YamlSerializable {
    public static final int SDK_BASE = 1;
    public static final int SDK_BASE_1_1 = 2;
    public static final int SDK_CUPCAKE = 3;
    public static final int SDK_DONUT = 4;
    public static final int SDK_ECLAIR = 5;
    public static final int SDK_ECLAIR_0_1 = 6;
    public static final int SDK_ECLAIR_MR1 = 7;
    public static final int SDK_FROYO = 8;
    public static final int SDK_GINGERBREAD = 9;
    public static final int SDK_GINGERBREAD_MR1 = 10;
    public static final int SDK_HONEYCOMB = 11;
    public static final int SDK_HONEYCOMB_MR1 = 12;
    public static final int SDK_HONEYCOMB_MR2 = 13;
    public static final int SDK_ICE_CREAM_SANDWICH = 14;
    public static final int SDK_ICE_CREAM_SANDWICH_MR1 = 15;
    public static final int SDK_JELLY_BEAN = 16;
    public static final int SDK_JELLY_BEAN_MR1 = 17;
    public static final int SDK_JELLY_BEAN_MR2 = 18;
    public static final int SDK_KITKAT = 19;
    public static final int SDK_KITKAT_WATCH = 20;
    public static final int SDK_LOLLIPOP = 21;
    public static final int SDK_LOLLIPOP_MR1 = 22;
    public static final int SDK_MARSHMALLOW = 23;
    public static final int SDK_NOUGAT = 24;
    public static final int SDK_NOUGAT_MR1 = 25;
    public static final int SDK_O = 26;
    public static final int SDK_O_MR1 = 27;
    public static final int SDK_P = 28;
    public static final int SDK_Q = 29;
    public static final int SDK_R = 30;
    public static final int SDK_S = 31;
    public static final int SDK_S_V2 = 32;
    public static final int SDK_TIRAMISU = 33;
    public static final int SDK_UPSIDE_DOWN_CAKE = 34;
    public static final int SDK_VANILLA_ICE_CREAM = 35;
    public static final int SDK_BAKLAVA = 36;
    public static final int SDK_CINNAMON_BUN = 37;
    public static final int SDK_CUR_DEVELOPMENT = 10000;

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
        return mMinSdkVersion == null
            && mTargetSdkVersion == null
            && mMaxSdkVersion == null;
    }

    @Override
    public void onEntry(YamlPullParser parser) throws IOException {
        switch (parser.getKey()) {
            case "minSdkVersion":
                mMinSdkVersion = parser.getString();
                break;
            case "targetSdkVersion":
                mTargetSdkVersion = parser.getString();
                break;
            case "maxSdkVersion":
                mMaxSdkVersion = parser.getString();
                break;
        }
    }

    @Override
    public void serialize(YamlSerializer serial) throws IOException {
        if (mMinSdkVersion != null) {
            serial.writeString("minSdkVersion", mMinSdkVersion);
        }
        if (mTargetSdkVersion != null) {
            serial.writeString("targetSdkVersion", mTargetSdkVersion);
        }
        if (mMaxSdkVersion != null) {
            serial.writeString("maxSdkVersion", mMaxSdkVersion);
        }
    }

    public String getMinSdkVersion() {
        return mMinSdkVersion;
    }

    public int getMinSdkVersionInt() {
        return toSdkVersionInt(mMinSdkVersion);
    }

    public void setMinSdkVersion(String minSdkVersion) {
        mMinSdkVersion = minSdkVersion;
    }

    public String getTargetSdkVersion() {
        return mTargetSdkVersion;
    }

    public int getTargetSdkVersionInt() {
        return toSdkVersionInt(mTargetSdkVersion);
    }

    public void setTargetSdkVersion(String targetSdkVersion) {
        mTargetSdkVersion = targetSdkVersion;
    }

    public String getMaxSdkVersion() {
        return mMaxSdkVersion;
    }

    public int getMaxSdkVersionInt() {
        return toSdkVersionInt(mMaxSdkVersion);
    }

    public void setMaxSdkVersion(String maxSdkVersion) {
        mMaxSdkVersion = maxSdkVersion;
    }

    private static int toSdkVersionInt(String sdkVersion) {
        if (sdkVersion == null || sdkVersion.isEmpty()) {
            return 0;
        }
        char first = sdkVersion.charAt(0);
        if (first < 'A' || first > 'Z') {
            long versionInt;
            try {
                versionInt = Long.parseLong(sdkVersion);
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Invalid version: " + sdkVersion);
            }
            if (versionInt < 0) {
                throw new IllegalArgumentException("Negative version: " + sdkVersion);
            }
            if (versionInt > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Version too large: " + sdkVersion);
            }
            return (int) versionInt;
        }
        switch (sdkVersion) {
            case "Base":
                return SDK_BASE;
            case "Base11":
                return SDK_BASE_1_1;
            case "Cupcake":
                return SDK_CUPCAKE;
            case "Donut":
                return SDK_DONUT;
            case "Eclair":
                return SDK_ECLAIR;
            case "Eclair01":
                return SDK_ECLAIR_0_1;
            case "EclairMr1":
                return SDK_ECLAIR_MR1;
            case "Froyo":
                return SDK_FROYO;
            case "Gingerbread":
                return SDK_GINGERBREAD;
            case "GingerbreadMr1":
                return SDK_GINGERBREAD_MR1;
            case "Honeycomb":
                return SDK_HONEYCOMB;
            case "HoneycombMr1":
                return SDK_HONEYCOMB_MR1;
            case "HoneycombMr2":
                return SDK_HONEYCOMB_MR2;
            case "IceCreamSandwich":
                return SDK_ICE_CREAM_SANDWICH;
            case "IceCreamSandwichMr1":
                return SDK_ICE_CREAM_SANDWICH_MR1;
            case "JellyBean":
                return SDK_JELLY_BEAN;
            case "JellyBeanMr1":
                return SDK_JELLY_BEAN_MR1;
            case "JellyBeanMr2":
                return SDK_JELLY_BEAN_MR2;
            case "Kitkat":
                return SDK_KITKAT;
            case "KitkatWatch":
                return SDK_KITKAT_WATCH;
            case "Lollipop":
                return SDK_LOLLIPOP;
            case "LollipopMr1":
                return SDK_LOLLIPOP_MR1;
            case "M":
                return SDK_MARSHMALLOW;
            case "N":
                return SDK_NOUGAT;
            case "NMr1":
                return SDK_NOUGAT_MR1;
            case "O":
                return SDK_O;
            case "OMr1":
                return SDK_O_MR1;
            case "P":
                return SDK_P;
            case "Q":
                return SDK_Q;
            case "R":
                return SDK_R;
            case "S":
                return SDK_S;
            case "Sv2":
                return SDK_S_V2;
            case "Tiramisu":
                return SDK_TIRAMISU;
            case "UpsideDownCake":
                return SDK_UPSIDE_DOWN_CAKE;
            case "VanillaIceCream":
                return SDK_VANILLA_ICE_CREAM;
            case "Baklava":
                return SDK_BAKLAVA;
            case "CinnamonBun":
                return SDK_CINNAMON_BUN;
            default:
                return SDK_CUR_DEVELOPMENT;
        }
    }
}

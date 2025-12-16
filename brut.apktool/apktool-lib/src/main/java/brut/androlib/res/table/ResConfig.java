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
package brut.androlib.res.table;

import java.util.Arrays;

public class ResConfig {
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
    public static final int SDK_LOLLIPOP = 21;
    public static final int SDK_LOLLIPOP_MR1 = 22;
    public static final int SDK_MNC = 23;
    public static final int SDK_NOUGAT = 24;
    public static final int SDK_NOUGAT_MR1 = 25;
    public static final int SDK_OREO = 26;
    public static final int SDK_OREO_MR1 = 27;
    public static final int SDK_P = 28;
    public static final int SDK_Q = 29;
    public static final int SDK_R = 30;
    public static final int SDK_S = 31;
    public static final int SDK_S_V2 = 32;
    public static final int SDK_TIRAMISU = 33;
    public static final int SDK_UPSIDEDOWN_CAKE = 34;
    public static final int SDK_VANILLA_ICE_CREAM = 35;
    public static final int SDK_BAKLAVA = 36;

    // AOSP has this as 10,000 for dev purposes.
    // platform_frameworks_base/commit/c7a1109a1fe0771d4c9b572dcf178e2779fc4f2d
    public static final int SDK_DEVELOPMENT = 10000;

    public static final int MNC_ZERO = 0xFFFF;

    public static final int ORIENTATION_ANY = 0x00;
    public static final int ORIENTATION_PORT = 0x01;
    public static final int ORIENTATION_LAND = 0x02;
    public static final int ORIENTATION_SQUARE = 0x03;

    public static final int TOUCHSCREEN_ANY = 0x00;
    public static final int TOUCHSCREEN_NOTOUCH = 0x01;
    public static final int TOUCHSCREEN_STYLUS = 0x02;
    public static final int TOUCHSCREEN_FINGER = 0x03;

    public static final int DENSITY_DEFAULT = 0;
    public static final int DENSITY_LOW = 120;
    public static final int DENSITY_MEDIUM = 160;
    public static final int DENSITY_TV = 213;
    public static final int DENSITY_HIGH = 240;
    public static final int DENSITY_XHIGH = 320;
    public static final int DENSITY_XXHIGH = 480;
    public static final int DENSITY_XXXHIGH = 640;
    public static final int DENSITY_ANY = 0xFFFE;
    public static final int DENSITY_NONE = 0xFFFF;

    public static final int KEYBOARD_ANY = 0x00;
    public static final int KEYBOARD_NOKEYS = 0x01;
    public static final int KEYBOARD_QWERTY = 0x02;
    public static final int KEYBOARD_12KEY = 0x03;

    public static final int NAVIGATION_ANY = 0x00;
    public static final int NAVIGATION_NONAV = 0x01;
    public static final int NAVIGATION_DPAD = 0x02;
    public static final int NAVIGATION_TRACKBALL = 0x03;
    public static final int NAVIGATION_WHEEL = 0x04;

    public static final int MASK_KEYSHIDDEN = 0x03;
    public static final int KEYSHIDDEN_ANY = 0x00;
    public static final int KEYSHIDDEN_NO = 0x01;
    public static final int KEYSHIDDEN_YES = 0x02;
    public static final int KEYSHIDDEN_SOFT = 0x03;

    public static final int SHIFT_NAVHIDDEN = 2;
    public static final int MASK_NAVHIDDEN = 0x03 << SHIFT_NAVHIDDEN; // 0x0C
    public static final int NAVHIDDEN_ANY = 0x00 << SHIFT_NAVHIDDEN; // 0x00
    public static final int NAVHIDDEN_NO = 0x01 << SHIFT_NAVHIDDEN; // 0x04
    public static final int NAVHIDDEN_YES = 0x02 << SHIFT_NAVHIDDEN; // 0x08

    public static final int MASK_GRAMMATICAL_GENDER = 0x03;
    public static final int GRAMMATICAL_GENDER_ANY = 0x00;
    public static final int GRAMMATICAL_GENDER_NEUTER = 0x01;
    public static final int GRAMMATICAL_GENDER_FEMININE = 0x02;
    public static final int GRAMMATICAL_GENDER_MASCULINE = 0x03;

    public static final int MASK_SCREENSIZE = 0x0F;
    public static final int SCREENSIZE_ANY = 0x00;
    public static final int SCREENSIZE_SMALL = 0x01;
    public static final int SCREENSIZE_NORMAL = 0x02;
    public static final int SCREENSIZE_LARGE = 0x03;
    public static final int SCREENSIZE_XLARGE = 0x04;

    public static final int SHIFT_SCREENLONG = 4;
    public static final int MASK_SCREENLONG = 0x03 << SHIFT_SCREENLONG; // 0x30
    public static final int SCREENLONG_ANY = 0x00 << SHIFT_SCREENLONG; // 0x00
    public static final int SCREENLONG_NO = 0x01 << SHIFT_SCREENLONG; // 0x10
    public static final int SCREENLONG_YES = 0x02 << SHIFT_SCREENLONG; // 0x20

    public static final int SHIFT_LAYOUTDIR = 6;
    public static final int MASK_LAYOUTDIR = 0x03 << SHIFT_LAYOUTDIR; // 0xC0
    public static final int LAYOUTDIR_ANY = 0x00 << SHIFT_LAYOUTDIR; // 0x00
    public static final int LAYOUTDIR_LTR = 0x01 << SHIFT_LAYOUTDIR; // 0x40
    public static final int LAYOUTDIR_RTL = 0x02 << SHIFT_LAYOUTDIR; // 0x80

    public static final int MASK_UI_MODE_TYPE = 0x0F;
    public static final int UI_MODE_TYPE_ANY = 0x00;
    public static final int UI_MODE_TYPE_NORMAL = 0x01;
    public static final int UI_MODE_TYPE_DESK = 0x02;
    public static final int UI_MODE_TYPE_CAR = 0x03;
    public static final int UI_MODE_TYPE_TELEVISION = 0x04;
    public static final int UI_MODE_TYPE_APPLIANCE = 0x05;
    public static final int UI_MODE_TYPE_WATCH = 0x06;
    public static final int UI_MODE_TYPE_VR_HEADSET = 0x07;
    public static final int UI_MODE_TYPE_GODZILLAUI = 0x0B; // MIUI
    public static final int UI_MODE_TYPE_SMALLUI = 0x0C; // MIUI
    public static final int UI_MODE_TYPE_MEDIUMUI = 0x0D; // MIUI
    public static final int UI_MODE_TYPE_LARGEUI = 0x0E; // MIUI
    public static final int UI_MODE_TYPE_HUGEUI = 0x0F; // MIUI

    public static final int SHIFT_UI_MODE_NIGHT = 4;
    public static final int MASK_UI_MODE_NIGHT = 0x03 << SHIFT_UI_MODE_NIGHT; // 0x30
    public static final int UI_MODE_NIGHT_ANY = 0x00 << SHIFT_UI_MODE_NIGHT; // 0x00
    public static final int UI_MODE_NIGHT_NO = 0x01 << SHIFT_UI_MODE_NIGHT; // 0x10
    public static final int UI_MODE_NIGHT_YES = 0x02 << SHIFT_UI_MODE_NIGHT; // 0x20

    public static final int MASK_SCREENROUND = 0x03;
    public static final int SCREENROUND_ANY = 0x00;
    public static final int SCREENROUND_NO = 0x01;
    public static final int SCREENROUND_YES = 0x02;

    public static final int MASK_COLOR_MODE_WIDECG = 0x03;
    public static final int COLOR_MODE_WIDECG_ANY = 0x00;
    public static final int COLOR_MODE_WIDECG_NO = 0x01;
    public static final int COLOR_MODE_WIDECG_YES = 0x02;

    public static final int SHIFT_COLOR_MODE_HDR = 2;
    public static final int MASK_COLOR_MODE_HDR = 0x03 << SHIFT_COLOR_MODE_HDR; // 0x0C
    public static final int COLOR_MODE_HDR_ANY = 0x00 << SHIFT_COLOR_MODE_HDR; // 0x00
    public static final int COLOR_MODE_HDR_NO = 0x01 << SHIFT_COLOR_MODE_HDR; // 0x04
    public static final int COLOR_MODE_HDR_YES = 0x02 << SHIFT_COLOR_MODE_HDR; // 0x08

    public static final ResConfig DEFAULT = new ResConfig();

    private final int mMcc;
    private final int mMnc;
    private final String mLanguage;
    private final String mRegion;
    private final int mOrientation;
    private final int mTouchscreen;
    private final int mDensity;
    private final int mKeyboard;
    private final int mNavigation;
    private final int mInputFlags;
    private final int mGrammaticalInflection;
    private final int mScreenWidth;
    private final int mScreenHeight;
    private final int mSdkVersion;
    private final int mMinorVersion;
    private final int mScreenLayout;
    private final int mUiMode;
    private final int mSmallestScreenWidthDp;
    private final int mScreenWidthDp;
    private final int mScreenHeightDp;
    private final String mLocaleScript;
    private final String mLocaleVariant;
    private final int mScreenLayout2;
    private final int mColorMode;
    private final byte[] mUnknown;

    private final String mQualifiers;
    private boolean mIsInvalid;

    private ResConfig() {
        mMcc = 0;
        mMnc = 0;
        mLanguage = "";
        mRegion = "";
        mOrientation = ORIENTATION_ANY;
        mTouchscreen = TOUCHSCREEN_ANY;
        mDensity = DENSITY_DEFAULT;
        mKeyboard = KEYBOARD_ANY;
        mNavigation = NAVIGATION_ANY;
        mInputFlags = KEYSHIDDEN_ANY | NAVHIDDEN_ANY;
        mGrammaticalInflection = GRAMMATICAL_GENDER_ANY;
        mScreenWidth = 0;
        mScreenHeight = 0;
        mSdkVersion = 0;
        mMinorVersion = 0;
        mScreenLayout = SCREENSIZE_ANY | SCREENLONG_ANY;
        mUiMode = UI_MODE_TYPE_ANY | UI_MODE_NIGHT_ANY;
        mSmallestScreenWidthDp = 0;
        mScreenWidthDp = 0;
        mScreenHeightDp = 0;
        mLocaleScript = "";
        mLocaleVariant = "";
        mScreenLayout2 = 0;
        mColorMode = COLOR_MODE_WIDECG_ANY | COLOR_MODE_HDR_ANY;
        mUnknown = null;
        mQualifiers = "";
    }

    public ResConfig(int mcc, int mnc, String language, String region, int orientation,
                     int touchscreen, int density, int keyboard, int navigation, int inputFlags,
                     int grammaticalInflection, int screenWidth, int screenHeight, int sdkVersion,
                     int minorVersion, int screenLayout, int uiMode, int smallestScreenWidthDp,
                     int screenWidthDp, int screenHeightDp, String localeScript, String localeVariant,
                     int screenLayout2, int colorMode, byte[] unknown) {
        mMcc = mcc;
        mMnc = mnc;
        mLanguage = language;
        mRegion = region;
        mOrientation = orientation;
        mTouchscreen = touchscreen;
        mDensity = density;
        mKeyboard = keyboard;
        mNavigation = navigation;
        mInputFlags = inputFlags;
        mGrammaticalInflection = grammaticalInflection;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mSdkVersion = sdkVersion;
        mMinorVersion = minorVersion;
        mScreenLayout = screenLayout;
        mUiMode = uiMode;
        mSmallestScreenWidthDp = smallestScreenWidthDp;
        mScreenWidthDp = screenWidthDp;
        mScreenHeightDp = screenHeightDp;
        mLocaleScript = localeScript;
        mLocaleVariant = localeVariant;
        mScreenLayout2 = screenLayout2;
        mColorMode = colorMode;
        mUnknown = unknown;
        mQualifiers = generateQualifiers();
    }

    private String generateQualifiers() {
        StringBuilder sb = new StringBuilder();
        if (mMcc != 0) {
            sb.append("-mcc").append(String.format("%03d", mMcc));
        }
        if (mMnc != 0) {
            sb.append("-mnc").append(String.format("%02d", mMnc == MNC_ZERO ? 0 : mMnc));
        }
        if (!mLanguage.isEmpty()) {
            if (mLocaleScript.isEmpty() && (mRegion.isEmpty() || mRegion.length() == 2)
                    && mLocaleVariant.isEmpty()) {
                // Legacy format.
                sb.append('-').append(mLanguage);
                if (!mRegion.isEmpty()) {
                    sb.append("-r").append(mRegion);
                }
            } else {
                // Modified BCP 47 format.
                sb.append("-b+");
                sb.append(mLanguage);
                if (!mLocaleScript.isEmpty()) {
                    sb.append('+').append(mLocaleScript);
                }
                if (!mRegion.isEmpty()) {
                    sb.append('+').append(mRegion);
                }
                if (!mLocaleVariant.isEmpty()) {
                    sb.append('+').append(mLocaleVariant);
                }
            }
        }
        switch (mGrammaticalInflection & MASK_GRAMMATICAL_GENDER) {
            case GRAMMATICAL_GENDER_ANY:
                break;
            case GRAMMATICAL_GENDER_NEUTER:
                sb.append("-neuter");
                break;
            case GRAMMATICAL_GENDER_FEMININE:
                sb.append("-feminine");
                break;
            case GRAMMATICAL_GENDER_MASCULINE:
                sb.append("-masculine");
                break;
            default:
                sb.append("-grammaticalGender=").append(mGrammaticalInflection & MASK_GRAMMATICAL_GENDER);
                mIsInvalid = true;
                break;
        }
        switch (mScreenLayout & MASK_LAYOUTDIR) {
            case LAYOUTDIR_ANY:
                break;
            case LAYOUTDIR_LTR:
                sb.append("-ldltr");
                break;
            case LAYOUTDIR_RTL:
                sb.append("-ldrtl");
                break;
            default:
                sb.append("-layoutDir=").append(mScreenLayout & MASK_LAYOUTDIR);
                mIsInvalid = true;
                break;
        }
        if (mSmallestScreenWidthDp != 0) {
            sb.append("-sw").append(mSmallestScreenWidthDp).append("dp");
        }
        if (mScreenWidthDp != 0) {
            sb.append("-w").append(mScreenWidthDp).append("dp");
        }
        if (mScreenHeightDp != 0) {
            sb.append("-h").append(mScreenHeightDp).append("dp");
        }
        switch (mScreenLayout & MASK_SCREENSIZE) {
            case SCREENSIZE_ANY:
                break;
            case SCREENSIZE_SMALL:
                sb.append("-small");
                break;
            case SCREENSIZE_NORMAL:
                sb.append("-normal");
                break;
            case SCREENSIZE_LARGE:
                sb.append("-large");
                break;
            case SCREENSIZE_XLARGE:
                sb.append("-xlarge");
                break;
            default:
                sb.append("-screenSize=").append(mScreenLayout & MASK_SCREENSIZE);
                mIsInvalid = true;
                break;
        }
        switch (mScreenLayout & MASK_SCREENLONG) {
            case SCREENLONG_ANY:
                break;
            case SCREENLONG_NO:
                sb.append("-notlong");
                break;
            case SCREENLONG_YES:
                sb.append("-long");
                break;
            default:
                sb.append("-screenLong=").append(mScreenLayout & MASK_SCREENLONG);
                mIsInvalid = true;
                break;
        }
        switch (mScreenLayout2 & MASK_SCREENROUND) {
            case SCREENROUND_ANY:
                break;
            case SCREENROUND_NO:
                sb.append("-notround");
                break;
            case SCREENROUND_YES:
                sb.append("-round");
                break;
            default:
                sb.append("-screenRound=").append(mScreenLayout2 & MASK_SCREENROUND);
                mIsInvalid = true;
                break;
        }
        switch (mColorMode & MASK_COLOR_MODE_WIDECG) {
            case COLOR_MODE_WIDECG_ANY:
                break;
            case COLOR_MODE_WIDECG_NO:
                sb.append("-nowidecg");
                break;
            case COLOR_MODE_WIDECG_YES:
                sb.append("-widecg");
                break;
            default:
                sb.append("-colorModeWideCG=").append(mColorMode & MASK_COLOR_MODE_WIDECG);
                mIsInvalid = true;
                break;
        }
        switch (mColorMode & MASK_COLOR_MODE_HDR) {
            case COLOR_MODE_HDR_ANY:
                break;
            case COLOR_MODE_HDR_NO:
                sb.append("-lowdr");
                break;
            case COLOR_MODE_HDR_YES:
                sb.append("-highdr");
                break;
            default:
                sb.append("-colorModeHdr=").append(mColorMode & MASK_COLOR_MODE_HDR);
                mIsInvalid = true;
                break;
        }
        switch (mOrientation) {
            case ORIENTATION_ANY:
                break;
            case ORIENTATION_PORT:
                sb.append("-port");
                break;
            case ORIENTATION_LAND:
                sb.append("-land");
                break;
            case ORIENTATION_SQUARE:
                sb.append("-square");
                break;
            default:
                sb.append("-orientation=").append(mOrientation);
                mIsInvalid = true;
                break;
        }
        switch (mUiMode & MASK_UI_MODE_TYPE) {
            case UI_MODE_TYPE_ANY:
            case UI_MODE_TYPE_NORMAL:
                break;
            case UI_MODE_TYPE_DESK:
                sb.append("-desk");
                break;
            case UI_MODE_TYPE_CAR:
                sb.append("-car");
                break;
            case UI_MODE_TYPE_TELEVISION:
                sb.append("-television");
                break;
            case UI_MODE_TYPE_APPLIANCE:
                sb.append("-appliance");
                break;
            case UI_MODE_TYPE_WATCH:
                sb.append("-watch");
                break;
            case UI_MODE_TYPE_VR_HEADSET:
                sb.append("-vrheadset");
                break;
            case UI_MODE_TYPE_GODZILLAUI:
                sb.append("-godzillaui");
                break;
            case UI_MODE_TYPE_SMALLUI:
                sb.append("-smallui");
                break;
            case UI_MODE_TYPE_MEDIUMUI:
                sb.append("-mediumui");
                break;
            case UI_MODE_TYPE_LARGEUI:
                sb.append("-largeui");
                break;
            case UI_MODE_TYPE_HUGEUI:
                sb.append("-hugeui");
                break;
            default:
                sb.append("-uiModeType=").append(mUiMode & MASK_UI_MODE_TYPE);
                mIsInvalid = true;
                break;
        }
        switch (mUiMode & MASK_UI_MODE_NIGHT) {
            case UI_MODE_NIGHT_ANY:
                break;
            case UI_MODE_NIGHT_NO:
                sb.append("-notnight");
                break;
            case UI_MODE_NIGHT_YES:
                sb.append("-night");
                break;
            default:
                sb.append("-uiModeNight=").append(mUiMode & MASK_UI_MODE_NIGHT);
                mIsInvalid = true;
                break;
        }
        switch (mDensity) {
            case DENSITY_DEFAULT:
                break;
            case DENSITY_LOW:
                sb.append("-ldpi");
                break;
            case DENSITY_MEDIUM:
                sb.append("-mdpi");
                break;
            case DENSITY_TV:
                sb.append("-tvdpi");
                break;
            case DENSITY_HIGH:
                sb.append("-hdpi");
                break;
            case DENSITY_XHIGH:
                sb.append("-xhdpi");
                break;
            case DENSITY_XXHIGH:
                sb.append("-xxhdpi");
                break;
            case DENSITY_XXXHIGH:
                sb.append("-xxxhdpi");
                break;
            case DENSITY_ANY:
                sb.append("-anydpi");
                break;
            case DENSITY_NONE:
                sb.append("-nodpi");
                break;
            default:
                sb.append('-').append(mDensity).append("dpi");
                break;
        }
        switch (mTouchscreen) {
            case TOUCHSCREEN_ANY:
                break;
            case TOUCHSCREEN_NOTOUCH:
                sb.append("-notouch");
                break;
            case TOUCHSCREEN_STYLUS:
                sb.append("-stylus");
                break;
            case TOUCHSCREEN_FINGER:
                sb.append("-finger");
                break;
            default:
                sb.append("-touchscreen=").append(mTouchscreen);
                mIsInvalid = true;
                break;
        }
        switch (mInputFlags & MASK_KEYSHIDDEN) {
            case KEYSHIDDEN_ANY:
                break;
            case KEYSHIDDEN_NO:
                sb.append("-keysexposed");
                break;
            case KEYSHIDDEN_YES:
                sb.append("-keyshidden");
                break;
            case KEYSHIDDEN_SOFT:
                sb.append("-keyssoft");
                break;
            default:
                sb.append("-keysHidden=").append(mInputFlags & MASK_KEYSHIDDEN);
                mIsInvalid = true;
                break;
        }
        switch (mKeyboard) {
            case KEYBOARD_ANY:
                break;
            case KEYBOARD_NOKEYS:
                sb.append("-nokeys");
                break;
            case KEYBOARD_QWERTY:
                sb.append("-qwerty");
                break;
            case KEYBOARD_12KEY:
                sb.append("-12key");
                break;
            default:
                sb.append("-keyboard=").append(mKeyboard);
                mIsInvalid = true;
                break;
        }
        switch (mInputFlags & MASK_NAVHIDDEN) {
            case NAVHIDDEN_ANY:
                break;
            case NAVHIDDEN_NO:
                sb.append("-navexposed");
                break;
            case NAVHIDDEN_YES:
                sb.append("-navhidden");
                break;
            default:
                sb.append("-navHidden=").append(mInputFlags & MASK_NAVHIDDEN);
                mIsInvalid = true;
                break;
        }
        switch (mNavigation) {
            case NAVIGATION_ANY:
                break;
            case NAVIGATION_NONAV:
                sb.append("-nonav");
                break;
            case NAVIGATION_DPAD:
                sb.append("-dpad");
                break;
            case NAVIGATION_TRACKBALL:
                sb.append("-trackball");
                break;
            case NAVIGATION_WHEEL:
                sb.append("-wheel");
                break;
            default:
                sb.append("-navigation=").append(mNavigation);
                mIsInvalid = true;
                break;
        }
        if (mScreenWidth != 0 && mScreenHeight != 0) {
            sb.append('-').append(mScreenWidth).append('x').append(mScreenHeight);
        }
        if (mSdkVersion != 0) {
            sb.append("-v").append(mSdkVersion);
        }
        if (mUnknown != null) {
            // We have to separate unknown resources to avoid conflicts.
            sb.append("-unk").append(String.format("%08X", Arrays.hashCode(mUnknown)));
            mIsInvalid = true;
        }
        return sb.toString();
    }

    public boolean isInvalid() {
        return mIsInvalid;
    }

    public String getQualifiers() {
        return mQualifiers;
    }

    @Override
    public String toString() {
        return "[" + (!mQualifiers.isEmpty() ? mQualifiers.substring(1) : "DEFAULT") + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResConfig) {
            ResConfig other = (ResConfig) obj;
            return mQualifiers.equals(other.mQualifiers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mQualifiers.hashCode();
    }
}

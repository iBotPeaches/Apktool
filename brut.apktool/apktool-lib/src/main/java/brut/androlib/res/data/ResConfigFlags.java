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
package brut.androlib.res.data;

import java.util.logging.Logger;

public class ResConfigFlags {
    private static final Logger LOGGER = Logger.getLogger(ResConfigFlags.class.getName());

    public static final byte SDK_BASE = 1;
    public static final byte SDK_BASE_1_1 = 2;
    public static final byte SDK_CUPCAKE = 3;
    public static final byte SDK_DONUT = 4;
    public static final byte SDK_ECLAIR = 5;
    public static final byte SDK_ECLAIR_0_1 = 6;
    public static final byte SDK_ECLAIR_MR1 = 7;
    public static final byte SDK_FROYO = 8;
    public static final byte SDK_GINGERBREAD = 9;
    public static final byte SDK_GINGERBREAD_MR1 = 10;
    public static final byte SDK_HONEYCOMB = 11;
    public static final byte SDK_HONEYCOMB_MR1 = 12;
    public static final byte SDK_HONEYCOMB_MR2 = 13;
    public static final byte SDK_ICE_CREAM_SANDWICH = 14;
    public static final byte SDK_ICE_CREAM_SANDWICH_MR1 = 15;
    public static final byte SDK_JELLY_BEAN = 16;
    public static final byte SDK_JELLY_BEAN_MR1 = 17;
    public static final byte SDK_JELLY_BEAN_MR2 = 18;
    public static final byte SDK_KITKAT = 19;
    public static final byte SDK_LOLLIPOP = 21;
    public static final byte SDK_LOLLIPOP_MR1 = 22;
    public static final byte SDK_MNC = 23;
    public static final byte SDK_NOUGAT = 24;
    public static final byte SDK_NOUGAT_MR1 = 25;
    public static final byte SDK_OREO = 26;
    public static final byte SDK_OREO_MR1 = 27;
    public static final byte SDK_P = 28;
    public static final byte SDK_Q = 29;
    public static final byte SDK_R = 30;
    public static final byte SDK_S = 31;
    public static final byte SDK_S_V2 = 32;
    public static final byte SDK_TIRAMISU = 33;
    public static final byte SDK_UPSIDEDOWN_CAKE = 34;
    public static final byte SDK_VANILLA_ICE_CREAM = 35;

    // AOSP changed Build IDs during QPR2 of API 34 (Upsidedown Cake), restarting at A.
    // However, API 35 (Vanilla) took letter A (AP2A), so we start at B.
    public static final byte SDK_BAKLAVA = 36;

    // AOSP has this as 10,000 for dev purposes.
    // platform_frameworks_base/commit/c7a1109a1fe0771d4c9b572dcf178e2779fc4f2d
    public static final int SDK_DEVELOPMENT = 10000;

    public static final byte ORIENTATION_ANY = 0;
    public static final byte ORIENTATION_PORT = 1;
    public static final byte ORIENTATION_LAND = 2;
    public static final byte ORIENTATION_SQUARE = 3;

    public static final byte TOUCHSCREEN_ANY = 0;
    public static final byte TOUCHSCREEN_NOTOUCH = 1;
    public static final byte TOUCHSCREEN_STYLUS = 2;
    public static final byte TOUCHSCREEN_FINGER = 3;

    public static final int DENSITY_DEFAULT = 0;
    public static final int DENSITY_LOW = 120;
    public static final int DENSITY_MEDIUM = 160;
    public static final int DENSITY_400 = 190;
    public static final int DENSITY_TV = 213;
    public static final int DENSITY_HIGH = 240;
    public static final int DENSITY_XHIGH = 320;
    public static final int DENSITY_XXHIGH = 480;
    public static final int DENSITY_XXXHIGH = 640;
    public static final int DENSITY_ANY = 0xFFFE;
    public static final int DENSITY_NONE = 0xFFFF;

    public static final int MNC_ZERO = -1;

    public static final short MASK_LAYOUTDIR = 0xc0;
    public static final short SCREENLAYOUT_LAYOUTDIR_ANY = 0x00;
    public static final short SCREENLAYOUT_LAYOUTDIR_LTR = 0x40;
    public static final short SCREENLAYOUT_LAYOUTDIR_RTL = 0x80;
    public static final short SCREENLAYOUT_LAYOUTDIR_SHIFT = 0x06;

    public static final short MASK_SCREENROUND = 0x03;
    public static final short SCREENLAYOUT_ROUND_ANY = 0;
    public static final short SCREENLAYOUT_ROUND_NO = 0x1;
    public static final short SCREENLAYOUT_ROUND_YES = 0x2;

    public static final byte GRAMMATICAL_GENDER_ANY = 0;
    public static final byte GRAMMATICAL_GENDER_NEUTER = 1;
    public static final byte GRAMMATICAL_GENDER_FEMININE = 2;
    public static final byte GRAMMATICAL_GENDER_MASCULINE = 3;

    public static final byte KEYBOARD_ANY = 0;
    public static final byte KEYBOARD_NOKEYS = 1;
    public static final byte KEYBOARD_QWERTY = 2;
    public static final byte KEYBOARD_12KEY = 3;

    public static final byte NAVIGATION_ANY = 0;
    public static final byte NAVIGATION_NONAV = 1;
    public static final byte NAVIGATION_DPAD = 2;
    public static final byte NAVIGATION_TRACKBALL = 3;
    public static final byte NAVIGATION_WHEEL = 4;

    public static final byte MASK_KEYSHIDDEN = 0x3;
    public static final byte KEYSHIDDEN_ANY = 0x0;
    public static final byte KEYSHIDDEN_NO = 0x1;
    public static final byte KEYSHIDDEN_YES = 0x2;
    public static final byte KEYSHIDDEN_SOFT = 0x3;

    public static final byte MASK_NAVHIDDEN = 0xc;
    public static final byte NAVHIDDEN_ANY = 0x0;
    public static final byte NAVHIDDEN_NO = 0x4;
    public static final byte NAVHIDDEN_YES = 0x8;

    public static final byte MASK_SCREENSIZE = 0x0f;
    public static final byte SCREENSIZE_ANY = 0x00;
    public static final byte SCREENSIZE_SMALL = 0x01;
    public static final byte SCREENSIZE_NORMAL = 0x02;
    public static final byte SCREENSIZE_LARGE = 0x03;
    public static final byte SCREENSIZE_XLARGE = 0x04;

    public static final byte MASK_SCREENLONG = 0x30;
    public static final byte SCREENLONG_ANY = 0x00;
    public static final byte SCREENLONG_NO = 0x10;
    public static final byte SCREENLONG_YES = 0x20;

    public static final byte MASK_UI_MODE_TYPE = 0x0f;
    public static final byte UI_MODE_TYPE_ANY = 0x00;
    public static final byte UI_MODE_TYPE_NORMAL = 0x01;
    public static final byte UI_MODE_TYPE_DESK = 0x02;
    public static final byte UI_MODE_TYPE_CAR = 0x03;
    public static final byte UI_MODE_TYPE_TELEVISION = 0x04;
    public static final byte UI_MODE_TYPE_APPLIANCE = 0x05;
    public static final byte UI_MODE_TYPE_WATCH = 0x06;
    public static final byte UI_MODE_TYPE_VR_HEADSET = 0x07;

    // start - miui
    public static final byte UI_MODE_TYPE_GODZILLAUI = 0x0b;
    public static final byte UI_MODE_TYPE_SMALLUI = 0x0c;
    public static final byte UI_MODE_TYPE_MEDIUMUI = 0x0d;
    public static final byte UI_MODE_TYPE_LARGEUI = 0x0e;
    public static final byte UI_MODE_TYPE_HUGEUI = 0x0f;
    // end - miui

    public static final byte MASK_UI_MODE_NIGHT = 0x30;
    public static final byte UI_MODE_NIGHT_ANY = 0x00;
    public static final byte UI_MODE_NIGHT_NO = 0x10;
    public static final byte UI_MODE_NIGHT_YES = 0x20;

    public static final byte COLOR_HDR_MASK = 0xC;
    public static final byte COLOR_HDR_NO = 0x4;
    public static final byte COLOR_HDR_SHIFT = 0x2;
    public static final byte COLOR_HDR_UNDEFINED = 0x0;
    public static final byte COLOR_HDR_YES = 0x8;

    public static final byte COLOR_UNDEFINED = 0x0;

    public static final byte COLOR_WIDE_UNDEFINED = 0x0;
    public static final byte COLOR_WIDE_NO = 0x1;
    public static final byte COLOR_WIDE_YES = 0x2;
    public static final byte COLOR_WIDE_MASK = 0x3;

    // TODO: Dirty static hack. This counter should be a part of ResPackage,
    // but it would be hard right now and this feature is very rarely used.
    private static int sErrCounter = 0;

    private final short mMcc;
    private final short mMnc;
    private final char[] mLanguage;
    private final char[] mRegion;
    private final byte mOrientation;
    private final byte mTouchscreen;
    private final int mDensity;
    private final byte mKeyboard;
    private final byte mNavigation;
    private final byte mInputFlags;
    private final byte mGrammaticalInflection;
    private final short mScreenWidth;
    private final short mScreenHeight;
    private final short mSdkVersion;
    private final byte mScreenLayout;
    private final byte mUiMode;
    private final short mSmallestScreenWidthDp;
    private final short mScreenWidthDp;
    private final short mScreenHeightDp;
    private final char[] mLocaleScript;
    private final char[] mLocaleVariant;
    private final byte mScreenLayout2;
    private final byte mColorMode;
    private final char[] mLocaleNumberingSystem;
    private final int mSize;
    private final boolean mIsInvalid;

    private final String mQualifiers;

    public ResConfigFlags() {
        mMcc = 0;
        mMnc = 0;
        mLanguage = new char[] { '\00', '\00' };
        mRegion = new char[] { '\00', '\00' };
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
        mScreenLayout = SCREENLONG_ANY | SCREENSIZE_ANY;
        mUiMode = UI_MODE_TYPE_ANY | UI_MODE_NIGHT_ANY;
        mSmallestScreenWidthDp = 0;
        mScreenWidthDp = 0;
        mScreenHeightDp = 0;
        mLocaleScript = null;
        mLocaleVariant = null;
        mScreenLayout2 = 0;
        mColorMode = COLOR_WIDE_UNDEFINED;
        mLocaleNumberingSystem = null;
        mSize = 0;
        mIsInvalid = false;
        mQualifiers = "";
    }

    public ResConfigFlags(short mcc, short mnc, char[] language, char[] region, byte orientation,
                          byte touchscreen, int density, byte keyboard, byte navigation, byte inputFlags,
                          byte grammaticalInflection, short screenWidth, short screenHeight,
                          short sdkVersion, byte screenLayout, byte uiMode, short smallestScreenWidthDp,
                          short screenWidthDp, short screenHeightDp, char[] localeScript, char[] localeVariant,
                          byte screenLayout2, byte colorMode, char[] localeNumberingSystem,
                          int size, boolean isInvalid) {
        if (orientation < 0 || orientation > 3) {
            LOGGER.warning("Invalid orientation value: " + orientation);
            orientation = 0;
            isInvalid = true;
        }
        if (touchscreen < 0 || touchscreen > 3) {
            LOGGER.warning("Invalid touchscreen value: " + touchscreen);
            touchscreen = 0;
            isInvalid = true;
        }
        if (density < -1) {
            LOGGER.warning("Invalid density value: " + density);
            density = 0;
            isInvalid = true;
        }
        if (keyboard < 0 || keyboard > 3) {
            LOGGER.warning("Invalid keyboard value: " + keyboard);
            keyboard = 0;
            isInvalid = true;
        }
        if (navigation < 0 || navigation > 4) {
            LOGGER.warning("Invalid navigation value: " + navigation);
            navigation = 0;
            isInvalid = true;
        }
        if (localeScript != null && (localeScript.length == 0 || localeScript[0] == '\00')) {
            localeScript = null;
        }
        if (localeVariant != null && (localeVariant.length == 0 || localeVariant[0] == '\00')) {
            localeVariant = null;
        }

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
        mScreenLayout = screenLayout;
        mUiMode = uiMode;
        mSmallestScreenWidthDp = smallestScreenWidthDp;
        mScreenWidthDp = screenWidthDp;
        mScreenHeightDp = screenHeightDp;
        mLocaleScript = localeScript;
        mLocaleVariant = localeVariant;
        mScreenLayout2 = screenLayout2;
        mColorMode = colorMode;
        mLocaleNumberingSystem = localeNumberingSystem;
        mSize = size;
        mIsInvalid = isInvalid;
        mQualifiers = generateQualifiers();
    }

    private String generateQualifiers() {
        StringBuilder sb = new StringBuilder();
        if (mMcc != 0) {
            sb.append("-mcc").append(String.format("%03d", mMcc));
            if (mMnc != MNC_ZERO) {
                if (mMnc != 0) {
                    sb.append("-mnc");
                    if (mSize <= 32) {
                        if (mMnc > 0 && mMnc < 10) {
                            sb.append(String.format("%02d", mMnc));
                        } else {
                            sb.append(String.format("%03d", mMnc));
                        }
                    } else {
                        sb.append(mMnc);
                    }
                }
            } else {
                sb.append("-mnc00");
            }
        } else {
            if (mMnc != 0) {
                sb.append("-mnc").append(mMnc);
            }
        }
        sb.append(getLocaleString());

        switch (mGrammaticalInflection) {
            case GRAMMATICAL_GENDER_NEUTER:
                sb.append("-neuter");
                break;
            case GRAMMATICAL_GENDER_FEMININE:
                sb.append("-feminine");
                break;
            case GRAMMATICAL_GENDER_MASCULINE:
                sb.append("-masculine");
                break;
        }

        switch (mScreenLayout & MASK_LAYOUTDIR) {
            case SCREENLAYOUT_LAYOUTDIR_RTL:
                sb.append("-ldrtl");
                break;
            case SCREENLAYOUT_LAYOUTDIR_LTR:
                sb.append("-ldltr");
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
        }
        switch (mScreenLayout & MASK_SCREENLONG) {
            case SCREENLONG_YES:
                sb.append("-long");
                break;
            case SCREENLONG_NO:
                sb.append("-notlong");
                break;
        }
        switch (mScreenLayout2 & MASK_SCREENROUND) {
            case SCREENLAYOUT_ROUND_NO:
                sb.append("-notround");
                break;
            case SCREENLAYOUT_ROUND_YES:
                sb.append("-round");
                break;
        }
        switch (mColorMode & COLOR_HDR_MASK) {
            case COLOR_HDR_YES:
                sb.append("-highdr");
                break;
            case COLOR_HDR_NO:
                sb.append("-lowdr");
                break;
        }
        switch (mColorMode & COLOR_WIDE_MASK) {
            case COLOR_WIDE_YES:
                sb.append("-widecg");
                break;
            case COLOR_WIDE_NO:
                sb.append("-nowidecg");
                break;
        }
        switch (mOrientation) {
            case ORIENTATION_PORT:
                sb.append("-port");
                break;
            case ORIENTATION_LAND:
                sb.append("-land");
                break;
            case ORIENTATION_SQUARE:
                sb.append("-square");
                break;
        }
        switch (mUiMode & MASK_UI_MODE_TYPE) {
            case UI_MODE_TYPE_CAR:
                sb.append("-car");
                break;
            case UI_MODE_TYPE_DESK:
                sb.append("-desk");
                break;
            case UI_MODE_TYPE_TELEVISION:
                sb.append("-television");
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
            case UI_MODE_TYPE_GODZILLAUI:
                sb.append("-godzillaui");
                break;
            case UI_MODE_TYPE_HUGEUI:
                sb.append("-hugeui");
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
        }
        switch (mUiMode & MASK_UI_MODE_NIGHT) {
            case UI_MODE_NIGHT_YES:
                sb.append("-night");
                break;
            case UI_MODE_NIGHT_NO:
                sb.append("-notnight");
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
            case DENSITY_HIGH:
                sb.append("-hdpi");
                break;
            case DENSITY_TV:
                sb.append("-tvdpi");
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
        }
        switch (mTouchscreen) {
            case TOUCHSCREEN_NOTOUCH:
                sb.append("-notouch");
                break;
            case TOUCHSCREEN_STYLUS:
                sb.append("-stylus");
                break;
            case TOUCHSCREEN_FINGER:
                sb.append("-finger");
                break;
        }
        switch (mInputFlags & MASK_KEYSHIDDEN) {
            case KEYSHIDDEN_NO:
                sb.append("-keysexposed");
                break;
            case KEYSHIDDEN_YES:
                sb.append("-keyshidden");
                break;
            case KEYSHIDDEN_SOFT:
                sb.append("-keyssoft");
                break;
        }
        switch (mKeyboard) {
            case KEYBOARD_NOKEYS:
                sb.append("-nokeys");
                break;
            case KEYBOARD_QWERTY:
                sb.append("-qwerty");
                break;
            case KEYBOARD_12KEY:
                sb.append("-12key");
                break;
        }
        switch (mInputFlags & MASK_NAVHIDDEN) {
            case NAVHIDDEN_NO:
                sb.append("-navexposed");
                break;
            case NAVHIDDEN_YES:
                sb.append("-navhidden");
                break;
        }
        switch (mNavigation) {
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
        }
        if (mScreenWidth != 0 && mScreenHeight != 0) {
            if (mScreenWidth > mScreenHeight) {
                sb.append(String.format("-%dx%d", mScreenWidth, mScreenHeight));
            } else {
                sb.append(String.format("-%dx%d", mScreenHeight, mScreenWidth));
            }
        }
        if (mSdkVersion > 0 && mSdkVersion >= getNaturalSdkVersionRequirement()) {
            sb.append("-v").append(mSdkVersion);
        }
        if (mIsInvalid) {
            sb.append("-ERR").append(sErrCounter++);
        }

        return sb.toString();
    }

    private short getNaturalSdkVersionRequirement() {
        if (mGrammaticalInflection != 0) {
            return SDK_UPSIDEDOWN_CAKE;
        }
        if ((mUiMode & MASK_UI_MODE_TYPE) == UI_MODE_TYPE_VR_HEADSET || (mColorMode & COLOR_WIDE_MASK) != 0 || ((mColorMode & COLOR_HDR_MASK) != 0)) {
            return SDK_OREO;
        }
        if ((mScreenLayout2 & MASK_SCREENROUND) != 0) {
            return SDK_MNC;
        }
        if (mDensity == DENSITY_ANY) {
            return SDK_LOLLIPOP;
        }
        if (mSmallestScreenWidthDp != 0 || mScreenWidthDp != 0 || mScreenHeightDp != 0) {
            return SDK_HONEYCOMB_MR2;
        }
        if ((mUiMode & (MASK_UI_MODE_TYPE | MASK_UI_MODE_NIGHT)) != UI_MODE_NIGHT_ANY) {
            return SDK_FROYO;
        }
        if ((mScreenLayout & (MASK_SCREENSIZE | MASK_SCREENLONG)) != SCREENSIZE_ANY || mDensity != DENSITY_DEFAULT) {
            return SDK_DONUT;
        }
        return 0;
    }

    private String getLocaleString() {
        StringBuilder sb = new StringBuilder();
        // check for old style non BCP47 tags
        // allows values-xx-rXX, values-xx, values-xxx-rXX
        // denies values-xxx, anything else
        if (mLocaleScript == null && mLocaleVariant == null && (mRegion[0] != '\00' || mLanguage[0] != '\00')
                && mRegion.length != 3) {
            sb.append("-").append(mLanguage);
            if (mRegion[0] != '\00') {
                sb.append("-r").append(mRegion);
            }
        } else { // BCP47
            if (mLanguage[0] == '\00' && mRegion[0] == '\00') {
                return sb.toString(); // early return, no language or region
            }
            sb.append("-b+");
            if (mLanguage[0] != '\00') {
                sb.append(mLanguage);
            }
            if (mLocaleScript != null && mLocaleScript.length == 4) {
                sb.append("+").append(mLocaleScript);
            }
            if ((mRegion.length == 2 || mRegion.length == 3) && mRegion[0] != '\00') {
                sb.append("+").append(mRegion);
            }
            if (mLocaleVariant != null && mLocaleVariant.length >= 5) {
                sb.append("+").append(toUpper(mLocaleVariant));
            }

            // If we have a numbering system - it isn't used in qualifiers for build tools, but AOSP understands it
            // So chances are - this may be valid, but aapt 1/2 will not like it.
            if (mLocaleNumberingSystem != null && mLocaleNumberingSystem.length > 0) {
                sb.append("+u+nu+").append(mLocaleNumberingSystem);
            }
        }
        return sb.toString();
    }

    private static String toUpper(char[] character) {
        StringBuilder sb = new StringBuilder();
        for (char ch : character) {
            sb.append(Character.toUpperCase(ch));
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
        return !getQualifiers().equals("") ? getQualifiers() : "[DEFAULT]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResConfigFlags) {
            ResConfigFlags other = (ResConfigFlags) obj;
            return mQualifiers.equals(other.mQualifiers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mQualifiers.hashCode();
    }
}

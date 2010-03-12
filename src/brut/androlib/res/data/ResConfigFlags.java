/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package brut.androlib.res.data;

import brut.androlib.AndrolibException;
import brut.androlib.res.jni.JniConfig;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResConfigFlags {
    public final int mcc;
    public final int mnc;

    public final char[] language;
    public final char[] country;

//    public final Orientation orientation;
//    public final Touchscreen touchscreen;
    public final int orientation;
    public final int touchscreen;
    public final int density;

//    public final Keyboard keyboard;
//    public final Navigation navigation;
//    public final Keys keys;
//    public final Nav nav;
    public final int keyboard;
    public final int navigation;
    public final int inputFlags;

    public final int screenWidth;
    public final int screenHeight;
//    public final ScreenSize screenSize;
//    public final ScreenLong screenLong;
    public final int screenLayout;

    public final int sdkVersion;

    private final String mQualifiers;

    public ResConfigFlags() {
        mcc = 0;
        mnc = 0;
        language = new char[]{'\00', '\00'};
        country = new char[]{'\00', '\00'};
        orientation = ORIENTATION_ANY;
        touchscreen = TOUCHSCREEN_ANY;
        density = DENSITY_DEFAULT;
        keyboard = KEYBOARD_ANY;
        navigation = NAVIGATION_ANY;
        inputFlags = KEYSHIDDEN_ANY | NAVHIDDEN_ANY;
        screenWidth = 0;
        screenHeight = 0;
        screenLayout = SCREENLONG_ANY | SCREENSIZE_ANY;
        sdkVersion = 0;
        mQualifiers = "";
    }

    public ResConfigFlags(JniConfig cfg) throws AndrolibException {
//        if (cfg.mcc == 0 && mnc != 0) {
//            throw new AndrolibException(String.format(
//                "Invalid IMSI: mcc=%3d, mnc=%3d", mcc, mnc));
//        }
        mcc = cfg.mcc;
        mnc = cfg.mnc;

//        if (cfg.language.length == 0 && cfg.country.length != 0) {
//            throw new AndrolibException(String.format(
//                "Invalid local: language=%s, country=%s",
//                cfg.language, cfg.country));
//        }
        language = cfg.language;
        country = cfg.country;
//
//        switch (cfg.orientation) {
//            case ORIENTATION_ANY:
//                orientation = Orientation.ANY;
//                break;
//            case ORIENTATION_LAND:
//                orientation = Orientation.LAND;
//                break;
//            case ORIENTATION_PORT:
//                orientation = Orientation.PORT;
//                break;
//            case ORIENTATION_SQUARE:
//                orientation = Orientation.SQUARE;
//                break;
//            default:
//                throw new AndrolibException(String.format(
//                    "Invalid orientation: %d", cfg.orientation));
//        }

        orientation = cfg.orientation;
        touchscreen = cfg.touchscreen;
        density = cfg.density;
        keyboard = cfg.keyboard;
        navigation = cfg.navigation;
        inputFlags = cfg.inputFlags;
        screenWidth = cfg.screenWidth;
        screenHeight = cfg.screenHeight;
        screenLayout = cfg.screenLayout;
        sdkVersion = cfg.sdkVersion;

        mQualifiers = generateQualifiers();
    }

    public String getQualifiers() {
        return mQualifiers;
    }

    private String generateQualifiers() {
        StringBuilder ret = new StringBuilder();
        if (mcc != 0) {
            ret.append("-mcc").append(mcc);
            if (mnc != 0) {
                ret.append("-mnc").append(mnc);
            }
        }
        if (language[0] != '\00') {
            ret.append('-').append(language);
            if (country[0] != '\00') {
                ret.append("-r").append(country);
            }
        }
        switch (screenLayout & MASK_SCREENSIZE) {
            case SCREENSIZE_SMALL:
                ret.append("-small");
                break;
            case SCREENSIZE_NORMAL:
                ret.append("-normal");
                break;
            case SCREENSIZE_LARGE:
                ret.append("-large");
                break;
        }
        switch (screenLayout & MASK_SCREENLONG) {
            case SCREENLONG_YES:
                ret.append("-long");
                break;
            case SCREENLONG_NO:
                ret.append("-notlong");
                break;
        }
        switch (orientation) {
            case ORIENTATION_PORT:
                ret.append("-port");
                break;
            case ORIENTATION_LAND:
                ret.append("-land");
                break;
            case ORIENTATION_SQUARE:
                ret.append("-square");
                break;
        }
        switch (density) {
            case DENSITY_DEFAULT:
                break;
            case DENSITY_LOW:
                ret.append("-ldpi");
                break;
            case DENSITY_MEDIUM:
                ret.append("-mdpi");
                break;
            case DENSITY_HIGH:
                ret.append("-hdpi");
                break;
            case DENSITY_NONE:
                ret.append("-nodpi");
                break;
            default:
                ret.append('-').append(density).append("dpi");
        }
        switch (touchscreen) {
            case TOUCHSCREEN_NOTOUCH:
                ret.append("-notouch");
                break;
            case TOUCHSCREEN_STYLUS:
                ret.append("-stylus");
                break;
            case TOUCHSCREEN_FINGER:
                ret.append("-finger");
                break;
        }
        switch (inputFlags & MASK_KEYSHIDDEN) {
            case KEYSHIDDEN_NO:
                ret.append("-keysexposed");
                break;
            case KEYSHIDDEN_YES:
                ret.append("-keyshidden");
                break;
            case KEYSHIDDEN_SOFT:
                ret.append("-keyssoft");
                break;
        }
        switch (keyboard) {
            case KEYBOARD_NOKEYS:
                ret.append("-nokeys");
                break;
            case KEYBOARD_QWERTY:
                ret.append("-qwerty");
                break;
            case KEYBOARD_12KEY:
                ret.append("-12key");
                break;
        }
        switch (inputFlags & MASK_NAVHIDDEN) {
            case NAVHIDDEN_NO:
                ret.append("-navexposed");
                break;
            case NAVHIDDEN_YES:
                ret.append("-navhidden");
                break;
        }
        switch (navigation) {
            case NAVIGATION_NONAV:
                ret.append("-nonav");
                break;
            case NAVIGATION_DPAD:
                ret.append("-dpad");
                break;
            case NAVIGATION_TRACKBALL:
                ret.append("-trackball");
                break;
            case NAVIGATION_WHEEL:
                ret.append("-wheel");
                break;
        }
        if (screenWidth != 0 && screenHeight != 0) {
            if (screenWidth > screenHeight) {
                ret.append(String.format("-%dx%d", screenWidth, screenHeight));
            } else {
                ret.append(String.format("-%dx%d", screenHeight, screenWidth));
            }
        }
        if (sdkVersion != 0) {
            ret.append("-v").append(sdkVersion);
        }

        return ret.toString();
    }

    @Override
    public String toString() {
        return ! getQualifiers().equals("") ? getQualifiers() : "[DEFAULT]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResConfigFlags other = (ResConfigFlags) obj;
        if ((this.mQualifiers == null) ? (other.mQualifiers != null) : !this.mQualifiers.equals(other.mQualifiers)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.mQualifiers != null ? this.mQualifiers.hashCode() : 0);
        return hash;
    }

//    public enum Orientation {ANY, PORT, LAND, SQUARE}
//    public enum Touchscreen {ANY, NOTOUCH, STYLUS, FINGER}
//    public enum Keyboard {ANY, NOKEYS, QWERTY, KEY12}
//    public enum Navigation {ANY, NONAV, DPAD, TRACKBALL, WHEEL}
//    public enum Keys {ANY, EXPOSED, HIDDEN, SOFT}
//    public enum Nav {ANY, EXPOSED, HIDDEN}
//    public enum ScreenSize {ANY, SMALL, NORMAL, LARGE}
//    public enum ScreenLong {ANY, LONG, NOTLONG}

    public final static int ORIENTATION_ANY  = 0x0000;
    public final static int ORIENTATION_PORT = 0x0001;
    public final static int ORIENTATION_LAND = 0x0002;
    public final static int ORIENTATION_SQUARE = 0x0003;

    public final static int TOUCHSCREEN_ANY  = 0x0000;
    public final static int TOUCHSCREEN_NOTOUCH  = 0x0001;
    public final static int TOUCHSCREEN_STYLUS  = 0x0002;
    public final static int TOUCHSCREEN_FINGER  = 0x0003;

    public final static int DENSITY_DEFAULT = 0;
    public final static int DENSITY_LOW = 120;
    public final static int DENSITY_MEDIUM = 160;
    public final static int DENSITY_HIGH = 240;
    public final static int DENSITY_NONE = 0xffff;

    public final static int KEYBOARD_ANY  = 0x0000;
    public final static int KEYBOARD_NOKEYS  = 0x0001;
    public final static int KEYBOARD_QWERTY  = 0x0002;
    public final static int KEYBOARD_12KEY  = 0x0003;

    public final static int NAVIGATION_ANY  = 0x0000;
    public final static int NAVIGATION_NONAV  = 0x0001;
    public final static int NAVIGATION_DPAD  = 0x0002;
    public final static int NAVIGATION_TRACKBALL  = 0x0003;
    public final static int NAVIGATION_WHEEL  = 0x0004;

    public final static int MASK_KEYSHIDDEN = 0x0003;
    public final static int KEYSHIDDEN_ANY = 0x0000;
    public final static int KEYSHIDDEN_NO = 0x0001;
    public final static int KEYSHIDDEN_YES = 0x0002;
    public final static int KEYSHIDDEN_SOFT = 0x0003;

    public final static int MASK_NAVHIDDEN = 0x000c;
    public final static int NAVHIDDEN_ANY = 0x0000;
    public final static int NAVHIDDEN_NO = 0x0004;
    public final static int NAVHIDDEN_YES = 0x0008;

    public final static int MASK_SCREENSIZE = 0x0f;
    public final static int SCREENSIZE_ANY  = 0x00;
    public final static int SCREENSIZE_SMALL = 0x01;
    public final static int SCREENSIZE_NORMAL = 0x02;
    public final static int SCREENSIZE_LARGE = 0x03;

    public final static int MASK_SCREENLONG = 0x30;
    public final static int SCREENLONG_ANY = 0x00;
    public final static int SCREENLONG_NO = 0x10;
    public final static int SCREENLONG_YES = 0x20;
}

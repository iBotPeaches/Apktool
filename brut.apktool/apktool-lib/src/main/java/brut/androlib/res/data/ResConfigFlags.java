/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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
 */

package brut.androlib.res.data;

import java.util.logging.Logger;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResConfigFlags {
	public final short mcc;
	public final short mnc;

	public final char[] language;
	public final char[] country;

	public final short layoutDirection;

	public final byte orientation;
	public final byte touchscreen;
	public final short density;

	public final byte keyboard;
	public final byte navigation;
	public final byte inputFlags;

	public final short screenWidth;
	public final short screenHeight;

	public final short sdkVersion;

	public final byte screenLayout;
	public final byte uiMode;
	public final short smallestScreenWidthDp;

	public final short screenWidthDp;
	public final short screenHeightDp;

	public final boolean isInvalid;

	private final String mQualifiers;

	public ResConfigFlags() {
		mcc = 0;
		mnc = 0;
		language = new char[] { '\00', '\00' };
		country = new char[] { '\00', '\00' };
		layoutDirection = SCREENLAYOUT_LAYOUTDIR_ANY;
		orientation = ORIENTATION_ANY;
		touchscreen = TOUCHSCREEN_ANY;
		density = DENSITY_DEFAULT;
		keyboard = KEYBOARD_ANY;
		navigation = NAVIGATION_ANY;
		inputFlags = KEYSHIDDEN_ANY | NAVHIDDEN_ANY;
		screenWidth = 0;
		screenHeight = 0;
		sdkVersion = 0;
		screenLayout = SCREENLONG_ANY | SCREENSIZE_ANY;
		uiMode = UI_MODE_TYPE_ANY | UI_MODE_NIGHT_ANY;
		smallestScreenWidthDp = 0;
		screenWidthDp = 0;
		screenHeightDp = 0;
		isInvalid = false;
		mQualifiers = "";
	}

	public ResConfigFlags(short mcc, short mnc, char[] language,
			char[] country, short layoutDirection, byte orientation,
			byte touchscreen, short density, byte keyboard, byte navigation,
			byte inputFlags, short screenWidth, short screenHeight,
			short sdkVersion, byte screenLayout, byte uiMode,
			short smallestScreenWidthDp, short screenWidthDp,
			short screenHeightDp, boolean isInvalid) {
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

		this.mcc = mcc;
		this.mnc = mnc;
		this.language = language;
		this.country = country;
		this.layoutDirection = layoutDirection;
		this.orientation = orientation;
		this.touchscreen = touchscreen;
		this.density = density;
		this.keyboard = keyboard;
		this.navigation = navigation;
		this.inputFlags = inputFlags;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.sdkVersion = sdkVersion;
		this.screenLayout = screenLayout;
		this.uiMode = uiMode;
		this.smallestScreenWidthDp = smallestScreenWidthDp;
		this.screenWidthDp = screenWidthDp;
		this.screenHeightDp = screenHeightDp;
		this.isInvalid = isInvalid;
		mQualifiers = generateQualifiers();
	}

	public String getQualifiers() {
		return mQualifiers;
	}

	private String generateQualifiers() {
		StringBuilder ret = new StringBuilder();
		if (mcc != 0) {
			ret.append("-mcc").append(String.format("%03d", mcc));
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
		switch (screenLayout & MASK_LAYOUTDIR) {
		case SCREENLAYOUT_LAYOUTDIR_RTL:
			ret.append("-ldrtl");
			break;
		case SCREENLAYOUT_LAYOUTDIR_LTR:
			ret.append("-ldltr");
			break;
		}
		if (smallestScreenWidthDp != 0) {
			ret.append("-sw").append(smallestScreenWidthDp).append("dp");
		}
		if (screenWidthDp != 0) {
			ret.append("-w").append(screenWidthDp).append("dp");
		}
		if (screenHeightDp != 0) {
			ret.append("-h").append(screenHeightDp).append("dp");
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
		case SCREENSIZE_XLARGE:
			ret.append("-xlarge");
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
		switch (uiMode & MASK_UI_MODE_TYPE) {
		case UI_MODE_TYPE_CAR:
			ret.append("-car");
			break;
		case UI_MODE_TYPE_DESK:
			ret.append("-desk");
			break;
		case UI_MODE_TYPE_TELEVISION:
			ret.append("-television");
			break;
		case UI_MODE_TYPE_SMALLUI:
			ret.append("-smallui");
			break;
		case UI_MODE_TYPE_MEDIUMUI:
			ret.append("-mediumui");
			break;
		case UI_MODE_TYPE_LARGEUI:
			ret.append("-largeui");
			break;
		case UI_MODE_TYPE_HUGEUI:
			ret.append("-hugeui");
			break;
		case UI_MODE_TYPE_APPLIANCE:
			ret.append("-appliance");
			break;
		case UI_MODE_TYPE_INVERTED:
			ret.append("-inverted");
			break;
		}
		switch (uiMode & MASK_UI_MODE_NIGHT) {
		case UI_MODE_NIGHT_YES:
			ret.append("-night");
			break;
		case UI_MODE_NIGHT_NO:
			ret.append("-notnight");
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
		case DENSITY_TV:
			ret.append("-tvdpi");
			break;
		case DENSITY_XHIGH:
			ret.append("-xhdpi");
			break;
		case DENSITY_XXHIGH:
			ret.append("-xxhdpi");
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
		if (sdkVersion > getNaturalSdkVersionRequirement()) {
			ret.append("-v").append(sdkVersion);
		}
		if (isInvalid) {
			ret.append("-ERR" + sErrCounter++);
		}

		return ret.toString();
	}

	private short getNaturalSdkVersionRequirement() {
		if (smallestScreenWidthDp != 0 || screenWidthDp != 0
				|| screenHeightDp != 0) {
			return SDK_HONEYCOMB_MR2;
		}
		if ((uiMode & (MASK_UI_MODE_TYPE | MASK_UI_MODE_NIGHT)) != 0) {
			return SDK_FROYO;
		}
		if ((screenLayout & (MASK_SCREENSIZE | MASK_SCREENLONG)) != 0
				|| density != DENSITY_DEFAULT) {
			return SDK_DONUT;
		}
		return 0;
	}

	@Override
	public String toString() {
		return !getQualifiers().equals("") ? getQualifiers() : "[DEFAULT]";
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
		return this.mQualifiers.equals(other.mQualifiers);
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31 * hash + this.mQualifiers.hashCode();
		return hash;
	}

	// TODO: Dirty static hack. This counter should be a part of ResPackage,
	// but it would be hard right now and this feature is very rarely used.
	private static int sErrCounter = 0;

	public final static byte SDK_BASE = 1;
	public final static byte SDK_BASE_1_1 = 2;
	public final static byte SDK_CUPCAKE = 3;
	public final static byte SDK_DONUT = 4;
	public final static byte SDK_ECLAIR = 5;
	public final static byte SDK_ECLAIR_0_1 = 6;
	public final static byte SDK_ECLAIR_MR1 = 7;
	public final static byte SDK_FROYO = 8;
	public final static byte SDK_GINGERBREAD = 9;
	public final static byte SDK_GINGERBREAD_MR1 = 10;
	public final static byte SDK_HONEYCOMB = 11;
	public final static byte SDK_HONEYCOMB_MR1 = 12;
	public final static byte SDK_HONEYCOMB_MR2 = 13;
	public final static byte SDK_ICE_CREAM_SANDWICH = 14;
	public final static byte SDK_ICE_CREAM_SANDWICH_MR1 = 15;
	public final static byte SDK_JELLY_BEAN = 16;
	public final static byte SDK_JELLY_BEAN_MR1 = 17;

	public final static byte ORIENTATION_ANY = 0;
	public final static byte ORIENTATION_PORT = 1;
	public final static byte ORIENTATION_LAND = 2;
	public final static byte ORIENTATION_SQUARE = 3;

	public final static byte TOUCHSCREEN_ANY = 0;
	public final static byte TOUCHSCREEN_NOTOUCH = 1;
	public final static byte TOUCHSCREEN_STYLUS = 2;
	public final static byte TOUCHSCREEN_FINGER = 3;

	public final static short DENSITY_DEFAULT = 0;
	public final static short DENSITY_LOW = 120;
	public final static short DENSITY_MEDIUM = 160;
	public final static short DENSITY_TV = 213;
	public final static short DENSITY_HIGH = 240;
	public final static short DENSITY_XHIGH = 320;
	public final static short DENSITY_XXHIGH = 480;
	public final static short DENSITY_NONE = -1;

	public final static short MASK_LAYOUTDIR = 0xc0;
	public final static short SCREENLAYOUT_LAYOUTDIR_ANY = 0x00;
	public final static short SCREENLAYOUT_LAYOUTDIR_LTR = 0x40;
	public final static short SCREENLAYOUT_LAYOUTDIR_RTL = 0x80;
	public final static short SCREENLAYOUT_LAYOUTDIR_SHIFT = 0x06;

	public final static byte KEYBOARD_ANY = 0;
	public final static byte KEYBOARD_NOKEYS = 1;
	public final static byte KEYBOARD_QWERTY = 2;
	public final static byte KEYBOARD_12KEY = 3;

	public final static byte NAVIGATION_ANY = 0;
	public final static byte NAVIGATION_NONAV = 1;
	public final static byte NAVIGATION_DPAD = 2;
	public final static byte NAVIGATION_TRACKBALL = 3;
	public final static byte NAVIGATION_WHEEL = 4;

	public final static byte MASK_KEYSHIDDEN = 0x3;
	public final static byte KEYSHIDDEN_ANY = 0x0;
	public final static byte KEYSHIDDEN_NO = 0x1;
	public final static byte KEYSHIDDEN_YES = 0x2;
	public final static byte KEYSHIDDEN_SOFT = 0x3;

	public final static byte MASK_NAVHIDDEN = 0xc;
	public final static byte NAVHIDDEN_ANY = 0x0;
	public final static byte NAVHIDDEN_NO = 0x4;
	public final static byte NAVHIDDEN_YES = 0x8;

	public final static byte MASK_SCREENSIZE = 0x0f;
	public final static byte SCREENSIZE_ANY = 0x00;
	public final static byte SCREENSIZE_SMALL = 0x01;
	public final static byte SCREENSIZE_NORMAL = 0x02;
	public final static byte SCREENSIZE_LARGE = 0x03;
	public final static byte SCREENSIZE_XLARGE = 0x04;

	public final static byte MASK_SCREENLONG = 0x30;
	public final static byte SCREENLONG_ANY = 0x00;
	public final static byte SCREENLONG_NO = 0x10;
	public final static byte SCREENLONG_YES = 0x20;

	public final static byte MASK_UI_MODE_TYPE = 0x0f;
	public final static byte UI_MODE_TYPE_ANY = 0x00;
	public final static byte UI_MODE_TYPE_NORMAL = 0x01;
	public final static byte UI_MODE_TYPE_DESK = 0x02;
	public final static byte UI_MODE_TYPE_CAR = 0x03;
	public final static byte UI_MODE_TYPE_TELEVISION = 0x04;
	public final static byte UI_MODE_TYPE_APPLIANCE = 0x05;
	public final static byte UI_MODE_TYPE_INVERTED = 0x45;
	public final static byte UI_MODE_TYPE_SMALLUI = 0x0c;
	public final static byte UI_MODE_TYPE_MEDIUMUI = 0x0d;
	public final static byte UI_MODE_TYPE_LARGEUI = 0x0e;
	public final static byte UI_MODE_TYPE_HUGEUI = 0x0f;

	public final static byte MASK_UI_MODE_NIGHT = 0x30;
	public final static byte UI_MODE_NIGHT_ANY = 0x00;
	public final static byte UI_MODE_NIGHT_NO = 0x10;
	public final static byte UI_MODE_NIGHT_YES = 0x20;

	private static final Logger LOGGER = Logger.getLogger(ResConfigFlags.class
			.getName());
}

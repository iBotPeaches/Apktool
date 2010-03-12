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

package brut.androlib.res.jni;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class JniConfig {
    public final int mcc;
    public final int mnc;

    public final char[] language;
    public final char[] country;

    public final int orientation;
    public final int touchscreen;
    public final int density;

    public final int keyboard;
    public final int navigation;
    public final int inputFlags;

    public final int screenWidth;
    public final int screenHeight;
    public final int screenLayout;

    public final int sdkVersion;

    public final JniEntry[] entries;

    public JniConfig(int mcc, int mnc, char[] language, char[] country,
            int orientation, int touchscreen, int density, int keyboard,
            int navigation, int inputFlags, int screenWidth, int screenHeight,
            int screenLayout, int sdkVersion, JniEntry[] entries) {
        this.mcc = mcc;
        this.mnc = mnc;
        this.language = language;
        this.country = country;
        this.orientation = orientation;
        this.touchscreen = touchscreen;
        this.density = density;
        this.keyboard = keyboard;
        this.navigation = navigation;
        this.inputFlags = inputFlags;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.screenLayout = screenLayout;
        this.sdkVersion = sdkVersion;
        this.entries = entries;
    }
}

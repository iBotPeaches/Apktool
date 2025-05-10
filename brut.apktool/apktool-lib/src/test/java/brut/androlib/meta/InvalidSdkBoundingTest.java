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

import brut.androlib.BaseTest;

import org.junit.*;
import static org.junit.Assert.*;

public class InvalidSdkBoundingTest extends BaseTest {

    @Test
    public void checkIfInvalidValuesPass() {
        SdkInfo sdkInfo = new SdkInfo();
        sdkInfo.setMinSdkVersion("15");
        sdkInfo.setTargetSdkVersion("25");
        sdkInfo.setMaxSdkVersion("19");

        assertEquals("19", sdkInfo.getTargetSdkVersionBounded());
    }

    @Test
    public void checkIfMissingMinPasses() {
        SdkInfo sdkInfo = new SdkInfo();
        sdkInfo.setTargetSdkVersion("25");
        sdkInfo.setMaxSdkVersion("19");

        assertEquals("19", sdkInfo.getTargetSdkVersionBounded());
    }

    @Test
    public void checkIfMissingMaxPasses() {
        SdkInfo sdkInfo = new SdkInfo();
        sdkInfo.setMinSdkVersion("15");
        sdkInfo.setTargetSdkVersion("25");

        assertEquals("25", sdkInfo.getTargetSdkVersionBounded());
    }

    @Test
    public void checkIfMissingBothPasses() {
        SdkInfo sdkInfo = new SdkInfo();
        sdkInfo.setTargetSdkVersion("25");

        assertEquals("25", sdkInfo.getTargetSdkVersionBounded());
    }

    @Test
    public void checkForSdkCodenameOTag() {
        SdkInfo sdkInfo = new SdkInfo();
        sdkInfo.setTargetSdkVersion("O");

        assertEquals("26", sdkInfo.getTargetSdkVersionBounded());
    }

    @Test
    public void checkForSdkCodenameSTag() {
        SdkInfo sdkInfo = new SdkInfo();
        sdkInfo.setTargetSdkVersion("S");

        assertEquals("31", sdkInfo.getTargetSdkVersionBounded());
    }

    @Test
    public void checkForSdkDevelopmentInsaneTestValue() {
        SdkInfo sdkInfo = new SdkInfo();
        sdkInfo.setTargetSdkVersion("SDK_CUR_DEVELOPMENT");

        assertEquals("10000", sdkInfo.getTargetSdkVersionBounded());
    }
}

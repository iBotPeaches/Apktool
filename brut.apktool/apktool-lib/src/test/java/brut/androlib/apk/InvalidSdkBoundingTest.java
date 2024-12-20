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
package brut.androlib.apk;

import brut.androlib.BaseTest;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

public class InvalidSdkBoundingTest extends BaseTest {

    @Test
    public void checkIfInvalidValuesPass() {
        ApkInfo apkInfo = new ApkInfo();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("minSdkVersion", "15");
        sdkInfo.put("targetSdkVersion", "25");
        sdkInfo.put("maxSdkVersion", "19");

        apkInfo.sdkInfo = sdkInfo;
        assertEquals("19", apkInfo.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkIfMissingMinPasses() {
        ApkInfo apkInfo = new ApkInfo();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "25");
        sdkInfo.put("maxSdkVersion", "19");

        apkInfo.sdkInfo = sdkInfo;
        assertEquals("19", apkInfo.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkIfMissingMaxPasses() {
        ApkInfo apkInfo = new ApkInfo();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("minSdkVersion", "15");
        sdkInfo.put("targetSdkVersion", "25");

        apkInfo.sdkInfo = sdkInfo;
        assertEquals("25", apkInfo.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkIfMissingBothPasses() {
        ApkInfo apkInfo = new ApkInfo();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "25");

        apkInfo.sdkInfo = sdkInfo;
        assertEquals("25", apkInfo.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkForShortHandSTag() {
        ApkInfo apkInfo = new ApkInfo();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "S");

        apkInfo.sdkInfo = sdkInfo;
        assertEquals("31", apkInfo.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkForShortHandSdkTag() {
        ApkInfo apkInfo = new ApkInfo();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "O");

        apkInfo.sdkInfo = sdkInfo;
        assertEquals("26", apkInfo.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkForSdkDevelopmentInsaneTestValue() {
        ApkInfo apkInfo = new ApkInfo();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "SDK_CUR_DEVELOPMENT");

        apkInfo.sdkInfo = sdkInfo;
        assertEquals("10000", apkInfo.checkTargetSdkVersionBounds());
    }
}

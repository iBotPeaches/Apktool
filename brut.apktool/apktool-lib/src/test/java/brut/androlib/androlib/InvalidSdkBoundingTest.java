/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib.androlib;

import brut.androlib.BaseTest;
import brut.androlib.res.AndrolibResources;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.*;

import static org.junit.Assert.assertEquals;

public class InvalidSdkBoundingTest extends BaseTest {

    @Test
    public void checkIfInvalidValuesPass() {
        AndrolibResources androlibResources = new AndrolibResources();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("minSdkVersion", "15");
        sdkInfo.put("targetSdkVersion", "25");
        sdkInfo.put("maxSdkVersion", "19");

        androlibResources.setSdkInfo(sdkInfo);
        assertEquals("19", androlibResources.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkIfMissingMinPasses() {
        AndrolibResources androlibResources = new AndrolibResources();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "25");
        sdkInfo.put("maxSdkVersion", "19");

        androlibResources.setSdkInfo(sdkInfo);
        assertEquals("19", androlibResources.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkIfMissingMaxPasses() {
        AndrolibResources androlibResources = new AndrolibResources();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("minSdkVersion", "15");
        sdkInfo.put("targetSdkVersion", "25");

        androlibResources.setSdkInfo(sdkInfo);
        assertEquals("25", androlibResources.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkIfMissingBothPasses() {
        AndrolibResources androlibResources = new AndrolibResources();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "25");

        androlibResources.setSdkInfo(sdkInfo);
        assertEquals("25", androlibResources.checkTargetSdkVersionBounds());
    }


    @Test
    public void checkForShortHandSdkTag() {
        AndrolibResources androlibResources = new AndrolibResources();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "O");

        androlibResources.setSdkInfo(sdkInfo);
        assertEquals("26", androlibResources.checkTargetSdkVersionBounds());
    }

    @Test
    public void checkForSdkQInsaneTestValue() {
        AndrolibResources androlibResources = new AndrolibResources();

        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("targetSdkVersion", "Q");

        androlibResources.setSdkInfo(sdkInfo);
        assertEquals("10000", androlibResources.checkTargetSdkVersionBounds());
    }
}
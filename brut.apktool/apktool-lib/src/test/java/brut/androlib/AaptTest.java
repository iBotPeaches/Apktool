/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
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
package brut.androlib;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class AaptTest {

    @Test
    public void isAaptInstalledTest() throws Exception {

        if (Boolean.parseBoolean(System.getenv("TRAVIS"))) {
            // skip aapt test on TRAVIS
            assertTrue(true);
        } else {
            assertEquals(true, isAaptPresent());
        }
    }

    private static boolean isAaptPresent() throws Exception {
        boolean result = true;
        try {
            Process proc = Runtime.getRuntime().exec("aapt");
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    proc.getErrorStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
            }
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }
}

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

import brut.androlib.res.xml.ResXmlEncoders;
import org.junit.Test;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class PositionalEnumerationTest {

    @Test
    public void noArgumentsTest() {
        assertEquals("test", enumerateArguments("test"));
    }

    @Test
    public void twoArgumentsTest() {
        assertEquals("%1$s, %2$s, and 1 other.", enumerateArguments("%s, %s, and 1 other."));
    }

    @Test
    public void twoPositionalArgumentsTest() {
        assertEquals("%1$s, %2$s and 1 other", enumerateArguments("%1$s, %2$s and 1 other"));
    }

    @Test
    public void threeArgumentsTest() {
        assertEquals("%1$s, %2$s, and %3$d other.", enumerateArguments("%s, %s, and %d other."));
    }

    @Test
    public void threePositionalArgumentsTest() {
        assertEquals(" %1$s, %2$s and %3$d other", enumerateArguments(" %1$s, %2$s and %3$d other"));
    }

    private String enumerateArguments(String value) {
        return ResXmlEncoders.enumerateNonPositionalSubstitutionsIfRequired(value);
    }

    private final static Logger LOGGER = Logger.getLogger(BuildAndDecodeJarTest.class.getName());
}
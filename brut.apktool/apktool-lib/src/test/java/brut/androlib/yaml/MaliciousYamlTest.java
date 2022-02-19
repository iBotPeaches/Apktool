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
package brut.androlib.yaml;

import brut.androlib.Androlib;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.options.BuildOptions;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.io.File;

public class MaliciousYamlTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();

        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestNewDir = new ExtFile(sTmpDir, "cve20220476");
        LOGGER.info("Unpacking cve20220476...");
        TestUtils.copyResourceDir(MaliciousYamlTest.class, "yaml/cve20220476/", sTestNewDir);
    }

    @Test(expected = ConstructorException.class)
    public void testMaliciousYamlNotLoaded() throws BrutException {
        BuildOptions buildOptions = new BuildOptions();
        File testApk = new File(sTmpDir, "cve20220476.apk");
        new Androlib(buildOptions).build(sTestNewDir, testApk);
    }
}

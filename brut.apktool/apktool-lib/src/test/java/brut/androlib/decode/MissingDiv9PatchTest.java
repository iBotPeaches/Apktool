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
package brut.androlib.decode;

import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.res.decoder.Res9patchStreamDecoder;
import brut.common.BrutException;
import brut.directory.ExtFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;

import org.junit.*;
import static org.junit.Assert.*;

public class MissingDiv9PatchTest extends BaseTest {
    private static final int NP_COLOR = 0xff000000;

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(MissingDiv9PatchTest.class, "decode/issue1522", sTmpDir);
    }

    @Test
    public void assertMissingDivAdded() throws Exception {
        File file = new File(sTmpDir, "pip_dismiss_scrim.9.png");
        byte[] data;

        try (InputStream in = Files.newInputStream(file.toPath())) {
            Res9patchStreamDecoder decoder = new Res9patchStreamDecoder();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            decoder.decode(in, out);
            data = out.toByteArray();
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        int height = image.getHeight() - 1;

        // First and last pixel will be invisible, so lets check the first column and ensure its all black
        for (int y = 1; y < height; y++) {
            assertEquals("y coordinate failed at: " + y, NP_COLOR, image.getRGB(0, y));
        }
    }
}

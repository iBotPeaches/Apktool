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

package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResFileDecoder {
    private final ResStreamDecoderContainer mDecoders;

    public ResFileDecoder(ResStreamDecoderContainer decoders) {
        this.mDecoders = decoders;
    }

    public void decode(Directory inDir, String inFileName, Directory outDir,
            String outResName) throws AndrolibException {
        String ext = "";
        int extPos = inFileName.lastIndexOf(".");
        if (extPos != -1) {
            ext = inFileName.substring(extPos);
        }

        if (inFileName.startsWith("raw/")) {
            decode(inDir, inFileName, outDir, outResName + ext, "raw");
            return;
        }
        if (inFileName.endsWith(".9.png")) {
            decode(inDir, inFileName, outDir, outResName + ".png", "raw");
            return;
        }
        if (inFileName.endsWith(".xml")) {
            decode(inDir, inFileName, outDir, outResName + ".xml", "xml");
            return;
        }
//        if (inFileName.endsWith(".html")) {
//            decode(inDir, inFileName, outDir, outResName + ".html", "xml");
//            return;
//        }

        decode(inDir, inFileName, outDir, outResName + ext, "raw");
    }

    public void decode(Directory inDir, String inFileName, Directory outDir,
            String outFileName, String decoder) throws AndrolibException {
        try {
            InputStream in = inDir.getFileInput(inFileName);
            OutputStream out = outDir.getFileOutput(outFileName);
            mDecoders.decode(in, out, decoder);
            in.close();
            out.close();
        } catch (AndrolibException ex) {
            LOGGER.log(Level.SEVERE, String.format(
                "Could not decode file \"%s\" to \"%s\"",
                inFileName, outFileName), ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, String.format(
                "Could not decode file \"%s\" to \"%s\"",
                inFileName, outFileName), ex);
        } catch (DirectoryException ex) {
            LOGGER.log(Level.SEVERE, String.format(
                "Could not decode file \"%s\" to \"%s\"",
                inFileName, outFileName), ex);
        }
    }

    private final static Logger LOGGER =
        Logger.getLogger(ResFileDecoder.class.getName());
}

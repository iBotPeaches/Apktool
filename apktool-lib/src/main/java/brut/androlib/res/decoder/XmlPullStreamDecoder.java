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

package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import java.io.*;
import java.util.logging.Logger;
import org.xmlpull.v1.*;
import org.xmlpull.v1.wrapper.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class XmlPullStreamDecoder implements ResStreamDecoder {
    public XmlPullStreamDecoder(XmlPullParser parser,
            XmlSerializer serializer) {
        this.mParser = parser;
        this.mSerial = serializer;
    }

    public void decode(InputStream in, OutputStream out)
            throws AndrolibException {
        try {
            XmlPullWrapperFactory factory = XmlPullWrapperFactory.newInstance();
            XmlPullParserWrapper par = factory.newPullParserWrapper(mParser);
            XmlSerializerWrapper ser = factory.newSerializerWrapper(mSerial);

            par.setInput(in, null);
            ser.setOutput(out, null);

            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                ser.event(par);
            }
            ser.flush();
            in.close();
            out.close();
        } catch (XmlPullParserException ex) {
            throw new AndrolibException("Could not decode XML", ex);
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode XML", ex);
        }
    }

    private final XmlPullParser mParser;
    private final XmlSerializer mSerial;
}

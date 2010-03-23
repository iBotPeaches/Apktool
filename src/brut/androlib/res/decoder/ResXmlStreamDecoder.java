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

import android.content.res.XmlBlock;
import brut.androlib.AndrolibException;
import java.io.*;
import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.*;
import org.xmlpull.v1.wrapper.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResXmlStreamDecoder implements ResStreamDecoder {
    private final ResXmlSerializer mSerializer;

    public ResXmlStreamDecoder(ResXmlSerializer serializer) {
        this.mSerializer = serializer;
    }

    public void decode(InputStream in, OutputStream out)
            throws AndrolibException {
        try {
            XmlPullParserWrapper par =
                getResXmlParserWrapper(in);
            XmlSerializerWrapper ser =
                getXmlWrapperFactory().newSerializerWrapper(mSerializer);
            ser.setOutput(out, null);
//            mSerializer.setDecodingEnabled(true);

            while (par.nextToken() != XmlPullParser.END_DOCUMENT) {
                ser.event(par);
            }
            ser.flush();
        } catch (IOException ex) {
            throw new AndrolibException("could not decode XML stream", ex);
        } catch (IllegalArgumentException ex) {
            throw new AndrolibException("could not decode XML stream", ex);
        } catch (IllegalStateException ex) {
            throw new AndrolibException("could not decode XML stream", ex);
        } catch (XmlPullParserException ex) {
            throw new AndrolibException("could not decode XML stream", ex);
        }
    }

    private XmlPullParserWrapper getResXmlParserWrapper(InputStream in)
            throws IOException, XmlPullParserException {
        XmlBlock xml = new XmlBlock(copyStreamToByteArray(in));
        XmlPullParser parser = xml.newParser();
        return getXmlWrapperFactory().newPullParserWrapper(parser);
    }

    private XmlPullWrapperFactory getXmlWrapperFactory()
            throws XmlPullParserException {
        return XmlPullWrapperFactory.newInstance();
    }

    private byte[] copyStreamToByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        return out.toByteArray();
    }

}

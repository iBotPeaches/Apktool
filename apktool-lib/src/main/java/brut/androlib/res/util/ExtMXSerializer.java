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

package brut.androlib.res.util;

import java.io.IOException;
import java.io.OutputStream;
import org.xmlpull.mxp1_serializer.MXSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ExtMXSerializer extends MXSerializer {
    @Override
    public void startDocument(String encoding, Boolean standalone) throws
            IOException, IllegalArgumentException, IllegalStateException {
        super.startDocument(encoding != null ? encoding : mDefaultEncoding,
            standalone);
        super.out.write(lineSeparator);
    }

    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException {
        super.setOutput(os, encoding != null ? encoding : mDefaultEncoding);
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (PROPERTY_DEFAULT_ENCODING.equals(name)) {
            return mDefaultEncoding;
        }
        return super.getProperty(name);
    }

    @Override
    public void setProperty(String name, Object value)
            throws IllegalArgumentException, IllegalStateException {
        if (PROPERTY_DEFAULT_ENCODING.equals(name)) {
            mDefaultEncoding = (String) value;
        } else {
            super.setProperty(name, value);
        }
    }

    public final String EXT_PROPERTY_SERIALIZER_INDENTATION =
            PROPERTY_SERIALIZER_INDENTATION;
    public final String EXT_PROPERTY_SERIALIZER_LINE_SEPARATOR =
            PROPERTY_SERIALIZER_LINE_SEPARATOR;

    public final static String PROPERTY_DEFAULT_ENCODING = "DEFAULT_ENCODING";

    private String mDefaultEncoding;
}

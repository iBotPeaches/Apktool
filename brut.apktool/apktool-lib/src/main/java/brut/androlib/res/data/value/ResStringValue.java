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

package brut.androlib.res.data.value;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.xml.ResXmlEncoders;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResStringValue extends ResScalarValue {

    public ResStringValue(String value, int rawValue) {
        this(value, rawValue, "string");
    }

    public ResStringValue(String value, int rawValue, String type) {
        super(type, rawValue, value);
    }

    @Override
    public String encodeAsResXmlAttr() {
        return makeSureItCantBeMistakenForAnInt(ResXmlEncoders.encodeAsResXmlAttr(mRawValue));
    }

    private static Pattern allDigits = Pattern.compile("\\d+");

    // If a meta-data value consists of digits only, then it is considered an int.
    // For instance, if the value of "answer" is "42", then the corresponding
    // BaseBundle.getString("answer") returns null instead of "42" (while getInt()
    // returns 42).
    // This hack, of prefixing the value with "\\x3", makes sure that the value is
    // considered a string nonetheless.
    //
    // See: http://stackoverflow.com/questions/2154945/how-to-force-a-meta-data-value-to-type-string
    // and  http://developer.android.com/guide/topics/manifest/meta-data-element.html
    private String makeSureItCantBeMistakenForAnInt(String v) {
        return allDigits.matcher(v).matches() ?  "\\x3" + v : v;
    }

    @Override
    public String encodeAsResXmlItemValue() {
        return ResXmlEncoders
                .enumerateNonPositionalSubstitutionsIfRequired(ResXmlEncoders
                        .encodeAsXmlValue(mRawValue));
    }

    @Override
    public String encodeAsResXmlValue() {
        return ResXmlEncoders.encodeAsXmlValue(mRawValue);
    }

    @Override
    protected String encodeAsResXml() throws AndrolibException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void serializeExtraXmlAttrs(XmlSerializer serializer,
                                          ResResource res) throws IOException {
        if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(mRawValue)) {
            serializer.attribute(null, "formatted", "false");
        }
    }
}

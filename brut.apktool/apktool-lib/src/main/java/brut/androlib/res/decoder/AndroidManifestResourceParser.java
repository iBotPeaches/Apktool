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
package brut.androlib.res.decoder;

import android.util.TypedValue;
import brut.androlib.res.data.ResTable;

import java.util.regex.Pattern;

/**
 * AXmlResourceParser specifically for parsing encoded AndroidManifest.xml.
 */
public class AndroidManifestResourceParser extends AXmlResourceParser {
    /**
     * Pattern for matching numeric string meta-data values. aapt automatically infers the
     * type for a manifest meta-data value based on the string in the unencoded XML. However,
     * some apps intentionally coerce integers to be strings by prepending an escaped space.
     * For details/discussion, see https://stackoverflow.com/questions/2154945/how-to-force-a-meta-data-value-to-type-string
     * With aapt1, the escaped space is dropped when encoded. For aapt2, the escaped space is preserved.
     */
    private static final Pattern PATTERN_NUMERIC_STRING = Pattern.compile("\\s?\\d+");

    public AndroidManifestResourceParser(ResTable resTable) {
        super(resTable);
    }

    @Override
    public String getAttributeValue(int index) {
        String value = super.getAttributeValue(index);
        if (value == null) {
            return "";
        }

        if (!isNumericStringMetadataAttributeValue(index, value)) {
            return value;
        }

        // Patch the numeric string value by prefixing it with an escaped space.
        // Otherwise, when the decoded app is rebuilt, aapt will incorrectly encode
        // the value as an int or float (depending on aapt version), breaking the original
        // app functionality.
        return "\\ " + value.trim();
    }

    private boolean isNumericStringMetadataAttributeValue(int index, String value) {
        return "meta-data".equals(super.getName())
                && "value".equals(super.getAttributeName(index))
                && super.getAttributeValueType(index) == TypedValue.TYPE_STRING
                && PATTERN_NUMERIC_STRING.matcher(value).matches();
    }
}
